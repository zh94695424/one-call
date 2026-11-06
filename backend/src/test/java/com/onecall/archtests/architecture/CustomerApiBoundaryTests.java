package com.onecall.archtests.architecture;

import static org.assertj.core.api.Assertions.*;

import com.onecall.core.OneCallApplication;
import com.onecall.core.customer_iam.internal.application.CustomerContext;
import com.onecall.core.customer_iam.internal.application.CustomerContextService;
import com.onecall.core.customer_iam.internal.application.SelectedCustomerWorkspace;
import com.onecall.core.customer_iam.internal.application.TrustedCustomerCaller;
import com.onecall.core.customer_iam.internal.persistence.CustomerContextRepository;
import com.onecall.core.customer_iam.internal.security.CustomerSessionVerifier;
import com.onecall.core.customer_iam.internal.web.CustomerMeController;
import com.onecall.core.customer_iam.internal.web.CustomerSharesController;
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
        assertThat(Arrays.stream(TrustedCustomerCaller.class.getRecordComponents()).map(component -> component.getName()))
                .containsExactly("accountId", "channel", "selectedWorkspace");
        assertThat(SelectedCustomerWorkspace.class.getRecordComponents()).hasSize(2);
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
        var sharesMapping = CustomerSharesController.class.getMethod("getCustomerShares", java.security.Principal.class,
                jakarta.servlet.http.HttpServletRequest.class).getAnnotation(org.springframework.web.bind.annotation.GetMapping.class);
        assertThat(sharesMapping.path()).containsExactly("/api/v1/customer/shares");
        assertThat(sharesMapping.produces()).containsExactly("application/json");
        var publicController = com.onecall.core.customer_iam.internal.web.PublicSharesController.class;
        var publicMethod = publicController.getMethod("getPublicShare", String.class, java.security.Principal.class);
        assertThat(publicMethod.getReturnType()).isEqualTo(com.onecall.core.sharing.internal.publicquery.PublicShareView.class);
        var publicMapping = publicMethod.getAnnotation(org.springframework.web.bind.annotation.GetMapping.class);
        assertThat(publicMapping.path()).containsExactly("/api/v1/public/shares/{capabilityToken}");
        assertThat(publicMapping.produces()).containsExactly("application/json");
        assertThat(publicMethod.getParameters()[0].getAnnotation(org.springframework.web.bind.annotation.PathVariable.class).value())
                .isEqualTo("capabilityToken");
        var constructor = publicController.getConstructors()[0];
        assertThat(Arrays.stream(constructor.getGenericParameterTypes()).map(java.lang.reflect.Type::getTypeName))
                .containsExactly("org.springframework.beans.factory.ObjectProvider<com.onecall.core.sharing.internal.publicquery.PublicShareQueryService>",
                        "org.springframework.beans.factory.ObjectProvider<com.onecall.core.customer_iam.internal.application.CustomerContextService>",
                        "com.onecall.core.customer_iam.internal.web.CustomerProblemWriter");
        assertThat(constructor.getParameters()[0].getAnnotation(org.springframework.beans.factory.annotation.Qualifier.class).value()).isEqualTo("publicShareQueryService");
        assertThat(constructor.getParameters()[1].getAnnotation(org.springframework.beans.factory.annotation.Qualifier.class).value()).isEqualTo("customerContextService");
    }

    @Test
    void customerDependenciesAndReadOnlySqlStayInsideBoundary() throws Exception {
        Path root = Path.of("src/main/java/com/onecall/core/customer_iam/internal");
        try (var files = Files.walk(root)) {
            var paths = files.filter(Files::isRegularFile).toList();
            assertThat(paths).hasSize(26);
            for (Path path : paths) {
                String source = Files.readString(path);
                assertThat(source).doesNotContain("com.onecall.core.organization_access",
                        "org.springframework.security", "org.flywaydb", "org.junit", "org.mockito", "io.jsonwebtoken",
                        "ThreadLocal", "getSession(", "RequestContextHolder", "Redis", "SELECT *", "FOR UPDATE", "FOR SHARE", "append_fact");
                if (!path.endsWith("CustomerSharesController.java") && !path.endsWith("CustomerSharePage.java")
                        && !path.endsWith("CustomerShareCursorCodec.java") && !path.endsWith("CustomerProblemWriter.java")
                        && !path.endsWith("PublicSharesController.java")) {
                    assertThat(source).doesNotContain("com.onecall.core.sharing");
                }
                if (path.endsWith("CustomerProblemWriter.java")) {
                    assertThat(source.lines().filter(line -> line.contains("com.onecall.core.sharing")))
                            .containsExactly("import com.onecall.core.sharing.internal.command.ShareCommandException;",
                                    "import com.onecall.core.sharing.internal.publicquery.PublicShareQueryException;");
                }
            }
        }
        String publicSource = Files.readString(root.resolve("web/PublicSharesController.java"));
        assertThat(publicSource.lines().filter(line -> line.startsWith("import com.onecall.core.sharing.")))
                .containsExactly("import com.onecall.core.sharing.internal.publicquery.OpaqueCapabilityToken;",
                        "import com.onecall.core.sharing.internal.publicquery.PublicShareQueryException;",
                        "import com.onecall.core.sharing.internal.publicquery.PublicShareQueryService;",
                        "import com.onecall.core.sharing.internal.publicquery.PublicShareView;");
        assertThat(publicSource).doesNotContain("Repository", "DSLContext", "DataSource", "PublicGrant", "getAttribute(",
                "getBean(", "Class.forName", "getDeclaredField", "getDeclaredMethod", "CustomerShareCommand");
        String shares = Files.readString(root.resolve("web/CustomerSharesController.java"));
        assertThat(shares).contains("CustomerShareQueryService", "listCustomerShares", "TrustedShareCaller")
                .doesNotContain("ShareQueryRepository", "DSLContext", "DataSource", "getAttribute(",
                        "ShareQueryDiagnostic", "new CustomerShareQueryService", "getBean(",
                        "Class.forName", "getDeclaredField", "getDeclaredMethod");
        assertThat(shares.lines().filter(line -> line.startsWith("import com.onecall.core.sharing.")))
                .containsExactlyInAnyOrder(
                        "import com.onecall.core.sharing.internal.application.CustomerShareQueryService;",
                        "import com.onecall.core.sharing.internal.application.ShareCursor;",
                        "import com.onecall.core.sharing.internal.application.SharePage;",
                        "import com.onecall.core.sharing.internal.application.SharePageRequest;",
                        "import com.onecall.core.sharing.internal.application.ShareQueryException;",
                        "import com.onecall.core.sharing.internal.application.TrustedShareCaller;",
                        "import com.onecall.core.sharing.internal.command.CustomerShareCommandService;",
                        "import com.onecall.core.sharing.internal.command.CustomerShareCommand;",
                        "import com.onecall.core.sharing.internal.command.CustomerShareCommandResult;",
                        "import com.onecall.core.sharing.internal.command.ShareCommandCaller;",
                        "import com.onecall.core.sharing.internal.command.ShareCommandException;");
        assertThat(shares.lines().filter(line -> line.startsWith("import com.onecall.core.sharing.internal.")))
                .allMatch(line -> line.startsWith("import com.onecall.core.sharing.internal.application.")
                        || line.startsWith("import com.onecall.core.sharing.internal.command."));
        String binder = Files.readString(root.resolve("web/CustomerShareCommandJsonBinder.java"));
        assertThat(binder).contains("STRICT_DUPLICATE_DETECTION", "FAIL_ON_TRAILING_TOKENS", "isObject()", "isIntegralNumber()")
                .doesNotContain("com.onecall.core.sharing");
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
        for (String name : List.of("web/CustomerWebConfiguration.java", "web/CustomerMeController.java",
                "web/CustomerSharesController.java", "web/CustomerApiErrorController.java", "web/PublicSharesController.java")) {
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
                .contains("DispatcherType.REQUEST", "Ordered.HIGHEST_PRECEDENCE", "new CustomerBoundaryFilter",
                        "import org.apache.tomcat.util.buf.EncodedSolidusHandling;",
                        "import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;",
                        "import org.springframework.boot.web.server.WebServerFactoryCustomizer;",
                        "WebServerFactoryCustomizer<TomcatServletWebServerFactory> customerEncodedSolidusHandlingCustomizer()",
                        "factory.addConnectorCustomizers(connector ->",
                        "connector.setEncodedSolidusHandling(EncodedSolidusHandling.PASS_THROUGH.getValue())",
                        "registration.addUrlPatterns(\"/*\")", "registration.setDispatcherTypes(DispatcherType.REQUEST)")
                .doesNotContain("DispatcherType.ERROR", "@Import", "new CustomerMeController", "Valve", "DECODE",
                        "System.setProperty", "encoded-solidus-handling", "addFilter(");
        String configuration = Files.readString(Path.of("src/main/java/com/onecall/core/customer_iam/internal/web/CustomerWebConfiguration.java"));
        assertThat(configuration.split("new CustomerBoundaryFilter", -1)).hasSize(2);
        assertThat(configuration.split("new FilterRegistrationBean", -1)).hasSize(2);
    }
}
