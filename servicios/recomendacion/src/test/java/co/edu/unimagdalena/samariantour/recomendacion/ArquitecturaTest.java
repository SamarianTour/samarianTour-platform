package co.edu.unimagdalena.samariantour.recomendacion;

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

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * Reglas de arquitectura del servicio recomendacion, copiadas del catálogo (diagrama de paquetes, "Reglas de
 * arquitectura"). Un módulo es cada paquete de primer nivel bajo el paquete base, salvo {@code compartido}.
 * <p>No se copian las reglas de MongoDB ni del control de concurrencia: este servicio no tiene base de datos.
 */
class ArquitecturaTest {

    private static final String BASE = "co.edu.unimagdalena.samariantour.recomendacion";
    private static final String COMPARTIDO = "compartido";

    private static JavaClasses clases;

    @BeforeAll
    static void importarClases() {
        clases = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(BASE);
    }

    @Test
    @DisplayName("1. dominio no importa Spring, Jakarta, Jackson, la caché, el corte de circuito, aplicacion ni "
            + "infraestructura")
    void dominioEsJavaPuro() {
        noClasses().that().resideInAPackage("..dominio..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework..", "jakarta..", "com.fasterxml.jackson..", "lombok..",
                        "com.github.benmanes.caffeine..", "io.github.resilience4j..", "org.slf4j..",
                        "..aplicacion..", "..infraestructura..")
                .check(clases);
    }

    @Test
    @DisplayName("2. aplicacion solo depende de dominio (se permite @Service)")
    void aplicacionSoloDependeDeDominio() {
        classes().that().resideInAPackage("..aplicacion..")
                .should().onlyDependOnClassesThat().resideInAnyPackage(
                        "..dominio..", "..aplicacion..", "java..", "org.springframework.stereotype..")
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
    @DisplayName("7. los controladores solo llaman puertos de entrada; nunca puertos ni adaptadores de salida")
    void entradaSoloUsaPuertosDeEntrada() {
        noClasses().that().resideInAPackage("..infraestructura.entrada..")
                .should().dependOnClassesThat().resideInAnyPackage("..dominio.puerto.salida..", "..infraestructura.salida..")
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
