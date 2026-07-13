package com.werkpilot;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;
import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import jakarta.persistence.Entity;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class ArchitectureRulesTest {

    private static final String BASE_PACKAGE = "com.werkpilot";
    private static final List<String> FEATURE_MODULES = List.of(
            "identity",
            "masterdata",
            "importing",
            "production",
            "energy",
            "quality",
            "downtime",
            "analytics",
            "maintenance",
            "reporting",
            "audit");
    private static final List<String> FEATURE_LAYERS = List.of(
            "api",
            "application",
            "application.port",
            "domain",
            "persistence");
    private static final List<String> SHARED_PACKAGES = List.of(
            "shared",
            "shared.api",
            "shared.error",
            "shared.time",
            "shared.validation");

    private final JavaClasses productionClasses = new ClassFileImporter()
            .withImportOption(new ImportOption.DoNotIncludeTests())
            .importPackages(BASE_PACKAGE);

    @Test
    void requiredModuleAndLayerPackagesExist() {
        FEATURE_MODULES.forEach(module -> {
            assertPackageExists(module);
            FEATURE_LAYERS.forEach(layer -> assertPackageExists(module + "." + layer));
        });
        SHARED_PACKAGES.forEach(this::assertPackageExists);
    }

    @Test
    void apiLayerDoesNotDependOnDomainOrPersistence() {
        ArchRule rule = noClasses()
                .that()
                .resideInAPackage("..api..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("..domain..", "..persistence..");

        rule.check(productionClasses);
    }

    @Test
    void applicationLayerDoesNotDependOnApiOrPersistence() {
        ArchRule rule = noClasses()
                .that()
                .resideInAPackage("..application..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("..api..", "..persistence..");

        rule.check(productionClasses);
    }

    @Test
    void domainLayerIsIsolatedFromOuterLayers() {
        ArchRule rule = noClasses()
                .that()
                .resideInAPackage("..domain..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("..api..", "..application..", "..persistence..");

        rule.check(productionClasses);
    }

    @Test
    void persistenceTypesDoNotCrossIntoApiOrOtherModules() {
        noClasses()
                .that()
                .resideInAPackage("..api..")
                .should()
                .accessClassesThat()
                .resideInAPackage("..persistence..")
                .check(productionClasses);

        FEATURE_MODULES.forEach(module -> noClasses()
                .that()
                .resideOutsideOfPackage(BASE_PACKAGE + "." + module + ".persistence..")
                .and()
                .resideOutsideOfPackage(BASE_PACKAGE + "." + module + ".application..")
                .and()
                .resideOutsideOfPackage(BASE_PACKAGE + "." + module + ".domain..")
                .should()
                .accessClassesThat()
                .resideInAPackage(BASE_PACKAGE + "." + module + ".persistence..")
                .check(productionClasses));
    }

    @Test
    void jpaEntitiesNeverCrossRestBoundary() {
        noClasses()
                .that()
                .resideInAPackage("..api..")
                .should()
                .beAnnotatedWith(Entity.class)
                .check(productionClasses);
    }

    @Test
    void classNamingMatchesApprovedLayers() {
        classes()
                .that()
                .haveSimpleNameEndingWith("Controller")
                .should()
                .resideInAPackage("..api..")
                .allowEmptyShould(true)
                .check(productionClasses);

        classes()
                .that()
                .haveSimpleNameEndingWith("Service")
                .should()
                .resideInAPackage("..application..")
                .allowEmptyShould(true)
                .check(productionClasses);

        classes()
                .that()
                .haveSimpleNameEndingWith("Repository")
                .should()
                .resideInAPackage("..persistence..")
                .allowEmptyShould(true)
                .check(productionClasses);
    }

    @Test
    void modulesAreFreeOfCycles() {
        slices()
                .matching(BASE_PACKAGE + ".(*)..")
                .should()
                .beFreeOfCycles()
                .check(productionClasses);
    }

    private void assertPackageExists(String packageName) {
        Path packageInfo = Path.of("src/main/java")
                .resolve((BASE_PACKAGE + "." + packageName).replace('.', '/'))
                .resolve("package-info.java");

        assertThat(Files.exists(packageInfo))
                .as("Package marker should exist for %s.%s", BASE_PACKAGE, packageName)
                .isTrue();
    }
}
