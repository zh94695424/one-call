package com.onecall.core.sharing.internal.keysource;

import com.onecall.core.sharing.internal.credential.MediaPlaybackSigningKeyProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration(proxyBeanMethods = false)
public class MediaPlaybackKeySourceConfiguration {
    @Bean
    @ConditionalOnProperty(prefix = "onecall.sharing.media-playback.signing-key-source", name = "provider",
            havingValue = "file", matchIfMissing = false)
    MediaPlaybackSigningKeyProvider fileMediaPlaybackSigningKeyProvider(Environment environment) {
        String path;
        try { path = environment.getProperty("onecall.sharing.media-playback.signing-key-source.bundle-path"); }
        catch (RuntimeException failure) { path = null; }
        return new FileMediaPlaybackSigningKeyProvider(path);
    }
}
