package com.onecall.archtests;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.onecall.core.evidence.mediaread.*;
import com.onecall.core.sharing.internal.admission.*;
import com.onecall.core.sharing.internal.playback.*;
import com.onecall.core.sharing.internal.publicadmission.MediaAuthorizationRequest.Purpose;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

class MediaPlaybackConfigurationTests {
    private final ApplicationContextRunner runner = new ApplicationContextRunner().withAllowBeanDefinitionOverriding(false)
            .withUserConfiguration(MediaPlaybackConfiguration.class);

    @Test void defaultAndWebProfilesProvideOnlyUnavailablePlaybackProviders() {
        for (String profile : List.of("default", "web")) runner.withPropertyValues("spring.profiles.active=" + profile).run(context -> {
            assertThat(context).hasNotFailed().hasSingleBean(MediaPlaybackService.class);
            assertThat(context).doesNotHaveBean(MediaCredentialVerifier.class).doesNotHaveBean(ControlledMediaReader.class)
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

    @Test void explicitVerifierEvidenceStateAndReaderAreUsedWithoutReplacement() {
        for (boolean configurationFirst : List.of(true, false)) {
            AtomicInteger calls = new AtomicInteger();
            var configured = new ApplicationContextRunner().withAllowBeanDefinitionOverriding(false);
            if (configurationFirst) configured = configured.withUserConfiguration(MediaPlaybackConfiguration.class);
            configured = providers(configured, calls);
            if (!configurationFirst) configured = configured.withUserConfiguration(MediaPlaybackConfiguration.class);
            configured.run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context.getBean(MediaPlaybackService.class).authorizeAndOpen(new OpaquePlaybackCredential("fixture"), Purpose.PLAYBACK))
                        .isInstanceOf(MediaPlaybackService.Result.Opened.class);
                assertThat(calls).hasValue(4);
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

    @Test void missingEvidenceOrProvidersFailClosedWithoutProductionAdapters() {
        for (Class<?> missing : List.of(PlaybackStateReader.class, EvidenceMediaObjectReader.class, MediaCredentialVerifier.class, ControlledMediaReader.class)) {
            providers(runner, new AtomicInteger()).withInitializer(context ->
                    context.addBeanFactoryPostProcessor(factory -> {
                        var registry = (BeanDefinitionRegistry) factory;
                        for (String name : factory.getBeanNamesForType(missing)) registry.removeBeanDefinition(name);
                    })).run(context -> {
                        assertThat(context).hasNotFailed();
                        var result = context.getBean(MediaPlaybackService.class).authorizeAndOpen(new OpaquePlaybackCredential("fixture"), Purpose.PLAYBACK);
                        assertThat(result).isEqualTo(new MediaPlaybackService.Result.Unavailable());
                        assertThat(result.toString()).doesNotContain("bucket", "jdbc", "password");
                    });
        }
    }

    private ApplicationContextRunner providers(ApplicationContextRunner base, AtomicInteger calls) {
        Instant now = Instant.now(); UUID mediaId = UUID.randomUUID();
        var claim = new VerifiedMediaClaim("onecall:media-playback:v1", UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), mediaId,
                0, 0, Purpose.PLAYBACK, now.minusSeconds(1), now.minusSeconds(1), now.plusSeconds(60), 10,
                new VerifiedMediaClaim.PlaybackWindow(Duration.ZERO, Duration.ofSeconds(10)));
        return base.withBean(MediaCredentialVerifier.class, () -> credential -> { calls.incrementAndGet(); return new MediaCredentialVerifier.Result.Verified(claim); })
                .withBean(PlaybackStateReader.class, () -> value -> { calls.incrementAndGet(); return new PlaybackStateReader.Result.Allowed(now); })
                .withBean(EvidenceMediaObjectReader.class, () -> evidenceId -> { calls.incrementAndGet(); return new EvidenceMediaObjectReader.Result.Found(new ResolvedMediaObject(mediaId, "a".repeat(64), 123, "audio/wav", 10)); })
                .withBean(ControlledMediaReader.class, () -> scope -> { calls.incrementAndGet(); return new ControlledMediaReader.Result.Opened(new ControlledMediaReader.ControlledReadHandle(scope)); });
    }
}
