package com.vetsync.notification;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.beans.factory.annotation.Autowired;

/** Hace cumplir las reglas de capas e interfaces de AGENTS.md (secciones 3 y 4.2). */
@AnalyzeClasses(packages = "com.vetsync.notification", importOptions = ImportOption.DoNotIncludeTests.class)
class ArquitecturaTest {

    @ArchTest
    static final ArchRule DOMINIO_SIN_FRAMEWORKS = noClasses().that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage("org.springframework..", "jakarta.persistence..");

    @ArchTest
    static final ArchRule DOMINIO_NO_DEPENDE_DE_CAPAS_SUPERIORES = noClasses().that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage("..application..", "..infrastructure..");

    @ArchTest
    static final ArchRule APLICACION_NO_DEPENDE_DE_INFRAESTRUCTURA = noClasses().that().resideInAPackage("..application..")
            .should().dependOnClassesThat().resideInAPackage("..infrastructure..");

    @ArchTest
    static final ArchRule SERVICIOS_IMPLEMENTAN_UN_CASO_DE_USO = classes().that().resideInAPackage("..application.service..")
            .and().doNotHaveSimpleName("package-info")
            .should().implement(resideInAPackage("..application.port.in.."));

    @ArchTest
    static final ArchRule ADAPTERS_IMPLEMENTAN_UN_PUERTO_DE_SALIDA = classes().that().resideInAPackage("..infrastructure..")
            .and().haveSimpleNameEndingWith("Adapter")
            .should().implement(resideInAPackage("..application.port.out.."));

    @ArchTest
    static final ArchRule WEB_SOLO_CONOCE_PUERTOS_DE_ENTRADA = noClasses().that().resideInAPackage("..infrastructure.web..")
            .should().dependOnClassesThat().resideInAnyPackage("..application.service..", "..infrastructure.persistence..");

    @ArchTest
    static final ArchRule SIN_INYECCION_POR_CAMPO = noFields().should().beAnnotatedWith(Autowired.class);
}
