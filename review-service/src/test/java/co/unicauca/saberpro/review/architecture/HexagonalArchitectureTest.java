package co.unicauca.saberpro.review.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

/**
 * Pruebas de arquitectura (ArchUnit): hacen cumplir la regla de dependencias
 * de la arquitectura limpia en cada compilacion. Si alguien importa Spring o
 * un adaptador desde el dominio, el build falla.
 *
 * <pre>
 *   infrastructure (adaptadores, Spring)  ->  application (casos de uso, puertos)  ->  domain
 * </pre>
 */
class HexagonalArchitectureTest {

    private static final String BASE = "co.unicauca.saberpro.review";

    private final JavaClasses classes = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(BASE);

    @Test
    void lasCapasSoloDependenHaciaAdentro() {
        layeredArchitecture().consideringOnlyDependenciesInLayers()
                .layer("Domain").definedBy(BASE + ".domain..")
                .layer("Application").definedBy(BASE + ".application..")
                .layer("Infrastructure").definedBy(BASE + ".infrastructure..")
                .whereLayer("Infrastructure").mayNotBeAccessedByAnyLayer()
                .whereLayer("Application").mayOnlyBeAccessedByLayers("Infrastructure")
                .whereLayer("Domain").mayOnlyBeAccessedByLayers("Application", "Infrastructure")
                .check(classes);
    }

    @Test
    void elDominioYLosCasosDeUsoNoConocenFrameworks() {
        noClasses().that().resideInAnyPackage(BASE + ".domain..", BASE + ".application..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework..", "jakarta..", "java.sql..", "org.slf4j..",
                        "com.fasterxml..", "co.unicauca.saberpro.common.web..", "co.unicauca.saberpro.common.events..",
                        "co.unicauca.saberpro.common.messaging..", "co.unicauca.saberpro.common.security..")
                .check(classes);
    }
}
