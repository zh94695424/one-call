package com.onecall.archtests;

import static org.assertj.core.api.Assertions.*;
import static com.onecall.archtests.support.MediaGatewayTestProviders.*;

import com.onecall.core.OneCallApplication;
import com.onecall.core.evidence.mediaread.ControlledMediaByteSource;
import com.onecall.core.sharing.internal.gateway.*;
import com.onecall.core.sharing.internal.playback.*;
import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.server.servlet.context.ServletWebServerApplicationContext;
import org.springframework.core.Ordered;

class MediaGatewayConfigurationTests {
    private WebApplicationContextRunner runner() {
        return new WebApplicationContextRunner().withPropertyValues("spring.profiles.active=web")
                .withUserConfiguration(MediaPlaybackConfiguration.class, MediaGatewayConfiguration.class);
    }
    private int invoke(org.springframework.context.ApplicationContext context) throws Exception {
        var registration = (FilterRegistrationBean<?>) context.getBean("mediaGatewayFilterRegistration");
        var response = new org.springframework.mock.web.MockHttpServletResponse();
        registration.getFilter().doFilter(request(null), response, (incoming, outgoing) -> fail("chain"));
        return response.getStatus();
    }
    @Test void nonWebProfilesDoNotRegisterGatewayOrActivateProviders() {
        for (String profile : List.of("default", "persistence")) {
            new ApplicationContextRunner().withPropertyValues("spring.profiles.active=" + profile)
                    .withUserConfiguration(MediaPlaybackConfiguration.class, MediaGatewayConfiguration.class).run(context -> {
                        assertThat(context).hasNotFailed().doesNotHaveBean(MediaGatewayFilter.class).doesNotHaveBean(ControlledMediaByteSource.class)
                                .doesNotHaveBean(javax.sql.DataSource.class);
                        assertThat(context.containsBean("mediaGatewayFilterRegistration")).isFalse();
                        assertThat(context.getBean(MediaPlaybackService.class).authorizeAndOpen(new OpaquePlaybackCredential(POSITIVE_HTTP_BEARER),
                                com.onecall.core.sharing.internal.publicadmission.MediaAuthorizationRequest.Purpose.PLAYBACK))
                                .isInstanceOf(MediaPlaybackService.Result.Unavailable.class);
                    });
        }
    }
    @Test void webGatewayPrecedesCustomerBoundaryAndDefaultBearerIsUnavailable() throws Exception {
        try (var context = new SpringApplicationBuilder(OneCallApplication.class).profiles("web")
                .properties("server.port=0", "spring.main.banner-mode=off").run(); var client = HttpClient.newHttpClient()) {
            int port = ((ServletWebServerApplicationContext) context).getWebServer().getPort();
            var gateway = (FilterRegistrationBean<?>) context.getBean("mediaGatewayFilterRegistration");
            var customer = (FilterRegistrationBean<?>) context.getBean("customerBoundaryFilterRegistration");
            assertThat(gateway.getOrder()).isEqualTo(Ordered.HIGHEST_PRECEDENCE);
            assertThat(customer.getOrder()).isEqualTo(Ordered.HIGHEST_PRECEDENCE + 1);
            assertThat(gateway.getUrlPatterns()).containsExactly("/*");
            for (boolean bearer : List.of(false, true)) {
                var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/internal/media-gateway/v1/playback"))
                        .timeout(Duration.ofSeconds(10));
                if (bearer) request.header("Authorization", "Bearer " + POSITIVE_HTTP_BEARER);
                var response = client.send(request.build(), HttpResponse.BodyHandlers.ofString());
                assertThat(response.statusCode()).isEqualTo(bearer ? 503 : 401);
                assertThat(response.body()).contains(bearer ? "MEDIA_UNAVAILABLE" : "UNAUTHENTICATED").doesNotContain(POSITIVE_HTTP_BEARER);
            }
            for (String path : List.of("/api/v1/customer/me", "/api/v1/public/shares/probe")) {
                var response = client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET().build(), HttpResponse.BodyHandlers.ofString());
                assertThat(response.statusCode()).isEqualTo(path.contains("customer") ? 401 : 503);
            }
            assertThat(context.getBeansOfType(ControlledMediaByteSource.class)).isEmpty();
            assertThat(context.getBeanFactory().containsSingleton("sharingAdmissionDataSource")).isFalse();
            assertThat(context.getBeanFactory().containsSingleton("evidenceMediaReadDataSource")).isFalse();
        }
        assertThat(Files.readString(Path.of("src/main/resources/application-web.yml"))).contains("log-request-details: false", "enabled: false");
    }
    @Test void singleExplicitByteAuthorityIsUsedInEitherRegistrationOrder() {
        runner().run(context -> { assertThat(context).hasNotFailed(); assertThat(invoke(context)).isEqualTo(503); });
        for (boolean first : List.of(true, false)) {
            var harness = new Harness();
            var configured = new WebApplicationContextRunner().withPropertyValues("spring.profiles.active=web");
            if (first) configured = configured.withUserConfiguration(MediaGatewayConfiguration.class);
            configured = configured.withBean(MediaPlaybackService.class, () -> harness.service)
                    .withBean(ControlledMediaByteSource.class, () -> harness.source);
            if (!first) configured = configured.withUserConfiguration(MediaGatewayConfiguration.class);
            configured.run(context -> {
                assertThat(context).hasNotFailed(); assertThat(context.getBean(ControlledMediaByteSource.class)).isSameAs(harness.source);
                assertThat(invoke(context)).isEqualTo(200); assertThat(harness.source.opens).hasValue(1);
            });
        }
        var harness = new Harness();
        new WebApplicationContextRunner().withPropertyValues("spring.profiles.active=web").withUserConfiguration(MediaGatewayConfiguration.class)
                .withBean(MediaPlaybackService.class, () -> harness.service).run(context -> assertThat(invoke(context)).isEqualTo(503));
        assertThat(harness.metadata).isEqualTo(1);
        runner().withBean(ControlledMediaByteSource.class, () -> harness.source).run(context -> assertThat(invoke(context)).isEqualTo(503));
        assertThat(harness.source.opens).hasValue(0);
    }
    @Test void duplicateByteAuthoritiesFailClosedEvenWithPrimary() {
        for (boolean primary : List.of(false, true)) {
            ControlledMediaByteSource source = new ControlledMediaByteSource() {
                @Override public Result open(OpenRequest request) { throw new AssertionError("Unexpected open"); }
                @Override public String toString() { throw new AssertionError("Provider disclosure"); }
            };
            runner().withBean("firstSource", ControlledMediaByteSource.class, () -> source)
                    .withBean("secondSource", ControlledMediaByteSource.class, () -> source, definition -> definition.setPrimary(primary))
                    .run(context -> {
                        assertThat(context).hasFailed();
                        assertThat(context.getStartupFailure()).hasRootCauseMessage("Ambiguous media byte authority");
                    });
        }
    }
}
