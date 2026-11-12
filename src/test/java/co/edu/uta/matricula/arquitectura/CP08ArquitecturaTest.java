package co.edu.uta.matricula.arquitectura;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * CP-08 · RNF-03 (mantenibilidad): verifica automáticamente que el código respete la
 * arquitectura en capas de la Fase 2 (ADR-001). Si alguien rompe una regla, la prueba falla.
 */
class CP08ArquitecturaTest {

    private static final String BASE = "co.edu.uta.matricula";
    private final JavaClasses clases = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(BASE);

    @Test
    @DisplayName("CP-08: cada capa solo usa la capa inferior (presentación → servicio → persistencia)")
    void capasRespetadas() {
        layeredArchitecture().consideringOnlyDependenciesInLayers()
                .layer("Presentacion").definedBy(BASE + ".presentacion..")
                .layer("Servicio").definedBy(BASE + ".servicio..")
                .layer("Persistencia").definedBy(BASE + ".persistencia..")
                .layer("Integracion").definedBy(BASE + ".integracion..")
                .layer("Dominio").definedBy(BASE + ".dominio..")
                .whereLayer("Presentacion").mayNotBeAccessedByAnyLayer()
                .whereLayer("Servicio").mayOnlyBeAccessedByLayers("Presentacion")
                .whereLayer("Persistencia").mayOnlyBeAccessedByLayers("Servicio")
                .whereLayer("Integracion").mayOnlyBeAccessedByLayers("Servicio")
                .check(clases);
    }

    @Test
    @DisplayName("CP-08: el dominio no depende de ninguna otra capa ni de Spring")
    void dominioIndependiente() {
        noClasses().that().resideInAPackage(BASE + ".dominio..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        BASE + ".servicio..", BASE + ".presentacion..", BASE + ".persistencia..",
                        BASE + ".integracion..", "org.springframework..")
                .check(clases);
    }

    @Test
    @DisplayName("CP-08: los controladores no acceden directamente a los repositorios")
    void controladoresSinRepositorios() {
        noClasses().that().resideInAPackage(BASE + ".presentacion..")
                .should().dependOnClassesThat().resideInAPackage(BASE + ".persistencia..")
                .check(clases);
    }

    @Test
    @DisplayName("CP-08: no hay dependencias cíclicas entre paquetes")
    void sinCiclos() {
        slices().matching(BASE + ".(*)..").should().beFreeOfCycles().check(clases);
    }
}
