package com.onecall.archtests.architecture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.onecall.archfixtures.cycle.CycleFixtureRoot;
import com.onecall.archfixtures.forbidden.ForbiddenFixtureRoot;
import com.onecall.archfixtures.internal.InternalFixtureRoot;
import com.onecall.core.OneCallApplication;
import com.tngtech.archunit.core.importer.ImportOption;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.ApplicationModule;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.core.NamedInterface;
import org.springframework.modulith.core.Violations;

class ModuleBoundaryTests {

    private static final String OPEN_TOKEN = "\u00af\\_(\u30c4)_/\u00af";

    /**
     * Modulith 2.1.2's {@code ApplicationModules.of(Class)} applies ArchUnit's {@code DoNotIncludeTests},
     * which would hide test-source fixture modules. Fixture roots are analyzed with an explicit include-all
     * import option; production boundaries above continue to use the default class factory.
     */
    private static final ImportOption INCLUDE_TEST_FIXTURES = javaClass -> true;

    private static final Set<String> EXPECTED_MODULES = Set.of(
            "customer_iam", "organization_access", "operations_iam", "subject_number",
            "activation_consent", "ivr", "catalog_order", "entitlement_metering",
            "evidence", "sharing", "ai_orchestration", "notary",
            "operations_audit", "platform_reliability");

    private static final Map<String, Set<String>> EXPECTED_ALLOWED = Map.ofEntries(
            Map.entry("customer_iam", Set.of("sharing::customer-query", "sharing::customer-command")),
            Map.entry("organization_access", Set.of("customer_iam")),
            Map.entry("operations_iam", Set.of("operations_audit")),
            Map.entry("subject_number", Set.of()),
            Map.entry("activation_consent", Set.of("subject_number")),
            Map.entry("ivr", Set.of("subject_number", "activation_consent",
                    "entitlement_metering", "evidence")),
            Map.entry("catalog_order", Set.of("customer_iam", "subject_number")),
            Map.entry("entitlement_metering", Set.of("catalog_order", "subject_number")),
            Map.entry("evidence", Set.of("entitlement_metering")),
            Map.entry("sharing", Set.of("evidence::share-projection")),
            Map.entry("ai_orchestration", Set.of("evidence", "entitlement_metering")),
            Map.entry("notary", Set.of("evidence")),
            Map.entry("operations_audit", Set.of()),
            Map.entry("platform_reliability", Set.of()));

    @Test
    void discoversExactlyTheFrozenModuleSet() {
        ApplicationModules modules = ApplicationModules.of(OneCallApplication.class);
        assertThat(modules.stream().map(it -> it.getIdentifier().toString()))
                .containsExactlyInAnyOrderElementsOf(EXPECTED_MODULES);
    }

    @Test
    void everyExpectedModuleDeclaresApplicationModule() throws Exception {
        for (String name : EXPECTED_MODULES) {
            Class<?> marker = Class.forName(
                    "com.onecall.core." + name + ".ModuleMarker", false,
                    getClass().getClassLoader());
            assertThat(marker.getPackage().isAnnotationPresent(ApplicationModule.class))
                    .as(name + " must declare @ApplicationModule")
                    .isTrue();
        }
    }

    @Test
    void assertsExactAllowedDependencyMapFromPlan62() throws Exception {
        for (Map.Entry<String, Set<String>> expected : EXPECTED_ALLOWED.entrySet()) {
            Class<?> marker = Class.forName(
                    "com.onecall.core." + expected.getKey() + ".ModuleMarker", false,
                    getClass().getClassLoader());
            ApplicationModule annotation =
                    marker.getPackage().getAnnotation(ApplicationModule.class);
            assertThat(annotation)
                    .as(expected.getKey() + " must declare @ApplicationModule")
                    .isNotNull();
            Set<String> actual =
                    new HashSet<>(Arrays.asList(annotation.allowedDependencies()));
            assertThat(actual)
                    .as(expected.getKey() + " must not use default OPEN_TOKEN")
                    .doesNotContain(OPEN_TOKEN);
            assertThat(actual)
                    .as(expected.getKey() + " allowedDependencies must equal plan \u00a76.2")
                    .containsExactlyInAnyOrderElementsOf(expected.getValue());
        }
    }

    @Test
    void discoveredProductionEdgesStayInsideTheConfiguredMap() {
        ApplicationModules modules = ApplicationModules.of(OneCallApplication.class);
        Map<String, Set<String>> discovered = new HashMap<>();
        modules.stream().forEach(module -> discovered.put(
                module.getIdentifier().toString(),
                module.getDirectDependencies(modules)
                        .uniqueModules()
                        .map(it -> it.getIdentifier().toString())
                        .collect(Collectors.toSet())));
        discovered.forEach((name, targets) -> assertThat(EXPECTED_ALLOWED.get(name).stream()
                .map(dependency -> dependency.split("::", 2)[0]).collect(Collectors.toSet()))
                .as(name + " discovered edges must stay within allowedDependencies")
                .containsAll(targets));
        assertThat(discovered.values().stream().flatMap(Set::stream))
                .as("exactly two production edges exist: customer_iam -> sharing -> evidence")
                .containsExactlyInAnyOrder("sharing", "evidence");
    }

    @Test
    void verifiesNoCyclesOrBoundaryViolations() {
        ApplicationModules modules = ApplicationModules.of(OneCallApplication.class);
        modules.verify();
        var allowed = modules.getModuleByName("customer_iam").orElseThrow()
                .getAllowedDependencies(modules);
        assertThat(allowed).hasSize(2);
        assertThat(allowed).extracting(dependency -> dependency.getTargetModule().getIdentifier().toString(),
                dependency -> dependency.getTargetNamedInterface().getName()).containsExactlyInAnyOrder(
                        org.assertj.core.api.Assertions.tuple("sharing", "customer-query"),
                        org.assertj.core.api.Assertions.tuple("sharing", "customer-command"));
        var sharing = modules.getModuleByName("sharing").orElseThrow().getNamedInterfaces();
        assertThat(sharing.stream().filter(NamedInterface::isNamed).map(NamedInterface::getName))
                .containsExactlyInAnyOrder("customer-query", "customer-command");
        assertThat(sharing.getByName("customer-query")).isPresent();
        assertThat(sharing.getByName("customer-qurey")).isEmpty();
        ImportOption withoutCustomerQuery = location -> {
            String path = location.asURI().getPath();
            if (path.contains("/com/onecall/archtests/")
                    || path.contains("/com/onecall/archfixtures/")) return false;
            return !path.endsWith("/com/onecall/core/sharing/internal/application/package-info.class");
        };
        assertThatThrownBy(() -> ApplicationModules.of(OneCallApplication.class, withoutCustomerQuery).verify())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageStartingWith("No named interface named 'customer-query' found!");
        ImportOption withoutCustomerCommand = location -> {
            String path = location.asURI().getPath();
            if (path.contains("/com/onecall/archtests/") || path.contains("/com/onecall/archfixtures/")) return false;
            return !path.endsWith("/com/onecall/core/sharing/internal/command/package-info.class");
        };
        assertThatThrownBy(() -> ApplicationModules.of(OneCallApplication.class, withoutCustomerCommand).verify())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageStartingWith("No named interface named 'customer-command' found!");
    }

    @Test
    void rejectsCycleFixture() {
        assertThatThrownBy(() -> ApplicationModules
                        .of(CycleFixtureRoot.class, INCLUDE_TEST_FIXTURES)
                        .verify())
                .isInstanceOf(Violations.class);
    }

    @Test
    void rejectsForbiddenDependencyFixture() {
        assertThatThrownBy(() -> ApplicationModules
                        .of(ForbiddenFixtureRoot.class, INCLUDE_TEST_FIXTURES)
                        .verify())
                .isInstanceOf(Violations.class);
    }

    @Test
    void rejectsInternalPackageAccessFixture() {
        assertThatThrownBy(() -> ApplicationModules
                        .of(InternalFixtureRoot.class, INCLUDE_TEST_FIXTURES)
                        .verify())
                .isInstanceOf(Violations.class);
        ApplicationModules fixture = ApplicationModules.of(InternalFixtureRoot.class, INCLUDE_TEST_FIXTURES);
        var providerInterfaces = fixture.getModuleByName("provider").orElseThrow().getNamedInterfaces();
        assertThat(providerInterfaces.stream().filter(NamedInterface::isNamed).map(NamedInterface::getName))
                .isEmpty();
        assertThat(providerInterfaces.getUnnamedInterface().asJavaClasses()
                .map(com.tngtech.archunit.core.domain.JavaClass::getSimpleName))
                .contains("Provider")
                .doesNotContain("Internal");
        ApplicationModules production = ApplicationModules.of(OneCallApplication.class);
        var sharing = production.getModuleByName("sharing").orElseThrow().getNamedInterfaces();
        assertThat(sharing.stream().filter(NamedInterface::isNamed).map(NamedInterface::getName))
                .containsExactlyInAnyOrder("customer-query", "customer-command");
        var allowed = production.getModuleByName("customer_iam").orElseThrow()
                .getAllowedDependencies(production);
        assertThat(allowed).hasSize(2);
        assertThat(allowed).extracting(dependency -> dependency.getTargetModule().getIdentifier().toString(),
                dependency -> dependency.getTargetNamedInterface().getName()).containsExactlyInAnyOrder(
                        org.assertj.core.api.Assertions.tuple("sharing", "customer-query"),
                        org.assertj.core.api.Assertions.tuple("sharing", "customer-command"));
    }
}
