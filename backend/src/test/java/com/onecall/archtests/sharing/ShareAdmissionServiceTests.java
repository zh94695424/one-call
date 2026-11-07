package com.onecall.archtests.sharing;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.onecall.core.sharing.internal.admission.*;
import com.onecall.core.sharing.internal.admission.ShareAdmissionRepository.*;
import com.onecall.core.sharing.internal.publicadmission.*;
import com.onecall.core.customer_iam.internal.web.CustomerProblemWriter;
import com.onecall.core.sharing.internal.persistence.ShareAdmissionPersistenceConfiguration;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.*;
import tools.jackson.databind.json.JsonMapper;

class ShareAdmissionServiceTests {
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Test void canonicalAdmissionKeyUsesExactUtf8AndNoRawRequestId() {
        Fixture fixture = new Fixture();
        String requestId = " exact-请求-é ";
        String key = fixture.service.admissionKey(fixture.id, requestId);
        assertThat(key).hasSize(74).startsWith("admission:").doesNotContain(requestId);
        assertThat(key).isEqualTo("admission:" + digest(JSON.writeValueAsString(new TreeMap<>(Map.of("shareGrantId", fixture.id.toString(), "admissionRequestId", requestId))).getBytes(StandardCharsets.UTF_8)));
        assertThat(fixture.service.admissionKey(fixture.id, requestId.strip())).isNotEqualTo(key);
        var token = new ShareAdmissionCapabilityToken(requestId);
        byte[] bytes = token.utf8(); bytes[0] = 0;
        assertThat(token.utf8()).isEqualTo(requestId.getBytes(StandardCharsets.UTF_8));
        assertThat(token.toString()).isEqualTo("[REDACTED]");
        for (String invalid : List.of("", "a".repeat(513), "\u0000", "\ud800")) assertThatThrownBy(() -> new ShareAdmissionCapabilityToken(invalid)).isInstanceOf(ShareAdmissionException.class);
    }
    @Test void closedAdmissionBinderRejectsUnknownDuplicateTrailingAndWrongBranchFields() {
        var binder = new ShareAdmissionJsonBinder();
        String valid = "{\"admissionRequestId\":\"key\",\"verification\":{\"mode\":\"NONE\"}}";
        assertThat(binder.bind(valid.getBytes(StandardCharsets.UTF_8)).verification()).isInstanceOf(ShareAdmissionRequest.Verification.None.class);
        for (String invalid : List.of(valid + "{}", valid.replace("\"key\"", "\"key\",\"admissionRequestId\":\"other\""),
                valid.replace("\"NONE\"", "\"NONE\",\"code\":\"private\""), valid.replace("\"NONE\"", "\"EXTRACTION_CODE\""),
                valid.replace("\"key\"", "null"), "null", "[]", "{")) {
            assertThatThrownBy(() -> binder.bind(invalid.getBytes(StandardCharsets.UTF_8))).isInstanceOfSatisfying(ShareAdmissionException.class, exception -> assertThat(exception.code()).isEqualTo(ShareAdmissionException.Code.VALIDATION_FAILED));
        }
    }
    @Test void verificationModesMapToFrozenResultsAndSecretsNeverEnterViews() {
        for (var verification : List.of(new ShareAdmissionRequest.Verification.None(), new ShareAdmissionRequest.Verification.ExtractionCode("code-private"),
                new ShareAdmissionRequest.Verification.DesignatedMobile("proof-private"), new ShareAdmissionRequest.Verification.OrganizationMember())) {
            assertThat(verification.toString()).isEqualTo("[REDACTED]");
            assertThat(new ShareAdmissionRequest("request-private", verification).toString()).isEqualTo("[REDACTED]");
        }
        assertThat(new TrustedShareAdmissionCaller(UUID.randomUUID(), UUID.randomUUID()).toString()).isEqualTo("[REDACTED]");
        Fixture fixture = new Fixture();
        assertThat(fixture.admit()).isInstanceOf(ShareAdmissionService.AdmissionCommitted.class);
        assertThat(fixture.row.verificationResult()).isEqualTo("NOT_REQUIRED");
        assertThat(fixture.events.values()).extracting(AccessEventInsert::eventType).containsExactly("ADMISSION_GRANTED", "TICKET_ISSUED");
        Fixture denied = new Fixture();
        denied.grant = new GrantSnapshot(denied.id, "synthetic", UUID.randomUUID(), denied.account, null, "ACTIVE", "EXTRACTION_CODE",
                NOW.minusSeconds(1), NOW.plusSeconds(600), 0, 0, 1, null, 0, "salt", "hash", denied.subject, null, false,
                JSON.readTree("{\"verification\":{\"mode\":\"EXTRACTION_CODE\"}}"));
        denied.codeResult = new ShareAdmissionCodeVerifier.Denied("CODE_INVALID");
        int priorBegins = denied.transactions.history.size();
        denied.service.admit(new ShareAdmissionCapabilityToken("token-private"),
                new ShareAdmissionRequest("denied-proof", new ShareAdmissionRequest.Verification.ExtractionCode("secret")), Optional.empty());
        assertThat(denied.events.values()).extracting(AccessEventInsert::eventType).containsExactly("VERIFICATION_FAILED");
        assertThat(denied.transactions.history.subList(priorBegins, denied.transactions.history.size()))
                .containsExactly("begin", "commit", "begin", "commit", "begin", "commit");
        assertThat(denied.row).isNull();
        verify(denied.repository, never()).updateGrantAdmissionCount(any(), anyLong(), anyLong());
    }
    @Test void policyAndProviderAbsenceFailClosedWithoutWrites() {
        Fixture fixture = new Fixture();
        fixture.policy = new ShareAdmissionPolicyProvider.Unavailable();
        denied(fixture.admit(), ShareAdmissionException.Code.SHARE_SESSION_POLICY_UNAVAILABLE);
        verify(fixture.repository, never()).insertAdmission(any());
        assertThat(fixture.events).isEmpty();
        fixture.policy = new ShareAdmissionPolicyProvider.ApprovedTicketPolicy(Duration.ZERO, Duration.ofSeconds(30));
        denied(fixture.admit(), ShareAdmissionException.Code.SHARE_SESSION_POLICY_UNAVAILABLE);
        try (var context = new org.springframework.context.annotation.AnnotationConfigApplicationContext()) {
            context.register(ShareAdmissionPersistenceConfiguration.class); context.refresh();
            assertThat(context.getBeansOfType(ShareAdmissionService.class)).isEmpty();
            assertThat(context.getBeansOfType(javax.sql.DataSource.class)).isEmpty();
        }
        for (boolean commands : List.of(false, true)) {
            for (boolean reverse : List.of(false, true)) {
                Class<?>[] configurations = reverse
                        ? new Class<?>[]{com.onecall.core.sharing.internal.persistence.PublicSharePersistenceConfiguration.class,
                            com.onecall.core.sharing.internal.persistence.SharingCommandPersistenceConfiguration.class, ShareAdmissionPersistenceConfiguration.class}
                        : new Class<?>[]{ShareAdmissionPersistenceConfiguration.class,
                            com.onecall.core.sharing.internal.persistence.SharingCommandPersistenceConfiguration.class,
                            com.onecall.core.sharing.internal.persistence.PublicSharePersistenceConfiguration.class};
                new org.springframework.boot.test.context.runner.ApplicationContextRunner()
                        .withInitializer(context -> {
                            context.getEnvironment().setActiveProfiles("persistence");
                            context.addBeanFactoryPostProcessor(factory -> {
                                for (String name : commands ? List.of("sharingAdmissionDataSource", "sharingCommandDataSource", "publicShareQueryDataSource") : List.of("sharingAdmissionDataSource"))
                                    factory.registerSingleton(name, mock(com.zaxxer.hikari.HikariDataSource.class));
                                if (commands) {
                                    factory.registerSingleton("shareableEvidenceProjectionReader", mock(com.onecall.core.evidence.shareprojection.ShareableEvidenceProjectionReader.class));
                                    factory.registerSingleton("publicShareableEvidenceProjectionReader", mock(com.onecall.core.evidence.shareprojection.ShareableEvidenceProjectionReader.class));
                                }
                            });
                        }).withUserConfiguration(configurations)
                        .withPropertyValues("onecall.sharing-admission.datasource.enabled=true", "onecall.sharing-command.enabled=" + commands,
                                "onecall.sharing-admission.datasource.url=jdbc:synthetic:admission", "onecall.sharing-admission.datasource.username=sharing_admission_role", "onecall.sharing-admission.datasource.password=synthetic",
                                "onecall.sharing-command.datasource.url=jdbc:synthetic:command", "onecall.sharing-command.datasource.username=synthetic_command", "onecall.sharing-command.datasource.password=synthetic",
                                "onecall.sharing-public-read.datasource.url=jdbc:synthetic:public", "onecall.sharing-public-read.datasource.username=sharing_public_read_role", "onecall.sharing-public-read.datasource.password=synthetic")
                        .run(context -> {
                            assertThat(context).hasNotFailed();
                            assertThat(context).hasBean("shareAdmissionService").hasBean("sharingAdmissionSha256Provider");
                            var admissionSha = context.getBean("sharingAdmissionSha256Provider", ShareAdmissionSha256Provider.class);
                            assertThat(admissionSha).isNotInstanceOf(com.onecall.core.sharing.internal.provider.Sha256Provider.class);
                            byte[] exact = " exact-请求 ".getBytes(StandardCharsets.UTF_8);
                            assertThat(admissionSha.digest(exact)).isEqualTo(digest(exact));
                            if (commands) {
                                assertThat(context).hasBean("sha256Provider").hasBean("customerShareCommandService").hasBean("publicShareQueryService");
                                assertThat(context.getBean("sha256Provider")).isInstanceOf(com.onecall.core.sharing.internal.provider.Sha256Provider.class);
                            } else {
                                assertThat(context).doesNotHaveBean("sha256Provider").doesNotHaveBean("customerShareCommandService").doesNotHaveBean("publicShareQueryService").doesNotHaveBean("sharingCommandDataSource");
                            }
                            assertThat(context.getBean(ShareAdmissionPolicyProvider.class).ticketPolicy(new ShareAdmissionPolicyProvider.SharePolicyRequest(UUID.randomUUID(),"NONE")))
                                    .isInstanceOf(ShareAdmissionPolicyProvider.Unavailable.class);
                            for (String name : commands ? List.of("sharingAdmissionDataSource", "sharingCommandDataSource", "publicShareQueryDataSource") : List.of("sharingAdmissionDataSource"))
                                verify(context.getBean(name, com.zaxxer.hikari.HikariDataSource.class), never()).getConnection();
                        });
            }
        }
        assertThatThrownBy(() -> new ShareAdmissionPersistenceConfiguration.Properties("jdbc:test", "sharing_command_role", "synthetic"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageNotContaining("synthetic");
        assertThat(new ShareAdmissionPersistenceConfiguration.Properties("jdbc:test", "sharing_admission_role", "synthetic").toString()).isEqualTo("[REDACTED]");
    }
    @Test void existingAdmissionReplaysBeforeLimitWithoutVerificationOrRecount() {
        Fixture fixture = new Fixture(); fixture.admit();
        var original = fixture.row; var witness = fixture.witness;
        fixture.grant = fixture.grant("LOCKED", 1L, 1, NOW.plusSeconds(600));
        fixture.policyCalls.set(0);
        assertThat(fixture.admit()).isInstanceOf(ShareAdmissionService.ReplayCommitted.class);
        assertThat(fixture.row).isEqualTo(original); assertThat(fixture.witness).isEqualTo(witness);
        assertThat(fixture.policyCalls).hasValue(1);
        verify(fixture.repository, times(1)).insertAdmission(any()); verify(fixture.repository, times(1)).updateGrantAdmissionCount(any(), anyLong(), anyLong());
        fixture.witness = null; fixture.open = true;
        denied(fixture.admit(), ShareAdmissionException.Code.SHARE_RISK_BLOCKED);
        fixture.witness = witness; fixture.open = false;
        Fixture malformed = new Fixture();
        malformed.grant = new GrantSnapshot(malformed.id, "synthetic", UUID.randomUUID(), malformed.account, null, "ACTIVE", "NONE",
                NOW.minusSeconds(1), NOW.plusSeconds(600), 0, 0, 1, 1L, 1, null, null, malformed.subject, null, true,
                JSON.readTree("{\"verification\":{\"mode\":\"NONE\"}}"));
        denied(malformed.admit(), ShareAdmissionException.Code.DEPENDENCY_UNAVAILABLE);
        assertThat(malformed.row).isNull();
        verify(malformed.repository, never()).insertAdmission(any());
        verify(malformed.repository, never()).updateGrantAdmissionCount(any(), anyLong(), anyLong());
    }
    @Test void expiredExistingAdmissionNeverRenews() {
        Fixture fixture = new Fixture(); fixture.admit();
        var eventBaseline = new LinkedHashMap<>(fixture.events);
        fixture.now = NOW.plusSeconds(120); fixture.policyCalls.set(0);
        AtomicInteger clockReads = new AtomicInteger();
        when(fixture.repository.now()).thenAnswer(invocation -> clockReads.incrementAndGet() == 1 ? NOW.plusSeconds(119) : NOW.plusSeconds(121));
        denied(fixture.admit(), ShareAdmissionException.Code.SHARE_SESSION_EXPIRED);
        assertThat(fixture.policyCalls).hasValue(0);
        assertThat(fixture.events).isEqualTo(eventBaseline);
        assertThat(fixture.events.values()).extracting(AccessEventInsert::eventType).containsExactly("ADMISSION_GRANTED", "TICKET_ISSUED");
        verify(fixture.repository, times(1)).insertAdmission(any());
    }
    @Test void ticketMaterialIsValidatedAndCredentialFailureClaimsNoSuccess() {
        Fixture fixture = new Fixture(); fixture.credential = new ShareMediaCredentialProvider.Issued("", NOW.plusSeconds(10));
        denied(fixture.admit(), ShareAdmissionException.Code.DEPENDENCY_UNAVAILABLE);
        assertThat(fixture.events.values()).extracting(AccessEventInsert::eventType).doesNotContain("TICKET_ISSUED");
        assertThat(fixture.events.values()).filteredOn(event -> event.eventType().equals("TICKET_FAILED")).singleElement()
                .satisfies(event -> assertThat(event.payload().path("reasonCode").asString()).isEqualTo("MEDIA_CREDENTIAL_MALFORMED"));
        Fixture ended = new Fixture(); ended.providerDelay = Duration.ofSeconds(121);
        denied(ended.admit(), ShareAdmissionException.Code.SHARE_SESSION_EXPIRED);
        assertThat(ended.events.values()).filteredOn(event -> event.eventType().equals("TICKET_FAILED")).singleElement()
                .satisfies(event -> assertThat(event.payload().path("reasonCode").asString()).isEqualTo("TICKET_WINDOW_EXPIRED"));
        verify(ended.repository, times(1)).insertAdmission(any());
        Fixture outage = new Fixture(); outage.credential = new ShareMediaCredentialProvider.Unavailable();
        denied(outage.admit(), ShareAdmissionException.Code.DEPENDENCY_UNAVAILABLE);
        assertThat(outage.events.values()).filteredOn(event -> event.eventType().equals("TICKET_FAILED")).singleElement()
                .satisfies(event -> assertThat(event.payload().path("reasonCode").asString()).isEqualTo("MEDIA_CREDENTIAL_UNAVAILABLE"));
    }
    @Test void ticketFailureIsRedactedAndSameKeyReconstructs() {
        Fixture fixture = new Fixture(); fixture.ticket = new ShareAdmissionTicketProvider.Unavailable();
        denied(fixture.admit(), ShareAdmissionException.Code.DEPENDENCY_UNAVAILABLE);
        fixture.ticket = new ShareAdmissionTicketProvider.Written();
        assertThat(fixture.admit()).isInstanceOf(ShareAdmissionService.ReplayCommitted.class);
        assertThat(fixture.events.values()).filteredOn(event -> event.eventType().equals("TICKET_FAILED")).singleElement().satisfies(event -> {
            assertThat(event.eventId()).startsWith("ticket-failed:");
            assertThat(event.payload().propertyNames()).containsExactlyInAnyOrder("shareGrantId", "admissionId", "admissionKeyDigest", "reasonCode");
            assertThat(event.payload().toString()).doesNotContain("request-private", "credential-private");
        });
        fixture.policyCalls.set(0); fixture.policy = new ShareAdmissionPolicyProvider.Unavailable();
        var events = new LinkedHashMap<>(fixture.events);
        denied(fixture.admit(), ShareAdmissionException.Code.SHARE_SESSION_POLICY_UNAVAILABLE);
        assertThat(fixture.policyCalls).hasValue(1); assertThat(fixture.events).isEqualTo(events);
        fixture.policy = new ShareAdmissionPolicyProvider.ApprovedTicketPolicy(null, Duration.ofSeconds(5));
        assertThat(fixture.admit()).isInstanceOf(ShareAdmissionService.ReplayCommitted.class);
        assertThat(fixture.row.admittedAt()).isEqualTo(NOW);
        fixture.open = true; fixture.policyCalls.set(0);
        denied(fixture.admit(), ShareAdmissionException.Code.SHARE_RISK_BLOCKED);
        assertThat(fixture.policyCalls).hasValue(0);
    }
    @Test void problemCodesPreserveFrozenStatusPrecedence() throws Exception {
        int[] statuses = {401,403,422,403,403,403,404,410,410,409,409,409,410,503,503,429,503,500};
        String[] titles = {"Authentication required","Access denied","Request validation failed","Share verification failed","Share risk blocked","Share tenant mismatch","Invalid share token","Share revoked","Share expired","Share locked","Share admission limit reached","Share generation stale","Share session expired","Share session policy unavailable","Dependency unavailable","Too many requests","Service unavailable","Internal error"};
        var codes = ShareAdmissionException.Code.values(); assertThat(codes).hasSize(18);
        for (int index = 0; index < codes.length; index++) {
            var response = new MockHttpServletResponse(); UUID trace = UUID.randomUUID(); new CustomerProblemWriter().write(response, codes[index], trace);
            var json = JSON.readTree(response.getContentAsByteArray());
            assertThat(json.propertyNames()).containsExactlyInAnyOrder("type", "title", "status", "code", "traceId");
            assertThat(response.getStatus()).isEqualTo(statuses[index]); assertThat(json.path("title").asString()).isEqualTo(titles[index]);
            assertThat(json.path("type").asString()).isEqualTo("about:blank"); assertThat(json.path("traceId").asString()).isEqualTo(trace.toString());
            assertThat(response.getHeader("Cache-Control")).isEqualTo("no-store"); assertThat(response.getHeader("Pragma")).isEqualTo("no-cache");
            assertThat(response.getHeader("X-Content-Type-Options")).isEqualTo("nosniff");
        }
        assertThat(Arrays.stream(com.onecall.core.customer_iam.internal.web.PublicSharesController.class.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(org.springframework.web.bind.annotation.ExceptionHandler.class))
                .flatMap(method -> Arrays.stream(method.getAnnotation(org.springframework.web.bind.annotation.ExceptionHandler.class).value())))
                .contains(ShareAdmissionException.class);
        Fixture fixture = new Fixture(); fixture.grant = fixture.grant("REVOKED", 0L, 0, NOW.minusSeconds(1)); fixture.open = true;
        denied(fixture.admit(), ShareAdmissionException.Code.SHARE_REVOKED);
        fixture.grant = fixture.grant("LOCKED", 0L, 0, NOW.plusSeconds(600));
        denied(fixture.admit(), ShareAdmissionException.Code.SHARE_RISK_BLOCKED);
    }
    @Test void organizationMemberRequiresTrustedCurrentSessionOnly() {
        Fixture fixture = new Fixture(); fixture.memberMode = true; fixture.grant = fixture.grant("ACTIVE", null, 0, NOW.plusSeconds(600));
        denied(fixture.admit(), ShareAdmissionException.Code.UNAUTHENTICATED);
        verify(fixture.repository, never()).insertAdmission(any());
        var request = new ShareAdmissionRequest("request-private", new ShareAdmissionRequest.Verification.OrganizationMember());
        var caller = Optional.of(new TrustedShareAdmissionCaller(fixture.account, fixture.organization));
        assertThat(fixture.service.admit(new ShareAdmissionCapabilityToken("token-private"), request, caller)).isInstanceOf(ShareAdmissionService.AdmissionCommitted.class);
        assertThat(fixture.witness.recipientAccountId()).isEqualTo(fixture.account);
        assertThat(fixture.witness.payload().propertyNames()).hasSize(8);
    }
    @Test void wrongTenantAndRiskFailuresCannotProduceSuccess() {
        Fixture invalid = new Fixture();
        denied(invalid.service.admit(new ShareAdmissionCapabilityToken("token-private"), new ShareAdmissionRequest("", new ShareAdmissionRequest.Verification.None()), Optional.empty()), ShareAdmissionException.Code.VALIDATION_FAILED);
        assertThat(invalid.events).isEmpty();
        verify(invalid.repository, never()).insertAdmission(any());
        verify(invalid.repository, never()).updateGrantAdmissionCount(any(), anyLong(), anyLong());
        Fixture fixture = new Fixture(); fixture.open = true; fixture.grant = fixture.grant("ACTIVE", 0L, 0, NOW.plusSeconds(600));
        denied(fixture.admit(), ShareAdmissionException.Code.SHARE_RISK_BLOCKED);
        verify(fixture.repository, never()).insertAdmission(any());
        fixture.open = false;
        when(fixture.repository.readSubjectOwnership(any())).thenReturn(Optional.of(new SubjectOwnership(fixture.subject, UUID.randomUUID(), null)));
        denied(fixture.admit(), ShareAdmissionException.Code.DEPENDENCY_UNAVAILABLE);
        verify(fixture.repository, never()).insertAdmission(any());
        Fixture wrongTenant = new Fixture();
        wrongTenant.memberMode = true;
        wrongTenant.grant = wrongTenant.grant("ACTIVE", null, 0, NOW.plusSeconds(600));
        denied(wrongTenant.service.admit(new ShareAdmissionCapabilityToken("token-private"), new ShareAdmissionRequest("request-private", new ShareAdmissionRequest.Verification.OrganizationMember()),
                Optional.of(new TrustedShareAdmissionCaller(wrongTenant.account, UUID.randomUUID()))), ShareAdmissionException.Code.SHARE_WRONG_TENANT);
    }
    @Test void responseProjectionContainsOnlyFrozenPublicFields() {
        Fixture fixture = new Fixture(); var outcome = (ShareAdmissionService.AdmissionCommitted) fixture.admit();
        var view = outcome.view(); assertThat(view.toString()).isEqualTo("[REDACTED]"); assertThat(view.mediaAuthorization().toString()).isEqualTo("[REDACTED]");
        assertThat(JSON.valueToTree(view).propertyNames()).containsExactlyInAnyOrder("admissionId","verificationMode","verificationResult","admittedAt","sessionExpiresAt","admissionExpiresAt","mediaAuthorization");
        assertThat(JSON.valueToTree(view.mediaAuthorization()).propertyNames()).containsExactlyInAnyOrder("credential","expiresAt");
        assertThat(view.verificationResult()).isEqualTo(fixture.row.verificationResult());
    }

    private static String digest(byte[] bytes) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
        catch (Exception failure) { throw new AssertionError("Synthetic digest unavailable"); }
    }
    private static void denied(ShareAdmissionService.AdmissionOutcome outcome, ShareAdmissionException.Code code) {
        assertThat(outcome).isEqualTo(new ShareAdmissionService.DeniedCommitted(code));
    }
    private static final class Transactions extends AbstractPlatformTransactionManager {
        final List<String> history = new ArrayList<>();
        @Override protected Object doGetTransaction() { return new Object(); }
        @Override protected void doBegin(Object transaction, TransactionDefinition definition) { history.add("begin"); }
        @Override protected void doCommit(DefaultTransactionStatus status) { history.add("commit"); }
        @Override protected void doRollback(DefaultTransactionStatus status) { }
    }
    private static final class Fixture {
        final UUID id = UUID.randomUUID(), subject = UUID.randomUUID(), binding = UUID.randomUUID(), account = UUID.randomUUID(), organization = UUID.randomUUID();
        final ShareAdmissionRepository repository = mock(ShareAdmissionRepository.class);
        final Map<String, AccessEventInsert> events = new LinkedHashMap<>();
        final AtomicInteger policyCalls = new AtomicInteger();
        final Transactions transactions = new Transactions();
        Instant now = NOW; boolean open; boolean memberMode;
        GrantSnapshot grant = grant("ACTIVE", null, 0, NOW.plusSeconds(600));
        AdmissionRow row; AdmissionWitness witness;
        ShareAdmissionPolicyProvider.Result policy = new ShareAdmissionPolicyProvider.ApprovedTicketPolicy(Duration.ofSeconds(120), Duration.ofSeconds(30));
        ShareMediaCredentialProvider.Result credential; Duration providerDelay = Duration.ZERO;
        ShareAdmissionTicketProvider.Result ticket = new ShareAdmissionTicketProvider.Written();
        ShareAdmissionCodeVerifier.Result codeResult;
        final DefaultShareAdmissionService service;
        Fixture() {
            when(repository.findGrantByTokenDigest(any())).thenAnswer(invocation -> Optional.of(grant));
            when(repository.lockGrant(any())).thenAnswer(invocation -> Optional.of(grant));
            when(repository.now()).thenAnswer(invocation -> now);
            when(repository.findAdmission(any(), any())).thenAnswer(invocation -> Optional.ofNullable(row));
            when(repository.findAdmissionWitness(any(), any())).thenAnswer(invocation -> Optional.ofNullable(witness));
            when(repository.readSubjectOwnership(any())).thenAnswer(invocation -> Optional.of(new SubjectOwnership(subject, memberMode ? null : account, memberMode ? organization : null)));
            when(repository.readCurrentBindings(any())).thenReturn(List.of(new BindingState(binding, subject, "ACTIVE", NOW, 1)));
            when(repository.readSubjectRisk(any(), any())).thenAnswer(invocation -> Optional.of(new RiskState(subject, List.of(binding), open ? List.of(new RiskRow(UUID.randomUUID(),"CONFLICT","OPEN",1)) : List.of())));
            when(repository.readCurrentOrganization(any())).thenReturn(Optional.of(new OrganizationState(organization,"ACTIVE")));
            when(repository.readCurrentMembership(any(), any())).thenReturn(Optional.of(new MembershipState(organization,account,"ACTIVE")));
            when(repository.readCurrentAccount(any())).thenReturn(Optional.of(new AccountState(account,"ACTIVE")));
            when(repository.insertAdmission(any())).thenAnswer(invocation -> {
                AdmissionInsert insert = invocation.getArgument(0); row = new AdmissionRow(insert.id(),insert.grantId(),insert.admissionKey(),insert.admissionNo(),insert.verificationMode(),insert.verificationResult(),insert.admittedAt()); return row;
            });
            when(repository.updateGrantAdmissionCount(any(),anyLong(),anyLong())).thenReturn(1);
            when(repository.insertAccessEvent(any())).thenAnswer(invocation -> {
                AccessEventInsert insert = invocation.getArgument(0); var first = events.putIfAbsent(insert.eventId(),insert); var value = first == null ? insert : first;
                if (insert.eventType().equals("ADMISSION_GRANTED")) {
                    var payload=insert.payload(); witness = new AdmissionWitness(insert.eventId(),payload,insert.payloadSha256(),payload.path("verificationMode").asString(),0,0,
                            Instant.parse(payload.path("grantExpiresAt").asString()),Instant.parse(payload.path("admissionExpiresAt").asString()),Instant.parse(payload.path("sessionExpiresAt").asString()),memberMode?account:null,memberMode?organization:null);
                }
                return new EventWriteResult(first == null ? EventWriteStatus.INSERTED : first.payloadSha256().equals(insert.payloadSha256()) ? EventWriteStatus.REUSED_IDENTICAL : EventWriteStatus.CONFLICT,
                        new AccessEventRow(value.eventId(),value.eventType(),value.payload(),value.payloadSha256(),value.result()));
            });
            var template = new TransactionTemplate(transactions); template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            service = new DefaultShareAdmissionService(template,repository,ShareAdmissionServiceTests::digest,request -> {
                assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse(); policyCalls.incrementAndGet(); return policy;
            },request -> { if (codeResult != null) return codeResult; throw new AssertionError("Unexpected code proof"); },request -> { throw new AssertionError("Unexpected mobile proof"); },request -> {
                assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
                var result = credential == null ? new ShareMediaCredentialProvider.Issued("credential-private",request.expiresAt()) : credential;
                now = now.plus(providerDelay); return result;
            },request -> { assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse(); assertThat(request.ttl()).isPositive(); return ticket; });
        }
        GrantSnapshot grant(String status, Long limit, long used, Instant expiry) {
            return new GrantSnapshot(id,"synthetic",UUID.randomUUID(),account,memberMode?organization:null,status,memberMode?"ORGANIZATION_MEMBER":"NONE",NOW.minusSeconds(1),expiry,0,0,1,limit,used,null,null,subject,null,false,JSON.readTree("{\"verification\":{\"mode\":\"NONE\"}}"));
        }
        ShareAdmissionService.AdmissionOutcome admit() { return service.admit(new ShareAdmissionCapabilityToken("token-private"),new ShareAdmissionRequest("request-private",memberMode?new ShareAdmissionRequest.Verification.OrganizationMember():new ShareAdmissionRequest.Verification.None()),Optional.empty()); }
    }
}
