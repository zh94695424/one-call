package com.onecall.archtests;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.onecall.core.evidence.mediaread.*;
import com.onecall.core.sharing.internal.admission.*;
import com.onecall.core.sharing.internal.playback.*;
import com.onecall.core.sharing.internal.credential.*;
import com.onecall.core.sharing.internal.keysource.FileMediaPlaybackSigningKeyProvider;
import com.onecall.core.sharing.internal.keysource.MediaPlaybackKeySourceConfiguration;
import com.onecall.core.sharing.internal.keysource.TencentSsmMediaPlaybackSigningKeyProvider;
import com.tencentcloudapi.common.provider.CvmRoleCredential;
import com.tencentcloudapi.ssm.v20190923.SsmClient;
import com.onecall.archtests.sharing.DeterministicShareCommandProviders;
import com.onecall.core.sharing.internal.publicadmission.MediaAuthorizationRequest.Purpose;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

class MediaPlaybackConfigurationTests {
    @org.junit.jupiter.api.io.TempDir java.nio.file.Path keyDirectory;
    private final ApplicationContextRunner runner = new ApplicationContextRunner().withAllowBeanDefinitionOverriding(false)
            .withUserConfiguration(MediaPlaybackConfiguration.class);

    @Test void defaultAndWebProfilesProvideOneUnavailableAuthority() {
        for (String profile : List.of("", "default", "web")) runner.withPropertyValues("spring.profiles.active=" + profile).run(context -> {
            assertThat(context).hasNotFailed().hasSingleBean(MediaPlaybackService.class);
            assertThat(context).hasSingleBean(MediaCredentialVerifier.class).hasSingleBean(ShareMediaCredentialProvider.class).hasSingleBean(MediaPlaybackCredentialAuthority.class).doesNotHaveBean(ControlledMediaReader.class)
                    .doesNotHaveBean(PlaybackStateReader.class).doesNotHaveBean(EvidenceMediaObjectReader.class);
            assertThat(context.getBean(MediaPlaybackService.class).authorizeAndOpen(new OpaquePlaybackCredential("fixture"), Purpose.PLAYBACK))
                    .isEqualTo(new MediaPlaybackService.Result.Unavailable());
        });
    }

    @Test void persistenceStateRequiresExplicitAdmissionEnablementAndQualifiedResources() {
        for (String flag : List.of("", "false")) runner.withPropertyValues("spring.profiles.active=persistence",
                "onecall.sharing-admission.datasource.enabled=" + flag).run(context -> assertThat(context).hasNotFailed().doesNotHaveBean(PlaybackStateReader.class));
        var template = new TransactionTemplate(mock(PlatformTransactionManager.class));
        runner.withPropertyValues("spring.profiles.active=persistence", "onecall.sharing-admission.datasource.enabled=true")
                .withBean("sharingAdmissionTransactionTemplate", TransactionTemplate.class, () -> template)
                .withBean("unqualifiedDecoy", TransactionTemplate.class, () -> { throw new AssertionError("Wrong transaction template"); }, definition -> definition.setLazyInit(true))
                .withBean(ShareAdmissionRepository.class, () -> mock(ShareAdmissionRepository.class))
                .withBean("sharingAdmissionSha256Provider", ShareAdmissionSha256Provider.class, () -> bytes -> "a".repeat(64))
                .run(context -> {
                    assertThat(context).hasNotFailed().hasSingleBean(PlaybackStateReader.class);
                    assertThat(context.getBean(PlaybackStateReader.class)).isInstanceOf(SharingPlaybackStateReader.class);
                    assertThat(template.isReadOnly()).isFalse();
                });
        runner.withPropertyValues("spring.profiles.active=persistence", "onecall.sharing-admission.datasource.enabled=true")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test void singleAuthorityEvidenceStateAndReaderAreUsedWithoutReplacement() {
        for (boolean configurationFirst : List.of(true, false)) {
            AtomicInteger calls = new AtomicInteger();
            var observedClaim = new AtomicReference<VerifiedMediaClaim>();
            var configured = new ApplicationContextRunner().withAllowBeanDefinitionOverriding(false);
            if (configurationFirst) configured = configured.withUserConfiguration(MediaPlaybackConfiguration.class);
            configured = providers(configured, calls, observedClaim);
            if (!configurationFirst) configured = configured.withUserConfiguration(MediaPlaybackConfiguration.class);
            configured.run(context -> {
                assertThat(context).hasNotFailed();
                Instant issuedAt = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS).minusSeconds(1);
                Instant notBefore = issuedAt;
                Instant expiresAt = issuedAt.plusSeconds(60);
                var anchor = new ShareMediaCredentialProvider.ReplayAnchor(
                        issuedAt.getEpochSecond() * 1_000_000 + issuedAt.getNano() / 1_000,
                        notBefore.getEpochSecond() * 1_000_000 + notBefore.getNano() / 1_000, "K1");
                var facts = new ShareMediaCredentialProvider.AuthorizedMediaFacts(new UUID(0, 3), new UUID(0, 4),
                        "a".repeat(64), 123, "audio/wav", 10, 0, 10_000_000_000L);
                var issued = context.getBean(MediaPlaybackCredentialAuthority.class).issueOrReplay(
                        new ShareMediaCredentialProvider.MediaAuthorizationCredentialRequest(new UUID(0, 1), new UUID(0, 2),
                                "PLAYBACK", "PLAYBACK_AUTHORIZED", "a".repeat(64), 11, 7, expiresAt, expiresAt,
                                expiresAt, expiresAt, "a".repeat(64), "event", facts, anchor));
                assertThat(issued).isInstanceOf(ShareMediaCredentialProvider.MediaAuthorizationCredentialResult.Issued.class);
                String credential = ((ShareMediaCredentialProvider.MediaAuthorizationCredentialResult.Issued) issued).credential();
                var expectedClaim = new VerifiedMediaClaim("onecall:media-playback:v1", new UUID(0, 1), new UUID(0, 2),
                        new UUID(0, 3), new UUID(0, 4), 7, 11, Purpose.PLAYBACK, issuedAt, notBefore, expiresAt, 10,
                        new VerifiedMediaClaim.PlaybackWindow(Duration.ZERO, Duration.ofSeconds(10)));
                assertThat(context.getBean(MediaPlaybackService.class).authorizeAndOpen(new OpaquePlaybackCredential(credential), Purpose.PLAYBACK))
                        .isInstanceOf(MediaPlaybackService.Result.Opened.class);
                assertThat(observedClaim.get()).isEqualTo(expectedClaim);
                assertThat(calls).hasValue(3);
                assertThat(context.getBean(MediaCredentialVerifier.class)).isSameAs(context.getBean(ShareMediaCredentialProvider.class));
                assertThat(context).hasSingleBean(MediaPlaybackCredentialAuthority.class).hasSingleBean(MediaCredentialVerifier.class).hasSingleBean(ShareMediaCredentialProvider.class);
                assertThat(context.getBean(MediaCredentialVerifier.class)).isSameAs(context.getBean(MediaPlaybackCredentialAuthority.class));
                assertThat(org.springframework.test.util.ReflectionTestUtils.getField(context.getBean(MediaPlaybackService.class), "verifier"))
                        .isSameAs(context.getBean(MediaPlaybackCredentialAuthority.class));
                String valid = token(context.getBean(MediaPlaybackCredentialAuthority.class));
                String[] parts = valid.split("\\.");
                byte[] mac = Base64.getUrlDecoder().decode(parts[3]); mac[0] ^= 1;
                String tampered = String.join(".", parts[0], parts[1], parts[2], Base64.getUrlEncoder().withoutPadding().encodeToString(mac));
                assertThat(context.getBean(MediaPlaybackService.class).authorizeAndOpen(new OpaquePlaybackCredential(tampered), Purpose.PLAYBACK))
                        .isEqualTo(new MediaPlaybackService.Result.Denied(MediaPlaybackService.Reason.INVALID_CREDENTIAL));
                assertThat(calls).hasValue(3);
            });
        }
        for (Class<?> type : List.of(MediaCredentialVerifier.class, PlaybackStateReader.class, EvidenceMediaObjectReader.class, ControlledMediaReader.class)) {
            providers(runner, new AtomicInteger()).withInitializer(context -> {
                var definition = new RootBeanDefinition(type);
                definition.setInstanceSupplier(() -> mock(type)); definition.setPrimary(true);
                ((BeanDefinitionRegistry) context.getBeanFactory()).registerBeanDefinition("duplicateAuthority", definition);
            }).run(context -> {
                assertThat(context).hasFailed();
                assertThat(context.getStartupFailure()).hasRootCauseMessage("Ambiguous playback authority");
            });
        }
    }

    @Test void missingEvidenceStateReaderOrKeysFailClosedWithoutProductionAdapters() {
        for (Class<?> missing : List.of(PlaybackStateReader.class, EvidenceMediaObjectReader.class, MediaPlaybackSigningKeyProvider.class, ControlledMediaReader.class)) {
            providers(runner, new AtomicInteger()).withInitializer(context ->
                    context.addBeanFactoryPostProcessor(factory -> {
                        var registry = (BeanDefinitionRegistry) factory;
                        for (String name : factory.getBeanNamesForType(missing)) registry.removeBeanDefinition(name);
                    })).run(context -> {
                        assertThat(context).hasNotFailed();
                        var result = context.getBean(MediaPlaybackService.class).authorizeAndOpen(new OpaquePlaybackCredential(validToken()), Purpose.PLAYBACK);
                        assertThat(result).isEqualTo(new MediaPlaybackService.Result.Unavailable());
                        assertThat(result.toString()).doesNotContain("bucket", "jdbc", "password");
                    });
        }
    }


    @Test void persistenceOnlyConsumersShareSingleAuthority() { persistence(false); }

    @Test void bothEnabledConsumersShareAuthorityInEitherOrder() { persistence(true); }

    @Test void zeroAndAmbiguousKeysDoNotFailStartup() {
        for (int count : List.of(0,2)) {
            var configured=runner;
            for(int index=0;index<count;index++) {
                boolean primary=index==0;
                configured=configured.withBean("keys"+index,MediaPlaybackSigningKeyProvider.class,
                        DeterministicShareCommandProviders::mediaPlaybackSigningKeys,definition -> definition.setPrimary(primary));
            }
            configured.run(context -> {
                assertThat(context).hasNotFailed().hasSingleBean(MediaPlaybackCredentialAuthority.class);
                assertUnavailable(context.getBean(MediaCredentialVerifier.class));
            });
        }
    }

    @Test void invalidAndThrowingKeysAreUnavailablePerCall() {
        for(MediaPlaybackSigningKeyProvider keys:Arrays.<MediaPlaybackSigningKeyProvider>asList(() -> null, () -> { throw new IllegalStateException("private-key"); },
                () -> new MediaPlaybackSigningKeyProvider.Result.Unavailable(),
                () -> new MediaPlaybackSigningKeyProvider.Result.Available(new MediaPlaybackSigningKeyProvider.Entry("bad.kid",new byte[31],MediaPlaybackSigningKeyProvider.Role.ACTIVE),List.of())))
            runner.withBean(MediaPlaybackSigningKeyProvider.class,() -> keys).run(context -> {
                assertThat(context).hasNotFailed(); assertUnavailable(context.getBean(MediaCredentialVerifier.class));
            });
    }

    @Test void secondVerifierStillFailsClosedWithoutFallback() {
        runner.withBean("secondVerifier",MediaCredentialVerifier.class,() -> credential -> new MediaCredentialVerifier.Result.Unavailable())
                .run(context -> { assertThat(context).hasFailed(); assertThat(context.getStartupFailure()).hasRootCauseMessage("Ambiguous playback authority"); });
    }

    @Test void fileSourceRequiresExplicitSelectionAndPath() throws Exception {
        String path = fileBundle();
        var composed = runner.withUserConfiguration(MediaPlaybackKeySourceConfiguration.class);
        for (String[] properties : List.of(new String[0], new String[]{filePath(path)}))
            composed.withPropertyValues(properties).run(context -> {
                assertThat(context).hasNotFailed().hasSingleBean(MediaPlaybackCredentialAuthority.class).doesNotHaveBean(MediaPlaybackSigningKeyProvider.class);
                assertUnavailable(context.getBean(MediaCredentialVerifier.class));
            });
        composed.withPropertyValues(fileSelection(), filePath(path)).run(context -> {
            assertThat(context).hasNotFailed().hasSingleBean(MediaPlaybackSigningKeyProvider.class).hasSingleBean(MediaPlaybackCredentialAuthority.class);
            assertThat(context.getBean(MediaPlaybackSigningKeyProvider.class)).isInstanceOf(FileMediaPlaybackSigningKeyProvider.class);
            var authority = context.getBean(MediaPlaybackCredentialAuthority.class);
            assertThat(authority.verify(new OpaquePlaybackCredential(token(authority)))).isInstanceOf(MediaCredentialVerifier.Result.Verified.class);
        });
        for (String invalid : List.of("", " ", "relative", "\0", "${missing.pa22.path}"))
            composed.withPropertyValues(fileSelection(), filePath(invalid)).run(context -> {
                assertThat(context).hasNotFailed().hasSingleBean(MediaPlaybackSigningKeyProvider.class);
                assertUnavailable(context.getBean(MediaCredentialVerifier.class));
            });
        composed.withPropertyValues(fileSelection()).run(context -> {
            assertThat(context).hasNotFailed().hasSingleBean(MediaPlaybackSigningKeyProvider.class);
            assertUnavailable(context.getBean(MediaCredentialVerifier.class));
        });
    }

    @Test void fileSourceSharesOneAuthorityAcrossProfiles() throws Exception {
        String path = fileBundle();
        for (String profile : List.of("", "web", "persistence", "web,persistence")) for (boolean consumers : List.of(false, true)) {
            var composed = runner.withUserConfiguration(MediaPlaybackKeySourceConfiguration.class)
                    .withPropertyValues(fileSelection(), filePath(path), "spring.profiles.active=" + profile);
            if (consumers) composed = composed.withUserConfiguration(com.onecall.core.sharing.internal.persistence.ShareAdmissionPersistenceConfiguration.class)
                    .withInitializer(context -> {
                        context.getEnvironment().setActiveProfiles("web", "persistence");
                        context.addBeanFactoryPostProcessor(factory -> factory.registerSingleton("sharingAdmissionDataSource", mock(com.zaxxer.hikari.HikariDataSource.class)));
                    }).withPropertyValues("onecall.sharing-admission.datasource.enabled=true", "onecall.sharing-admission.datasource.url=jdbc:synthetic:admission",
                            "onecall.sharing-admission.datasource.username=sharing_admission_role", "onecall.sharing-admission.datasource.password=synthetic");
            boolean enabled = consumers;
            composed.run(context -> {
                assertThat(context).hasNotFailed().hasSingleBean(MediaPlaybackCredentialAuthority.class).hasSingleBean(ShareMediaCredentialProvider.class)
                        .hasSingleBean(MediaCredentialVerifier.class).hasSingleBean(MediaPlaybackSigningKeyProvider.class)
                        .doesNotHaveBean(ControlledMediaReader.class).doesNotHaveBean(EvidenceMediaObjectReader.class);
                var authority = context.getBean(MediaPlaybackCredentialAuthority.class);
                assertThat(context.getBean(ShareMediaCredentialProvider.class)).isSameAs(authority);
                assertThat(context.getBean(MediaCredentialVerifier.class)).isSameAs(authority);
                assertThat(org.springframework.test.util.ReflectionTestUtils.getField(context.getBean(MediaPlaybackService.class), "verifier")).isSameAs(authority);
                if (enabled) for (String name : List.of("shareAdmissionService", "shareMediaAuthorizationService"))
                    assertThat(org.springframework.test.util.ReflectionTestUtils.getField(context.getBean(name), "credentials")).isSameAs(authority);
                else assertThat(context).doesNotHaveBean(PlaybackStateReader.class).doesNotHaveBean(javax.sql.DataSource.class);
                assertThat(authority.verify(new OpaquePlaybackCredential(token(authority)))).isInstanceOf(MediaCredentialVerifier.Result.Verified.class);
            });
        }
    }

    @Test void fileSourceAndCustomProviderAreAmbiguousInEitherOrder() throws Exception {
        String path = fileBundle();
        for (boolean file : List.of(false, true)) for (boolean reverse : List.of(false, true)) {
            var primary = mock(MediaPlaybackSigningKeyProvider.class, withSettings().extraInterfaces(org.springframework.core.Ordered.class));
            when(((org.springframework.core.Ordered) primary).getOrder()).thenReturn(Integer.MIN_VALUE);
            clearInvocations(primary);
            var other = mock(MediaPlaybackSigningKeyProvider.class);
            AtomicReference<FileMediaPlaybackSigningKeyProvider> observed = new AtomicReference<>();
            var composed = new ApplicationContextRunner().withAllowBeanDefinitionOverriding(false)
                    .withInitializer(context -> context.addBeanFactoryPostProcessor(factory -> factory.addBeanPostProcessor(
                            new org.springframework.beans.factory.config.BeanPostProcessor() {
                                @Override public Object postProcessAfterInitialization(Object bean, String name) {
                                    if (bean instanceof FileMediaPlaybackSigningKeyProvider provider) {
                                        var tracked = spy(provider); observed.set(tracked); return tracked;
                                    }
                                    return bean;
                                }
                            })));
            if (!reverse) composed = composed.withUserConfiguration(MediaPlaybackConfiguration.class, MediaPlaybackKeySourceConfiguration.class);
            if (!file && reverse) composed = composed.withBean("otherKeys", MediaPlaybackSigningKeyProvider.class, () -> other);
            composed = composed.withBean("primaryKeys", MediaPlaybackSigningKeyProvider.class, () -> primary, definition -> definition.setPrimary(true));
            if (!file && !reverse) composed = composed.withBean("otherKeys", MediaPlaybackSigningKeyProvider.class, () -> other);
            if (reverse) composed = composed.withUserConfiguration(MediaPlaybackKeySourceConfiguration.class, MediaPlaybackConfiguration.class);
            if (file) composed = composed.withPropertyValues(fileSelection(), filePath(path));
            composed.run(context -> {
                assertThat(context).hasNotFailed().hasSingleBean(MediaPlaybackCredentialAuthority.class);
                assertThat(context.getBeansOfType(MediaPlaybackSigningKeyProvider.class)).hasSize(2);
                var authority = context.getBean(MediaPlaybackCredentialAuthority.class);
                assertUnavailable(authority);
                assertThat(authority.issueOrReplay(null)).isInstanceOf(ShareMediaCredentialProvider.MediaAuthorizationCredentialResult.Unavailable.class);
                var opaque = mock(OpaquePlaybackCredential.class);
                assertThat(authority.verify(opaque)).isInstanceOf(MediaCredentialVerifier.Result.Unavailable.class);
                verify(primary, never()).currentKeys(); verify(other, never()).currentKeys(); verifyNoInteractions(opaque);
                if (file) verify(observed.get(), never()).currentKeys();
            });
        }
    }

    @Test void unsupportedSourceAndInvalidFileKeepUnavailable() throws Exception {
        String path = fileBundle();
        var composed = runner.withUserConfiguration(MediaPlaybackKeySourceConfiguration.class);
        for (String selector : List.of("", "false", "unknown", "file,custom", "file custom"))
            composed.withPropertyValues("onecall.sharing.media-playback.signing-key-source.provider=" + selector, filePath(path)).run(context -> {
                assertThat(context).hasNotFailed().doesNotHaveBean(MediaPlaybackSigningKeyProvider.class);
                assertUnavailable(context.getBean(MediaCredentialVerifier.class));
            });
        for (String invalid : List.of("missing", "malformed", "unreadable")) {
            var target = keyDirectory.resolve(invalid);
            if (invalid.equals("malformed")) java.nio.file.Files.writeString(target, "{}");
            composed.withPropertyValues(fileSelection(), filePath(target.toString())).run(context -> {
                assertThat(context).hasNotFailed().hasSingleBean(MediaPlaybackSigningKeyProvider.class);
                var authority = context.getBean(MediaPlaybackCredentialAuthority.class);
                try (var files = mockStatic(java.nio.file.Files.class, CALLS_REAL_METHODS)) {
                    if (invalid.equals("unreadable")) files.when(() -> java.nio.file.Files.readAttributes(target,
                            java.nio.file.attribute.BasicFileAttributes.class, java.nio.file.LinkOption.NOFOLLOW_LINKS))
                            .thenThrow(new java.nio.file.AccessDeniedException("synthetic-denied"));
                    assertUnavailable(authority);
                    assertThat(authority.issueOrReplay(null)).isInstanceOf(ShareMediaCredentialProvider.MediaAuthorizationCredentialResult.Unavailable.class);
                }
            });
        }
    }

    private String fileBundle() throws java.io.IOException {
        byte[] material = new byte[32];
        for (int index = 0; index < material.length; index++) material[index] = (byte) (index + 1);
        return java.nio.file.Files.writeString(keyDirectory.resolve("source"), "{\"schemaVersion\":1,\"keys\":[{\"kid\":\"K1\",\"role\":\"ACTIVE\",\"keyHex\":\""
                + HexFormat.of().formatHex(material) + "\"}]}").toString();
    }
    private static String fileSelection() { return "onecall.sharing.media-playback.signing-key-source.provider=file"; }
    private static String filePath(String path) { return "onecall.sharing.media-playback.signing-key-source.bundle-path=" + path; }

    private void persistence(boolean combined) {
        for(boolean reverse:List.of(false,true)) for (String readers : List.of("zero", "single", "multiple", "primary", "throwing", "nullResult", "throwingProvider")) {
            Class<?>[] configuration=combined ? (reverse ? new Class<?>[]{com.onecall.core.sharing.internal.persistence.ShareAdmissionPersistenceConfiguration.class,MediaPlaybackConfiguration.class}
                    : new Class<?>[]{MediaPlaybackConfiguration.class,com.onecall.core.sharing.internal.persistence.ShareAdmissionPersistenceConfiguration.class})
                    : new Class<?>[]{com.onecall.core.sharing.internal.persistence.ShareAdmissionPersistenceConfiguration.class};
            AtomicInteger readerCalls = new AtomicInteger();
            new ApplicationContextRunner().withAllowBeanDefinitionOverriding(false).withUserConfiguration(configuration)
                    .withInitializer(context -> { context.getEnvironment().setActiveProfiles(combined ? new String[]{"web","persistence"}:new String[]{"persistence"});
                        context.addBeanFactoryPostProcessor(factory -> factory.registerSingleton("sharingAdmissionDataSource",mock(com.zaxxer.hikari.HikariDataSource.class))); })
                    .withPropertyValues("onecall.sharing-admission.datasource.enabled=true","onecall.sharing-admission.datasource.url=jdbc:synthetic:admission",
                            "onecall.sharing-admission.datasource.username=sharing_admission_role","onecall.sharing-admission.datasource.password=synthetic")
                    .run(context -> {
                        assertThat(context).hasNotFailed().hasSingleBean(MediaPlaybackCredentialAuthority.class).hasSingleBean(ShareMediaCredentialProvider.class).hasSingleBean(MediaCredentialVerifier.class);
                        var authority=context.getBean(MediaPlaybackCredentialAuthority.class);
                        assertThat(context.getBean(ShareMediaCredentialProvider.class)).isSameAs(authority);
                        assertThat(context.getBean(MediaCredentialVerifier.class)).isSameAs(authority);
                        for(Object consumer:List.of(context.getBean("shareAdmissionService"),context.getBean("shareMediaAuthorizationService")))
                            assertThat(org.springframework.test.util.ReflectionTestUtils.getField(consumer,"credentials")).isSameAs(authority);
                        assertThat(org.springframework.test.util.ReflectionTestUtils.getField(context.getBean(MediaPlaybackService.class),"verifier")).isSameAs(authority);
                        assertThat(context).doesNotHaveBean("unavailableShareMediaCredentialProvider");
                        var factory = (org.springframework.beans.factory.support.DefaultListableBeanFactory) context.getSourceApplicationContext().getBeanFactory();
                        EvidenceMediaObjectReader reader = evidenceId -> {
                            readerCalls.incrementAndGet();
                            if (readers.equals("throwing")) throw new IllegalStateException("private-evidence-canary");
                            return readers.equals("nullResult") ? null : new EvidenceMediaObjectReader.Result.Missing();
                        };
                        if (!readers.equals("zero")) factory.registerSingleton("evidenceReader", reader);
                        if (List.of("multiple", "primary", "throwingProvider").contains(readers)) {
                            var definition = new RootBeanDefinition(EvidenceMediaObjectReader.class);
                            definition.setInstanceSupplier(() -> {
                                if (readers.equals("throwingProvider")) throw new IllegalStateException("private-evidence-canary");
                                return (EvidenceMediaObjectReader) evidenceId -> { throw new AssertionError("Ambiguous reader invoked"); };
                            });
                            definition.setPrimary(readers.equals("primary")); definition.setLazyInit(true);
                            factory.registerBeanDefinition("otherEvidenceReader", definition);
                        }
                        var evidence = (EvidenceMediaObjectReader) org.springframework.test.util.ReflectionTestUtils.getField(context.getBean("shareMediaAuthorizationService"), "evidence");
                        var resolved = evidence.resolve(new UUID(0, 3));
                        if (readers.equals("single")) assertThat(resolved).isInstanceOf(EvidenceMediaObjectReader.Result.Missing.class);
                        else if (readers.equals("nullResult")) assertThat(resolved).isNull();
                        else assertThat(resolved).isInstanceOf(EvidenceMediaObjectReader.Result.Unavailable.class);
                        assertThat(readerCalls).hasValue(List.of("single", "throwing", "nullResult").contains(readers) ? 1 : 0);
                        assertUnavailable(authority);
                    });
        }
    }

    @Test void tencentSourceRequiresExactExplicitSelection() throws Exception {
        try (var tencent = mockConstruction(TencentSsmMediaPlaybackSigningKeyProvider.class);
                var files = mockConstruction(FileMediaPlaybackSigningKeyProvider.class);
                var clients = mockConstruction(cloudClientType()); var sdk = mockConstruction(SsmClient.class);
                var roles = mockConstruction(CvmRoleCredential.class)) {
            for (String selector : Arrays.asList(null, "", " ", "TENCENT-SSM", "Tencent-ssm", "tencent-ssm ", " tencent-ssm", "unknown",
                    "tencent-ssm,file", "tencent-ssm file", "FILE", "File", " file", "file ", "file,tencent-ssm", "file tencent-ssm", "${missing.pa23.selector}")) {
                var values = tencentMetadata();
                if (selector != null) values.put(KEY_SOURCE + "provider", selector);
                exactProperties(values).run(context -> {
                    assertThat(context).hasNotFailed().doesNotHaveBean(MediaPlaybackSigningKeyProvider.class);
                    assertUnavailable(context.getBean(MediaCredentialVerifier.class));
                });
            }
            for (String path : List.of(fileBundle(), "", " ", "relative", "\0", "${missing.pa23.path}")) {
                var values = tencentMetadata(); values.put(KEY_SOURCE + "provider", "tencent-ssm"); values.put(KEY_SOURCE + "bundle-path", path);
                exactProperties(values).withInitializer(context -> context.getEnvironment().getPropertySources().addFirst(
                        new org.springframework.core.env.PropertySource<Object>("path-resolution-guard") {
                            @Override public boolean containsProperty(String name) { return name.equals(KEY_SOURCE + "bundle-path"); }
                            @Override public Object getProperty(String name) {
                                if (name.equals(KEY_SOURCE + "bundle-path")) throw new AssertionError("Path must not be resolved");
                                return null;
                            }
                        })).run(context -> assertThat(context).hasNotFailed().doesNotHaveBean(MediaPlaybackSigningKeyProvider.class));
            }
            for (String failing : List.of("provider", "bundle-path")) {
                var values = tencentMetadata(); values.put(KEY_SOURCE + "provider", "tencent-ssm");
                exactProperties(values).withInitializer(context -> context.getEnvironment().getPropertySources().addFirst(
                        new org.springframework.core.env.PropertySource<Object>("inspection-failure") {
                            @Override public boolean containsProperty(String name) {
                                if (failing.equals("bundle-path") && name.equals(KEY_SOURCE + failing)) throw new IllegalStateException();
                                return false;
                            }
                            @Override public Object getProperty(String name) {
                                if (failing.equals("provider") && name.equals(KEY_SOURCE + failing)) throw new IllegalStateException();
                                return null;
                            }
                        })).run(context -> assertThat(context).hasNotFailed().doesNotHaveBean(MediaPlaybackSigningKeyProvider.class));
            }
            assertThat(tencent.constructed()).isEmpty(); assertThat(files.constructed()).isEmpty();
            assertThat(clients.constructed()).isEmpty(); assertThat(sdk.constructed()).isEmpty(); assertThat(roles.constructed()).isEmpty();
        }
        try (var clients = mockConstruction(cloudClientType()); var sdk = mockConstruction(SsmClient.class); var roles = mockConstruction(CvmRoleCredential.class)) {
            var values = tencentMetadata(); values.put(KEY_SOURCE + "provider", "tencent-ssm");
            exactProperties(values).run(context -> {
                assertThat(context).hasNotFailed().hasSingleBean(MediaPlaybackSigningKeyProvider.class);
                assertThat(context.getBean(MediaPlaybackSigningKeyProvider.class)).isInstanceOf(TencentSsmMediaPlaybackSigningKeyProvider.class);
                assertThat(clients.constructed()).hasSize(1); assertThat(sdk.constructed()).isEmpty(); assertThat(roles.constructed()).isEmpty();
                verifyNoInteractions(clients.constructed().getFirst());
            });
        }
    }

    @Test void tencentMissingMetadataIsUnavailableWithoutNetwork() throws Exception {
        try (var clients = mockConstruction(cloudClientType()); var sdk = mockConstruction(SsmClient.class);
                var roles = mockConstruction(CvmRoleCredential.class); var files = mockConstruction(FileMediaPlaybackSigningKeyProvider.class)) {
            for (String field : List.of("region", "secret-name", "version-id", "connect-timeout-seconds", "read-timeout-seconds"))
                for (String invalid : Arrays.asList(null, "", " ", "${missing.pa23.value}", "\0", field.equals("region") ? "INVALID" : field.endsWith("seconds") ? "99" : " trailing ")) {
                    if (invalid == null && field.endsWith("seconds")) continue;
                    var values = tencentMetadata(); values.put(KEY_SOURCE + "provider", "tencent-ssm");
                    if (invalid == null) values.remove(KEY_SOURCE + "tencent." + field);
                    else values.put(KEY_SOURCE + "tencent." + field, invalid);
                    exactProperties(values).run(context -> {
                        assertThat(context).hasNotFailed().hasSingleBean(MediaPlaybackSigningKeyProvider.class);
                        assertThat(context.getBean(MediaPlaybackSigningKeyProvider.class)).isInstanceOf(TencentSsmMediaPlaybackSigningKeyProvider.class);
                        assertUnavailable(context.getBean(MediaCredentialVerifier.class));
                        assertThat(context.getBean(MediaPlaybackSigningKeyProvider.class).currentKeys()).isInstanceOf(MediaPlaybackSigningKeyProvider.Result.Unavailable.class);
                    });
                }
            assertThat(clients.constructed()).isEmpty(); assertThat(sdk.constructed()).isEmpty(); assertThat(roles.constructed()).isEmpty(); assertThat(files.constructed()).isEmpty();
        }
    }

    @Test void staleTencentMetadataDoesNotSuppressExplicitFileProvider() throws Exception {
        String path = fileBundle();
        var fields = List.of("region", "secret-name", "version-id", "connect-timeout-seconds", "read-timeout-seconds");
        try (var tencent = mockConstruction(TencentSsmMediaPlaybackSigningKeyProvider.class); var clients = mockConstruction(cloudClientType());
                var sdk = mockConstruction(SsmClient.class); var roles = mockConstruction(CvmRoleCredential.class)) {
            for (int selected = -1; selected < fields.size(); selected++) for (String form : List.of("absent", "empty", "whitespace", "valid", "invalid", "unresolved")) {
                Map<String, Object> stale = new HashMap<>();
                for (int index = 0; index < fields.size(); index++) if ((selected == -1 || index == selected) && !form.equals("absent")) {
                    String field = fields.get(index);
                    stale.put(KEY_SOURCE + "tencent." + field, switch (form) {
                        case "empty" -> ""; case "whitespace" -> " \t"; case "invalid" -> "\0";
                        case "unresolved" -> "${missing.pa23.stale}"; default -> tencentMetadata().get(KEY_SOURCE + "tencent." + field);
                    });
                }
                AtomicInteger lookups = new AtomicInteger();
                exactProperties(Map.of(KEY_SOURCE + "provider", "file", KEY_SOURCE + "bundle-path", path)).withInitializer(context ->
                        context.getEnvironment().getPropertySources().addFirst(new org.springframework.core.env.PropertySource<Object>("stale-tencent", stale) {
                            @Override public Object getProperty(String name) {
                                if (name.startsWith(KEY_SOURCE + "tencent.")) { lookups.incrementAndGet(); throw new AssertionError("Unselected metadata inspected"); }
                                return null;
                            }
                        })).run(context -> {
                            assertThat(context).hasNotFailed().hasSingleBean(MediaPlaybackSigningKeyProvider.class);
                            assertThat(context.getBean(MediaPlaybackSigningKeyProvider.class)).isInstanceOf(FileMediaPlaybackSigningKeyProvider.class);
                            assertThat(lookups).hasValue(0);
                            var authority = context.getBean(MediaPlaybackCredentialAuthority.class);
                            assertThat(authority.verify(new OpaquePlaybackCredential(token(authority)))).isInstanceOf(MediaCredentialVerifier.Result.Verified.class);
                            assertThat(lookups).hasValue(0);
                            assertThat(tencent.constructed()).isEmpty(); assertThat(clients.constructed()).isEmpty();
                            assertThat(sdk.constructed()).isEmpty(); assertThat(roles.constructed()).isEmpty();
                        });
            }
        }
    }

    @Test void tencentSharesSoleAuthorityAcrossProfiles() throws Exception {
        for (String profile : List.of("", "web", "persistence", "web,persistence")) for (boolean consumers : List.of(false, true)) {
            try (var clients = mockConstruction(cloudClientType()); var sdk = mockConstruction(SsmClient.class); var roles = mockConstruction(CvmRoleCredential.class)) {
                var values = tencentMetadata(); values.put(KEY_SOURCE + "provider", "tencent-ssm"); values.put("spring.profiles.active", profile);
                var composed = exactProperties(values);
                if (consumers) composed = composed.withUserConfiguration(com.onecall.core.sharing.internal.persistence.ShareAdmissionPersistenceConfiguration.class)
                        .withInitializer(context -> {
                            context.getEnvironment().setActiveProfiles("web", "persistence");
                            context.addBeanFactoryPostProcessor(factory -> factory.registerSingleton("sharingAdmissionDataSource", mock(com.zaxxer.hikari.HikariDataSource.class)));
                        }).withPropertyValues("onecall.sharing-admission.datasource.enabled=true", "onecall.sharing-admission.datasource.url=jdbc:synthetic:admission",
                                "onecall.sharing-admission.datasource.username=sharing_admission_role", "onecall.sharing-admission.datasource.password=synthetic");
                composed.run(context -> {
                    assertThat(context).hasNotFailed().hasSingleBean(MediaPlaybackCredentialAuthority.class).hasSingleBean(MediaCredentialVerifier.class).hasSingleBean(ShareMediaCredentialProvider.class);
                    var authority = context.getBean(MediaPlaybackCredentialAuthority.class);
                    assertThat(context.getBean(MediaCredentialVerifier.class)).isSameAs(authority);
                    assertThat(context.getBean(ShareMediaCredentialProvider.class)).isSameAs(authority);
                    assertThat(org.springframework.test.util.ReflectionTestUtils.getField(context.getBean(MediaPlaybackService.class), "verifier")).isSameAs(authority);
                    for (String name : List.of("shareAdmissionService", "shareMediaAuthorizationService")) if (context.containsBean(name))
                        assertThat(org.springframework.test.util.ReflectionTestUtils.getField(context.getBean(name), "credentials")).isSameAs(authority);
                    assertThat(clients.constructed()).hasSize(1); verifyNoInteractions(clients.constructed().getFirst());
                    assertThat(sdk.constructed()).isEmpty(); assertThat(roles.constructed()).isEmpty();
                });
            }
        }
    }

    @Test void tencentAndCustomProvidersAreAmbiguousBeforeAnyRead() throws Exception {
        String path = fileBundle();
        for (String scenario : List.of("custom", "tencent", "file", "stale-file", "manual")) for (boolean reverse : List.of(false, true))
            for (boolean configurationFirst : List.of(false, true)) {
                try (var clients = mockConstruction(cloudClientType()); var sdk = mockConstruction(SsmClient.class);
                        var roles = mockConstruction(CvmRoleCredential.class); var files = mockConstruction(FileMediaPlaybackSigningKeyProvider.class)) {
                    MediaPlaybackSigningKeyProvider custom = scenario.equals("manual")
                            ? mock(FileMediaPlaybackSigningKeyProvider.class)
                            : mock(MediaPlaybackSigningKeyProvider.class, withSettings().extraInterfaces(org.springframework.core.Ordered.class));
                    if (custom instanceof org.springframework.core.Ordered ordered) when(ordered.getOrder()).thenReturn(Integer.MIN_VALUE);
                    MediaPlaybackSigningKeyProvider other = scenario.equals("manual") ? mock(TencentSsmMediaPlaybackSigningKeyProvider.class) : mock(MediaPlaybackSigningKeyProvider.class);
                    var composed = new ApplicationContextRunner().withAllowBeanDefinitionOverriding(false);
                    if (configurationFirst) composed = composed.withUserConfiguration(MediaPlaybackConfiguration.class, MediaPlaybackKeySourceConfiguration.class);
                    String first = reverse ? "otherKeys" : "customKeys", second = reverse ? "customKeys" : "otherKeys";
                    boolean pair = scenario.equals("custom") || scenario.equals("manual");
                    if (pair || !reverse) composed = composed.withBean(first, MediaPlaybackSigningKeyProvider.class, () -> reverse ? other : custom, definition -> definition.setPrimary(true));
                    if (pair || reverse) composed = composed.withBean(second, MediaPlaybackSigningKeyProvider.class, () -> reverse ? custom : other);
                    if (!configurationFirst) composed = composed.withUserConfiguration(MediaPlaybackKeySourceConfiguration.class, MediaPlaybackConfiguration.class);
                    if (scenario.equals("tencent")) {
                        var values = tencentMetadata(); values.put(KEY_SOURCE + "provider", "tencent-ssm"); composed = properties(composed, values);
                    } else if (scenario.contains("file")) {
                        var values = scenario.equals("stale-file") ? tencentMetadata() : new HashMap<String, Object>();
                        values.put(KEY_SOURCE + "provider", "file"); values.put(KEY_SOURCE + "bundle-path", path); composed = properties(composed, values);
                    }
                    composed.run(context -> {
                        assertThat(context).hasNotFailed().hasSingleBean(MediaPlaybackCredentialAuthority.class);
                        assertThat(context.getBeansOfType(MediaPlaybackSigningKeyProvider.class)).hasSize(2);
                        var authority = context.getBean(MediaPlaybackCredentialAuthority.class); assertUnavailable(authority);
                        assertThat(authority.issueOrReplay(null)).isInstanceOf(ShareMediaCredentialProvider.MediaAuthorizationCredentialResult.Unavailable.class);
                        var opaque = mock(OpaquePlaybackCredential.class); authority.verify(opaque); verifyNoInteractions(opaque);
                        for (MediaPlaybackSigningKeyProvider provider : context.getBeansOfType(MediaPlaybackSigningKeyProvider.class).values()) {
                            if (mockingDetails(provider).isMock()) verify(provider, never()).currentKeys();
                        }
                        for (Object client : clients.constructed()) verifyNoInteractions(client);
                        assertThat(sdk.constructed()).isEmpty(); assertThat(roles.constructed()).isEmpty();
                    });
                }
            }
    }

    @Test void explicitFileSelectionRemainsIndependentOfTencentClient() throws Exception {
        try (var tencent = mockConstruction(TencentSsmMediaPlaybackSigningKeyProvider.class); var clients = mockConstruction(cloudClientType());
                var sdk = mockConstruction(SsmClient.class); var roles = mockConstruction(CvmRoleCredential.class)) {
            String valid = fileBundle();
            for (String path : Arrays.asList(null, "", " ", "relative", "\0", "${missing.pa23.file}", valid)) {
                var values = tencentMetadata(); values.put(KEY_SOURCE + "provider", "file");
                if (path != null) values.put(KEY_SOURCE + "bundle-path", path);
                exactProperties(values).run(context -> {
                    assertThat(context).hasNotFailed().hasSingleBean(MediaPlaybackSigningKeyProvider.class);
                    assertThat(context.getBean(MediaPlaybackSigningKeyProvider.class)).isInstanceOf(FileMediaPlaybackSigningKeyProvider.class);
                    var authority = context.getBean(MediaPlaybackCredentialAuthority.class);
                    if (valid.equals(path)) assertThat(authority.verify(new OpaquePlaybackCredential(token(authority)))).isInstanceOf(MediaCredentialVerifier.Result.Verified.class);
                    else assertUnavailable(authority);
                });
            }
            assertThat(tencent.constructed()).isEmpty(); assertThat(clients.constructed()).isEmpty(); assertThat(sdk.constructed()).isEmpty(); assertThat(roles.constructed()).isEmpty();
        }
    }

    private static final String KEY_SOURCE = "onecall.sharing.media-playback.signing-key-source.";
    private static Class<?> cloudClientType() throws ClassNotFoundException {
        return Class.forName("com.onecall.core.sharing.internal.keysource.TencentSsmSecretStringClient");
    }
    private static Map<String, Object> tencentMetadata() {
        var values = new HashMap<String, Object>();
        values.put(KEY_SOURCE + "tencent.region", "test-region"); values.put(KEY_SOURCE + "tencent.secret-name", "synthetic-name");
        values.put(KEY_SOURCE + "tencent.version-id", "synthetic-version"); values.put(KEY_SOURCE + "tencent.connect-timeout-seconds", "2");
        values.put(KEY_SOURCE + "tencent.read-timeout-seconds", "3"); return values;
    }
    private ApplicationContextRunner exactProperties(Map<String, Object> values) {
        return properties(runner.withUserConfiguration(MediaPlaybackKeySourceConfiguration.class), values);
    }
    private static ApplicationContextRunner properties(ApplicationContextRunner base, Map<String, Object> values) {
        return base.withInitializer(context -> context.getEnvironment().getPropertySources().addFirst(new org.springframework.core.env.MapPropertySource("exact-pa23", values)));
    }

    private static void assertUnavailable(MediaCredentialVerifier verifier) {
        for(String malformed:List.of("pa20-positive-playback-token","ocmp1","ocmp1.bad.!.*"))
            assertThat(verifier.verify(new OpaquePlaybackCredential(malformed))).isInstanceOf(MediaCredentialVerifier.Result.Unavailable.class);
    }

    private static String validToken() {
        var beans=new org.springframework.beans.factory.support.DefaultListableBeanFactory();
        beans.registerSingleton("keys",DeterministicShareCommandProviders.mediaPlaybackSigningKeys());
        return token(new MediaPlaybackCredentialAuthority(beans.getBeanProvider(MediaPlaybackSigningKeyProvider.class),Clock.systemUTC()));
    }

    private static String token(MediaPlaybackCredentialAuthority authority) {
        Instant expiry=Instant.now().plusSeconds(60);
        var facts=new ShareMediaCredentialProvider.AuthorizedMediaFacts(new UUID(0,3),new UUID(0,4),"a".repeat(64),123,"audio/wav",10,0,10_000_000_000L);
        var issued=authority.issueOrReplay(new ShareMediaCredentialProvider.MediaAuthorizationCredentialRequest(new UUID(0,1),new UUID(0,2),"PLAYBACK","PLAYBACK_AUTHORIZED",
                "a".repeat(64),0,0,expiry,expiry,expiry,expiry,"a".repeat(64),"event",facts,null));
        return ((ShareMediaCredentialProvider.MediaAuthorizationCredentialResult.Issued)issued).credential();
    }

    private ApplicationContextRunner providers(ApplicationContextRunner base, AtomicInteger calls) {
        return providers(base, calls, new AtomicReference<>());
    }

    private ApplicationContextRunner providers(ApplicationContextRunner base, AtomicInteger calls, AtomicReference<VerifiedMediaClaim> observedClaim) {
        UUID mediaId = new UUID(0,4);
        return base.withBean(MediaPlaybackSigningKeyProvider.class, DeterministicShareCommandProviders::mediaPlaybackSigningKeys)
                .withBean(PlaybackStateReader.class, () -> value -> { observedClaim.set(value); calls.incrementAndGet(); return new PlaybackStateReader.Result.Allowed(Instant.now()); })
                .withBean(EvidenceMediaObjectReader.class, () -> evidenceId -> { calls.incrementAndGet(); return new EvidenceMediaObjectReader.Result.Found(new ResolvedMediaObject(mediaId, "a".repeat(64), 123, "audio/wav", 10)); })
                .withBean(ControlledMediaReader.class, () -> scope -> { calls.incrementAndGet(); return new ControlledMediaReader.Result.Opened(new ControlledMediaReader.ControlledReadHandle(scope)); });
    }
}
