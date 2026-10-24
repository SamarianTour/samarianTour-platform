package co.edu.unimagdalena.samariantour.recomendacion.recomendacion.infraestructura.salida.reglas;

import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.Idioma;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.OfertaCandidata;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.OfertaEvaluada;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.Preferencias;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.Recomendacion;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.politica.ReglaCriteriosExplicitos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static co.edu.unimagdalena.samariantour.recomendacion.recomendacion.OfertasDePrueba.MINCA;
import static co.edu.unimagdalena.samariantour.recomendacion.recomendacion.OfertasDePrueba.oferta;
import static co.edu.unimagdalena.samariantour.recomendacion.recomendacion.OfertasDePrueba.preferencias;
import static co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.CriterioAfinidad.ACCESIBILIDAD;
import static co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.CriterioAfinidad.PRECIO;
import static co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.CriterioAfinidad.TIPO_ACTIVIDAD;
import static co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.CriterioAfinidad.ZONA;
import static co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.MotorRecomendacion.REGLAS;
import static co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.NivelEsfuerzo.BAJO;
import static co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.TipoActividad.AVISTAMIENTO_FAUNA;
import static co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.TipoActividad.CICLISMO;
import static co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.TipoActividad.SENDERISMO;
import static co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.TipoOferta.PAQUETE;
import static org.assertj.core.api.Assertions.assertThat;

class RecomendadorPorReglasAdapterTest {

    private final ReglaCriteriosExplicitos regla = new ReglaCriteriosExplicitos();
    private final RecomendadorPorReglasAdapter reglas = new RecomendadorPorReglasAdapter();

    @Test
    @DisplayName("toma las 5 primeras candidatas, en el orden que recibe, con posiciones desde 1")
    void cincoPrimeras() {
        Preferencias preferencias = preferencias().actividades(SENDERISMO).crear();
        List<OfertaEvaluada> candidatas = regla.evaluar(
                IntStream.rangeClosed(1, 7).mapToObj(i -> oferta("o" + i).crear()).toList(), preferencias);

        List<Recomendacion> recomendaciones = reglas.recomendar(preferencias, candidatas);

        assertThat(reglas.motor()).isEqualTo(REGLAS);
        assertThat(recomendaciones).extracting(r -> r.oferta().id()).containsExactly("o1", "o2", "o3", "o4", "o5");
        assertThat(recomendaciones).extracting(Recomendacion::posicion).containsExactly(1, 2, 3, 4, 5);
    }

    @Test
    @DisplayName("la explicación en español cita los criterios cumplidos: actividades pedidas, zona, accesible, precio")
    void explicacionEnEspanol() {
        Preferencias preferencias = preferencias().actividades(SENDERISMO, AVISTAMIENTO_FAUNA, CICLISMO).zona(MINCA)
                .accesible().precioMaximo(100_000).crear();
        OfertaCandidata paquete = oferta("o1").tipo(PAQUETE).actividades(AVISTAMIENTO_FAUNA, SENDERISMO).crear();

        Recomendacion recomendacion = unica(paquete, preferencias);

        assertThat(recomendacion.explicacion().criterios()).containsExactly(TIPO_ACTIVIDAD, ZONA, ACCESIBILIDAD, PRECIO);
        assertThat(recomendacion.explicacion().texto()).isEqualTo("Coincide con: senderismo, avistamiento de fauna, "
                + "zona Minca, accesible en silla de ruedas, precio dentro de tu presupuesto.");
    }

    @Test
    @DisplayName("en inglés, con la misma plantilla; el esfuerzo se nombra por su nivel")
    void explicacionEnIngles() {
        Preferencias preferencias = preferencias().idioma(Idioma.EN).nivelMaximo(BAJO).crear();

        Recomendacion recomendacion = unica(oferta("o1").nivel(BAJO).crear(), preferencias);

        assertThat(recomendacion.explicacion().texto()).isEqualTo("Matches: low effort.");
    }

    @Test
    @DisplayName("sin ningún criterio cumplido lo dice: es una oferta visible en orden aleatorio, no una coincidencia")
    void sinCriterios() {
        Recomendacion recomendacion = unica(oferta("o1").crear(), preferencias().texto("algo tranquilo").crear());

        assertThat(recomendacion.explicacion().criterios()).isEmpty();
        assertThat(recomendacion.explicacion().texto()).startsWith("Sin coincidencias con tus filtros");
    }

    @Test
    @DisplayName("si la zona no tiene nombre, dice 'la zona que elegiste'")
    void zonaSinNombre() {
        Recomendacion recomendacion = unica(oferta("o1").zona(MINCA, null).crear(), preferencias().zona(MINCA).crear());

        assertThat(recomendacion.explicacion().texto()).isEqualTo("Coincide con: la zona que elegiste.");
    }

    private Recomendacion unica(OfertaCandidata oferta, Preferencias preferencias) {
        return reglas.recomendar(preferencias, regla.evaluar(List.of(oferta), preferencias)).getFirst();
    }
}
