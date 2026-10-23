package co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.salida.demo;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Oferta;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.demo.DatosSantaMarta;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.modelo.EstadoOpinion;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.modelo.HabilitacionOpinion;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.modelo.Opinion;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Las opiniones de demostración son coherentes con las ofertas de demostración del catálogo. La prueba sí puede
 * mirar los dos módulos: la regla de dependencias es para el código de producción.
 */
class DatosOpinionesTest {

    private final List<Opinion> opiniones = DatosOpiniones.opiniones();
    private final Map<String, Oferta> ofertas = DatosSantaMarta.ofertas().stream()
            .collect(Collectors.toMap(Oferta::id, Function.identity()));

    @Test
    @DisplayName("9 aprobadas en 4 ofertas visibles, 2 en moderación y 1 rechazada")
    void reparto() {
        Map<EstadoOpinion, Long> porEstado = opiniones.stream()
                .collect(Collectors.groupingBy(Opinion::estado, Collectors.counting()));

        assertThat(porEstado).containsEntry(EstadoOpinion.APROBADA, 9L)
                .containsEntry(EstadoOpinion.EN_MODERACION, 2L)
                .containsEntry(EstadoOpinion.RECHAZADA, 1L);
        assertThat(opiniones.stream().filter(Opinion::estaAprobada).map(Opinion::ofertaId).distinct())
                .hasSize(4)
                .allSatisfy(ofertaId -> assertThat(ofertas.get(ofertaId).estaVisible()).isTrue());
    }

    @Test
    @DisplayName("cada opinión es de una oferta que existe, con ids ObjectId que no se repiten (DC-30)")
    void identificadores() {
        assertThat(opiniones).extracting(Opinion::id).allMatch(ObjectId::isValid).doesNotHaveDuplicates();
        assertThat(opiniones).extracting(Opinion::ofertaId).allMatch(ofertas::containsKey);
        assertThat(DatosOpiniones.habilitaciones()).extracting(HabilitacionOpinion::ofertaId)
                .allMatch(ofertas::containsKey);
    }

    @Test
    @DisplayName("una opinión por turista y oferta, y cada una con su habilitación (RF-60, RF-61)")
    void unaPorTuristaYOferta() {
        List<HabilitacionOpinion> pares = opiniones.stream()
                .map(opinion -> new HabilitacionOpinion(opinion.turistaId(), opinion.ofertaId()))
                .toList();

        assertThat(new HashSet<>(pares)).hasSameSizeAs(pares);
        assertThat(DatosOpiniones.habilitaciones()).containsAll(pares).doesNotHaveDuplicates();
    }
}
