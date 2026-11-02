package co.edu.unimagdalena.samariantour.catalogo.catalogo.aplicacion;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Oferta;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.OfertasDePrestador;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.comando.DecisionModeracion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static co.edu.unimagdalena.samariantour.catalogo.catalogo.aplicacion.EscenarioAplicacion.AGENCIA;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.aplicacion.EscenarioAplicacion.OTRA_AGENCIA;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.aplicacion.EscenarioAplicacion.actividad;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.FOTO_OTRO_PRESTADOR;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.OTRO_PRESTADOR;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.PRESTADOR;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.ZONA_ACTIVA;
import static org.assertj.core.api.Assertions.assertThat;

/** La oferta elegible de cada prestador para el índice de exposición del núcleo (RET-02, DC-51). */
class ContarOfertasVisiblesServiceTest {

    private final EscenarioAplicacion escenario = new EscenarioAplicacion();

    @Test
    @DisplayName("cuenta solo las visibles de cada prestador, aunque ocupen más de una página")
    void porPrestador() {
        for (int i = 0; i < 101; i++) {
            escenario.ofertaPublicada("Caminata " + i, "50000", ZONA_ACTIVA);
        }
        escenario.publicar(AGENCIA, actividad("Sin aprobar", "45000", ZONA_ACTIVA));
        escenario.habilitar(OTRO_PRESTADOR, true, 1);
        for (int i = 0; i < 2; i++) {
            Oferta kayak = escenario.publicar(OTRA_AGENCIA, actividad("Kayak " + i, "70000", ZONA_ACTIVA,
                    FOTO_OTRO_PRESTADOR));
            escenario.moderar(kayak.id(), 1, DecisionModeracion.APROBAR);
        }

        assertThat(escenario.conteoVisibles.porPrestador()).containsExactly(
                new OfertasDePrestador(PRESTADOR, 101), new OfertasDePrestador(OTRO_PRESTADOR, 2));
    }

    @Test
    @DisplayName("un prestador sin el RNT activo no tiene oferta elegible, y sin ofertas la lista está vacía")
    void sinVisibles() {
        assertThat(escenario.conteoVisibles.porPrestador()).isEmpty();

        escenario.ofertaPublicada("Caminata", "50000", ZONA_ACTIVA);
        escenario.habilitar(PRESTADOR, false, 2);

        assertThat(escenario.conteoVisibles.porPrestador()).isEmpty();
    }
}
