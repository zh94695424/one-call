package com.onecall.archtests.container;

import static org.assertj.core.api.Assertions.*;
import com.onecall.core.sharing.internal.admission.*;
import com.onecall.core.sharing.internal.admission.ShareAdmissionRepository.*;
import com.onecall.core.sharing.internal.publicadmission.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.jooq.*;
import org.jooq.impl.*;
import org.jooq.conf.Settings;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.datasource.*;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.*;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import com.onecall.core.customer_iam.internal.web.*;
import com.onecall.core.customer_iam.internal.security.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class PublicShareAdmissionIT {
    private static final SharingQueryFixture BASE = new SharingQueryFixture();
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private Fixture fixture;
    @BeforeAll static void start() throws Exception { BASE.start(); }
    @AfterAll static void stop() throws Exception { BASE.close(); }
    @BeforeEach void seed() throws Exception { BASE.reset(); fixture = new Fixture("NONE", null); }

    @Test void postAdmissionUsesExactRouteAndClosedBody() throws Exception {
        var beans = new StaticListableBeanFactory(); beans.addBean("shareAdmissionService", fixture.service);
        var writer = new CustomerProblemWriter();
        var controller = new PublicSharesController(beans.getBeanProvider(com.onecall.core.sharing.internal.publicquery.PublicShareQueryService.class), beans.getBeanProvider(com.onecall.core.customer_iam.internal.application.CustomerContextService.class), writer, beans.getBeanProvider(ShareAdmissionService.class));
        var mvc = org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(controller)
                .addFilters(new CustomerBoundaryFilter(new CustomerBearerAuthenticator(beans.getBeanProvider(CustomerSessionVerifier.class)),writer)).build();
        String path="/api/v1/public/shares/"+fixture.token+"/admissions";
        String body="{\"admissionRequestId\":\"http-key\",\"verification\":{\"mode\":\"NONE\"}}";
        for (int attempt=0;attempt<2;attempt++) assertThat(mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(path).servletPath(path).contentType("application/json").content(body)).andReturn().getResponse().getStatus()).isEqualTo(201);
        for(String invalid:List.of(body+"{}",body.replace("\"NONE\"","\"NONE\",\"code\":\"private\""),"{}"))
            assertThat(mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(path).servletPath(path).contentType("application/json").content(invalid)).andReturn().getResponse().getStatus()).isEqualTo(422);
        assertThat(mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(path).servletPath(path).contentType("text/plain").content(body)).andReturn().getResponse().getStatus()).isEqualTo(422);
        assertThat(mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(path).servletPath(path).header("Authorization","bad").content("{}")).andReturn().getResponse().getStatus()).isEqualTo(401);
        assertThat(fixture.count("sharing.share_admission")).isEqualTo(1);
    }
    @Test void unknownTokenIsUniformAndDoesNotLeakDigestOrGrant() {
        var result=fixture.service.admit(new ShareAdmissionCapabilityToken("unknown-private"),request("key","NONE"),Optional.empty());
        denied(result,ShareAdmissionException.Code.SHARE_TOKEN_INVALID);
        assertThat(result.toString()).doesNotContain("unknown-private",fixture.id.toString());
    }
    @Test void noneAdmissionCommitsOneRowEventCountAndRevision() throws Exception {
        fixture.afterCommit=() -> fixture.mutate("LOCKED");
        var view=view(fixture.admit("key"));
        assertThat(view.verificationResult()).isEqualTo("NOT_REQUIRED");
        assertThat(fixture.count("sharing.share_admission")).isEqualTo(1);
        assertThat(fixture.scalar("SELECT admissions_used FROM sharing.evidence_share_grant WHERE id=?",fixture.id)).isEqualTo(1);
        assertThat(fixture.scalar("SELECT revision FROM sharing.evidence_share_grant WHERE id=?",fixture.id)).isEqualTo(3);
        var event=fixture.event("ADMISSION_GRANTED");
        assertThat(event.eventId()).isEqualTo("admission-granted:"+fixture.id+":"+view.admissionId());
        assertThat(event.payload().propertyNames()).containsExactlyInAnyOrder("verificationMode","revocationGeneration","codeGeneration","grantExpiresAt","admissionExpiresAt","sessionExpiresAt");
        assertThat(event.payloadSha256()).isEqualTo(digest(canonical(event.payload())));
        assertThat(event.result()).isEqualTo("SUCCESS");
        assertThat(view.admittedAt()).isEqualTo(fixture.row().admittedAt());
        assertThat(fixture.admit("key")).isInstanceOf(ShareAdmissionService.ReplayCommitted.class);
    }
    @Test void extractionCodeSuccessAndFailureAreRedacted() throws Exception {
        fixture=new Fixture("EXTRACTION_CODE",null); fixture.codeResult=new ShareAdmissionCodeVerifier.Denied("WRONG_CODE");
        denied(fixture.admit("request-private"),ShareAdmissionException.Code.SHARE_VERIFICATION_FAILED);
        var event=fixture.event("VERIFICATION_FAILED");
        assertThat(event.payload().propertyNames()).containsExactlyInAnyOrder("shareGrantId","eventType","result","reasonCode","requestKeyDigest","verificationMode");
        assertThat(event.payload().path("requestKeyDigest").asString()).isEqualTo(digest("request-private"));
        assertThat(event.payloadSha256()).isEqualTo(digest(canonical(event.payload())));
        assertThat(event.payload().toString()).doesNotContain("request-private","code-private");
        denied(fixture.admit("request-private"),ShareAdmissionException.Code.SHARE_VERIFICATION_FAILED);
        assertThat(fixture.eventCount("VERIFICATION_FAILED")).isEqualTo(1);
        fixture.codeResult=new ShareAdmissionCodeVerifier.Verified();
        assertThat(view(fixture.admit("request-private")).verificationResult()).isEqualTo("VERIFIED");
        assertThat(fixture.row().admissionKey()).hasSize(74).doesNotContain("request-private");
    }
    @Test void designatedMobileRequiresTrustedProofProvider() throws Exception {
        fixture=new Fixture("DESIGNATED_MOBILE",null); fixture.mobileResult=new DesignatedMobileProofVerifier.Unavailable();
        denied(fixture.admit("key"),ShareAdmissionException.Code.DEPENDENCY_UNAVAILABLE);
        assertThat(fixture.count("sharing.share_admission")).isZero();
        fixture.mobileResult=new DesignatedMobileProofVerifier.Denied(null);
        denied(fixture.admit("key"),ShareAdmissionException.Code.SHARE_VERIFICATION_FAILED);
        assertThat(fixture.event("VERIFICATION_FAILED").payload().path("reasonCode").isNull()).isTrue();
        fixture.mobileResult=new DesignatedMobileProofVerifier.Verified(); assertThat(view(fixture.admit("key")).verificationResult()).isEqualTo("VERIFIED");
    }
    @Test void organizationMemberUsesCurrentAccountOrganizationMembership() throws Exception {
        fixture=new Fixture("ORGANIZATION_MEMBER",null);
        var view=view(fixture.admit("key")); var witness=fixture.event("ADMISSION_GRANTED").payload();
        assertThat(witness.propertyNames()).hasSize(8);
        assertThat(witness.path("recipientAccountId").asString()).isEqualTo(BASE.account.toString());
        assertThat(witness.path("recipientOrganizationId").asString()).isEqualTo(BASE.org.toString());
        assertThat(view.verificationResult()).isEqualTo("VERIFIED");
        assertThat(fixture.admit("key")).isInstanceOf(ShareAdmissionService.ReplayCommitted.class);
    }
    @Test void wrongTenantAccountOrMembershipFailsClosed() throws Exception {
        fixture=new Fixture("ORGANIZATION_MEMBER",null); fixture.admit("key");
        fixture.caller=Optional.of(new TrustedShareAdmissionCaller(BASE.other,BASE.org));
        denied(fixture.admit("key"),ShareAdmissionException.Code.SHARE_WRONG_TENANT);
        fixture.caller=Optional.of(new TrustedShareAdmissionCaller(BASE.account,BASE.org2));
        denied(fixture.admit("key"),ShareAdmissionException.Code.SHARE_WRONG_TENANT);
        fixture.caller=Optional.empty(); denied(fixture.admit("key"),ShareAdmissionException.Code.UNAUTHENTICATED);
        fixture.caller=Optional.of(new TrustedShareAdmissionCaller(BASE.account,BASE.org));
        fixture.sql("UPDATE organization_access.organization_membership SET status='REVOKED',revision=revision+1 WHERE id=?",BASE.membership);
        denied(fixture.admit("key"),ShareAdmissionException.Code.FORBIDDEN);
    }
    @Test void openRiskBlocksWithoutAdmissionOrCount() throws Exception {
        fixture=new Fixture("NONE",1); fixture.openRisk();
        denied(fixture.admit("key"),ShareAdmissionException.Code.SHARE_RISK_BLOCKED);
        assertThat(fixture.count("sharing.share_admission")).isZero();
        assertThat(fixture.scalar("SELECT admissions_used FROM sharing.evidence_share_grant WHERE id=?",fixture.id)).isZero();
    }
    @Test void revokedExpiredLockedAndStaleGenerationMapExactly() throws Exception {
        fixture.sql("UPDATE sharing.evidence_share_grant SET status='REVOKED',revocation_generation=revocation_generation+1,revision=revision+1 WHERE id=?",fixture.id);
        denied(fixture.admit("key"),ShareAdmissionException.Code.SHARE_REVOKED);
        fixture=new Fixture("NONE",null); fixture.sql("UPDATE sharing.evidence_share_grant SET status='LOCKED',revision=revision+1 WHERE id=?",fixture.id);
        denied(fixture.admit("key"),ShareAdmissionException.Code.SHARE_LOCKED);
        fixture=new Fixture("NONE",null,Duration.ofSeconds(2)); Thread.sleep(2200);
        denied(fixture.admit("key"),ShareAdmissionException.Code.SHARE_EXPIRED);
        var event=fixture.event("EXPIRED"); assertThat(event.eventId()).startsWith("share-transition:");
        assertThat(event.payload().path("revocationGeneration").asInt()).isEqualTo(1);
        assertThat(event.result()).isEqualTo("DENIED"); assertThat(event.payloadSha256()).isEqualTo(digest(canonical(event.payload())));
        denied(fixture.admit("key"),ShareAdmissionException.Code.SHARE_EXPIRED); assertThat(fixture.eventCount("EXPIRED")).isEqualTo(1);
        for (String terminal : List.of("REVOKED", "EXPIRED")) {
            fixture=new Fixture("NONE",null,terminal.equals("EXPIRED") ? Duration.ofSeconds(2) : Duration.ofDays(1)); fixture.afterCommit=() -> fixture.mutate(terminal);
            denied(fixture.admit("terminal"), terminal.equals("REVOKED")?ShareAdmissionException.Code.SHARE_REVOKED:ShareAdmissionException.Code.SHARE_EXPIRED);
            assertThat(fixture.count("sharing.share_admission")).isEqualTo(1);
            assertThat(fixture.eventCount("TICKET_FAILED")).isZero(); assertThat(fixture.eventCount("TICKET_ISSUED")).isZero();
        }
    }
    @Test void limitReachedLeavesStateAndLastSessionUnchanged() throws Exception {
        fixture=new Fixture("NONE",1); var first=view(fixture.admit("first"));
        denied(fixture.admit("second"),ShareAdmissionException.Code.SHARE_ADMISSION_LIMIT_REACHED);
        assertThat(view(fixture.admit("first")).sessionExpiresAt()).isEqualTo(first.sessionExpiresAt());
        assertThat(fixture.count("sharing.share_admission")).isEqualTo(1);
    }
    @Test void sameKeyReplayPrecedesLimitAndDoesNotRecount() throws Exception {
        fixture=new Fixture("EXTRACTION_CODE",1); var first=view(fixture.admit("key")); int proofs=fixture.proofs.get();
        fixture.codeResult=new ShareAdmissionCodeVerifier.Unavailable(); fixture.policyCalls.set(0);
        var replay=view(fixture.admit("key")); assertThat(replay.admissionId()).isEqualTo(first.admissionId());
        assertThat(replay.sessionExpiresAt()).isEqualTo(first.sessionExpiresAt()); assertThat(fixture.proofs).hasValue(proofs);
        assertThat(fixture.policyCalls).hasValue(1); assertThat(fixture.eventCount("ADMISSION_GRANTED")).isEqualTo(1);
    }
    @Test void sameKeyReplayRequiresMatchingGenerationWitness() throws Exception {
        fixture=new Fixture("EXTRACTION_CODE",null); fixture.admit("key"); fixture.policyCalls.set(0);
        fixture.sql("UPDATE sharing.evidence_share_grant SET code_salt=repeat('T',22),code_hash=repeat('c',64),code_generation=code_generation+1,revocation_generation=revocation_generation+1,revision=revision+1 WHERE id=?",fixture.id);
        denied(fixture.admit("key"),ShareAdmissionException.Code.SHARE_GENERATION_STALE); assertThat(fixture.policyCalls).hasValue(0);
        assertThat(fixture.count("sharing.share_admission")).isEqualTo(1);
    }
    @Test void sessionExpiryReturnsGoneWithoutRenewal() throws Exception {
        fixture.policy=new ShareAdmissionPolicyProvider.ApprovedTicketPolicy(Duration.ofMillis(200),Duration.ofMillis(100));
        fixture.admit("key"); Thread.sleep(250); fixture.policyCalls.set(0);
        denied(fixture.admit("key"),ShareAdmissionException.Code.SHARE_SESSION_EXPIRED); assertThat(fixture.policyCalls).hasValue(0);
        assertThat(fixture.count("sharing.share_admission")).isEqualTo(1);
    }
    @Test void ticketPolicyUnavailableBeforeAdmissionLeavesNoRows() throws Exception {
        fixture.policy=new ShareAdmissionPolicyProvider.Unavailable(); denied(fixture.admit("key"),ShareAdmissionException.Code.SHARE_SESSION_POLICY_UNAVAILABLE);
        assertThat(fixture.count("sharing.share_admission")).isZero(); assertThat(fixture.count("sharing.share_access_event")).isZero();
        fixture.policy=new ShareAdmissionPolicyProvider.ApprovedTicketPolicy(Duration.ZERO,Duration.ofSeconds(10));
        denied(fixture.admit("key"),ShareAdmissionException.Code.SHARE_SESSION_POLICY_UNAVAILABLE);
        fixture.policy=new ShareAdmissionPolicyProvider.ApprovedTicketPolicy(Duration.ofSeconds(120),Duration.ofSeconds(10));
        fixture.admit("key"); var original=fixture.event("ADMISSION_GRANTED"); long events=fixture.count("sharing.share_access_event");
        fixture.policyCalls.set(0); fixture.policy=new ShareAdmissionPolicyProvider.Unavailable();
        denied(fixture.admit("key"),ShareAdmissionException.Code.SHARE_SESSION_POLICY_UNAVAILABLE);
        assertThat(fixture.policyCalls).hasValue(1); assertThat(fixture.event("ADMISSION_GRANTED")).isEqualTo(original);
        assertThat(fixture.count("sharing.share_access_event")).isEqualTo(events); assertThat(fixture.eventCount("TICKET_FAILED")).isZero();
        fixture.policy=new ShareAdmissionPolicyProvider.ApprovedTicketPolicy(Duration.ofSeconds(999),null); fixture.policyCalls.set(0);
        denied(fixture.admit("key"),ShareAdmissionException.Code.SHARE_SESSION_POLICY_UNAVAILABLE); assertThat(fixture.policyCalls).hasValue(1);
        fixture.openRisk(); fixture.policyCalls.set(0);
        denied(fixture.admit("key"),ShareAdmissionException.Code.SHARE_RISK_BLOCKED); assertThat(fixture.policyCalls).hasValue(0);
    }
    @Test void redisUnavailableAfterCommitReturns503AndPersistsAdmission() throws Exception {
        fixture.ticket=new ShareAdmissionTicketProvider.Unavailable(); denied(fixture.admit("key"),ShareAdmissionException.Code.DEPENDENCY_UNAVAILABLE);
        assertThat(fixture.count("sharing.share_admission")).isEqualTo(1); assertThat(fixture.eventCount("TICKET_FAILED")).isEqualTo(1);
        fixture.ticket=new ShareAdmissionTicketProvider.Written(); assertThat(fixture.admit("key")).isInstanceOf(ShareAdmissionService.ReplayCommitted.class);
    }
    @Test void ticketFailureEventIsDeterministicRedactedAndSingle() throws Exception {
        fixture.ticket=new ShareAdmissionTicketProvider.Unavailable(); fixture.admit("key"); fixture.admit("key");
        assertThat(fixture.eventCount("TICKET_FAILED")).isEqualTo(1); var event=fixture.event("TICKET_FAILED");
        assertThat(event.payload().propertyNames()).containsExactlyInAnyOrder("shareGrantId","admissionId","admissionKeyDigest","reasonCode");
        assertThat(event.payload().path("reasonCode").asString()).isEqualTo("TICKET_WRITE_UNAVAILABLE");
        assertThat(event.payloadSha256()).isEqualTo(digest(canonical(event.payload())));
        fixture.ticket=new ShareAdmissionTicketProvider.Malformed(); fixture.allowTicketFailureEventConflict=true;
        try { fixture.admit("key"); } finally { fixture.allowTicketFailureEventConflict=false; }
        assertThat(fixture.eventCount("TICKET_FAILED")).isEqualTo(1);
        assertThat(fixture.event("TICKET_FAILED")).isEqualTo(event);
    }
    @Test void lostResponseRetryReconstructsWithoutSecondAdmission() throws Exception {
        var first=view(fixture.admit("key")); var witness=fixture.event("ADMISSION_GRANTED");
        fixture.sql("UPDATE sharing.evidence_share_grant SET expires_at=expires_at+interval '1 day',revision=revision+1 WHERE id=?",fixture.id);
        for(long seconds:List.of(2L,45L)) {
            fixture.policyCalls.set(0); fixture.policy=new ShareAdmissionPolicyProvider.ApprovedTicketPolicy(Duration.ofDays(1),Duration.ofSeconds(seconds));
            var replay=view(fixture.admit("key")); assertThat(fixture.policyCalls).hasValue(1);
            assertThat(replay.admissionId()).isEqualTo(first.admissionId()); assertThat(replay.sessionExpiresAt()).isEqualTo(first.sessionExpiresAt());
            assertThat(replay.mediaAuthorization().expiresAt()).isBeforeOrEqualTo(first.sessionExpiresAt());
            assertThat(fixture.event("ADMISSION_GRANTED")).isEqualTo(witness);
            assertThat(replay.admissionExpiresAt()).isEqualTo(first.admissionExpiresAt());
            assertThat(replay.mediaAuthorization().expiresAt()).isBeforeOrEqualTo(first.admissionExpiresAt());
            assertThat(Duration.between(Instant.now(),replay.mediaAuthorization().expiresAt()).toMillis()).isBetween(1L,seconds*1000L);
        }
        assertThat(fixture.count("sharing.share_admission")).isEqualTo(1); assertThat(fixture.eventCount("TICKET_ISSUED")).isEqualTo(1);
    }
    @Test void malformedTicketMaterialFailsClosedWithoutForgedSuccess() throws Exception {
        fixture.badCredential=true; denied(fixture.admit("key"),ShareAdmissionException.Code.DEPENDENCY_UNAVAILABLE);
        assertThat(fixture.eventCount("TICKET_ISSUED")).isZero(); assertThat(fixture.eventCount("TICKET_FAILED")).isEqualTo(1);
    }
    @Test void differentKeysAtLastSlotHaveOneWinner() throws Exception {
        fixture=new Fixture("NONE",1); var outcomes=race("first","second");
        assertThat(outcomes).filteredOn(outcome -> outcome instanceof ShareAdmissionService.AdmissionCommitted).hasSize(1);
        assertThat(outcomes).contains(new ShareAdmissionService.DeniedCommitted(ShareAdmissionException.Code.SHARE_ADMISSION_LIMIT_REACHED));
        assertThat(fixture.count("sharing.share_admission")).isEqualTo(1);
    }
    @Test void duplicateKeysConcurrentlyCreateOneAdmission() throws Exception {
        fixture=new Fixture("EXTRACTION_CODE",1);
        var outcomes=race("same","same"); assertThat(outcomes).filteredOn(outcome -> outcome instanceof ShareAdmissionService.AdmissionCommitted).hasSize(1);
        assertThat(outcomes).filteredOn(outcome -> outcome instanceof ShareAdmissionService.ReplayCommitted).hasSize(1);
        assertThat(fixture.count("sharing.share_admission")).isEqualTo(1); assertThat(fixture.eventCount("ADMISSION_GRANTED")).isEqualTo(1);
        assertThat(fixture.policyCalls).hasValue(3); assertThat(fixture.proofs).hasValue(2); assertThat(fixture.backendPids.size()).isGreaterThanOrEqualTo(2);
    }
    @Test void admissionRaceWithRevokeOrRotateObservesCommittedWinner() throws Exception {
        for(String mutation:List.of("REVOKED","ROTATE","EXPIRED","LOCKED","RISK","MEMBER")) {
            fixture=new Fixture(mutation.equals("ROTATE")?"EXTRACTION_CODE":mutation.equals("MEMBER")?"ORGANIZATION_MEMBER":"NONE",null,
                    mutation.equals("EXPIRED") ? Duration.ofSeconds(2) : Duration.ofDays(1));
            try(Connection connection=BASE.admin()) {
                connection.setAutoCommit(false);
                SharingQueryFixture.execute(connection,"SELECT id FROM sharing.evidence_share_grant WHERE id=? FOR UPDATE",fixture.id);
                try(var executor=Executors.newSingleThreadExecutor()) {
                    fixture.policyEntered=new CountDownLatch(1); fixture.policyRelease=new CountDownLatch(1);
                    Future<ShareAdmissionService.AdmissionOutcome> result=executor.submit(() -> fixture.admit("race"));
                    assertThat(fixture.policyEntered.await(10,TimeUnit.SECONDS)).isTrue();
                    if(mutation.equals("ROTATE")) SharingQueryFixture.execute(connection,"UPDATE sharing.evidence_share_grant SET code_salt=repeat('T',22),code_hash=repeat('c',64),code_generation=code_generation+1,revocation_generation=revocation_generation+1,revision=revision+1 WHERE id=?",fixture.id);
                    else if(mutation.equals("RISK")) SharingQueryFixture.execute(connection,"INSERT INTO subject_number.subject_risk_state(id,caller_number_binding_id,risk_type,state,source) VALUES (?,?,'RECYCLED','OPEN','OPERATIONS')",UUID.randomUUID(),fixture.binding);
                    else if(mutation.equals("MEMBER")) SharingQueryFixture.execute(connection,"UPDATE organization_access.organization_membership SET status='REVOKED',revision=revision+1 WHERE id=?",BASE.membership);
                    else if(mutation.equals("EXPIRED")) { Thread.sleep(2200); SharingQueryFixture.execute(connection,"UPDATE sharing.evidence_share_grant SET status='EXPIRED',revocation_generation=revocation_generation+1,revision=revision+1 WHERE id=?",fixture.id); }
                    else SharingQueryFixture.execute(connection,"UPDATE sharing.evidence_share_grant SET status=?,revocation_generation=revocation_generation+?,revision=revision+1 WHERE id=?",mutation,mutation.equals("REVOKED")?1:0,fixture.id);
                    connection.commit(); fixture.policyRelease.countDown();
                    var outcome=result.get(10,TimeUnit.SECONDS);
                    if(mutation.equals("ROTATE")) denied(outcome,ShareAdmissionException.Code.SHARE_GENERATION_STALE);
                    else denied(outcome,switch(mutation) {
                        case "REVOKED" -> ShareAdmissionException.Code.SHARE_REVOKED;
                        case "EXPIRED" -> ShareAdmissionException.Code.SHARE_EXPIRED;
                        case "LOCKED" -> ShareAdmissionException.Code.SHARE_LOCKED;
                        case "RISK" -> ShareAdmissionException.Code.SHARE_RISK_BLOCKED;
                        default -> ShareAdmissionException.Code.FORBIDDEN;
                    });
                }
            }
            assertThat(fixture.count("sharing.share_admission")).isZero();
        }
    }
    @Test void failureAtAdmissionInsertEventOrCountRollsBackAllEffects() throws Exception {
        for(String point:List.of("insertAdmission","insertAccessEvent","updateGrantAdmissionCount")) {
            fixture.failAt=point; denied(fixture.admit(point),ShareAdmissionException.Code.DEPENDENCY_UNAVAILABLE);
            assertThat(fixture.count("sharing.share_admission")).isZero(); assertThat(fixture.count("sharing.share_access_event")).isZero();
            assertThat(fixture.scalar("SELECT admissions_used FROM sharing.evidence_share_grant WHERE id=?",fixture.id)).isZero();
            assertThat(fixture.scalar("SELECT revision FROM sharing.evidence_share_grant WHERE id=?",fixture.id)).isEqualTo(1);
        }
    }
    @Test void postAdmissionResponseNeverContainsForbiddenIdentifiersOrSecrets() {
        var view=view(fixture.admit("request-private")); var json=JSON.valueToTree(view);
        assertThat(json.propertyNames()).containsExactlyInAnyOrder("admissionId","verificationMode","verificationResult","admittedAt","sessionExpiresAt","admissionExpiresAt","mediaAuthorization");
        assertThat(json.toString()).doesNotContain(fixture.id.toString(),"token-private","request-private","code-private","proof-private","codeGeneration","revocationGeneration","share-ticket:");
        assertThat(view.toString()).isEqualTo("[REDACTED]"); assertThat(view.mediaAuthorization().toString()).isEqualTo("[REDACTED]");
    }
    @Test void publicGetAndCreatorCreateRevokeRotateListMeRemainUnchanged() throws Exception {
        assertThat(fixture.count("sharing.share_admission")).isZero();
        assertThat(BASE.context.getBeansOfType(com.onecall.core.sharing.internal.application.CustomerShareQueryService.class)).hasSize(1);
        assertThat(BASE.flyway.info().applied()).hasSize(12);
        assertThat(com.onecall.core.customer_iam.internal.web.PublicSharesController.class.getMethod("getPublicShare",String.class,java.security.Principal.class)
                .getAnnotation(org.springframework.web.bind.annotation.GetMapping.class).path()).containsExactly("/api/v1/public/shares/{capabilityToken}");
    }

    private List<ShareAdmissionService.AdmissionOutcome> race(String first,String second) throws Exception {
        var barrier=new CyclicBarrier(2); fixture.proofBarrier=barrier;
        try(var executor=Executors.newFixedThreadPool(2)) {
            var left=executor.submit(() -> fixture.admit(first)); var right=executor.submit(() -> fixture.admit(second));
            return List.of(left.get(20,TimeUnit.SECONDS),right.get(20,TimeUnit.SECONDS));
        }
    }
    private static ShareAdmissionRequest request(String key,String mode) {
        return new ShareAdmissionRequest(key,switch(mode) {
            case "EXTRACTION_CODE" -> new ShareAdmissionRequest.Verification.ExtractionCode("code-private");
            case "DESIGNATED_MOBILE" -> new ShareAdmissionRequest.Verification.DesignatedMobile("proof-private");
            case "ORGANIZATION_MEMBER" -> new ShareAdmissionRequest.Verification.OrganizationMember();
            default -> new ShareAdmissionRequest.Verification.None();
        });
    }
    private static ShareAdmissionView view(ShareAdmissionService.AdmissionOutcome outcome) {
        if(outcome instanceof ShareAdmissionService.AdmissionCommitted committed) return committed.view();
        assertThat(outcome).isInstanceOf(ShareAdmissionService.ReplayCommitted.class); return ((ShareAdmissionService.ReplayCommitted)outcome).view();
    }
    private static void denied(ShareAdmissionService.AdmissionOutcome outcome,ShareAdmissionException.Code code) { assertThat(outcome).isEqualTo(new ShareAdmissionService.DeniedCommitted(code)); }
    private static String digest(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch(Exception failure) { throw new AssertionError("Synthetic SHA unavailable"); }
    }
    private static String canonical(JsonNode payload) {
        Map<String,Object> sorted=new TreeMap<>(); payload.properties().forEach(entry -> sorted.put(entry.getKey(),entry.getValue().isObject()?JSON.readTree(canonical(entry.getValue())):entry.getValue()));
        return JSON.writeValueAsString(sorted);
    }
    private static final class Fixture {
        final UUID id,subject,binding=UUID.randomUUID();
        final String mode,token="token-private-"+UUID.randomUUID();
        final AtomicInteger proofs=new AtomicInteger(),policyCalls=new AtomicInteger();
        final ShareAdmissionRepository repository;
        final DefaultShareAdmissionService service;
        Optional<TrustedShareAdmissionCaller> caller=Optional.empty();
        volatile ShareAdmissionPolicyProvider.Result policy=new ShareAdmissionPolicyProvider.ApprovedTicketPolicy(Duration.ofSeconds(120),Duration.ofSeconds(30));
        volatile ShareAdmissionCodeVerifier.Result codeResult=new ShareAdmissionCodeVerifier.Verified();
        volatile DesignatedMobileProofVerifier.Result mobileResult=new DesignatedMobileProofVerifier.Verified();
        volatile ShareAdmissionTicketProvider.Result ticket=new ShareAdmissionTicketProvider.Written();
        volatile boolean badCredential,allowTicketFailureEventConflict; volatile String failAt; volatile CyclicBarrier proofBarrier; volatile Runnable afterCommit; volatile CountDownLatch policyEntered,policyRelease;
        final Set<Integer> backendPids=ConcurrentHashMap.newKeySet();
        Fixture(String mode,Integer maximum) throws Exception { this(mode,maximum,Duration.ofDays(1)); }
        Fixture(String mode,Integer maximum,Duration grantLifetime) throws Exception {
            this.mode=mode;
            try(Connection connection=BASE.admin()) {
                try(var statement=connection.prepareStatement("SELECT subject_id FROM evidence.evidence_record WHERE id=?")) { statement.setObject(1,BASE.evidence); try(var rows=statement.executeQuery()) { rows.next(); subject=rows.getObject(1,UUID.class); } }
                SharingQueryFixture.execute(connection,"UPDATE subject_number.business_subject SET owner_account_id=NULL,owner_organization_id=?,revision=revision+1 WHERE id=?",BASE.org,subject);
                SharingQueryFixture.execute(connection,"UPDATE subject_number.caller_number_binding SET status='RETIRED',revision=revision+1 WHERE subject_id=? AND status='ACTIVE'",subject);
                SharingQueryFixture.execute(connection,"INSERT INTO subject_number.caller_number_binding(id,subject_id,number_type,number_ciphertext,number_hmac,number_masked,verified_at) VALUES (?,?,'MOBILE','v1.synthetic.payload',?,'synthetic',clock_timestamp())",binding,subject,digest(binding.toString()));
                SharingQueryFixture.execute(connection,"INSERT INTO subject_number.subject_risk_state(id,caller_number_binding_id,risk_type,state,source,observed_at,cleared_at) VALUES (?,?,'CONFLICT','CLEARED','OPERATIONS',clock_timestamp(),clock_timestamp())",UUID.randomUUID(),binding);
            }
            id=UUID.randomUUID();
            String share="admission-"+UUID.randomUUID();
            try(Connection connection=BASE.admin()) {
                SharingQueryFixture.execute(connection,"""
                    INSERT INTO sharing.evidence_share_grant(id,share_id,capability_token_hash,evidence_id,
                        created_by_account_id,owner_organization_id,verification_mode,valid_from,expires_at,max_admissions,
                        content_snapshot,content_sha256,code_salt,code_hash,code_generation,status)
                    SELECT ?,?,?,?,?,?,?,clock_timestamp()-interval '60 seconds',clock_timestamp()+(? * interval '1 millisecond'),?,
                        jsonb_build_object('content',jsonb_build_object('evidenceId',?::text),
                            'verification',jsonb_build_object('mode',?::text) || CASE WHEN ?='DESIGNATED_MOBILE'
                                THEN jsonb_build_object('mobileHmac',repeat('c',64),'keyVersion','synthetic') ELSE '{}'::jsonb END,
                            'creation',jsonb_build_object('creatorAccountId',?::text,'evidenceId',?::text,'ownerOrganizationId',?::text,
                                'scopeDigest',repeat('d',64),'creationRequestDigest',repeat('e',64),'creationFingerprint',repeat('f',64))),
                        repeat('a',64),CASE WHEN ?='EXTRACTION_CODE' THEN repeat('S',22) END,
                        CASE WHEN ?='EXTRACTION_CODE' THEN repeat('b',64) END,CASE WHEN ?='EXTRACTION_CODE' THEN 1 ELSE 0 END,'ACTIVE'
                    """,id,share,digest(token),BASE.evidence,BASE.account,BASE.org,mode,grantLifetime.toMillis(),maximum,
                        BASE.evidence,mode,mode,BASE.account,BASE.evidence,BASE.org,mode,mode,mode);
            }
            if(mode.equals("ORGANIZATION_MEMBER")) caller=Optional.of(new TrustedShareAdmissionCaller(BASE.account,BASE.org));
            try(Connection connection=BASE.admin()) { SharingQueryFixture.execute(connection,"ALTER ROLE sharing_admission_role PASSWORD '"+BASE.password+"'"); }
            var source=new DriverManagerDataSource(BASE.url,"sharing_admission_role",BASE.password);
            var configuration=new DefaultConfiguration(); configuration.set(SQLDialect.POSTGRES); configuration.set(new Settings().withExecuteLogging(false));
            configuration.set(new DataSourceConnectionProvider(new TransactionAwareDataSourceProxy(source)));
            ShareAdmissionRepository real=new JooqShareAdmissionRepository(DSL.using(configuration));
            repository=(ShareAdmissionRepository)java.lang.reflect.Proxy.newProxyInstance(getClass().getClassLoader(),new Class<?>[]{ShareAdmissionRepository.class},(proxy,method,arguments)-> {
                try {
                    if(method.getName().equals("lockGrant")) {
                        Connection connection=DataSourceUtils.getConnection(source);
                        try(var statement=connection.createStatement();var rows=statement.executeQuery("SELECT pg_backend_pid()")) { rows.next(); backendPids.add(rows.getInt(1)); }
                    }
                    Object value=method.invoke(real,arguments);
                    if(method.getName().equals("expireGrant") && !Integer.valueOf(1).equals(value))
                        throw new AssertionError("expireGrant affected-row count was not exactly one");
                    if(method.getName().equals("insertAccessEvent")) {
                        var status=((EventWriteResult)value).status();
                        if(status!=EventWriteStatus.INSERTED && status!=EventWriteStatus.REUSED_IDENTICAL
                                && !(allowTicketFailureEventConflict && status==EventWriteStatus.CONFLICT))
                            throw new AssertionError("insertAccessEvent returned an unexpected status");
                    }
                    if(method.getName().equals(failAt)) throw new IllegalStateException("Synthetic rollback");
                    if(method.getName().equals("updateGrantAdmissionCount") && afterCommit!=null) {
                        Runnable callback=afterCommit; afterCommit=null;
                        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                            @Override public void afterCommit() { callback.run(); }
                        });
                    }
                    return value;
                }
                catch(java.lang.reflect.InvocationTargetException failure) {
                    Throwable cause = failure.getCause();
                    if (cause instanceof RuntimeException runtime) {
                        throw new AssertionError("Admission repository operation failed: " + method.getName(), runtime);
                    }
                    throw failure;
                }
            });
            var manager=new DataSourceTransactionManager(source); manager.setEnforceReadOnly(true); manager.setRollbackOnCommitFailure(true);
            var transactions=new TransactionTemplate(manager); transactions.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW); transactions.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
            service=new DefaultShareAdmissionService(transactions,repository,bytes -> digest(new String(bytes,StandardCharsets.UTF_8)),request -> {
                assertOutside(); policyCalls.incrementAndGet();
                if(policyEntered!=null) {
                    policyEntered.countDown();
                    try { if(!policyRelease.await(10,TimeUnit.SECONDS)) throw new IllegalStateException("Synthetic release timeout"); }
                    catch(InterruptedException interrupted) { Thread.currentThread().interrupt(); throw new IllegalStateException("Synthetic interruption"); }
                }
                var barrier=proofBarrier;
                if(barrier!=null) try { barrier.await(10,TimeUnit.SECONDS); proofBarrier=null; } catch(Exception failure) { throw new IllegalStateException("Synthetic barrier"); }
                return policy;
            },request -> { assertOutside(); proofs.incrementAndGet(); return codeResult; },request -> {
                assertOutside(); proofs.incrementAndGet(); assertThat(request.mobileHmac()).hasSize(64); assertThat(request.verificationProofId()).isEqualTo("proof-private"); return mobileResult;
            },request -> { assertOutside(); try { assertThat(count("sharing.share_admission")).isGreaterThanOrEqualTo(1); } catch(Exception failure) { throw new AssertionError(failure); }
                return new ShareMediaCredentialProvider.Issued(badCredential?"":"credential-private",request.expiresAt());
            },request -> { assertOutside(); assertThat(request.ttl()).isPositive(); assertThat(request.key()).isEqualTo("share-ticket:"+id+":"+request.admissionId()); return ticket; });
        }
        void assertOutside() { assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse(); }
        ShareAdmissionService.AdmissionOutcome admit(String key) { return service.admit(new ShareAdmissionCapabilityToken(token),request(key,mode),caller); }
        void sql(String sql,Object... values) throws Exception { try(Connection connection=BASE.admin()) { SharingQueryFixture.execute(connection,sql,values); } }
        long scalar(String sql,Object... values) throws Exception { try(Connection connection=BASE.admin(); var statement=connection.prepareStatement(sql)) { for(int index=0;index<values.length;index++) statement.setObject(index+1,values[index]); try(var rows=statement.executeQuery()) { rows.next(); return rows.getLong(1); } } }
        long count(String table) throws Exception { return scalar("SELECT count(*) FROM "+table+" WHERE "+(table.endsWith("evidence_share_grant")?"id":"share_grant_id")+"=?",id); }
        long eventCount(String type) throws Exception { return scalar("SELECT count(*) FROM sharing.share_access_event WHERE share_grant_id=? AND event_type=?",id,type); }
        AccessEventRow event(String type) throws Exception {
            try(Connection connection=BASE.admin(); var statement=connection.prepareStatement("SELECT event_id,event_type,payload_redacted,payload_sha256,result FROM sharing.share_access_event WHERE share_grant_id=? AND event_type=?")) {
                statement.setObject(1,id); statement.setString(2,type); try(var rows=statement.executeQuery()) { assertThat(rows.next()).isTrue(); var event=new AccessEventRow(rows.getString(1),rows.getString(2),JSON.readTree(rows.getString(3)),rows.getString(4),rows.getString(5)); assertThat(rows.next()).isFalse(); return event; }
            }
        }
        AdmissionRow row() throws Exception { try(Connection connection=BASE.admin(); var statement=connection.prepareStatement("SELECT id,admission_key,admission_no,verification_mode,verification_result,admitted_at FROM sharing.share_admission WHERE share_grant_id=?")) { statement.setObject(1,id); try(var rows=statement.executeQuery()) { assertThat(rows.next()).isTrue(); return new AdmissionRow(rows.getObject(1,UUID.class),id,rows.getString(2),rows.getLong(3),rows.getString(4),rows.getString(5),rows.getObject(6,OffsetDateTime.class).toInstant()); } } }
        void mutate(String state) {
            try {
                if(state.equals("EXPIRED")) {
                    Thread.sleep(2200);
                    sql("UPDATE sharing.evidence_share_grant SET status='EXPIRED',revocation_generation=revocation_generation+1,revision=revision+1 WHERE id=?",id);
                }
                else sql("UPDATE sharing.evidence_share_grant SET status=?,revocation_generation=revocation_generation+?,revision=revision+1 WHERE id=?",state,state.equals("REVOKED")?1:0,id);
            } catch(Exception failure) { throw new AssertionError("Synthetic transition failed",failure); }
        }
        void openRisk() throws Exception { sql("INSERT INTO subject_number.subject_risk_state(id,caller_number_binding_id,risk_type,state,source) VALUES (?,?,'RECYCLED','OPEN','OPERATIONS')",UUID.randomUUID(),binding); }
    }
}
