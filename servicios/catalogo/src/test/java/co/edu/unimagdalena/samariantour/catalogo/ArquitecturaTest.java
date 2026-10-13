package co.edu.unimagdalena.samariantour.catalogo;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.repository.Repository;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * Reglas de arquitectura del servicio catalogo (diagrama de paquetes, "Reglas de arquitectura").
 * Un módulo es cada paquete de primer nivel bajo el paquete base, salvo {@code compartido}.
 */
class ArquitecturaTest {

    private static final String BASE = "co.edu.unimagdalena.samariantour.catalogo";
    private static final String COMPARTIDO = "compartido";

    private static JavaClasses clases;

    @BeforeAll
    static void importarClases() {
        clases = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(BASE);
    }

    @Test
    @DisplayName("1. dominio no importa Spring, Jakarta, Jackson, MongoDB, aplicacion ni infraestructura")
    void dominioEsJavaPuro() {
        noClasses().that().resideInAPackage("..dominio..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework..", "jakarta..", "com.fasterxml.jackson..",
                        "org.bson..", "com.mongodb..", "lombok..",
                        "..aplicacion..", "..infraestructura..")
                .check(clases);
    }

    @Test
    @DisplayName("2. aplicacion solo depende de dominio y del control de concurrencia (se permiten @Service y "
            + "@Transactional; ADR-05)")
    void aplicacionSoloDependeDeDominio() {
        classes().that().resideInAPackage("..aplicacion..")
                .should().onlyDependOnClassesThat().resideInAnyPackage(
                        "..dominio..", "..aplicacion..", "java..", BASE + ".compartido.concurrencia..",
                        "org.springframework.stereotype..", "org.springframework.transaction.annotation..")
                .check(clases);
    }

    @Test
    @DisplayName("3. ningún paquete depende de infraestructura")
    void nadieDependeDeInfraestructura() {
        noClasses().that().resideOutsideOfPackage("..infraestructura..")
                .should().dependOnClassesThat().resideInAPackage("..infraestructura..")
                .check(clases);
    }

    @Test
    @DisplayName("4. un módulo solo importa el paquete api de otro módulo")
    void otrosModulosSoloPorSuApi() {
        classes().should(usarDeOtrosModulosSoloElPaqueteApi()).check(clases);
    }

    @Test
    @DisplayName("5. los módulos no forman ciclos")
    void modulosSinCiclos() {
        slices().matching(BASE + ".(*)..").should().beFreeOfCycles().check(clases);
    }

    @Test
    @DisplayName("6. compartido no importa ningún módulo")
    void compartidoNoImportaModulos() {
        noClasses().that().resideInAPackage(BASE + "." + COMPARTIDO + "..")
                .should().dependOnClassesThat(pertenecenAUnModulo())
                .check(clases);
    }

    @Test
    @DisplayName("7. los controladores solo llaman puertos de entrada; nunca repositorios ni puertos de salida")
    void entradaSoloUsaPuertosDeEntrada() {
        noClasses().that().resideInAPackage("..infraestructura.entrada..")
                .should().dependOnClassesThat().resideInAnyPackage("..dominio.puerto.salida..", "..infraestructura.salida..")
                .check(clases);
    }

    @Test
    @DisplayName("8. los documentos Mongo y los repositorios viven en infraestructura/salida/persistencia")
    void persistenciaEnSuPaquete() {
        classes().that().areAnnotatedWith(Document.class)
                .or().areAssignableTo(Repository.class)
                .should().resideInAPackage("..infraestructura.salida.persistencia..")
                .allowEmptyShould(true)
                .check(clases);
    }

    @Test
    @DisplayName("9. el control de concurrencia es Java puro: lo usa aplicacion (ADR-05)")
    void concurrenciaEsJavaPuro() {
        noClasses().that().resideInAPackage(BASE + ".compartido.concurrencia..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework..", "jakarta..", "com.fasterxml.jackson..", "org.bson..", "com.mongodb..")
                .check(clases);
    }

    /** Módulo al que pertenece la clase, o null si es compartido, la clase Application o ajena al servicio. */
    private static String moduloDe(JavaClass clase) {
        String paquete = clase.getBaseComponentType().getPackageName();
        if (!paquete.startsWith(BASE + ".")) {
            return null;
        }
        String resto = paquete.substring(BASE.length() + 1);
        String modulo = resto.contains(".") ? resto.substring(0, resto.indexOf('.')) : resto;
        return COMPARTIDO.equals(modulo) ? null : modulo;
    }

    private static DescribedPredicate<JavaClass> pertenecenAUnModulo() {
        return DescribedPredicate.describe("pertenecen a un módulo", clase -> moduloDe(clase) != null);
    }

    private static ArchCondition<JavaClass> usarDeOtrosModulosSoloElPaqueteApi() {
        return new ArchCondition<>("usar de otros módulos solo su paquete api") {
            @Override
            public void check(JavaClass origen, ConditionEvents eventos) {
                String moduloOrigen = moduloDe(origen);
                if (moduloOrigen == null) {
                    return;
                }
                for (Dependency dependencia : origen.getDirectDependenciesFromSelf()) {
                    JavaClass destino = dependencia.getTargetClass().getBaseComponentType();
                    String moduloDestino = moduloDe(destino);
                    if (moduloDestino == null || moduloDestino.equals(moduloOrigen)) {
                        continue;
                    }
                    String api = BASE + "." + moduloDestino + ".api";
                    String paquete = destino.getPackageName();
                    if (!paquete.equals(api) && !paquete.startsWith(api + ".")) {
                        eventos.add(SimpleConditionEvent.violated(dependencia, dependencia.getDescription()));
                    }
                }
            }
        };
    }
}
