package com.onecall.archtests.architecture;

import static org.assertj.core.api.Assertions.*;

import com.onecall.core.OneCallApplication;
import com.onecall.core.customer_iam.internal.application.CustomerContext;
import com.onecall.core.customer_iam.internal.application.CustomerContextService;
import com.onecall.core.customer_iam.internal.application.TrustedCustomerCaller;
import com.onecall.core.customer_iam.internal.persistence.CustomerContextRepository;
import com.onecall.core.customer_iam.internal.security.CustomerSessionVerifier;
import com.onecall.core.customer_iam.internal.web.CustomerMeController;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class CustomerApiBoundaryTests {
    @Test
    void customerApiSignaturesAndProjectionAreExact() throws Exception {
        assertThat(CustomerContextService.class.getMethod("getCustomerMe", TrustedCustomerCaller.class).getReturnType()).isEqualTo(CustomerContext.class);
        assertThat(CustomerSessionVerifier.class.getDeclaredMethods()).hasSize(1);
        assertThat(CustomerContextRepository.class.getDeclaredMethods()).hasSize(1);
        assertThat(Arrays.stream(CustomerContext.class.getRecordComponents()).map(component -> component.getName()))
                .containsExactly("account", "workspaces", "channel");
        assertThat(Arrays.stream(CustomerContext.AccountSummary.class.getRecordComponents()).map(component -> component.getName()))
                .containsExactly("accountId", "displayName", "status");
        assertThat(Arrays.stream(CustomerContext.WorkspaceSummary.class.getRecordComponents()).map(component -> component.getName()))
                .containsExactly("workspaceType", "organizationId", "organizationDisplayName", "role", "status");
        var mapping = CustomerMeController.class.getMethod("getCustomerMe", java.security.Principal.class)
                .getAnnotation(org.springframework.web.bind.annotation.GetMapping.class);
        assertThat(mapping.path()).containsExactly("/api/v1/customer/me");
        assertThat(mapping.produces()).containsExactly("application/json");
    }

    @Test
    void customerDependenciesAndReadOnlySqlStayInsideBoundary() throws Exception {
        Path root = Path.of("src/main/java/com/onecall/core/customer_iam/internal");
        try (var files = Files.walk(root)) {
            var paths = files.filter(Files::isRegularFile).toList();
            assertThat(paths).hasSize(19);
            for (Path path : paths) {
                String source = Files.readString(path);
                assertThat(source).doesNotContain("com.onecall.core.sharing", "com.onecall.core.organization_access",
                        "org.springframework.security", "org.flywaydb", "org.junit", "org.mockito", "io.jsonwebtoken",
                        "ThreadLocal", "getSession(", "RequestContextHolder", "Redis", "SELECT *", "FOR UPDATE", "FOR SHARE", "append_fact");
            }
        }
        String sql = Files.readString(root.resolve("persistence/JooqCustomerContextRepository.java"));
        assertThat(sql).contains("a.id, a.display_name, a.status", "w.organization_id, w.organization_display_name, w.role",
                "m.status = ? AND o.status = ?", "a.id = ? AND a.status = ?", "ORDER BY w.organization_id ASC");
        assertThat(sql).doesNotContain("revision", "department", "auth_identity", "INSERT ", "UPDATE ", "DELETE ", "compareTo");
        ApplicationModules modules = ApplicationModules.of(OneCallApplication.class);
        assertThat(modules.stream().count()).isEqualTo(14);
        modules.verify();
    }

    @Test
    void profileAndVerifierArtifactsCannotEscapeIntoProduction() throws Exception {
        for (String name : List.of("web/CustomerWebConfiguration.java", "web/CustomerMeController.java", "web/CustomerApiErrorController.java")) {
            String source = Files.readString(Path.of("src/main/java/com/onecall/core/customer_iam/internal", name));
            assertThat(source).contains("@Profile(\"web\")", "ConditionalOnWebApplication.Type.SERVLET");
        }
        try (var files = Files.walk(Path.of("src/main/java"))) {
            for (Path path : files.filter(Files::isRegularFile).toList()) {
                assertThat(Files.readString(path)).doesNotContain("implements CustomerSessionVerifier", "@TestConfiguration",
                        "VerifiedCustomerSession(UUID.fromString", "@Profile(\"test\")", "@Profile(\"dev\")");
            }
        }
        assertThat(Files.readString(Path.of("src/main/java/com/onecall/core/customer_iam/internal/web/CustomerWebConfiguration.java")))
                .contains("DispatcherType.REQUEST", "Ordered.HIGHEST_PRECEDENCE", "new CustomerBoundaryFilter")
                .doesNotContain("DispatcherType.ERROR", "@Import", "new CustomerMeController");
    }
}
