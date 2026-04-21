package net.fmjaeschke.quantumhealth;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "net.fmjaeschke.quantumhealth")
class HexagonalArchitectureTest {

    @ArchTest
    static final ArchRule domain_must_not_depend_on_application_or_infrastructure = noClasses().that()
            .resideInAPackage("..domain..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("..application..", "..infrastructure..");

    @ArchTest
    static final ArchRule domain_must_not_use_frameworks = noClasses().that()
            .resideInAPackage("..domain..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("io.quarkus..", "jakarta..", "io.quarkiverse..", "io.smallrye..");

    @ArchTest
    static final ArchRule application_must_not_depend_on_infrastructure = noClasses().that()
            .resideInAPackage("..application..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("..infrastructure..");

    @ArchTest
    static final ArchRule incoming_ports_must_be_interfaces = classes().that()
            .resideInAPackage("..application.ports.in..")
            .should()
            .beInterfaces();

    @ArchTest
    static final ArchRule outgoing_ports_must_be_interfaces = classes().that()
            .resideInAPackage("..application.ports.out..")
            .should()
            .beInterfaces();
}
