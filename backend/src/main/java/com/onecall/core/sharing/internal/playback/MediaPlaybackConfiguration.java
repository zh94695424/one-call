package com.onecall.core.sharing.internal.playback;

import com.onecall.core.evidence.mediaread.EvidenceMediaObjectReader;
import com.onecall.core.sharing.internal.admission.ShareAdmissionRepository;
import com.onecall.core.sharing.internal.admission.ShareAdmissionSha256Provider;
import java.time.Clock;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration(proxyBeanMethods = false)
public class MediaPlaybackConfiguration {
    @Bean
    MediaPlaybackService mediaPlaybackService(ObjectProvider<MediaCredentialVerifier> verifier,
            ObjectProvider<PlaybackStateReader> state, ObjectProvider<EvidenceMediaObjectReader> evidence,
            ObjectProvider<ControlledMediaReader> reader) {
        return new MediaPlaybackService(resolve(verifier, unavailableMediaCredentialVerifier()),
                resolve(state, claim -> new PlaybackStateReader.Result.Unavailable()),
                resolve(evidence, evidenceId -> new EvidenceMediaObjectReader.Result.Unavailable()),
                resolve(reader, unavailableControlledMediaReader()), Clock.systemUTC());
    }

    private static <T> T resolve(ObjectProvider<T> provider, T fallback) {
        if (provider.stream().limit(2).count() > 1) throw new IllegalStateException("Ambiguous playback authority");
        return provider.getIfAvailable(() -> fallback);
    }
    private MediaCredentialVerifier unavailableMediaCredentialVerifier() {
        return credential -> new MediaCredentialVerifier.Result.Unavailable();
    }
    private ControlledMediaReader unavailableControlledMediaReader() {
        return scope -> new ControlledMediaReader.Result.Unavailable();
    }

    @Configuration(proxyBeanMethods = false)
    @Profile("persistence")
    @ConditionalOnProperty(prefix = "onecall.sharing-admission.datasource", name = "enabled", havingValue = "true")
    static class PersistenceStateConfiguration {
        @Bean
        PlaybackStateReader playbackStateReader(@Qualifier("sharingAdmissionTransactionTemplate") TransactionTemplate transactions,
                ShareAdmissionRepository repository, @Qualifier("sharingAdmissionSha256Provider") ShareAdmissionSha256Provider sha256) {
            return new SharingPlaybackStateReader(transactions, repository, sha256);
        }
    }
}
