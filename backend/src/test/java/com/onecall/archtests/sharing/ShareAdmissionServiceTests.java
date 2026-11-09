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
import java.util.concurrent.*;
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
        final List<String> history = new CopyOnWriteArrayList<>();
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
    @Test void mediaAuthorizationBinderAcceptsOnlyFrozenFieldsAndPurpose() {
        var binder = new ShareAdmissionJsonBinder();
        var request = binder.bindMediaAuthorization("{\"authorizationRequestId\":\"雪-é\",\"purpose\":\"SEEK\"}".getBytes(StandardCharsets.UTF_8));
        assertThat(request.authorizationRequestId()).isEqualTo("雪-é");
        assertThat(request.purpose()).isEqualTo(MediaAuthorizationRequest.Purpose.SEEK);
        for (String invalid : List.of("{\"authorizationRequestId\":\"x\",\"purpose\":\"PLAYBACK\",\"extra\":1}",
                "{\"authorizationRequestId\":\"x\",\"authorizationRequestId\":\"y\",\"purpose\":\"PLAYBACK\"}",
                "{\"authorizationRequestId\":\"x\",\"purpose\":\"OTHER\"}", "{\"authorizationRequestId\":\"x\",\"purpose\":\"PLAYBACK\"}{}"))
            assertThatThrownBy(() -> binder.bindMediaAuthorization(invalid.getBytes(StandardCharsets.UTF_8)))
                    .isInstanceOfSatisfying(ShareAdmissionException.class, e -> assertThat(e.code()).isEqualTo(ShareAdmissionException.Code.VALIDATION_FAILED));
    }

    @Test void mediaAuthorizationIdentityUsesExactUtf8DigestAndPurposeEventMapping() {
        for (var purpose : MediaAuthorizationRequest.Purpose.values()) {
            MediaFixture f = new MediaFixture();
            f.authorize("req-é", purpose);
            var request = f.issued.get(0);
            assertThat(request.requestKeyDigest()).isEqualTo(digest("req-é".getBytes(StandardCharsets.UTF_8)));
            assertThat(request.eventType()).isEqualTo(switch (purpose) { case PLAYBACK -> "PLAYBACK_AUTHORIZED"; case SEEK -> "SEEK_AUTHORIZED"; case RENEW -> "RENEWED"; });
            assertThat(request.eventId()).doesNotContain("req-é");
        }
        MediaFixture fractional = new MediaFixture(Instant.parse("2026-01-01T00:10:00.123Z"));
        fractional.authorize("fractional", MediaAuthorizationRequest.Purpose.PLAYBACK);
        assertThat(fractional.events).hasSize(1);
        var payload = fractional.events.get(0).payload();
        assertThat(payload.path("grantExpiresAt").asString()).isEqualTo("2026-01-01T00:10:00.123000Z");
        assertThat(payload.path("admissionExpiresAt").asString()).isEqualTo("2026-01-01T00:09:00.123000Z");
        assertThat(payload.path("sessionExpiresAt").asString()).isEqualTo("2026-01-01T00:08:00.123000Z");
        assertThat(payload.path("credentialExpiresAt").asString()).isEqualTo("2026-01-01T00:07:00.123000Z");
        assertThat(fractional.events.get(0).payloadSha256()).isEqualTo(digest(JSON.writeValueAsString(payload).getBytes(StandardCharsets.UTF_8)));
    }

    @Test void activeAndAdmittedLockedAuthorizeWithoutAdmissionMutation() {
        for (String status : List.of("ACTIVE", "LOCKED")) {
            MediaFixture f = new MediaFixture(); f.status = status;
            assertThat(f.authorize("a", MediaAuthorizationRequest.Purpose.PLAYBACK).credential()).isEqualTo("credential-private");
            assertThat(f.events).hasSize(1);
            verify(f.repository, never()).insertAdmission(any());
            verify(f.repository, never()).updateGrantAdmissionCount(any(), anyLong(), anyLong());
        }
    }

    @Test void sameAuthorizationRequestAndPurposeReplaysCredentialAndEvent() {
        for (boolean subMicrosecond : List.of(false, true)) {
            MediaFixture f = new MediaFixture();
            if (subMicrosecond) f.providerResult = new ShareMediaCredentialProvider.MediaAuthorizationCredentialResult.Issued(
                    "credential-private", f.witness.sessionExpiresAt().minusNanos(1), com.onecall.archtests.sharing.DeterministicShareCommandProviders.mediaReplayAnchor());
            Instant expectedExpiry = subMicrosecond ? Instant.parse("2026-01-01T00:07:59.999999Z") : NOW.plusSeconds(420);
            var first = f.authorize("same", MediaAuthorizationRequest.Purpose.PLAYBACK);
            assertThat(first.expiresAt()).isEqualTo(expectedExpiry);
            assertThat(f.events).hasSize(1);
            var success = f.events.get(0);
            assertThat(success.result()).isEqualTo("SUCCESS");
            assertThat(success.eventType()).isEqualTo("PLAYBACK_AUTHORIZED");
            assertThat(success.payload().path("credentialExpiresAt").asString()).isEqualTo(expectedExpiry.toString());
            assertMediaSuccess(f, "same", expectedExpiry);
            String firstPayload = JSON.writeValueAsString(success.payload());
            String firstHash = success.payloadSha256();
            var second = f.authorize("same", MediaAuthorizationRequest.Purpose.PLAYBACK);
            assertThat(second).isEqualTo(first);
            assertThat(f.events).containsExactly(success);
            assertThat(f.eventWrites).containsExactly(EventWriteStatus.INSERTED, EventWriteStatus.REUSED_IDENTICAL);
            assertThat(f.issued).hasSize(2);
            assertMediaSuccess(f, "same", expectedExpiry);
            assertThat(JSON.writeValueAsString(f.events.getFirst().payload())).isEqualTo(firstPayload);
            assertThat(f.events.getFirst().payloadSha256()).isEqualTo(firstHash);
            assertThat(f.issued.get(1).priorReplayAnchor()).isEqualTo(
                    ((ShareMediaCredentialProvider.MediaAuthorizationCredentialResult.Issued) (subMicrosecond ? f.providerResult : f.credentials.get(f.issued.getFirst().eventId()))).replayAnchor());
        }
    }

    @Test void sameAuthorizationRequestAcrossPurposesConflictsWithoutSecondCredentialOrEvent() throws Exception {
        MediaFixture f = new MediaFixture(); f.authorize("same", MediaAuthorizationRequest.Purpose.PLAYBACK);
        assertThatThrownBy(() -> f.authorize("same", MediaAuthorizationRequest.Purpose.SEEK))
                .isInstanceOfSatisfying(ShareAdmissionException.class, e -> assertThat(e.code()).isEqualTo(ShareAdmissionException.Code.SHARE_GENERATION_STALE));
        assertThat(f.issued).hasSize(1); assertThat(f.events).hasSize(1);

        MediaFixture changed = new MediaFixture();
        Map<String, Object> identity = new TreeMap<>(Map.of("shareGrantId", changed.grantId.toString(),
                "shareAdmissionId", changed.admission.toString(), "eventType", "PLAYBACK_AUTHORIZED",
                "requestKeyDigest", digest("same".getBytes(StandardCharsets.UTF_8))));
        Map<String, Object> fingerprint = new TreeMap<>(identity);
        fingerprint.put("purpose", "PLAYBACK"); fingerprint.put("codeGeneration", 0); fingerprint.put("revocationGeneration", 0);
        fingerprint.put("grantExpiresAt", changed.witness.grantExpiresAt().toString());
        fingerprint.put("admissionExpiresAt", changed.witness.admissionExpiresAt().toString());
        fingerprint.put("sessionExpiresAt", changed.witness.sessionExpiresAt().toString());
        fingerprint.put("credentialExpiresAt", changed.witness.sessionExpiresAt().toString());
        fingerprint.putAll(mediaFactFields(DeterministicShareCommandProviders.mediaFacts(changed.evidenceId)));
        String currentFingerprint = digest(JSON.writeValueAsString(fingerprint).getBytes(StandardCharsets.UTF_8));
        fingerprint.put("sessionExpiresAt", changed.witness.sessionExpiresAt().minusSeconds(1).toString());
        Map<String, Object> payload = new TreeMap<>(fingerprint);
        payload.put("preProviderFingerprint", digest(JSON.writeValueAsString(fingerprint).getBytes(StandardCharsets.UTF_8)));
        payload.put("result", "SUCCESS"); payload.put("credentialExpiresAt", changed.expires.minusSeconds(180).toString());
        var changedPayload = JSON.valueToTree(payload);
        assertThat(changedPayload.path("preProviderFingerprint").asString()).isNotEqualTo(currentFingerprint);
        AccessEventInsert prior = new AccessEventInsert(changed.grantId, changed.admission,
                "share-access:" + digest(JSON.writeValueAsString(identity).getBytes(StandardCharsets.UTF_8)),
                "PLAYBACK_AUTHORIZED", "SUCCESS", NOW.minusSeconds(1), changedPayload,
                digest(JSON.writeValueAsString(changedPayload).getBytes(StandardCharsets.UTF_8)));
        changed.events.add(prior);
        String priorPayload = JSON.writeValueAsString(prior.payload());
        var conflict = catchThrowableOfType(ShareAdmissionException.class,
                () -> changed.authorize("same", MediaAuthorizationRequest.Purpose.PLAYBACK));
        assertThat(conflict).isNotNull();
        assertThat(conflict.code()).isEqualTo(ShareAdmissionException.Code.SHARE_GENERATION_STALE);
        var problem = new MockHttpServletResponse();
        new CustomerProblemWriter().write(problem, conflict.code(), UUID.randomUUID());
        assertThat(problem.getStatus()).isEqualTo(409);
        assertThat(JSON.readTree(problem.getContentAsByteArray()).path("code").asString()).isEqualTo("SHARE_GENERATION_STALE");
        assertThat(changed.issued).isEmpty();
        assertThat(changed.events).containsExactly(prior);
        assertThat(changed.events.get(0)).isSameAs(prior);
        assertThat(JSON.writeValueAsString(changed.events.get(0).payload())).isEqualTo(priorPayload);
        assertThat(changed.events.get(0).payloadSha256()).isEqualTo(digest(priorPayload.getBytes(StandardCharsets.UTF_8)));
        assertThat(changed.eventWrites).isEmpty();
        verify(changed.repository, never()).insertAccessEvent(any());
        verify(changed.repository, never()).insertAdmission(any());
        verify(changed.repository, never()).updateGrantAdmissionCount(any(), anyLong(), anyLong());
        verify(changed.repository, never()).expireGrant(any(), anyLong());
    }

    @Test void organizationMemberReauthenticatesWitnessBeforeGrantRead() {
        MediaFixture f = new MediaFixture(); f.member = true; f.grant = f.makeGrant(); f.prepareMemberWitness();
        AtomicInteger resolutions = new AtomicInteger();
        f.service.authorize(new ShareAdmissionCapabilityToken("token-private"), f.admission,
                new MediaAuthorizationRequest("member", MediaAuthorizationRequest.Purpose.PLAYBACK), () -> {
                    resolutions.incrementAndGet(); f.trace.add("resolver");
                    return Optional.of(new TrustedShareAdmissionCaller(f.account, f.organization));
                });
        assertThat(resolutions).hasValue(1);
        assertThat(f.trace).containsExactly("discovery", "resolver", "org-lock", "membership-lock", "account-lock", "grant-lock");
        verify(f.repository).readCurrentMembership(f.organization, f.account);
        MediaFixture nonMember = new MediaFixture();
        AtomicInteger nonMemberResolutions = new AtomicInteger();
        nonMember.service.authorize(new ShareAdmissionCapabilityToken("token-private"), nonMember.admission,
                new MediaAuthorizationRequest("non-member", MediaAuthorizationRequest.Purpose.PLAYBACK), () -> {
                    nonMemberResolutions.incrementAndGet(); return Optional.empty();
                });
        assertThat(nonMemberResolutions).hasValue(0);
        assertThat(nonMember.trace).containsExactly("discovery", "grant-lock");
    }

    @Test void riskBindingGenerationAndTimeDenialsFailClosedWithoutMutation() {
        for (String denial : List.of("risk", "binding", "generation", "session")) {
            MediaFixture f = new MediaFixture(); f.denial = denial;
            var expected = switch (denial) {
                case "risk", "binding" -> ShareAdmissionException.Code.SHARE_RISK_BLOCKED;
                case "generation" -> ShareAdmissionException.Code.SHARE_GENERATION_STALE;
                case "session" -> ShareAdmissionException.Code.SHARE_SESSION_EXPIRED;
                default -> throw new AssertionError(denial);
            };
            assertThatThrownBy(() -> f.authorize("blocked", MediaAuthorizationRequest.Purpose.PLAYBACK)).as(denial)
                    .isInstanceOfSatisfying(ShareAdmissionException.class, failure -> assertThat(failure.code()).isEqualTo(expected));
            if (denial.equals("session")) {
                assertThat(f.repository.now()).isBefore(f.grant.expiresAt());
                assertThat(f.repository.now()).isAfterOrEqualTo(f.witness.sessionExpiresAt());
                assertThat(f.repository.now()).isAfterOrEqualTo(f.witness.admissionExpiresAt());
            }
            assertThat(f.issued).isEmpty(); assertThat(f.events).isEmpty();
            verify(f.repository, never()).insertAdmission(any()); verify(f.repository, never()).updateGrantAdmissionCount(any(), anyLong(), anyLong());
        }
        MediaFixture expiredStatus = new MediaFixture(); expiredStatus.status = "EXPIRED";
        assertThatThrownBy(() -> expiredStatus.authorize("expired-status", MediaAuthorizationRequest.Purpose.PLAYBACK))
                .isInstanceOfSatisfying(ShareAdmissionException.class, e -> assertThat(e.code()).isEqualTo(ShareAdmissionException.Code.SHARE_EXPIRED));
        assertThat(expiredStatus.issued).isEmpty(); assertThat(expiredStatus.events).isEmpty();
        for (String scenario : List.of("absent-session-bound", "malformed-session-bound", "epoch-session-bound")) {
            MediaFixture f = new MediaFixture();
            Instant admissionBound = f.witness.admissionExpiresAt();
            f.witness = new AdmissionWitness(f.witness.eventId(), f.witness.payload(), f.witness.payloadSha256(), f.witness.verificationMode(),
                    f.witness.codeGeneration(), f.witness.revocationGeneration(), f.witness.grantExpiresAt(), admissionBound,
                    scenario.equals("epoch-session-bound") ? Instant.EPOCH : null, null, null);
            when(f.repository.findAdmissionWitness(any(), any())).thenReturn(Optional.of(f.witness));
            assertThatThrownBy(() -> f.authorize("invalid-bound", MediaAuthorizationRequest.Purpose.PLAYBACK)).as(scenario)
                    .isInstanceOfSatisfying(ShareAdmissionException.class, e -> assertThat(e.code()).isEqualTo(ShareAdmissionException.Code.SHARE_SESSION_POLICY_UNAVAILABLE));
            assertThat(f.issued).isEmpty(); assertThat(f.events).isEmpty();
        }
    }

    @Test void concurrentSameKeyCallsConvergeAfterTransactionClosureAndCredentialNeverEntersEvent() throws Exception {
        MediaFixture f = new MediaFixture();
        CyclicBarrier start = new CyclicBarrier(3);
        f.providerBarrier = new CyclicBarrier(2);
        Callable<ShareAdmissionView.MediaAuthorization> authorize = () -> {
            start.await(10, TimeUnit.SECONDS);
            return f.service.authorize(new ShareAdmissionCapabilityToken("token-private"), f.admission,
                    new MediaAuthorizationRequest("parallel", MediaAuthorizationRequest.Purpose.RENEW), null);
        };
        ShareAdmissionView.MediaAuthorization first;
        ShareAdmissionView.MediaAuthorization second;
        try (ExecutorService callers = Executors.newFixedThreadPool(2)) {
            var firstCall = callers.submit(authorize);
            var secondCall = callers.submit(authorize);
            start.await(10, TimeUnit.SECONDS);
            first = firstCall.get(15, TimeUnit.SECONDS);
            second = secondCall.get(15, TimeUnit.SECONDS);
        }
        assertThat(first).isEqualTo(second); assertThat(f.events).hasSize(1);
        assertThat(first.credential()).isEqualTo("credential-private");
        assertThat(first.expiresAt()).isEqualTo(f.expires.minusSeconds(180));
        assertThat(second.credential()).isEqualTo(first.credential());
        assertThat(second.expiresAt()).isEqualTo(first.expiresAt());
        assertThat(f.issued).hasSize(2).containsOnly(f.issued.get(0));
        assertThat(f.credentials).hasSize(1).containsOnlyKeys(f.issued.get(0).eventId());
        assertThat(f.eventWrites).containsExactlyInAnyOrder(EventWriteStatus.INSERTED, EventWriteStatus.REUSED_IDENTICAL);
        assertThat(f.events.get(0).eventType()).isEqualTo("RENEWED");
        assertThat(f.events.get(0).result()).isEqualTo("SUCCESS");
        assertThat(f.events.get(0).payload().path("credentialExpiresAt").asString()).isEqualTo(first.expiresAt().toString());
        assertThat(f.providerOutsideTransaction).containsExactly(true, true);
        assertThat(f.events.get(0).payload().toString()).doesNotContain("credential-private");
        verify(f.repository, times(2)).findAccessEventsByRequestKeyDigest(eq(f.grantId), eq(f.admission), any());
        verify(f.repository, times(2)).insertAccessEvent(any());
        verify(f.repository, never()).insertAdmission(any());
        verify(f.repository, never()).updateGrantAdmissionCount(any(), anyLong(), anyLong());
    }

    @Test void providerFailureAppendsOnlyDeterministicFailureAndRetrySucceeds() throws Exception {
        Map<String, ShareMediaCredentialProvider.MediaAuthorizationCredentialResult.Issued> malformed = new LinkedHashMap<>();
        malformed.put("missing-anchor",new ShareMediaCredentialProvider.MediaAuthorizationCredentialResult.Issued("credential-private",NOW.plusSeconds(10),null));
        malformed.put("over-code-point-limit", new ShareMediaCredentialProvider.MediaAuthorizationCredentialResult.Issued("😀".repeat(2049), NOW.plusSeconds(10), com.onecall.archtests.sharing.DeterministicShareCommandProviders.mediaReplayAnchor()));
        malformed.put("nul", new ShareMediaCredentialProvider.MediaAuthorizationCredentialResult.Issued("credential\0private", NOW.plusSeconds(10), com.onecall.archtests.sharing.DeterministicShareCommandProviders.mediaReplayAnchor()));
        malformed.put("unpaired-surrogate", new ShareMediaCredentialProvider.MediaAuthorizationCredentialResult.Issued("credential\uD800", NOW.plusSeconds(10), com.onecall.archtests.sharing.DeterministicShareCommandProviders.mediaReplayAnchor()));
        malformed.put("null-expiry", new ShareMediaCredentialProvider.MediaAuthorizationCredentialResult.Issued("credential-private", null, com.onecall.archtests.sharing.DeterministicShareCommandProviders.mediaReplayAnchor()));
        malformed.put("past-expiry", new ShareMediaCredentialProvider.MediaAuthorizationCredentialResult.Issued("credential-private", NOW.minusNanos(1), com.onecall.archtests.sharing.DeterministicShareCommandProviders.mediaReplayAnchor()));
        malformed.put("equal-expiry", new ShareMediaCredentialProvider.MediaAuthorizationCredentialResult.Issued("credential-private", NOW, com.onecall.archtests.sharing.DeterministicShareCommandProviders.mediaReplayAnchor()));
        malformed.put("non-future-after-canonicalization", new ShareMediaCredentialProvider.MediaAuthorizationCredentialResult.Issued("credential-private", NOW.plusNanos(1), com.onecall.archtests.sharing.DeterministicShareCommandProviders.mediaReplayAnchor()));
        for (var scenario : malformed.entrySet()) {
            MediaFixture rejected = new MediaFixture(); rejected.providerResult = scenario.getValue();
            Map<String, Object> failureIdentity = new TreeMap<>(Map.of("shareGrantId", rejected.grantId.toString(),
                    "shareAdmissionId", rejected.admission.toString(), "eventType", "FAILURE",
                    "requestKeyDigest", digest(scenario.getKey().getBytes(StandardCharsets.UTF_8))));
            String failureId = "share-access:" + digest(JSON.writeValueAsString(failureIdentity).getBytes(StandardCharsets.UTF_8));
            for (int attempt = 0; attempt < 2; attempt++) {
                var denial = catchThrowableOfType(ShareAdmissionException.class,
                        () -> rejected.authorize(scenario.getKey(), MediaAuthorizationRequest.Purpose.PLAYBACK));
                assertThat(denial).as(scenario.getKey()).isNotNull();
                assertThat(denial.code()).isEqualTo(ShareAdmissionException.Code.DEPENDENCY_UNAVAILABLE);
                var problem = new MockHttpServletResponse();
                new CustomerProblemWriter().write(problem, denial.code(), UUID.randomUUID());
                assertThat(problem.getStatus()).isEqualTo(503);
                assertThat(JSON.readTree(problem.getContentAsByteArray()).path("code").asString()).isEqualTo("DEPENDENCY_UNAVAILABLE");
                assertThat(rejected.events).hasSize(1);
                assertThat(rejected.events.get(0).eventId()).isEqualTo(failureId);
                assertThat(rejected.events).extracting(AccessEventInsert::eventType).containsExactly("FAILURE");
                assertThat(rejected.events).extracting(AccessEventInsert::result).containsExactly("FAILED");
                assertThat(rejected.events.get(0).payload().path("reasonCode").asString()).isEqualTo("MEDIA_CREDENTIAL_MALFORMED");
                assertThat(rejected.credentials).isEmpty();
            }
            assertThat(rejected.issued).hasSize(2);
            assertThat(rejected.eventWrites).containsExactly(EventWriteStatus.INSERTED, EventWriteStatus.REUSED_IDENTICAL);
            verify(rejected.repository, never()).insertAdmission(any());
            verify(rejected.repository, never()).updateGrantAdmissionCount(any(), anyLong(), anyLong());
        }
        MediaFixture f = new MediaFixture(); f.throwProvider = true;
        assertThatThrownBy(() -> f.authorize("retry", MediaAuthorizationRequest.Purpose.PLAYBACK)).isInstanceOf(ShareAdmissionException.class);
        assertThat(f.events).hasSize(1);
        AccessEventInsert failure = f.events.get(0);
        assertThat(failure.eventType()).isEqualTo("FAILURE"); assertThat(failure.result()).isEqualTo("FAILED");
        assertThat(failure.payload().path("reasonCode").asString()).isEqualTo("MEDIA_CREDENTIAL_UNAVAILABLE");
        String requestKeyDigest = digest("retry".getBytes(StandardCharsets.UTF_8));
        Map<String, Object> identity = new TreeMap<>(Map.of("shareGrantId", f.grantId.toString(),
                "shareAdmissionId", f.admission.toString(), "eventType", "FAILURE", "requestKeyDigest", requestKeyDigest));
        assertThat(failure.eventId()).isEqualTo("share-access:" + digest(JSON.writeValueAsString(identity).getBytes(StandardCharsets.UTF_8)));
        Map<String, Object> fingerprint = new TreeMap<>(identity);
        fingerprint.put("eventType", "PLAYBACK_AUTHORIZED");
        fingerprint.put("purpose", "PLAYBACK");
        fingerprint.put("codeGeneration", 0);
        fingerprint.put("revocationGeneration", 0);
        fingerprint.put("grantExpiresAt", f.expires.toString());
        fingerprint.put("admissionExpiresAt", f.expires.minusSeconds(60).toString());
        fingerprint.put("sessionExpiresAt", f.expires.minusSeconds(120).toString());
        fingerprint.put("credentialExpiresAt", f.expires.minusSeconds(120).toString());
        fingerprint.putAll(mediaFactFields(DeterministicShareCommandProviders.mediaFacts(f.evidenceId)));
        Map<String, Object> payload = new TreeMap<>(identity);
        payload.put("purpose", "PLAYBACK");
        payload.put("result", "FAILED");
        payload.put("reasonCode", "MEDIA_CREDENTIAL_UNAVAILABLE");
        payload.put("preProviderFingerprint", digest(JSON.writeValueAsString(fingerprint).getBytes(StandardCharsets.UTF_8)));
        payload.put("grantExpiresAt", f.expires.toString());
        payload.put("admissionExpiresAt", f.expires.minusSeconds(60).toString());
        payload.put("sessionExpiresAt", f.expires.minusSeconds(120).toString());
        payload.put("credentialExpiresAt", f.expires.minusSeconds(120).toString());
        payload.putAll(mediaFactFields(DeterministicShareCommandProviders.mediaFacts(f.evidenceId)));
        assertThat(failure.payload()).isEqualTo(JSON.valueToTree(payload));
        assertThat(failure.payloadSha256()).isEqualTo(digest(JSON.writeValueAsString(payload).getBytes(StandardCharsets.UTF_8)));
        assertThat(failure.grantId()).isEqualTo(f.grantId);
        assertThat(failure.admissionId()).isEqualTo(f.admission);
        assertThat(failure.payload().toString()).doesNotContain("SUCCESS", "credential-private", "token-private", "retry");
        assertThat(f.events).extracting(AccessEventInsert::result).containsExactly("FAILED");
        f.throwProvider = false;
        assertThat(f.authorize("retry", MediaAuthorizationRequest.Purpose.PLAYBACK).credential()).isEqualTo("credential-private");
        assertThat(f.events).hasSize(2);
        assertThat(f.events).extracting(AccessEventInsert::eventType).containsExactly("FAILURE", "PLAYBACK_AUTHORIZED");
        assertThat(f.events).extracting(AccessEventInsert::result).containsExactly("FAILED", "SUCCESS");
        assertThat(f.providerOutsideTransaction).containsExactly(true, true);
        AccessEventInsert firstSuccess = f.events.get(1);
        String firstSuccessPayload = JSON.writeValueAsString(firstSuccess.payload());
        String firstSuccessHash = firstSuccess.payloadSha256();
        f.throwProvider = true;
        var outage = catchThrowableOfType(ShareAdmissionException.class,
                () -> f.authorize("retry", MediaAuthorizationRequest.Purpose.PLAYBACK));
        assertThat(outage).isNotNull();
        assertThat(outage.code()).isEqualTo(ShareAdmissionException.Code.DEPENDENCY_UNAVAILABLE);
        var response = new MockHttpServletResponse();
        new CustomerProblemWriter().write(response, outage.code(), UUID.randomUUID());
        assertThat(response.getStatus()).isEqualTo(503);
        assertThat(JSON.readTree(response.getContentAsByteArray()).path("code").asString()).isEqualTo("DEPENDENCY_UNAVAILABLE");
        assertThat(f.events).containsExactly(failure, firstSuccess);
        assertThat(f.events.get(1)).isSameAs(firstSuccess);
        assertThat(JSON.writeValueAsString(f.events.get(1).payload())).isEqualTo(firstSuccessPayload);
        assertThat(f.events.get(1).payloadSha256()).isEqualTo(firstSuccessHash);
        assertThat(f.events).extracting(AccessEventInsert::eventType).containsExactly("FAILURE", "PLAYBACK_AUTHORIZED");
        verify(f.repository, times(2)).insertAccessEvent(any());
        assertThat(f.eventWrites).containsExactly(EventWriteStatus.INSERTED, EventWriteStatus.INSERTED);
        assertThat(f.issued).hasSize(3);
        assertThat(f.issued.get(2).preProviderFingerprint()).isEqualTo(f.issued.get(1).preProviderFingerprint());
        assertThat(f.issued.get(2).credentialExpiresAt()).isEqualTo(Instant.parse(firstSuccess.payload().path("credentialExpiresAt").asString()));
        assertThat(f.providerOutsideTransaction).containsExactly(true, true, true);
    }

    @Test void resolvedMediaFactsBoundBeforeProviderAndCommittedExactly() {
        MediaFixture fixture = new MediaFixture();
        fixture.authorize("facts", MediaAuthorizationRequest.Purpose.PLAYBACK);
        var request = fixture.issued.getFirst();
        assertThat(request.mediaFacts()).isEqualTo(DeterministicShareCommandProviders.mediaFacts(fixture.evidenceId));
        var fingerprint = new TreeMap<String,Object>(mediaFactFields(request.mediaFacts()));
        fingerprint.putAll(Map.of("shareGrantId",fixture.grantId.toString(),"shareAdmissionId",fixture.admission.toString(),"eventType","PLAYBACK_AUTHORIZED",
                "purpose","PLAYBACK","requestKeyDigest",digest("facts".getBytes(StandardCharsets.UTF_8)),"codeGeneration",0,"revocationGeneration",0,
                "grantExpiresAt",fixture.expires.toString(),"admissionExpiresAt",fixture.expires.minusSeconds(60).toString(),"sessionExpiresAt",fixture.expires.minusSeconds(120).toString()));
        fingerprint.put("credentialExpiresAt",fixture.expires.minusSeconds(120).toString());
        assertThat(fingerprint).hasSize(19);
        assertThat(request.preProviderFingerprint()).isEqualTo(digest(JSON.writeValueAsString(fingerprint).getBytes(StandardCharsets.UTF_8)));
        var event = fixture.events.getFirst();
        assertThat(event.payload().size()).isEqualTo(24);
        for (var fact : mediaFactFields(request.mediaFacts()).entrySet()) assertThat(event.payload().path(fact.getKey())).isEqualTo(JSON.valueToTree(fact.getValue()));
        assertThat(event.payloadSha256()).isEqualTo(digest(JSON.writeValueAsString(event.payload()).getBytes(StandardCharsets.UTF_8)));
        assertThat(fixture.evidenceOutsideTransaction).containsExactly(true);
    }

    @Test void changedMediaFactsConflictBeforeReplayProvider() {
        for (int index = 0; index < 6; index++) {
            MediaFixture fixture = new MediaFixture(); fixture.authorize("drift", MediaAuthorizationRequest.Purpose.PLAYBACK);
            var facts = DeterministicShareCommandProviders.mediaFacts(fixture.evidenceId);
            assertThat(facts.windowStartNanos()).isZero();
            switch (index) {
                case 0 -> fixture.evidenceId = UUID.randomUUID();
                case 1 -> fixture.media = new com.onecall.core.evidence.mediaread.ResolvedMediaObject(UUID.randomUUID(),facts.sha256(),16,"audio/wav",3600);
                case 2 -> fixture.media = new com.onecall.core.evidence.mediaread.ResolvedMediaObject(facts.mediaObjectId(),"b".repeat(64),16,"audio/wav",3600);
                case 3 -> fixture.media = new com.onecall.core.evidence.mediaread.ResolvedMediaObject(facts.mediaObjectId(),facts.sha256(),17,"audio/wav",3600);
                case 4 -> fixture.media = new com.onecall.core.evidence.mediaread.ResolvedMediaObject(facts.mediaObjectId(),facts.sha256(),16,"audio/mpeg",3600);
                case 5 -> fixture.media = new com.onecall.core.evidence.mediaread.ResolvedMediaObject(facts.mediaObjectId(),facts.sha256(),16,"audio/wav",3601);
                default -> throw new AssertionError("Unexpected media fact mutation");
            }
            assertThatThrownBy(() -> fixture.authorize("drift", MediaAuthorizationRequest.Purpose.PLAYBACK))
                    .isInstanceOfSatisfying(ShareAdmissionException.class, failure -> assertThat(failure.code()).isEqualTo(ShareAdmissionException.Code.SHARE_GENERATION_STALE));
            assertThat(fixture.issued).hasSize(1); assertThat(fixture.events).hasSize(1);
            assertThat(fixture.eventWrites).containsExactly(EventWriteStatus.INSERTED);
            verify(fixture.repository, times(1)).insertAccessEvent(any());
        }
    }

    @Test void concurrentAnchorsConvergeOnFirstCommittedCredential() throws Exception {
        for(boolean missingWinningKey:List.of(false,true)) {
            MediaFixture fixture = new MediaFixture();
            AtomicInteger anchors = new AtomicInteger();
            var key=((com.onecall.core.sharing.internal.credential.MediaPlaybackSigningKeyProvider.Result.Available)
                    DeterministicShareCommandProviders.mediaPlaybackSigningKeys().currentKeys()).active().material();
            fixture.realProvider = request -> {
                int candidate=request.priorReplayAnchor()==null ? anchors.incrementAndGet():3;
                var beans = new org.springframework.beans.factory.support.DefaultListableBeanFactory();
                com.onecall.core.sharing.internal.credential.MediaPlaybackSigningKeyProvider provider=() -> {
                    var role=com.onecall.core.sharing.internal.credential.MediaPlaybackSigningKeyProvider.Role.ACTIVE;
                    var overlapRole=com.onecall.core.sharing.internal.credential.MediaPlaybackSigningKeyProvider.Role.VERIFY_ONLY;
                    var active=new com.onecall.core.sharing.internal.credential.MediaPlaybackSigningKeyProvider.Entry("K"+candidate,key,role);
                    return new com.onecall.core.sharing.internal.credential.MediaPlaybackSigningKeyProvider.Result.Available(active,
                            candidate<3 || missingWinningKey ? List.of():List.of(
                                    new com.onecall.core.sharing.internal.credential.MediaPlaybackSigningKeyProvider.Entry("K1",key,overlapRole),
                                    new com.onecall.core.sharing.internal.credential.MediaPlaybackSigningKeyProvider.Entry("K2",key,overlapRole)));
                };
                beans.registerSingleton("keys",provider);
                return new com.onecall.core.sharing.internal.credential.MediaPlaybackCredentialAuthority(
                        beans.getBeanProvider(com.onecall.core.sharing.internal.credential.MediaPlaybackSigningKeyProvider.class),
                        java.time.Clock.fixed(NOW.plusSeconds(candidate),java.time.ZoneOffset.UTC)).issueOrReplay(request);
            };
            fixture.providerBarrier = new CyclicBarrier(2);
            List<ShareAdmissionView.MediaAuthorization> returned=new ArrayList<>();
            List<Throwable> errors=new ArrayList<>();
            try (var pool = Executors.newFixedThreadPool(2)) {
                var first = pool.submit(() -> fixture.authorize("race",MediaAuthorizationRequest.Purpose.PLAYBACK));
                var second = pool.submit(() -> fixture.authorize("race",MediaAuthorizationRequest.Purpose.PLAYBACK));
                for(var future:List.of(first,second)) try { returned.add(future.get(15,TimeUnit.SECONDS)); }
                catch(java.util.concurrent.ExecutionException failure) { errors.add(failure.getCause()); }
            }
            assertThat(fixture.events).hasSize(1); assertThat(fixture.issued).hasSize(3);
            assertThat(fixture.eventWrites).containsExactlyInAnyOrder(EventWriteStatus.INSERTED,EventWriteStatus.CONFLICT);
            String winningKid=fixture.events.getFirst().payload().path("kid").asString();
            assertThat(returned).allSatisfy(result -> assertThat(result.credential()).startsWith("ocmp1."+winningKid+"."));
            if(missingWinningKey) {
                assertThat(returned).hasSize(1); assertThat(errors).singleElement().isInstanceOfSatisfying(ShareAdmissionException.class,
                        failure -> assertThat(failure.code()).isEqualTo(ShareAdmissionException.Code.DEPENDENCY_UNAVAILABLE));
            } else {
                assertThat(errors).isEmpty(); assertThat(returned).hasSize(2); assertThat(returned.getFirst()).isEqualTo(returned.getLast());
            }
        }
    }

    @Test void legacyOrUnavailableReplayMetadataNeverIssuesReplacement() {
        MediaFixture fixture = new MediaFixture(); fixture.authorize("legacy",MediaAuthorizationRequest.Purpose.PLAYBACK);
        var first=fixture.events.getFirst();
        var payload=JSON.readTree(first.payload().toString()); ((tools.jackson.databind.node.ObjectNode)payload).remove("kid");
        fixture.events.set(0,new AccessEventInsert(first.grantId(),first.admissionId(),first.eventId(),first.eventType(),first.result(),first.occurredAt(),payload,digest(JSON.writeValueAsString(payload).getBytes(StandardCharsets.UTF_8))));
        assertThatThrownBy(() -> fixture.authorize("legacy",MediaAuthorizationRequest.Purpose.PLAYBACK)).isInstanceOf(ShareAdmissionException.class);
        assertThat(fixture.issued).hasSize(1); assertThat(fixture.eventWrites).hasSize(1);
    }

    @Test void evidenceResolutionFailuresNeverInvokeProvider() {
        for (String fault : List.of("NULL", "MISSING", "UNAVAILABLE", "THROWN", "ZERO_READERS", "MULTIPLE_READERS", "PRIMARY_READERS",
                "NULL_READER", "THROWING_PROVIDER", "NULL_MEDIA", "NULL_EVIDENCE_ID", "INVALID_EVIDENCE_UUID", "NULL_OBJECT_ID", "INVALID_OBJECT_UUID",
                "NULL_HASH", "INVALID_HASH", "UPPERCASE_HASH", "ZERO_SIZE", "NEGATIVE_SIZE", "NULL_MIME", "BLANK_MIME",
                "DURATION", "ZERO_DURATION", "NEGATIVE_DURATION", "WINDOW_OVERFLOW")) {
            MediaFixture fixture = new MediaFixture(); fixture.evidenceFault=fault;
            var grant = fixture.grant;
            var witness = fixture.witness;
            var admission = fixture.repository.findAdmissionById(fixture.grantId, fixture.admission);
            assertThatThrownBy(() -> fixture.authorize("recover",MediaAuthorizationRequest.Purpose.PLAYBACK))
                    .isInstanceOfSatisfying(ShareAdmissionException.class, failure -> {
                        assertThat(failure.code()).isEqualTo(ShareAdmissionException.Code.DEPENDENCY_UNAVAILABLE);
                        assertThat(failure.toString()).doesNotContain("private-evidence-canary", "token-private", "recover");
                    });
            assertThat(fixture.events).isEmpty(); assertThat(fixture.issued).isEmpty(); verify(fixture.repository,never()).insertAccessEvent(any());
            assertThat(fixture.eventWrites).isEmpty();
            assertThat(fixture.evidenceOutsideTransaction).containsExactly(true);
            assertThat(fixture.trace).containsExactly("discovery", "grant-lock");
            assertThat(fixture.grant).isEqualTo(grant); assertThat(fixture.witness).isSameAs(witness);
            assertThat(fixture.repository.findAdmissionById(fixture.grantId, fixture.admission)).isEqualTo(admission);
            verify(fixture.repository, never()).insertAdmission(any());
            verify(fixture.repository, never()).updateGrantAdmissionCount(any(), anyLong(), anyLong());
            fixture.evidenceFault="";
            var first = fixture.authorize("recover",MediaAuthorizationRequest.Purpose.PLAYBACK);
            assertThat(fixture.issued).hasSize(1);
            assertThat(fixture.issued.getFirst().priorReplayAnchor()).isNull();
            verify(fixture.repository, times(1)).insertAccessEvent(any());
            assertMediaSuccess(fixture, "recover", first.expiresAt());
            var success = fixture.events.getFirst();
            String payload = JSON.writeValueAsString(success.payload());
            String hash = success.payloadSha256();
            var anchor = fixture.credentials.get(fixture.issued.getFirst().eventId()).replayAnchor();
            assertThat(fixture.authorize("recover",MediaAuthorizationRequest.Purpose.PLAYBACK)).isEqualTo(first);
            assertThat(fixture.issued).hasSize(2);
            assertThat(fixture.issued.get(1).priorReplayAnchor()).isEqualTo(anchor);
            assertThat(fixture.credentials.get(fixture.issued.get(1).eventId()).replayAnchor()).isEqualTo(anchor);
            assertMediaSuccess(fixture, "recover", first.expiresAt());
            assertThat(fixture.events).containsExactly(success);
            assertThat(JSON.writeValueAsString(success.payload())).isEqualTo(payload);
            assertThat(success.payloadSha256()).isEqualTo(hash);
            assertThat(fixture.eventWrites).containsExactly(EventWriteStatus.INSERTED, EventWriteStatus.REUSED_IDENTICAL);
            verify(fixture.repository, times(2)).insertAccessEvent(any());
            assertThat(fixture.evidenceOutsideTransaction).containsExactly(true, true, true);
            assertThat(fixture.providerOutsideTransaction).containsExactly(true, true);
            assertThat(fixture.grant).isEqualTo(grant); assertThat(fixture.witness).isSameAs(witness);
            assertThat(fixture.repository.findAdmissionById(fixture.grantId, fixture.admission)).isEqualTo(admission);
            verify(fixture.repository, never()).insertAdmission(any());
            verify(fixture.repository, never()).updateGrantAdmissionCount(any(), anyLong(), anyLong());
        }
    }

    private static void assertMediaSuccess(MediaFixture fixture, String key, Instant expiry) {
        Map<String,Object> fingerprint = new TreeMap<>();
        fingerprint.put("shareGrantId", fixture.grantId.toString());
        fingerprint.put("shareAdmissionId", fixture.admission.toString());
        fingerprint.put("eventType", "PLAYBACK_AUTHORIZED"); fingerprint.put("purpose", "PLAYBACK");
        fingerprint.put("requestKeyDigest", digest(key.getBytes(StandardCharsets.UTF_8)));
        fingerprint.put("codeGeneration", 0); fingerprint.put("revocationGeneration", 0);
        fingerprint.put("grantExpiresAt", fixture.witness.grantExpiresAt().toString());
        fingerprint.put("admissionExpiresAt", fixture.witness.admissionExpiresAt().toString());
        fingerprint.put("sessionExpiresAt", fixture.witness.sessionExpiresAt().toString());
        fingerprint.put("credentialExpiresAt", fixture.witness.sessionExpiresAt().toString());
        fingerprint.putAll(mediaFactFields(DeterministicShareCommandProviders.mediaFacts(fixture.evidenceId)));
        assertThat(fingerprint).hasSize(19);
        String preFingerprint = digest(JSON.writeValueAsString(fingerprint).getBytes(StandardCharsets.UTF_8));
        assertThat(fixture.issued).allSatisfy(request -> assertThat(request.preProviderFingerprint()).isEqualTo(preFingerprint));
        var issued = (ShareMediaCredentialProvider.MediaAuthorizationCredentialResult.Issued) (fixture.providerResult != null
                ? fixture.providerResult : fixture.credentials.get(fixture.issued.getFirst().eventId()));
        Map<String,Object> expected = new TreeMap<>(fingerprint);
        expected.put("credentialExpiresAt", expiry.toString()); expected.put("result", "SUCCESS"); expected.put("preProviderFingerprint", preFingerprint);
        expected.put("issuedAtMicros", issued.replayAnchor().issuedAtMicros()); expected.put("notBeforeMicros", issued.replayAnchor().notBeforeMicros());
        expected.put("kid", issued.replayAnchor().kid());
        assertThat(expected).hasSize(24);
        assertThat(fixture.events).hasSize(1).allSatisfy(event -> {
            assertThat(event.eventType()).isEqualTo("PLAYBACK_AUTHORIZED"); assertThat(event.result()).isEqualTo("SUCCESS");
            assertThat(event.payload().propertyNames()).containsExactlyInAnyOrderElementsOf(expected.keySet());
            String canonical = JSON.writeValueAsString(expected);
            assertThat(JSON.writeValueAsString(event.payload())).isEqualTo(canonical);
            assertThat(event.payloadSha256()).isEqualTo(digest(canonical.getBytes(StandardCharsets.UTF_8)));
        });
    }

    private static Map<String,Object> mediaFactFields(ShareMediaCredentialProvider.AuthorizedMediaFacts facts) {
        return Map.of("evidenceId",facts.evidenceId().toString(),"mediaObjectId",facts.mediaObjectId().toString(),"sha256",facts.sha256(),"sizeBytes",facts.sizeBytes(),
                "mimeType",facts.mimeType(),"durationSeconds",facts.durationSeconds(),"windowStartNanos",facts.windowStartNanos(),"windowEndNanos",facts.windowEndNanos());
    }

    private static com.onecall.core.evidence.mediaread.ResolvedMediaObject resolvedMedia(UUID evidenceId, String fault) {
        var valid = new com.onecall.core.evidence.mediaread.ResolvedMediaObject(DeterministicShareCommandProviders.mediaFacts(evidenceId).mediaObjectId(),
                "a".repeat(64), 16, "audio/wav", 3600);
        if (fault.isEmpty()) return valid;
        var malformed = mock(com.onecall.core.evidence.mediaread.ResolvedMediaObject.class);
        when(malformed.mediaObjectId()).thenReturn(fault.equals("NULL_OBJECT_ID") ? null : valid.mediaObjectId());
        when(malformed.sha256()).thenReturn(switch (fault) { case "NULL_HASH" -> null; case "INVALID_HASH" -> "invalid"; case "UPPERCASE_HASH" -> "A".repeat(64); default -> valid.sha256(); });
        when(malformed.sizeBytes()).thenReturn(switch (fault) { case "ZERO_SIZE" -> 0L; case "NEGATIVE_SIZE" -> -1L; default -> valid.sizeBytes(); });
        when(malformed.mimeType()).thenReturn(switch (fault) { case "NULL_MIME" -> null; case "BLANK_MIME" -> " \t"; default -> valid.mimeType(); });
        when(malformed.durationSeconds()).thenReturn(switch (fault) { case "DURATION" -> null; case "ZERO_DURATION" -> 0; case "NEGATIVE_DURATION" -> -1; default -> valid.durationSeconds(); });
        return malformed;
    }

    private static final class MediaFixture {
        final UUID grantId = UUID.randomUUID(), admission = UUID.randomUUID(), subject = UUID.randomUUID(), binding = UUID.randomUUID();
        final UUID account = UUID.randomUUID(), organization = UUID.randomUUID();
        UUID evidenceId = UUID.randomUUID();
        String evidenceFault = "";
        com.onecall.core.evidence.mediaread.ResolvedMediaObject media;
        final List<Boolean> evidenceOutsideTransaction = new CopyOnWriteArrayList<>();
        java.util.function.Function<ShareMediaCredentialProvider.MediaAuthorizationCredentialRequest,ShareMediaCredentialProvider.MediaAuthorizationCredentialResult> realProvider;
        final ShareAdmissionRepository repository = mock(ShareAdmissionRepository.class);
        final Transactions transactions = new Transactions();
        final List<AccessEventInsert> events = new CopyOnWriteArrayList<>();
        final List<ShareMediaCredentialProvider.MediaAuthorizationCredentialRequest> issued = new CopyOnWriteArrayList<>();
        final List<String> trace = new CopyOnWriteArrayList<>(); final List<Boolean> providerOutsideTransaction = new CopyOnWriteArrayList<>();
        final List<EventWriteStatus> eventWrites = new CopyOnWriteArrayList<>();
        final Map<String, ShareMediaCredentialProvider.MediaAuthorizationCredentialResult.Issued> credentials = new ConcurrentHashMap<>();
        CyclicBarrier providerBarrier;
        ShareMediaCredentialProvider.MediaAuthorizationCredentialResult providerResult;
        final Instant expires;
        String status = "ACTIVE", denial = ""; boolean member, throwProvider;
        GrantSnapshot grant;
        AdmissionWitness witness;
        final DefaultShareMediaAuthorizationService service;
        MediaFixture() {
            this(NOW.plusSeconds(600));
        }
        MediaFixture(Instant expires) {
            this.expires = expires;
            grant = makeGrant();
            Map<String,Object> values = new TreeMap<>(); values.put("verificationMode","NONE"); values.put("codeGeneration",0); values.put("revocationGeneration",0);
            values.put("grantExpiresAt",expires.toString()); values.put("admissionExpiresAt",expires.minusSeconds(60).toString()); values.put("sessionExpiresAt",expires.minusSeconds(120).toString());
            var payload = JSON.valueToTree(values);
            witness = new AdmissionWitness("admission-granted:"+grantId+":"+admission,payload,digest(JSON.writeValueAsString(values).getBytes(StandardCharsets.UTF_8)),"NONE",0,0,expires,expires.minusSeconds(60),expires.minusSeconds(120),null,null);
            when(repository.findGrantByTokenDigest(any())).thenAnswer(i -> {
                assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isTrue();
                trace.add("discovery"); return Optional.of(grant);
            });
            when(repository.lockGrant(any())).thenAnswer(i -> { trace.add("grant-lock"); return Optional.of(grant); });
            when(repository.now()).thenReturn(NOW);
            when(repository.findAdmissionById(any(),any())).thenReturn(Optional.of(new AdmissionRow(admission,grantId,"key",1,"NONE","NOT_REQUIRED",NOW.minusSeconds(300))));
            when(repository.findAdmissionWitness(any(),any())).thenReturn(Optional.of(witness));
            when(repository.readSubjectOwnership(any())).thenAnswer(i -> Optional.of(new SubjectOwnership(subject,member?null:account,member?organization:null)));
            when(repository.readCapturedBinding(any())).thenReturn(Optional.of(new BindingState(binding,subject,"ACTIVE",NOW.minusSeconds(1),1)));
            when(repository.readCurrentBindings(any())).thenAnswer(i -> denial.equals("binding") ? List.of() : List.of(new BindingState(binding,subject,"ACTIVE",NOW.minusSeconds(1),1)));
            when(repository.readSubjectRisk(any(),any())).thenAnswer(i -> Optional.of(new RiskState(subject,List.of(binding),List.of(new RiskRow(subject,"RISK",denial.equals("risk")?"OPEN":"CLOSED",1)))));
            when(repository.findAccessEventsByRequestKeyDigest(any(),any(),any())).thenAnswer(i -> events.stream().map(e -> new AccessEventRow(e.eventId(),e.eventType(),e.payload(),e.payloadSha256(),e.result())).toList());
            when(repository.insertAccessEvent(any())).thenAnswer(i -> {
                synchronized (events) {
                    var event = i.<AccessEventInsert>getArgument(0);
                    var first = events.stream().filter(prior -> prior.eventId().equals(event.eventId())).findFirst().orElse(null);
                    if (first == null) events.add(event);
                    var value = first == null ? event : first;
                    var row = new AccessEventRow(value.eventId(), value.eventType(), value.payload(), value.payloadSha256(), value.result());
                    var writeStatus = first == null ? EventWriteStatus.INSERTED
                            : first.payloadSha256().equals(event.payloadSha256()) ? EventWriteStatus.REUSED_IDENTICAL : EventWriteStatus.CONFLICT;
                    eventWrites.add(writeStatus);
                    return new EventWriteResult(writeStatus, row);
                }
            });
            when(repository.readCurrentOrganization(any())).thenReturn(Optional.of(new OrganizationState(organization,"ACTIVE")));
            when(repository.readCurrentMembership(any(),any())).thenReturn(Optional.of(new MembershipState(organization,account,"ACTIVE")));
            when(repository.readCurrentAccount(any())).thenReturn(Optional.of(new AccountState(account,"ACTIVE")));
            doAnswer(i -> { trace.add("org-lock"); return null; }).when(repository).acquireOrganizationLifecycleLock(any());
            doAnswer(i -> { trace.add("membership-lock"); return null; }).when(repository).acquireMembershipLifecycleLock(any(),any());
            doAnswer(i -> { trace.add("account-lock"); return null; }).when(repository).acquireAccountLifecycleLock(any());
            TransactionTemplate template = new TransactionTemplate(transactions);
            var credentialsProvider = new ShareMediaCredentialProvider() {
                public Result issue(MediaCredentialRequest r) { throw new AssertionError(); }
                public MediaAuthorizationCredentialResult issueOrReplay(MediaAuthorizationCredentialRequest r) {
                    providerOutsideTransaction.add(!TransactionSynchronizationManager.isActualTransactionActive()); issued.add(r);
                    if (providerBarrier != null && r.priorReplayAnchor() == null) {
                        assertThat(events).isEmpty();
                        try { providerBarrier.await(10, TimeUnit.SECONDS); }
                        catch (Exception failure) { throw new AssertionError("Concurrent provider calls did not overlap", failure); }
                    }
                    if (throwProvider) throw new IllegalStateException("provider");
                    if (realProvider != null) return realProvider.apply(r);
                    if (providerResult != null) return providerResult;
                    return credentials.computeIfAbsent(r.eventId(), ignored -> new MediaAuthorizationCredentialResult.Issued("credential-private", expires.minusSeconds(180), com.onecall.archtests.sharing.DeterministicShareCommandProviders.mediaReplayAnchor()));
                }
            };
            com.onecall.core.evidence.mediaread.EvidenceMediaObjectReader reader = evidence -> {
                assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
                switch (evidenceFault) {
                    case "NULL_EVIDENCE_ID" -> new ShareMediaCredentialProvider.AuthorizedMediaFacts(null, UUID.randomUUID(), "a".repeat(64), 16, "audio/wav", 3600, 0, 3_600_000_000_000L);
                    case "INVALID_EVIDENCE_UUID", "INVALID_OBJECT_UUID" -> UUID.fromString("private-evidence-canary");
                    case "WINDOW_OVERFLOW" -> Duration.ofSeconds(Long.MAX_VALUE).toNanos();
                }
                return switch(evidenceFault) {
                    case "NULL" -> null;
                    case "MISSING" -> new com.onecall.core.evidence.mediaread.EvidenceMediaObjectReader.Result.Missing();
                    case "UNAVAILABLE" -> new com.onecall.core.evidence.mediaread.EvidenceMediaObjectReader.Result.Unavailable();
                    case "THROWN" -> throw new IllegalStateException("private-evidence-canary");
                    case "NULL_MEDIA" -> mock(com.onecall.core.evidence.mediaread.EvidenceMediaObjectReader.Result.Found.class);
                    default -> new com.onecall.core.evidence.mediaread.EvidenceMediaObjectReader.Result.Found(media == null
                            ? resolvedMedia(evidence, evidenceFault) : media);
                };
            };
            var readers = mock(org.springframework.beans.factory.ObjectProvider.class);
            when(readers.stream()).thenAnswer(invocation -> {
                evidenceOutsideTransaction.add(!TransactionSynchronizationManager.isActualTransactionActive());
                assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
                assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
                if (!evidenceFault.isEmpty()) assertThat(transactions.history).containsExactly("begin", "commit", "begin", "commit");
                if (evidenceFault.equals("THROWING_PROVIDER")) throw new IllegalStateException("private-evidence-canary");
                if (evidenceFault.equals("NULL_READER")) return java.util.stream.Stream.of((com.onecall.core.evidence.mediaread.EvidenceMediaObjectReader) null);
                var beans = new org.springframework.beans.factory.support.DefaultListableBeanFactory();
                if (!evidenceFault.equals("ZERO_READERS")) beans.registerSingleton("reader", reader);
                if (List.of("MULTIPLE_READERS", "PRIMARY_READERS").contains(evidenceFault)) {
                    var definition = new org.springframework.beans.factory.support.RootBeanDefinition(com.onecall.core.evidence.mediaread.EvidenceMediaObjectReader.class);
                    definition.setInstanceSupplier(() -> reader); definition.setPrimary(evidenceFault.equals("PRIMARY_READERS"));
                    beans.registerBeanDefinition("secondReader", definition);
                }
                return beans.getBeanProvider(com.onecall.core.evidence.mediaread.EvidenceMediaObjectReader.class).stream();
            });
            try {
                var factory = ShareAdmissionPersistenceConfiguration.class.getDeclaredMethod("shareMediaAuthorizationService", TransactionTemplate.class,
                        ShareAdmissionRepository.class, ShareAdmissionSha256Provider.class, ShareMediaCredentialProvider.class, org.springframework.beans.factory.ObjectProvider.class);
                factory.setAccessible(true);
                service = (DefaultShareMediaAuthorizationService) factory.invoke(new ShareAdmissionPersistenceConfiguration(), template, repository,
                        (ShareAdmissionSha256Provider) ShareAdmissionServiceTests::digest, credentialsProvider, readers);
            } catch (ReflectiveOperationException failure) { throw new AssertionError("Production evidence resolver unavailable", failure); }
        }
        ShareAdmissionView.MediaAuthorization authorize(String key, MediaAuthorizationRequest.Purpose purpose) {
            grant = makeGrant();
            if (denial.equals("generation")) when(repository.findAdmissionWitness(any(),any())).thenReturn(Optional.of(new AdmissionWitness(witness.eventId(),witness.payload(),witness.payloadSha256(),"NONE",1,0,witness.grantExpiresAt(),witness.admissionExpiresAt(),witness.sessionExpiresAt(),null,null)));
            if (denial.equals("session")) when(repository.now()).thenReturn(witness.admissionExpiresAt());
            return service.authorize(new ShareAdmissionCapabilityToken("token-private"),admission,new MediaAuthorizationRequest(key,purpose),
                    member ? () -> Optional.of(new TrustedShareAdmissionCaller(account,organization)) : null);
        }
        private GrantSnapshot makeGrant() { return new GrantSnapshot(grantId,"share",evidenceId,account,member?organization:null,status,member?"ORGANIZATION_MEMBER":"NONE",NOW.minusSeconds(1),expires,0,0,1,null,1,null,null,subject,binding,false,JSON.readTree("{}")); }
        private void prepareMemberWitness() {
            Map<String,Object> values = new TreeMap<>(); values.put("verificationMode","ORGANIZATION_MEMBER"); values.put("codeGeneration",0); values.put("revocationGeneration",0);
            values.put("grantExpiresAt",expires.toString()); values.put("admissionExpiresAt",expires.minusSeconds(60).toString()); values.put("sessionExpiresAt",expires.minusSeconds(120).toString());
            values.put("recipientAccountId",account.toString()); values.put("recipientOrganizationId",organization.toString());
            var payload=JSON.valueToTree(values);
            witness=new AdmissionWitness("admission-granted:"+grantId+":"+admission,payload,digest(JSON.writeValueAsString(values).getBytes(StandardCharsets.UTF_8)),"ORGANIZATION_MEMBER",0,0,expires,expires.minusSeconds(60),expires.minusSeconds(120),account,organization);
            when(repository.findAdmissionById(any(),any())).thenReturn(Optional.of(new AdmissionRow(admission,grantId,"key",1,"ORGANIZATION_MEMBER","VERIFIED",NOW.minusSeconds(300))));
            when(repository.findAdmissionWitness(any(),any())).thenReturn(Optional.of(witness));
        }
    }

}
