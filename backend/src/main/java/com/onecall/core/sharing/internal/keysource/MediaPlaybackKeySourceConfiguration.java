package com.onecall.core.sharing.internal.keysource;

import com.onecall.core.sharing.internal.credential.MediaPlaybackSigningKeyProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;

@Configuration(proxyBeanMethods = false)
public class MediaPlaybackKeySourceConfiguration {
    private static final String PREFIX = "onecall.sharing.media-playback.signing-key-source.";

    @Bean
    @Conditional(FileSelection.class)
    MediaPlaybackSigningKeyProvider fileMediaPlaybackSigningKeyProvider(Environment environment) {
        String path;
        try { path = environment.getProperty("onecall.sharing.media-playback.signing-key-source.bundle-path"); }
        catch (RuntimeException failure) { path = null; }
        return new FileMediaPlaybackSigningKeyProvider(path);
    }

    @Bean
    @Conditional(TencentSelection.class)
    MediaPlaybackSigningKeyProvider tencentSsmMediaPlaybackSigningKeyProvider(Environment environment) {
        TencentSsmMediaPlaybackSigningKeyProvider.Settings settings;
        try {
            settings = TencentSsmMediaPlaybackSigningKeyProvider.Settings.create(
                    environment.getProperty(PREFIX + "tencent.region"), environment.getProperty(PREFIX + "tencent.secret-name"),
                    environment.getProperty(PREFIX + "tencent.version-id"), environment.getProperty(PREFIX + "tencent.connect-timeout-seconds"),
                    environment.getProperty(PREFIX + "tencent.read-timeout-seconds"));
        } catch (RuntimeException failure) { settings = null; }
        return new TencentSsmMediaPlaybackSigningKeyProvider(settings);
    }

    static final class FileSelection implements Condition {
        @Override public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            try { return "file".equals(context.getEnvironment().getProperty(PREFIX + "provider")); }
            catch (RuntimeException failure) { return false; }
        }
    }

    static final class TencentSelection implements Condition {
        @Override public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            try {
                return "tencent-ssm".equals(context.getEnvironment().getProperty(PREFIX + "provider"))
                        && !context.getEnvironment().containsProperty(PREFIX + "bundle-path");
            } catch (RuntimeException failure) { return false; }
        }
    }
}
