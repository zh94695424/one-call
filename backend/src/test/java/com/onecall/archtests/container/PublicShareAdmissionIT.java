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
        beans.addBean("shareMediaAuthorizationService", (ShareMediaAuthorizationService) (token, admissionId, request, caller) -> { throw new ShareAdmissionException(ShareAdmissionException.Code.SERVICE_UNAVAILABLE); });
        var controller = new PublicSharesController(beans.getBeanProvider(com.onecall.core.sharing.internal.publicquery.PublicShareQueryService.class), beans.getBeanProvider(com.onecall.core.customer_iam.internal.application.CustomerContextService.class), writer, beans.getBeanProvider(ShareAdmissionService.class), beans.getBeanProvider(ShareMediaAuthorizationService.class));
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
        assertThat(BASE.flyway.info().applied()).hasSize(13);
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
        final UUID id,subject,evidenceId,binding=UUID.randomUUID();
        final String mode,token="token-private-"+UUID.randomUUID();
        final AtomicInteger proofs=new AtomicInteger(),policyCalls=new AtomicInteger();
        final ShareAdmissionRepository repository;
        final DefaultShareAdmissionService service;
        final DefaultShareMediaAuthorizationService mediaService;
        final Map<String, ShareMediaCredentialProvider.MediaAuthorizationCredentialRequest> credentialRequests = new ConcurrentHashMap<>();
        final Map<String, ShareMediaCredentialProvider.MediaAuthorizationCredentialResult.Issued> issuedCredentials = new ConcurrentHashMap<>();
        final AtomicInteger credentialCalls = new AtomicInteger(), semanticCredentials = new AtomicInteger();
        final Set<Integer> mediaBackendPids=ConcurrentHashMap.newKeySet();
        final Set<Boolean> mediaProviderTransactions=ConcurrentHashMap.newKeySet();
        volatile String mediaProviderMode; volatile CountDownLatch mediaProviderEntered,mediaProviderRelease;
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
            try(Connection connection=BASE.admin()) {
                connection.setAutoCommit(false);
                SharingQueryFixture.execute(connection,"ALTER TABLE evidence.evidence_record DISABLE TRIGGER trg_evidence_record_guard");
                SharingQueryFixture.execute(connection,"UPDATE evidence.evidence_record SET identity_at_capture=identity_at_capture || jsonb_build_object('bindingId',?::text) WHERE id=?",binding,BASE.evidence);
                SharingQueryFixture.execute(connection,"ALTER TABLE evidence.evidence_record ENABLE TRIGGER trg_evidence_record_guard");
                connection.commit();
            }
            evidenceId=BASE.evidence;
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
                    """,id,share,digest(token),evidenceId,BASE.account,BASE.org,mode,grantLifetime.toMillis(),maximum,
                        evidenceId,mode,mode,BASE.account,evidenceId,BASE.org,mode,mode,mode);
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
            ShareMediaCredentialProvider mediaCredentials = new ShareMediaCredentialProvider() {
                @Override public Result issue(MediaCredentialRequest request) { return new Unavailable(); }
                @Override public MediaAuthorizationCredentialResult issueOrReplay(MediaAuthorizationCredentialRequest request) {
                    assertOutside();
                    mediaProviderTransactions.add(TransactionSynchronizationManager.isActualTransactionActive());
                    credentialCalls.incrementAndGet();
                    var mode=mediaProviderMode;
                    var entered=mediaProviderEntered; var release=mediaProviderRelease;
                    if(entered!=null && release!=null) {
                        entered.countDown();
                        try { if(!release.await(10,TimeUnit.SECONDS)) throw new IllegalStateException("Synthetic media provider release timeout"); }
                        catch(InterruptedException interrupted) { Thread.currentThread().interrupt(); throw new IllegalStateException("Synthetic media provider interruption"); }
                    }
                    if(mode!=null && !"SUB_MICROSECOND".equals(mode)) return switch(mode) {
                        case "UNAVAILABLE" -> new MediaAuthorizationCredentialResult.Unavailable();
                        case "MALFORMED" -> new MediaAuthorizationCredentialResult.Malformed();
                        case "THROWN" -> throw new IllegalStateException("Synthetic media provider outage");
                        case "BLANK" -> new MediaAuthorizationCredentialResult.Issued(" ",request.credentialExpiresAt());
                        case "OVER_LENGTH" -> new MediaAuthorizationCredentialResult.Issued("😀".repeat(2049),request.credentialExpiresAt());
                        case "NUL" -> new MediaAuthorizationCredentialResult.Issued("media\0credential",request.credentialExpiresAt());
                        case "UNPAIRED_SURROGATE" -> new MediaAuthorizationCredentialResult.Issued("media\uD800",request.credentialExpiresAt());
                        case "NULL_EXPIRY" -> new MediaAuthorizationCredentialResult.Issued("media-credential",null);
                        case "PAST_EXPIRY" -> new MediaAuthorizationCredentialResult.Issued("media-credential",Instant.EPOCH);
                        case "BEYOND_BOUND" -> new MediaAuthorizationCredentialResult.Issued("media-credential-beyond-bound",request.credentialExpiresAt().plusMillis(1));
                        default -> throw new AssertionError("Unknown media provider failure mode: "+mode);
                    };
                    var firstRequest = credentialRequests.putIfAbsent(request.eventId(), request);
                    if (firstRequest != null && !firstRequest.equals(request)) throw new AssertionError("Provider idempotency input changed");
                    var issued = issuedCredentials.computeIfAbsent(request.eventId(), eventId -> {
                        semanticCredentials.incrementAndGet();
                        return new MediaAuthorizationCredentialResult.Issued("media-credential-"+digest(eventId),
                                "SUB_MICROSECOND".equals(mode) ? request.credentialExpiresAt().minusNanos(1) : request.credentialExpiresAt());
                    });
                    return issued;
                }
            };
            ShareAdmissionRepository mediaRepository = (ShareAdmissionRepository) java.lang.reflect.Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{ShareAdmissionRepository.class}, (proxy, method, arguments) -> {
                if (method.getName().equals("lockGrant")) {
                    Connection connection = DataSourceUtils.getConnection(source);
                    try (var statement = connection.createStatement(); var rows = statement.executeQuery("SELECT pg_backend_pid()")) { rows.next(); mediaBackendPids.add(rows.getInt(1)); }
                }
                try { return method.invoke(real, arguments); }
                catch (java.lang.reflect.InvocationTargetException failure) {
                    Throwable cause = failure.getCause();
                    if (cause instanceof RuntimeException known) throw known;
                    if (cause instanceof Error error) throw error;
                    throw failure;
                }
            });
            mediaService = new DefaultShareMediaAuthorizationService(transactions, mediaRepository, bytes -> digest(new String(bytes,StandardCharsets.UTF_8)), mediaCredentials);
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
    @Test void mediaAuthorizationUsesExactRouteAndFrozenResponse() throws Exception {
        var admission=view(fixture.admit("route-admission"));
        fixture.mediaProviderMode="SUB_MICROSECOND";
        Instant expectedExpiry=Instant.parse(mediaBoundText(fixture.event("ADMISSION_GRANTED").payload()))
                .minusNanos(1).truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        String expectedExpiryText=new java.time.format.DateTimeFormatterBuilder().appendInstant(6).toFormatter().format(expectedExpiry);
        var beans=new StaticListableBeanFactory(); beans.addBean("shareAdmissionService",fixture.service); beans.addBean("shareMediaAuthorizationService",fixture.mediaService);
        var writer=new CustomerProblemWriter();
        var controller=new PublicSharesController(beans.getBeanProvider(com.onecall.core.sharing.internal.publicquery.PublicShareQueryService.class),
                beans.getBeanProvider(com.onecall.core.customer_iam.internal.application.CustomerContextService.class),writer,
                beans.getBeanProvider(ShareAdmissionService.class),beans.getBeanProvider(ShareMediaAuthorizationService.class));
        var mvc=org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(controller)
                .addFilters(new CustomerBoundaryFilter(new CustomerBearerAuthenticator(beans.getBeanProvider(CustomerSessionVerifier.class)),writer)).build();
        String path="/api/v1/public/shares/"+fixture.token+"/admissions/"+admission.admissionId()+"/media-authorizations";
        var response=mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(path).servletPath(path)
                .contentType("application/json").content("{\"authorizationRequestId\":\"route-request\",\"purpose\":\"PLAYBACK\"}"))
                .andReturn().getResponse();
        assertThat(response.getStatus()).isEqualTo(200);
        JsonNode body=JSON.readTree(response.getContentAsString());
        assertThat(body.propertyNames()).containsExactlyInAnyOrder("credential","expiresAt");
        assertThat(body.path("credential").asString()).startsWith("media-credential-");
        assertThat(Instant.parse(body.path("expiresAt").asString())).isEqualTo(expectedExpiry);
        assertThat(fixture.eventCount("PLAYBACK_AUTHORIZED")).isEqualTo(1);
        var success=fixture.event("PLAYBACK_AUTHORIZED");
        assertThat(success.result()).isEqualTo("SUCCESS");
        assertThat(success.payload().path("credentialExpiresAt").asString()).isEqualTo(expectedExpiryText);
        assertThat(success.payload().toString()).doesNotContain(body.path("credential").asString());
        var replay=mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(path).servletPath(path)
                .contentType("application/json").content("{\"authorizationRequestId\":\"route-request\",\"purpose\":\"PLAYBACK\"}"))
                .andReturn().getResponse();
        assertThat(replay.getStatus()).isEqualTo(200);
        assertThat(JSON.readTree(replay.getContentAsString())).isEqualTo(body);
        assertThat(fixture.eventCount("PLAYBACK_AUTHORIZED")).isEqualTo(1);
        assertThat(fixture.event("PLAYBACK_AUTHORIZED")).isEqualTo(success);
        assertThat(fixture.eventCount("FAILURE")).isZero();
        assertThat(fixture.credentialCalls).hasValue(2);
        assertThat(fixture.semanticCredentials).hasValue(1);
    }

    @Test void invalidTokenOrUnassociatedAdmissionDoesNotDiscloseState() throws Exception {
        var admission=view(fixture.admit("known-admission"));
        var invalid=catchThrowable(() -> fixture.mediaService.authorize(new ShareAdmissionCapabilityToken("not-a-real-token"),admission.admissionId(),mediaRequest("invalid-token",MediaAuthorizationRequest.Purpose.PLAYBACK),null));
        var unassociated=catchThrowable(() -> fixture.mediaService.authorize(new ShareAdmissionCapabilityToken(fixture.token),UUID.randomUUID(),mediaRequest("unknown-admission",MediaAuthorizationRequest.Purpose.PLAYBACK),null));
        assertThat(invalid).isInstanceOf(ShareAdmissionException.class);
        assertThat(((ShareAdmissionException)invalid).code()).isEqualTo(ShareAdmissionException.Code.SHARE_TOKEN_INVALID);
        assertThat(unassociated).isInstanceOf(ShareAdmissionException.class);
        assertThat(((ShareAdmissionException)unassociated).code()).isEqualTo(ShareAdmissionException.Code.FORBIDDEN);
        assertThat(invalid.toString()).doesNotContain(fixture.token,fixture.id.toString(),admission.admissionId().toString());
        assertThat(unassociated.toString()).doesNotContain(fixture.token,fixture.id.toString(),admission.admissionId().toString());
        assertThat(fixture.credentialCalls).hasValue(0);
        assertThat(fixture.eventCount("PLAYBACK_AUTHORIZED")).isZero();
    }

    @Test void activeGrantAuthorizesPlaybackSeekAndRenewWithoutAdmissionMutation() throws Exception {
        var admission=view(fixture.admit("stable-admission"));
        long count=fixture.scalar("SELECT admissions_used FROM sharing.evidence_share_grant WHERE id=?",fixture.id);
        long revision=fixture.scalar("SELECT revision FROM sharing.evidence_share_grant WHERE id=?",fixture.id);
        for(var purpose:MediaAuthorizationRequest.Purpose.values()) {
            var result=fixture.mediaService.authorize(new ShareAdmissionCapabilityToken(fixture.token),admission.admissionId(),mediaRequest("request-"+purpose,purpose),null);
            assertThat(result.credential()).startsWith("media-credential-");
            assertThat(result.expiresAt()).isBeforeOrEqualTo(admission.sessionExpiresAt());
            assertThat(fixture.eventCount(switch(purpose) { case PLAYBACK -> "PLAYBACK_AUTHORIZED"; case SEEK -> "SEEK_AUTHORIZED"; case RENEW -> "RENEWED"; })).isEqualTo(1);
        }
        assertThat(fixture.count("sharing.share_admission")).isEqualTo(1);
        assertThat(fixture.scalar("SELECT admissions_used FROM sharing.evidence_share_grant WHERE id=?",fixture.id)).isEqualTo(count);
        assertThat(fixture.scalar("SELECT revision FROM sharing.evidence_share_grant WHERE id=?",fixture.id)).isEqualTo(revision);
    }

    @Test void alreadyAdmittedLockedSessionAuthorizesAndShareLockedIsNotReturned() throws Exception {
        var admission=view(fixture.admit("before-lock")); fixture.mutate("LOCKED");
        var authorized=fixture.mediaService.authorize(new ShareAdmissionCapabilityToken(fixture.token),admission.admissionId(),mediaRequest("locked-playback",MediaAuthorizationRequest.Purpose.PLAYBACK),null);
        assertThat(authorized.credential()).startsWith("media-credential-");
        denied(fixture.admit("after-lock"),ShareAdmissionException.Code.SHARE_LOCKED);
        assertThat(fixture.count("sharing.share_admission")).isEqualTo(1);
        assertThat(fixture.eventCount("PLAYBACK_AUTHORIZED")).isEqualTo(1);
    }

    @Test void sameRequestAndPurposeReplaysOneCredentialAndOneEvent() throws Exception {
        var admission=view(fixture.admit("replay-admission"));
        var request=mediaRequest("same-request",MediaAuthorizationRequest.Purpose.PLAYBACK);
        var first=fixture.mediaService.authorize(new ShareAdmissionCapabilityToken(fixture.token),admission.admissionId(),request,null);
        var replay=fixture.mediaService.authorize(new ShareAdmissionCapabilityToken(fixture.token),admission.admissionId(),request,null);
        assertThat(replay).isEqualTo(first);
        assertThat(fixture.credentialCalls).hasValue(2);
        assertThat(fixture.semanticCredentials).hasValue(1);
        assertThat(fixture.issuedCredentials).hasSize(1);
        assertThat(fixture.eventCount("PLAYBACK_AUTHORIZED")).isEqualTo(1);
        var payload=fixture.event("PLAYBACK_AUTHORIZED").payload();
        assertThat(payload.propertyNames()).doesNotContain("credential");
        assertThat(payload.toString()).doesNotContain(first.credential());
    }

    @Test void sameKeyPlaybackThenSeekConflictsWithoutSecondCredentialOrEvent() throws Exception {
        var admission=view(fixture.admit("conflict-admission"));
        var playback=fixture.mediaService.authorize(new ShareAdmissionCapabilityToken(fixture.token),admission.admissionId(),mediaRequest("same-key",MediaAuthorizationRequest.Purpose.PLAYBACK),null);
        var conflict=catchThrowable(() -> fixture.mediaService.authorize(new ShareAdmissionCapabilityToken(fixture.token),admission.admissionId(),mediaRequest("same-key",MediaAuthorizationRequest.Purpose.SEEK),null));
        assertThat(conflict).isInstanceOf(ShareAdmissionException.class);
        assertThat(((ShareAdmissionException)conflict).code()).isEqualTo(ShareAdmissionException.Code.SHARE_GENERATION_STALE);
        assertThat(fixture.credentialCalls).hasValue(1);
        assertThat(fixture.semanticCredentials).hasValue(1);
        assertThat(fixture.eventCount("PLAYBACK_AUTHORIZED")).isEqualTo(1);
        assertThat(fixture.eventCount("SEEK_AUTHORIZED")).isZero();
        assertThat(fixture.eventCount("FAILURE")).isZero();
        assertThat(fixture.event("PLAYBACK_AUTHORIZED").payload().toString()).doesNotContain(playback.credential());
    }

    private static MediaAuthorizationRequest mediaRequest(String key,MediaAuthorizationRequest.Purpose purpose) { return new MediaAuthorizationRequest(key,purpose); }

    @Test void organizationMemberReauthenticatesOriginalAccountTenantAndMembership() throws Exception {
        fixture=new Fixture("ORGANIZATION_MEMBER",null);
        var admission=view(fixture.admit("original-recipient"));
        var resolverCalls=new AtomicInteger();
        var before=mediaAdmissionState();
        var authorized=fixture.mediaService.authorize(new ShareAdmissionCapabilityToken(fixture.token),admission.admissionId(),
                mediaRequest("original-session",MediaAuthorizationRequest.Purpose.PLAYBACK),() -> {
                    fixture.assertOutside(); resolverCalls.incrementAndGet(); return fixture.caller;
                });
        assertThat(authorized.credential()).startsWith("media-credential-");
        assertThat(resolverCalls).hasValue(1);
        assertThat(fixture.eventCount("PLAYBACK_AUTHORIZED")).isEqualTo(1);
        assertThat(mediaAdmissionState()).isEqualTo(before);

        for(String scenario:List.of("account","organization","membership-missing","membership-revoked","membership-suspended",
                "account-disabled","organization-disabled","absent","empty","null-result","invalid","malformed-witness","changed-recipient")) {
            BASE.reset(); fixture=new Fixture("ORGANIZATION_MEMBER",null);
            var original=view(fixture.admit("member-denial"));
            ShareAdmissionException.Code expected=ShareAdmissionException.Code.FORBIDDEN;
            switch(scenario) {
                case "account" -> fixture.caller=Optional.of(new TrustedShareAdmissionCaller(BASE.other,BASE.org));
                case "organization" -> { fixture.caller=Optional.of(new TrustedShareAdmissionCaller(BASE.account,BASE.org2)); expected=ShareAdmissionException.Code.SHARE_WRONG_TENANT; }
                case "membership-missing" -> fixture.sql("DELETE FROM organization_access.organization_membership WHERE id=?",BASE.membership);
                case "membership-revoked", "membership-suspended" -> fixture.sql("UPDATE organization_access.organization_membership SET status=?,revision=revision+1 WHERE id=?",scenario.equals("membership-revoked")?"REVOKED":"SUSPENDED",BASE.membership);
                case "account-disabled" -> fixture.sql("UPDATE customer_iam.customer_account SET status='DISABLED',revision=revision+1 WHERE id=?",BASE.account);
                case "organization-disabled" -> fixture.sql("UPDATE organization_access.organization SET status='DISABLED',revision=revision+1 WHERE id=?",BASE.org);
                case "absent", "empty", "null-result", "invalid" -> expected=ShareAdmissionException.Code.UNAUTHENTICATED;
                case "malformed-witness" -> { rewriteMediaWitness(Map.of("recipientAccountId","not-a-uuid")); expected=ShareAdmissionException.Code.DEPENDENCY_UNAVAILABLE; }
                case "changed-recipient" -> rewriteMediaWitness(Map.of("recipientAccountId",BASE.other.toString()));
                default -> throw new AssertionError(scenario);
            }
            resolverCalls.set(0);
            ShareMediaAuthorizationService.CallerResolver resolver=scenario.equals("absent")?null:() -> {
                fixture.assertOutside(); resolverCalls.incrementAndGet();
                if(scenario.equals("invalid")) throw new ShareAdmissionException(ShareAdmissionException.Code.UNAUTHENTICATED);
                if(scenario.equals("null-result")) return null;
                return scenario.equals("empty")?Optional.empty():fixture.caller;
            };
            deniedMedia(original,scenario,resolver,expected);
            assertThat(resolverCalls).as(scenario).hasValue(scenario.equals("absent")?0:1);
        }

        for(String mode:List.of("NONE","EXTRACTION_CODE","DESIGNATED_MOBILE")) {
            BASE.reset(); fixture=new Fixture(mode,null);
            var nonMember=view(fixture.admit("non-member")); resolverCalls.set(0);
            fixture.mediaService.authorize(new ShareAdmissionCapabilityToken(fixture.token),nonMember.admissionId(),
                    mediaRequest("lazy-non-member",MediaAuthorizationRequest.Purpose.PLAYBACK),() -> {
                        resolverCalls.incrementAndGet(); throw new ShareAdmissionException(ShareAdmissionException.Code.UNAUTHENTICATED);
                    });
            assertThat(resolverCalls).as(mode).hasValue(0);
            assertThat(fixture.eventCount("PLAYBACK_AUTHORIZED")).isEqualTo(1);
        }
    }

    @Test void capturedAndCurrentBindingAndOpenOrMissingRiskDenyWithoutEvent() throws Exception {
        BASE.reset(); fixture=new Fixture("NONE",null);
        corruptMediaRow("evidence.evidence_record","trg_evidence_record_guard","UPDATE evidence.evidence_record SET identity_at_capture=identity_at_capture-'bindingId' WHERE id=?",fixture.evidenceId);
        var currentAdmission=view(fixture.admit("current-binding"));
        var currentBefore=mediaAdmissionState();
        var currentAuthorized=fixture.mediaService.authorize(new ShareAdmissionCapabilityToken(fixture.token),currentAdmission.admissionId(),mediaRequest("missing-binding-id",MediaAuthorizationRequest.Purpose.PLAYBACK),null);
        assertThat(currentAuthorized.credential()).startsWith("media-credential-");
        assertThat(fixture.credentialCalls).hasValue(1);
        assertThat(fixture.eventCount("PLAYBACK_AUTHORIZED")).isEqualTo(1);
        assertThat(mediaAdmissionState()).isEqualTo(currentBefore);

        for(String scenario:List.of("disabled","retired","unverified","different-current","missing-captured","no-current","ambiguous-current","current-missing-risk","current-open-risk","malformed-binding-id","missing-risk","open-risk")) {
            BASE.reset(); fixture=new Fixture("NONE",null);
            var admission=view(fixture.admit("binding-risk"));
            ShareAdmissionException.Code expected=ShareAdmissionException.Code.SHARE_RISK_BLOCKED;
            switch(scenario) {
                case "disabled", "retired", "unverified", "different-current" -> {
                    fixture.sql("UPDATE subject_number.caller_number_binding SET status=?,verified_at=CASE WHEN ? THEN NULL ELSE verified_at END,revision=revision+1 WHERE id=?",
                            scenario.equals("disabled")?"DISABLED":"RETIRED",scenario.equals("unverified"),fixture.binding);
                    if(scenario.equals("different-current")) fixture.sql("INSERT INTO subject_number.caller_number_binding(id,subject_id,number_type,number_ciphertext,number_hmac,number_masked,verified_at) VALUES (?,?,'MOBILE','v1.synthetic.payload',?,'synthetic',clock_timestamp())",UUID.randomUUID(),fixture.subject,digest(scenario));
                }
                case "missing-captured", "malformed-binding-id" -> {
                    String identity=JSON.writeValueAsString(Map.of("bindingId",scenario.equals("missing-captured")?UUID.randomUUID().toString():"not-a-uuid"));
                    corruptMediaRow("evidence.evidence_record","trg_evidence_record_guard","UPDATE evidence.evidence_record SET identity_at_capture=(identity_at_capture-'bindingId') || ?::jsonb WHERE id=?",identity,fixture.evidenceId);
                    if(!scenario.equals("missing-captured")) expected=ShareAdmissionException.Code.DEPENDENCY_UNAVAILABLE;
                }
                case "no-current", "ambiguous-current", "current-missing-risk", "current-open-risk" -> {
                    corruptMediaRow("evidence.evidence_record","trg_evidence_record_guard","UPDATE evidence.evidence_record SET identity_at_capture=identity_at_capture-'bindingId' WHERE id=?",fixture.evidenceId);
                    switch(scenario) {
                        case "no-current" -> fixture.sql("UPDATE subject_number.caller_number_binding SET status='RETIRED',revision=revision+1 WHERE id=?",fixture.binding);
                        case "ambiguous-current" -> fixture.sql("INSERT INTO subject_number.caller_number_binding(id,subject_id,number_type,number_ciphertext,number_hmac,number_masked,verified_at) VALUES (?,?,'MOBILE','v1.synthetic.payload',?,'synthetic',clock_timestamp())",UUID.randomUUID(),fixture.subject,digest(scenario));
                        case "current-missing-risk" -> fixture.sql("DELETE FROM subject_number.subject_risk_state WHERE caller_number_binding_id=?",fixture.binding);
                        case "current-open-risk" -> fixture.openRisk();
                        default -> throw new AssertionError(scenario);
                    }
                }
                case "missing-risk" -> fixture.sql("DELETE FROM subject_number.subject_risk_state WHERE caller_number_binding_id=?",fixture.binding);
                case "open-risk" -> fixture.openRisk();
                default -> throw new AssertionError(scenario);
            }
            deniedMedia(admission,scenario,null,expected);
        }

        BASE.reset(); fixture=new Fixture("NONE",null);
        var admission=view(fixture.admit("closed-risk")); fixture.openRisk();
        deniedMedia(admission,"open-before-clear",null,ShareAdmissionException.Code.SHARE_RISK_BLOCKED);
        fixture.sql("UPDATE subject_number.subject_risk_state SET state='CLEARED',cleared_at=clock_timestamp(),revision=revision+1 WHERE caller_number_binding_id=? AND state='OPEN'",fixture.binding);
        var before=mediaAdmissionState();
        var authorized=fixture.mediaService.authorize(new ShareAdmissionCapabilityToken(fixture.token),admission.admissionId(),mediaRequest("cleared-risk",MediaAuthorizationRequest.Purpose.PLAYBACK),null);
        assertThat(authorized.credential()).startsWith("media-credential-");
        assertThat(fixture.credentialCalls).hasValue(1);
        assertThat(fixture.eventCount("PLAYBACK_AUTHORIZED")).isEqualTo(1);
        assertThat(mediaAdmissionState()).isEqualTo(before);
    }

    @Test void revokedExpiredGenerationStaleAndSessionExpiredMapExactly() throws Exception {
        for(String scenario:List.of("revoked","time-expired","expired","rotation","session-ended","admission-ended","future-valid-from","malformed-witness","revoked-malformed","revoked-member","stale-member",
                "absent-session-bound","malformed-session-bound","nonpositive-session-bound",
                "generation-text","generation-negative","generation-out-of-int")) {
            BASE.reset();
            boolean shortGrant=scenario.equals("time-expired") || scenario.equals("expired");
            boolean member=scenario.endsWith("member");
            fixture=new Fixture(member?"ORGANIZATION_MEMBER":"EXTRACTION_CODE",null,shortGrant?Duration.ofSeconds(2):Duration.ofDays(1));
            var admission=view(fixture.admit("terminal-time-generation"));
            ShareAdmissionException.Code expected=ShareAdmissionException.Code.DEPENDENCY_UNAVAILABLE;
            switch(scenario) {
                case "revoked", "revoked-malformed", "revoked-member" -> {
                    fixture.mutate("REVOKED"); expected=ShareAdmissionException.Code.SHARE_REVOKED;
                    if(scenario.equals("revoked-malformed")) rewriteMediaWitness(Map.of("sessionExpiresAt","not-an-instant"));
                }
                case "time-expired", "expired" -> {
                    if(scenario.equals("expired")) fixture.mutate("EXPIRED");
                    else {
                        Thread.sleep(2200);
                        fixture.sql("UPDATE sharing.evidence_share_grant SET code_salt=repeat('T',22),code_hash=repeat('c',64),code_generation=code_generation+1,revocation_generation=revocation_generation+1,revision=revision+1 WHERE id=?",fixture.id);
                    }
                    expected=ShareAdmissionException.Code.SHARE_EXPIRED;
                }
                case "rotation", "stale-member" -> {
                    if(member) fixture.sql("UPDATE sharing.evidence_share_grant SET revocation_generation=revocation_generation+1,revision=revision+1 WHERE id=?",fixture.id);
                    else fixture.sql("UPDATE sharing.evidence_share_grant SET code_salt=repeat('T',22),code_hash=repeat('c',64),code_generation=code_generation+1,revocation_generation=revocation_generation+1,revision=revision+1 WHERE id=?",fixture.id);
                    fixture.openRisk(); expected=ShareAdmissionException.Code.SHARE_GENERATION_STALE;
                }
                case "session-ended", "admission-ended" -> {
                    String ended=fixture.row().admittedAt().plusNanos(1000).toString();
                    rewriteMediaWitness(scenario.equals("session-ended")?Map.of("sessionExpiresAt",ended):Map.of("sessionExpiresAt",ended,"admissionExpiresAt",ended));
                    fixture.sql("UPDATE sharing.evidence_share_grant SET revocation_generation=revocation_generation+1,revision=revision+1 WHERE id=?",fixture.id);
                    expected=ShareAdmissionException.Code.SHARE_SESSION_EXPIRED;
                }
                case "future-valid-from" -> {
                    corruptMediaRow("sharing.evidence_share_grant","trg_share_grant_guard","UPDATE sharing.evidence_share_grant SET valid_from=clock_timestamp()+interval '1 hour' WHERE id=?",fixture.id);
                    fixture.sql("UPDATE sharing.evidence_share_grant SET revocation_generation=revocation_generation+1,revision=revision+1 WHERE id=?",fixture.id);
                    expected=ShareAdmissionException.Code.DEPENDENCY_UNAVAILABLE;
                }
                case "malformed-witness" -> {
                    rewriteMediaWitness(Map.of("verificationMode","NONE"));
                    fixture.sql("UPDATE sharing.evidence_share_grant SET revocation_generation=revocation_generation+1,revision=revision+1 WHERE id=?",fixture.id);
                    expected=ShareAdmissionException.Code.DEPENDENCY_UNAVAILABLE;
                }
                case "absent-session-bound" -> {
                    Map<String,JsonNode> absent=new HashMap<>(); absent.put("sessionExpiresAt",JSON.readTree("null"));
                    rewriteMediaWitnessValues(absent);
                    expected=ShareAdmissionException.Code.SHARE_SESSION_POLICY_UNAVAILABLE;
                }
                case "malformed-session-bound" -> {
                    rewriteMediaWitnessValues(Map.of("sessionExpiresAt",JSON.valueToTree("not-an-instant")));
                    expected=ShareAdmissionException.Code.SHARE_SESSION_POLICY_UNAVAILABLE;
                }
                case "nonpositive-session-bound" -> {
                    rewriteMediaWitnessValues(Map.of("sessionExpiresAt",JSON.valueToTree(Instant.EPOCH.toString())));
                    expected=ShareAdmissionException.Code.SHARE_SESSION_POLICY_UNAVAILABLE;
                }
                case "generation-text", "generation-negative", "generation-out-of-int" -> {
                    fixture.sql("ALTER TABLE sharing.share_access_event DROP CONSTRAINT ck_share_access_event_admission_facts");
                    rewriteMediaWitnessValues(Map.of("codeGeneration",switch(scenario) {
                        case "generation-text" -> JSON.valueToTree("1");
                        case "generation-negative" -> JSON.valueToTree(-1);
                        default -> JSON.valueToTree(2147483648L);
                    }));
                    restoreMediaAdmissionFactsConstraint();
                }
                default -> throw new AssertionError(scenario);
            }
            if(member) fixture.sql("UPDATE organization_access.organization_membership SET status='SUSPENDED',revision=revision+1 WHERE id=?",BASE.membership);
            deniedMedia(admission,scenario,member?() -> fixture.caller:null,expected);
        }
    }

    private void deniedMedia(ShareAdmissionView admission,String scenario,ShareMediaAuthorizationService.CallerResolver resolver,ShareAdmissionException.Code expected) throws Exception {
        var before=mediaAdmissionState();
        String events=mediaTableState("sharing.share_access_event");
        int providerCalls=fixture.credentialCalls.get(),credentialCount=fixture.semanticCredentials.get();
        var failure=catchThrowable(() -> fixture.mediaService.authorize(new ShareAdmissionCapabilityToken(fixture.token),admission.admissionId(),mediaRequest(scenario,MediaAuthorizationRequest.Purpose.PLAYBACK),resolver));
        assertThat(failure).as(scenario).isInstanceOf(ShareAdmissionException.class);
        assertThat(((ShareAdmissionException)failure).code()).as(scenario).isEqualTo(expected);
        var response=new MockHttpServletResponse(); new CustomerProblemWriter().write(response,expected,UUID.randomUUID());
        assertThat(response.getStatus()).as(scenario).isEqualTo(switch(expected) {
            case UNAUTHENTICATED -> 401;
            case FORBIDDEN,SHARE_WRONG_TENANT,SHARE_RISK_BLOCKED -> 403;
            case SHARE_GENERATION_STALE -> 409;
            case SHARE_REVOKED,SHARE_EXPIRED,SHARE_SESSION_EXPIRED -> 410;
            case DEPENDENCY_UNAVAILABLE,SHARE_SESSION_POLICY_UNAVAILABLE -> 503;
            default -> throw new AssertionError(expected);
        });
        assertThat(JSON.readTree(response.getContentAsString()).path("code").asString()).isEqualTo(expected.name());
        assertThat(fixture.credentialCalls).as(scenario).hasValue(providerCalls);
        assertThat(fixture.semanticCredentials).as(scenario).hasValue(credentialCount);
        assertThat(fixture.issuedCredentials).as(scenario).isEmpty();
        assertThat(fixture.credentialRequests).as(scenario).isEmpty();
        assertThat(mediaAdmissionState()).as(scenario).isEqualTo(before);
        assertThat(mediaTableState("sharing.share_access_event")).as(scenario).isEqualTo(events);
        for(String type:List.of("PLAYBACK_AUTHORIZED","SEEK_AUTHORIZED","RENEWED","FAILURE")) assertThat(fixture.eventCount(type)).as(scenario+":"+type).isZero();
    }

    private List<String> mediaAdmissionState() throws Exception {
        return List.of(mediaTableState("sharing.evidence_share_grant"),mediaTableState("sharing.share_admission"));
    }

    private String mediaTableState(String table) throws Exception {
        try(Connection connection=BASE.admin(); var statement=connection.prepareStatement("SELECT coalesce(jsonb_agg(to_jsonb(state) ORDER BY id),'[]'::jsonb)::text FROM "+table+" state WHERE "+(table.endsWith("evidence_share_grant")?"id":"share_grant_id")+"=?")) {
            statement.setObject(1,fixture.id);
            try(var rows=statement.executeQuery()) { assertThat(rows.next()).isTrue(); return rows.getString(1); }
        }
    }

    private void rewriteMediaWitness(Map<String,String> replacements) throws Exception {
        rewriteMediaWitnessValues(replacements.entrySet().stream().collect(java.util.stream.Collectors.toMap(Map.Entry::getKey,entry -> JSON.valueToTree(entry.getValue()))));
    }

    private void rewriteMediaWitnessValues(Map<String,JsonNode> replacements) throws Exception {
        var payload=fixture.event("ADMISSION_GRANTED").payload();
        Map<String,JsonNode> fields=new TreeMap<>(); payload.properties().forEach(entry -> fields.put(entry.getKey(),entry.getValue()));
        replacements.forEach((key,value) -> { if(value == null) fields.remove(key); else fields.put(key,value); });
        String updated=JSON.writeValueAsString(fields);
        corruptMediaRow("sharing.share_access_event","trg_share_access_event_immutable","UPDATE sharing.share_access_event SET payload_redacted=?::jsonb,payload_sha256=? WHERE share_grant_id=? AND event_type='ADMISSION_GRANTED'",updated,digest(canonical(JSON.readTree(updated))),fixture.id);
    }

    private void restoreMediaAdmissionFactsConstraint() throws Exception {
        try(Connection connection=BASE.admin(); var statement=connection.createStatement()) {
            statement.execute("ALTER TABLE sharing.share_access_event ADD CONSTRAINT ck_share_access_event_admission_facts "
                    + "CHECK (event_type <> 'ADMISSION_GRANTED' OR (payload_redacted ? 'verificationMode' AND payload_redacted->>'verificationMode' "
                    + "IN ('NONE','EXTRACTION_CODE','DESIGNATED_MOBILE','ORGANIZATION_MEMBER') AND payload_redacted ? 'revocationGeneration' "
                    + "AND payload_redacted ? 'codeGeneration' AND payload_redacted ? 'grantExpiresAt' AND payload_redacted ? 'admissionExpiresAt' "
                    + "AND payload_redacted ? 'sessionExpiresAt' AND (payload_redacted->>'verificationMode' <> 'ORGANIZATION_MEMBER' "
                    + "OR (payload_redacted ? 'recipientAccountId' AND payload_redacted ? 'recipientOrganizationId'))))");
        }
    }

    private void corruptMediaRow(String table,String trigger,String sql,Object... values) throws Exception {
        try(Connection connection=BASE.admin()) {
            connection.setAutoCommit(false);
            SharingQueryFixture.execute(connection,"ALTER TABLE "+table+" DISABLE TRIGGER "+trigger);
            SharingQueryFixture.execute(connection,sql,values);
            SharingQueryFixture.execute(connection,"ALTER TABLE "+table+" ENABLE TRIGGER "+trigger);
            connection.commit();
        }
    }

    private static final List<String> MEDIA_FAILURE_REASONS=List.of("MEDIA_CREDENTIAL_UNAVAILABLE","MEDIA_CREDENTIAL_MALFORMED");

    @Test void twoConnectionConcurrentFirstCallsConvergeOnOneCredentialAndOneEvent() throws Exception {
        var admission=view(fixture.admit("two-connection-admission"));
        var witness=fixture.event("ADMISSION_GRANTED");
        var state=mediaAdmissionState();
        long events=fixture.count("sharing.share_access_event");
        String key="two-connection-playback";
        String eventId=mediaEventId(fixture.id,admission.admissionId(),"PLAYBACK_AUTHORIZED",key);
        String bound=mediaBoundText(witness.payload());
        fixture.mediaProviderEntered=new CountDownLatch(2); fixture.mediaProviderRelease=new CountDownLatch(1);
        List<ShareAdmissionView.MediaAuthorization> responses;
        int probePid=0;
        try(var executor=Executors.newFixedThreadPool(2)) {
            var token=new ShareAdmissionCapabilityToken(fixture.token);
            var request=mediaRequest(key,MediaAuthorizationRequest.Purpose.PLAYBACK);
            var left=executor.submit(() -> fixture.mediaService.authorize(token,admission.admissionId(),request,null));
            var right=executor.submit(() -> fixture.mediaService.authorize(token,admission.admissionId(),request,null));
            assertThat(fixture.mediaProviderEntered.await(20,TimeUnit.SECONDS)).as("both callers reach provider I/O").isTrue();
            try(Connection connection=BASE.admin()) {
                connection.setAutoCommit(false);
                try(var statement=connection.createStatement()) { statement.execute("SET lock_timeout = '2500ms'"); }
                try {
                    try(var statement=connection.prepareStatement("SELECT pg_backend_pid(),id FROM sharing.evidence_share_grant WHERE id=? FOR UPDATE")) {
                        statement.setObject(1,fixture.id);
                        try(var rows=statement.executeQuery()) {
                            assertThat(rows.next()).as("a third connection takes the grant row while both callers are parked inside the provider").isTrue();
                            probePid=rows.getInt(1);
                            assertThat(rows.getObject(2,UUID.class)).isEqualTo(fixture.id);
                        }
                    }
                    connection.rollback();
                }
                catch(SQLException held) { connection.rollback(); throw new AssertionError("A PostgreSQL transaction or grant row lock spanned provider I/O",held); }
            }
            fixture.mediaProviderRelease.countDown();
            responses=awaitBoth(left,right);
        }
        assertThat(responses).hasSize(2);
        assertThat(responses.get(1)).as("both callers converge on one credential and one expiry").isEqualTo(responses.get(0));
        String credential=responses.get(0).credential();
        assertThat(credential).startsWith("media-credential-").isEqualTo("media-credential-"+digest(eventId));
        assertThat(responses).allSatisfy(response -> assertThat(response.expiresAt()).isEqualTo(Instant.parse(bound)));
        assertThat(fixture.credentialCalls).as("both callers reached the deterministic provider").hasValue(2);
        assertThat(fixture.semanticCredentials).as("exactly one semantic credential issued").hasValue(1);
        assertThat(fixture.issuedCredentials).hasSize(1).containsKey(eventId);
        assertThat(fixture.credentialRequests).as("identical provider idempotency input").hasSize(1);
        var providerRequest=fixture.credentialRequests.get(eventId);
        assertThat(providerRequest.eventId()).isEqualTo(eventId);
        assertThat(providerRequest.shareGrantId()).isEqualTo(fixture.id);
        assertThat(providerRequest.shareAdmissionId()).isEqualTo(admission.admissionId());
        assertThat(providerRequest.purpose()).isEqualTo("PLAYBACK");
        assertThat(providerRequest.eventType()).isEqualTo("PLAYBACK_AUTHORIZED");
        assertThat(providerRequest.requestKeyDigest()).isEqualTo(digest(key));
        assertThat(providerRequest.codeGeneration()).isEqualTo(witness.payload().path("codeGeneration").asInt());
        assertThat(providerRequest.revocationGeneration()).isEqualTo(witness.payload().path("revocationGeneration").asInt());
        assertThat(providerRequest.grantExpiresAt()).isEqualTo(Instant.parse(witness.payload().path("grantExpiresAt").asString()));
        assertThat(providerRequest.admissionExpiresAt()).isEqualTo(Instant.parse(witness.payload().path("admissionExpiresAt").asString()));
        assertThat(providerRequest.sessionExpiresAt()).isEqualTo(Instant.parse(witness.payload().path("sessionExpiresAt").asString()));
        assertThat(providerRequest.credentialExpiresAt()).as("witness bound, never a fresh clock sample").isEqualTo(Instant.parse(bound));
        assertThat(providerRequest.preProviderFingerprint()).isEqualTo(mediaFingerprint(witness.payload(),fixture.id,admission.admissionId(),"PLAYBACK_AUTHORIZED","PLAYBACK",key));
        assertThat(providerRequest.toString()).isEqualTo("[REDACTED]");
        assertThat(fixture.mediaProviderTransactions).as("no transaction is open during provider I/O").containsExactly(false);
        assertThat(fixture.mediaBackendPids).as("two independent PostgreSQL connections served the callers").hasSizeGreaterThanOrEqualTo(2);
        assertThat(fixture.mediaBackendPids).as("the lock probe used its own backend").doesNotContain(probePid);
        assertThat(fixture.count("sharing.share_access_event")).as("one append for two calls").isEqualTo(events+1);
        assertThat(fixture.eventCount("PLAYBACK_AUTHORIZED")).isEqualTo(1);
        for(String type:List.of("SEEK_AUTHORIZED","RENEWED","FAILURE")) assertThat(fixture.eventCount(type)).as(type).isZero();
        var durable=fixture.event("PLAYBACK_AUTHORIZED");
        assertThat(durable.eventId()).as("frozen success event identity").isEqualTo(eventId);
        assertThat(durable.result()).isEqualTo("SUCCESS");
        assertMediaSuccessFields(durable.payload());
        assertThat(durable.payloadSha256()).isEqualTo(digest(canonical(durable.payload())));
        assertThat(durable.payload().path("credentialExpiresAt").asString()).as("first durable expiry is authoritative").isEqualTo(bound);
        assertThat(durable.payload().path("preProviderFingerprint").asString()).isEqualTo(providerRequest.preProviderFingerprint());
        assertNoMediaSecrets(durable.payload().toString(),credential,key);
        assertNoMediaPayloadIdentityFacts(durable.payload());
        assertThat(mediaAdmissionState()).as("admission and grant rows are untouched").isEqualTo(state);
        assertThat(fixture.count("sharing.share_admission")).isEqualTo(1);
    }

    @Test void providerUnavailableOrMalformedAppendsFailureWithoutSuccessAndRetrySucceeds() throws Exception {
        var admission=view(fixture.admit("provider-failure-admission"));
        var witness=fixture.event("ADMISSION_GRANTED");
        var token=new ShareAdmissionCapabilityToken(fixture.token);
        var state=mediaAdmissionState();
        long events=fixture.count("sharing.share_access_event");
        Map<String,String> scenarios=new LinkedHashMap<>();
        scenarios.put("UNAVAILABLE","MEDIA_CREDENTIAL_UNAVAILABLE");
        scenarios.put("THROWN","MEDIA_CREDENTIAL_UNAVAILABLE");
        scenarios.put("MALFORMED","MEDIA_CREDENTIAL_MALFORMED");
        scenarios.put("BLANK","MEDIA_CREDENTIAL_MALFORMED");
        for(String mode:List.of("OVER_LENGTH","NUL","UNPAIRED_SURROGATE","NULL_EXPIRY","PAST_EXPIRY"))
            scenarios.put(mode,"MEDIA_CREDENTIAL_MALFORMED");
        scenarios.put("BEYOND_BOUND","MEDIA_CREDENTIAL_MALFORMED");
        int providerCalls=0;
        for(Map.Entry<String,String> scenario:scenarios.entrySet()) {
            String mode=scenario.getKey(); String reason=scenario.getValue(); String key="failure-"+mode;
            for(int attempt=0;attempt<2;attempt++) {
                fixture.mediaProviderMode=mode;
                var denial=catchThrowable(() -> fixture.mediaService.authorize(token,admission.admissionId(),mediaRequest(key,MediaAuthorizationRequest.Purpose.PLAYBACK),null));
                fixture.mediaProviderMode=null;
                assertThat(denial).as(mode+"#"+attempt).isInstanceOf(ShareAdmissionException.class);
                assertThat(((ShareAdmissionException)denial).code()).as(mode+"#"+attempt).isEqualTo(ShareAdmissionException.Code.DEPENDENCY_UNAVAILABLE);
                var response=new MockHttpServletResponse();
                new CustomerProblemWriter().write(response,ShareAdmissionException.Code.DEPENDENCY_UNAVAILABLE,UUID.randomUUID());
                assertThat(response.getStatus()).as(mode+"#"+attempt).isEqualTo(503);
                assertThat(JSON.readTree(response.getContentAsString()).path("code").asString()).isEqualTo("DEPENDENCY_UNAVAILABLE");
                providerCalls++;
                assertThat(fixture.credentialCalls).as(mode+"#"+attempt).hasValue(providerCalls);
                assertThat(mediaEventCount("FAILURE",key)).as(mode+"#"+attempt+" appends one deterministic failure").isEqualTo(1);
                assertThat(mediaEventCount("PLAYBACK_AUTHORIZED",key)).as(mode+"#"+attempt+" claims no success").isZero();
                assertThat(fixture.issuedCredentials).as(mode+"#"+attempt).isEmpty();
                assertThat(fixture.semanticCredentials).as(mode+"#"+attempt).hasValue(0);
            }
            var failure=mediaEventByTypeAndKey("FAILURE",key);
            JsonNode payload=failure.payload();
            assertThat(failure.eventId()).as("frozen failure identity with eventType=FAILURE").isEqualTo("share-access:"+digest(JSON.writeValueAsString(mediaFailureIdentity(fixture.id,admission.admissionId(),key))));
            assertThat(failure.eventType()).isEqualTo("FAILURE");
            assertThat(failure.result()).isEqualTo("FAILED");
            assertMediaFailureFields(payload);
            assertThat(payload.path("shareGrantId").asString()).isEqualTo(fixture.id.toString());
            assertThat(payload.path("shareAdmissionId").asString()).isEqualTo(admission.admissionId().toString());
            assertThat(payload.path("eventType").asString()).isEqualTo("FAILURE");
            assertThat(payload.path("purpose").asString()).isEqualTo("PLAYBACK");
            assertThat(payload.path("requestKeyDigest").asString()).isEqualTo(digest(key));
            assertThat(payload.path("result").asString()).isEqualTo("FAILED");
            assertThat(payload.path("reasonCode").asString()).isEqualTo(reason).isIn(MEDIA_FAILURE_REASONS);
            assertThat(payload.path("preProviderFingerprint").asString()).isEqualTo(mediaFingerprint(witness.payload(),fixture.id,admission.admissionId(),"PLAYBACK_AUTHORIZED","PLAYBACK",key));
            assertThat(payload.path("grantExpiresAt").asString()).isEqualTo(witness.payload().path("grantExpiresAt").asString());
            assertThat(payload.path("admissionExpiresAt").asString()).isEqualTo(witness.payload().path("admissionExpiresAt").asString());
            assertThat(payload.path("sessionExpiresAt").asString()).isEqualTo(witness.payload().path("sessionExpiresAt").asString());
            assertThat(payload.path("credentialExpiresAt").asString()).isEqualTo(mediaBoundText(witness.payload()));
            assertThat(failure.payloadSha256()).isEqualTo(digest(canonical(payload)));
            assertNoMediaSecrets(payload.toString(),key);
            assertNoMediaPayloadIdentityFacts(payload);
        }
        assertThat(fixture.eventCount("FAILURE")).isEqualTo(scenarios.size());
        for(String type:List.of("PLAYBACK_AUTHORIZED","SEEK_AUTHORIZED","RENEWED")) assertThat(fixture.eventCount(type)).as(type).isZero();
        assertThat(mediaAdmissionState()).as("provider outage never mutates admission state").isEqualTo(state);

        String retryKey="failure-UNAVAILABLE";
        String successId=mediaEventId(fixture.id,admission.admissionId(),"PLAYBACK_AUTHORIZED",retryKey);
        var retried=fixture.mediaService.authorize(token,admission.admissionId(),mediaRequest(retryKey,MediaAuthorizationRequest.Purpose.PLAYBACK),null);
        assertThat(retried.credential()).isEqualTo("media-credential-"+digest(successId));
        assertThat(retried.expiresAt()).isEqualTo(Instant.parse(mediaBoundText(witness.payload())));
        assertThat(mediaEventCount("PLAYBACK_AUTHORIZED",retryKey)).as("fresh retry after a same-purpose failure succeeds").isEqualTo(1);
        assertThat(mediaEventCount("FAILURE",retryKey)).isEqualTo(1);
        var success=mediaEventById(successId);
        assertThat(success.result()).isEqualTo("SUCCESS");
        assertMediaSuccessFields(success.payload());
        assertThat(success.payload().path("reasonCode").isMissingNode()).isTrue();
        assertThat(success.payloadSha256()).isEqualTo(digest(canonical(success.payload())));
        assertThat(fixture.semanticCredentials).hasValue(1);

        fixture.mediaProviderMode="UNAVAILABLE";
        var outage=catchThrowable(() -> fixture.mediaService.authorize(token,admission.admissionId(),mediaRequest(retryKey,MediaAuthorizationRequest.Purpose.PLAYBACK),null));
        fixture.mediaProviderMode=null;
        assertThat(outage).isInstanceOf(ShareAdmissionException.class);
        assertThat(((ShareAdmissionException)outage).code()).isEqualTo(ShareAdmissionException.Code.DEPENDENCY_UNAVAILABLE);
        assertThat(mediaEventCount("FAILURE",retryKey)).as("outage replay appends no later FAILURE").isEqualTo(1);
        assertThat(mediaEventCount("PLAYBACK_AUTHORIZED",retryKey)).isEqualTo(1);
        assertThat(fixture.eventCount("FAILURE")).as("no additional failure rows").isEqualTo(scenarios.size());
        assertThat(mediaEventById(successId)).as("first success fact is preserved").isEqualTo(success);

        var replay=fixture.mediaService.authorize(token,admission.admissionId(),mediaRequest(retryKey,MediaAuthorizationRequest.Purpose.PLAYBACK),null);
        assertThat(replay).isEqualTo(retried);
        assertThat(mediaEventById(successId)).isEqualTo(success);
        assertThat(mediaEventCount("PLAYBACK_AUTHORIZED",retryKey)).isEqualTo(1);
        assertThat(fixture.semanticCredentials).hasValue(1);
        assertThat(fixture.issuedCredentials).hasSize(1);
        assertThat(mediaAdmissionState()).isEqualTo(state);
        assertThat(fixture.count("sharing.share_access_event")).isEqualTo(events+scenarios.size()+1);
    }

    @Test void responseAndAccessPayloadRedactAllForbiddenMediaFacts() throws Exception {
        var admission=view(fixture.admit("redaction-admission"));
        var witness=fixture.event("ADMISSION_GRANTED");
        var beans=new StaticListableBeanFactory();
        beans.addBean("shareAdmissionService",fixture.service); beans.addBean("shareMediaAuthorizationService",fixture.mediaService);
        var writer=new CustomerProblemWriter();
        var controller=new PublicSharesController(beans.getBeanProvider(com.onecall.core.sharing.internal.publicquery.PublicShareQueryService.class),
                beans.getBeanProvider(com.onecall.core.customer_iam.internal.application.CustomerContextService.class),writer,
                beans.getBeanProvider(ShareAdmissionService.class),beans.getBeanProvider(ShareMediaAuthorizationService.class));
        var mvc=org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(controller)
                .addFilters(new CustomerBoundaryFilter(new CustomerBearerAuthenticator(beans.getBeanProvider(CustomerSessionVerifier.class)),writer)).build();
        String path="/api/v1/public/shares/"+fixture.token+"/admissions/"+admission.admissionId()+"/media-authorizations";
        String key="redaction-request-private";
        var ok=mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(path).servletPath(path)
                .contentType("application/json").content("{\"authorizationRequestId\":\""+key+"\",\"purpose\":\"PLAYBACK\"}")).andReturn().getResponse();
        assertThat(ok.getStatus()).isEqualTo(200);
        JsonNode body=JSON.readTree(ok.getContentAsString());
        assertThat(body.propertyNames()).containsExactlyInAnyOrder("credential","expiresAt");
        String credential=body.path("credential").asString();
        assertThat(credential).startsWith("media-credential-").isNotBlank();
        assertThat(Instant.parse(body.path("expiresAt").asString())).isEqualTo(Instant.parse(mediaBoundText(witness.payload())));
        for(String invalid:List.of("{\"authorizationRequestId\":\""+key+"\",\"purpose\":\"PLAYBACK\",\"objectKey\":\"private-object\"}","{}",
                "{\"authorizationRequestId\":\"\",\"purpose\":\"PLAYBACK\"}","{\"authorizationRequestId\":\""+key+"\",\"purpose\":\"private\"}")) {
            var rejected=mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(path).servletPath(path)
                    .contentType("application/json").content(invalid)).andReturn().getResponse();
            assertThat(rejected.getStatus()).as(invalid).isEqualTo(422);
            assertNoMediaSecrets(rejected.getContentAsString(),credential,key);
        }
        fixture.mediaProviderMode="UNAVAILABLE";
        var outage=mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(path).servletPath(path)
                .contentType("application/json").content("{\"authorizationRequestId\":\"outage-request-private\",\"purpose\":\"SEEK\"}")).andReturn().getResponse();
        fixture.mediaProviderMode=null;
        assertThat(outage.getStatus()).isEqualTo(503);
        assertThat(JSON.readTree(outage.getContentAsString()).path("code").asString()).isEqualTo("DEPENDENCY_UNAVAILABLE");
        assertNoMediaSecrets(outage.getContentAsString(),credential,key,"outage-request-private");

        var renewed=fixture.mediaService.authorize(new ShareAdmissionCapabilityToken(fixture.token),admission.admissionId(),
                mediaRequest("renew-request-private",MediaAuthorizationRequest.Purpose.RENEW),null);
        assertThat(renewed.toString()).as("response record is redacted").isEqualTo("[REDACTED]");
        assertThat(renewed.credential()).isNotEqualTo(credential);
        var success=fixture.event("PLAYBACK_AUTHORIZED");
        assertMediaSuccessFields(success.payload());
        assertThat(success.payload().path("credentialExpiresAt").asString()).isEqualTo(mediaBoundText(witness.payload()));
        assertNoMediaSecrets(success.payload().toString(),credential,renewed.credential(),key,"renew-request-private","outage-request-private");
        assertNoMediaPayloadIdentityFacts(success.payload());
        var renewedEvent=mediaEventByTypeAndKey("RENEWED","renew-request-private");
        assertThat(renewedEvent.eventId()).as("frozen success identity with eventType=RENEWED").isEqualTo(mediaEventId(fixture.id,admission.admissionId(),"RENEWED","renew-request-private"));
        assertMediaSuccessFields(renewedEvent.payload());
        assertNoMediaSecrets(renewedEvent.payload().toString(),credential,renewed.credential(),"renew-request-private");
        assertNoMediaPayloadIdentityFacts(renewedEvent.payload());
        fixture.mediaProviderMode="MALFORMED";
        assertThat(catchThrowable(() -> fixture.mediaService.authorize(new ShareAdmissionCapabilityToken(fixture.token),admission.admissionId(),
                mediaRequest("failure-request-private",MediaAuthorizationRequest.Purpose.RENEW),null))).isInstanceOf(ShareAdmissionException.class);
        fixture.mediaProviderMode=null;
        var failure=mediaEventByTypeAndKey("FAILURE","failure-request-private");
        assertThat(failure.eventId()).as("frozen failure identity with eventType=FAILURE").isEqualTo(mediaEventId(fixture.id,admission.admissionId(),"FAILURE","failure-request-private"));
        assertMediaFailureFields(failure.payload());
        assertThat(failure.result()).isEqualTo("FAILED");
        assertThat(failure.payload().path("reasonCode").asString()).isIn(MEDIA_FAILURE_REASONS);
        assertNoMediaSecrets(failure.payload().toString(),credential,renewed.credential(),"failure-request-private");
        assertNoMediaPayloadIdentityFacts(failure.payload());
        assertNoMediaSecrets(mediaDurableState(),credential,renewed.credential(),key,"renew-request-private","outage-request-private","failure-request-private");
        assertThat(fixture.credentialRequests).hasSize(2);
        assertThat(fixture.issuedCredentials).hasSize(2);
        assertThat(fixture.credentialRequests.values()).allSatisfy(value -> assertThat(value.toString()).isEqualTo("[REDACTED]"));
        assertThat(fixture.issuedCredentials.values()).allSatisfy(value -> assertThat(value.toString()).isEqualTo("[REDACTED]"));
    }

    @Test void mediaAuthorizationDoesNotChangeAdmissionCountRevisionOrSession() throws Exception {
        var admission=view(fixture.admit("mutation-admission"));
        var witness=fixture.event("ADMISSION_GRANTED");
        var token=new ShareAdmissionCapabilityToken(fixture.token);
        String bound=mediaBoundText(witness.payload());
        assertThat(Instant.parse(witness.payload().path("sessionExpiresAt").asString())).isEqualTo(admission.sessionExpiresAt());
        assertThat(Instant.parse(witness.payload().path("admissionExpiresAt").asString())).isEqualTo(admission.admissionExpiresAt());
        String grantBefore=mediaTableState("sharing.evidence_share_grant");
        String admissionBefore=mediaTableState("sharing.share_admission");
        String eventsBefore=mediaTableState("sharing.share_access_event");
        var ticketBefore=fixture.event("TICKET_ISSUED");
        long usedBefore=fixture.scalar("SELECT admissions_used FROM sharing.evidence_share_grant WHERE id=?",fixture.id);
        long revisionBefore=fixture.scalar("SELECT revision FROM sharing.evidence_share_grant WHERE id=?",fixture.id);
        long codeBefore=fixture.scalar("SELECT code_generation FROM sharing.evidence_share_grant WHERE id=?",fixture.id);
        long revocationBefore=fixture.scalar("SELECT revocation_generation FROM sharing.evidence_share_grant WHERE id=?",fixture.id);
        long admissionsBefore=fixture.count("sharing.share_admission");
        long grantsBefore=fixture.scalar("SELECT count(*) FROM sharing.evidence_share_grant");
        long eventRowsBefore=fixture.count("sharing.share_access_event");
        List<ShareAdmissionView.MediaAuthorization> issued=new ArrayList<>();
        List<String> appended=new ArrayList<>();
        for(var purpose:MediaAuthorizationRequest.Purpose.values()) {
            String key="mutation-"+purpose;
            String eventId=mediaEventId(fixture.id,admission.admissionId(),purposeEventName(purpose),key);
            appended.add(eventId);
            for(int call=0;call<2;call++) {
                var result=fixture.mediaService.authorize(token,admission.admissionId(),mediaRequest(key,purpose),null);
                assertThat(result.credential()).as(key+"#"+call).isEqualTo("media-credential-"+digest(eventId));
                assertThat(result.expiresAt()).as(key+"#"+call).isEqualTo(Instant.parse(bound));
                issued.add(result);
                assertThat(mediaEventCount(purposeEventName(purpose),key)).as(key+"#"+call).isEqualTo(1);
            }
        }
        fixture.mediaProviderMode="UNAVAILABLE";
        assertThat(catchThrowable(() -> fixture.mediaService.authorize(token,admission.admissionId(),mediaRequest("mutation-failure",MediaAuthorizationRequest.Purpose.SEEK),null)))
                .isInstanceOf(ShareAdmissionException.class);
        fixture.mediaProviderMode=null;
        appended.add(mediaEventId(fixture.id,admission.admissionId(),"FAILURE","mutation-failure"));
        assertThat(catchThrowable(() -> fixture.mediaService.authorize(token,UUID.randomUUID(),mediaRequest("mutation-denied",MediaAuthorizationRequest.Purpose.PLAYBACK),null)))
                .isInstanceOf(ShareAdmissionException.class);
        var credentials=issued.stream().map(ShareAdmissionView.MediaAuthorization::credential).distinct().toList();
        assertThat(credentials).as("one credential per purpose across authorizations and replays").hasSize(MediaAuthorizationRequest.Purpose.values().length);
        for(int call=0;call<issued.size();call+=2) assertThat(issued.get(call+1)).as("same-purpose replay converges").isEqualTo(issued.get(call));
        for(String credential:credentials) assertThat(fixture.issuedCredentials.values()).anySatisfy(value -> assertThat(value.credential()).isEqualTo(credential));
        assertThat(mediaTableState("sharing.evidence_share_grant")).as("grant row is unchanged by authorization and replay").isEqualTo(grantBefore);
        assertThat(mediaTableState("sharing.share_admission")).as("admission row is unchanged").isEqualTo(admissionBefore);
        assertThat(mediaEventStateExcept(appended)).as("durable events are append-only").isEqualTo(eventsBefore);
        assertThat(fixture.event("TICKET_ISSUED")).isEqualTo(ticketBefore);
        assertThat(fixture.event("ADMISSION_GRANTED")).isEqualTo(witness);
        assertThat(fixture.count("sharing.share_admission")).isEqualTo(admissionsBefore).isEqualTo(1);
        assertThat(fixture.scalar("SELECT admissions_used FROM sharing.evidence_share_grant WHERE id=?",fixture.id)).isEqualTo(usedBefore);
        assertThat(fixture.scalar("SELECT revision FROM sharing.evidence_share_grant WHERE id=?",fixture.id)).isEqualTo(revisionBefore);
        assertThat(fixture.scalar("SELECT code_generation FROM sharing.evidence_share_grant WHERE id=?",fixture.id)).isEqualTo(codeBefore);
        assertThat(fixture.scalar("SELECT revocation_generation FROM sharing.evidence_share_grant WHERE id=?",fixture.id)).isEqualTo(revocationBefore);
        assertThat(fixture.scalar("SELECT count(*) FROM sharing.evidence_share_grant")).as("no grant is added or removed").isEqualTo(grantsBefore);
        assertThat(fixture.count("sharing.share_access_event")).as("only the event append changed").isEqualTo(eventRowsBefore+appended.size());
        assertThat(Instant.parse(mediaEventById(appended.getFirst()).payload().path("sessionExpiresAt").asString())).isEqualTo(admission.sessionExpiresAt());
        assertThat(Instant.parse(mediaEventById(appended.getFirst()).payload().path("admissionExpiresAt").asString())).isEqualTo(admission.admissionExpiresAt());
        assertThat(fixture.issuedCredentials).hasSize(MediaAuthorizationRequest.Purpose.values().length);
        assertThat(fixture.semanticCredentials).hasValue(MediaAuthorizationRequest.Purpose.values().length);
    }

    private static String purposeEventName(MediaAuthorizationRequest.Purpose purpose) {
        return switch(purpose) { case PLAYBACK -> "PLAYBACK_AUTHORIZED"; case SEEK -> "SEEK_AUTHORIZED"; case RENEW -> "RENEWED"; };
    }

    private static Map<String,Object> mediaFailureIdentity(UUID grantId,UUID admissionId,String requestKey) {
        Map<String,Object> identity=new TreeMap<>();
        identity.put("shareGrantId",grantId.toString());
        identity.put("shareAdmissionId",admissionId.toString());
        identity.put("eventType","FAILURE");
        identity.put("requestKeyDigest",digest(requestKey));
        return identity;
    }

    private static String mediaEventId(UUID grantId,UUID admissionId,String eventType,String requestKey) {
        Map<String,Object> identity=new TreeMap<>();
        identity.put("shareGrantId",grantId.toString());
        identity.put("shareAdmissionId",admissionId.toString());
        identity.put("eventType",eventType);
        identity.put("requestKeyDigest",digest(requestKey));
        return "share-access:"+digest(JSON.writeValueAsString(identity));
    }

    private static String mediaFingerprint(JsonNode witness,UUID grantId,UUID admissionId,String eventType,String purpose,String requestKey) {
        Map<String,Object> fields=new TreeMap<>();
        fields.put("shareGrantId",grantId.toString());
        fields.put("shareAdmissionId",admissionId.toString());
        fields.put("eventType",eventType);
        fields.put("purpose",purpose);
        fields.put("requestKeyDigest",digest(requestKey));
        fields.put("codeGeneration",witness.path("codeGeneration").asInt());
        fields.put("revocationGeneration",witness.path("revocationGeneration").asInt());
        fields.put("grantExpiresAt",witness.path("grantExpiresAt").asString());
        fields.put("admissionExpiresAt",witness.path("admissionExpiresAt").asString());
        fields.put("sessionExpiresAt",witness.path("sessionExpiresAt").asString());
        return digest(JSON.writeValueAsString(fields));
    }

    private static String mediaBoundText(JsonNode witness) {
        return List.of(witness.path("grantExpiresAt").asString(),witness.path("admissionExpiresAt").asString(),witness.path("sessionExpiresAt").asString())
                .stream().min(java.util.Comparator.comparing(Instant::parse)).orElseThrow();
    }

    private static void assertMediaSuccessFields(JsonNode payload) {
        assertThat(payload.propertyNames()).containsExactlyInAnyOrder("shareGrantId","shareAdmissionId","eventType","purpose","requestKeyDigest",
                "codeGeneration","revocationGeneration","grantExpiresAt","admissionExpiresAt","sessionExpiresAt","credentialExpiresAt","result","preProviderFingerprint");
    }

    private static void assertMediaFailureFields(JsonNode payload) {
        assertThat(payload.propertyNames()).containsExactlyInAnyOrder("shareGrantId","shareAdmissionId","eventType","purpose","requestKeyDigest",
                "result","reasonCode","preProviderFingerprint","grantExpiresAt","admissionExpiresAt","sessionExpiresAt","credentialExpiresAt");
    }

    private void assertNoMediaSecrets(String text,String... mediaFacts) {
        Set<String> forbidden=new LinkedHashSet<>(List.of(fixture.token,"token-private","code-private","proof-private","credential-private",
                "share-ticket:","http://","https://","objectKey","object_key","bucket","cos:","redis","Redis","signature","SecretKey","q-sign-algorithm",
                "x-cos-","accessKey","Authorization","Bearer",BASE.password,BASE.login,BASE.deniedLogin,"media-credential-"));
        forbidden.addAll(Arrays.asList(mediaFacts));
        for(String value:forbidden) assertThat(text).as("forbidden media fact "+value).doesNotContain(value);
    }

    private void assertNoMediaPayloadIdentityFacts(JsonNode payload) {
        String text=payload.toString();
        for(String value:List.of(BASE.evidence.toString(),fixture.subject.toString(),fixture.binding.toString()))
            assertThat(text).as("forbidden identity fact "+value).doesNotContain(value);
    }

    private String mediaEventStateExcept(List<String> appended) throws Exception {
        var sql=new StringBuilder("SELECT coalesce(jsonb_agg(to_jsonb(state) ORDER BY state.id),'[]'::jsonb)::text FROM sharing.share_access_event state WHERE state.share_grant_id=?");
        if(!appended.isEmpty()) sql.append(" AND state.event_id <> ALL(ARRAY[").append(String.join(",",Collections.nCopies(appended.size(),"?"))).append("]::text[])");
        try(Connection connection=BASE.admin(); var statement=connection.prepareStatement(sql.toString())) {
            statement.setObject(1,fixture.id);
            for(int index=0;index<appended.size();index++) statement.setString(index+2,appended.get(index));
            try(var rows=statement.executeQuery()) { assertThat(rows.next()).isTrue(); return rows.getString(1); }
        }
    }

    private String mediaDurableState() throws Exception {
        return mediaTableState("sharing.evidence_share_grant")+mediaTableState("sharing.share_admission")+mediaTableState("sharing.share_access_event");
    }

    private long mediaEventCount(String type,String requestKey) throws Exception {
        return fixture.scalar("SELECT count(*) FROM sharing.share_access_event WHERE share_grant_id=? AND event_type=? AND payload_redacted->>'requestKeyDigest'=?",fixture.id,type,digest(requestKey));
    }

    private AccessEventRow mediaEventById(String eventId) throws Exception {
        return mediaEvent(" AND event_domain='SHARE_ACCESS' AND event_id=?",eventId);
    }

    private AccessEventRow mediaEventByTypeAndKey(String type,String requestKey) throws Exception {
        return mediaEvent(" AND event_domain='SHARE_ACCESS' AND event_type=? AND payload_redacted->>'requestKeyDigest'=?",type,digest(requestKey));
    }

    private AccessEventRow mediaEvent(String predicate,Object... values) throws Exception {
        String description=predicate+Arrays.toString(values);
        try(Connection connection=BASE.admin(); var statement=connection.prepareStatement("SELECT event_id,event_type,payload_redacted,payload_sha256,result FROM sharing.share_access_event WHERE share_grant_id=?"+predicate)) {
            statement.setObject(1,fixture.id);
            for(int index=0;index<values.length;index++) statement.setObject(index+2,values[index]);
            try(var rows=statement.executeQuery()) {
                assertThat(rows.next()).as("durable event "+description).isTrue();
                var event=new AccessEventRow(rows.getString(1),rows.getString(2),JSON.readTree(rows.getString(3)),rows.getString(4),rows.getString(5));
                assertThat(rows.next()).as("single durable event "+description).isFalse();
                return event;
            }
        }
    }

    private static <T> List<T> awaitBoth(Future<T> left,Future<T> right) throws Exception {
        try { return List.of(left.get(20,TimeUnit.SECONDS),right.get(20,TimeUnit.SECONDS)); }
        catch(java.util.concurrent.ExecutionException failed) { throw new AssertionError("Concurrent media authorization failed",failed.getCause()); }
    }

}
