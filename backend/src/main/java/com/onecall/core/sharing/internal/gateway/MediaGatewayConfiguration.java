package com.onecall.core.sharing.internal.gateway;

import com.onecall.core.evidence.mediaread.ControlledMediaByteSource;
import com.onecall.core.sharing.internal.playback.MediaPlaybackService;
import jakarta.servlet.DispatcherType;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;

@Configuration(proxyBeanMethods = false)
@Profile("web")
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class MediaGatewayConfiguration {
    @Bean
    FilterRegistrationBean<MediaGatewayFilter> mediaGatewayFilterRegistration(MediaPlaybackService service,
            ObjectProvider<ControlledMediaByteSource> sources) {
        var authorities = sources.stream().limit(2).toList();
        if (authorities.size() > 1) throw new IllegalStateException("Ambiguous media byte authority");
        ControlledMediaByteSource source = authorities.isEmpty()
                ? request -> new ControlledMediaByteSource.Result.Unavailable() : authorities.getFirst();
        var registration = new FilterRegistrationBean<>(new MediaGatewayFilter(service, source, new MediaGatewayProblemWriter()));
        registration.addUrlPatterns("/*");
        registration.setDispatcherTypes(DispatcherType.REQUEST);
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }
}
