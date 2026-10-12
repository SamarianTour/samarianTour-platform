package co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.politica;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.DatoInvalidoException;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.TipoOfertaNoPermitidoException;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.ModalidadRecorrido;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.NivelEsfuerzo;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.TipoActividad;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.TipoPrestador;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.VersionActividad;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.VersionOferta;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalTime;
import java.util.List;

import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.actividad;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.actividadCompletaDeGuia;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.alojamiento;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.contenido;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.paquete;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.restaurante;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReglaTipoOfertaTest {

    @ParameterizedTest(name = "{0} publica ACTIVIDAD: {1}; PAQUETE: {2}; ALOJAMIENTO: {3}; RESTAURANTE: {4}")
    @CsvSource({
            "ALOJAMIENTO,        false, false, true,  false",
            "RESTAURANTE,        false, false, false, true",
            "AGENCIA_OPERADORA,  true,  true,  false, false",
            "GUIA_INDEPENDIENTE, true,  true,  false, false"})
    @DisplayName("matriz de tipo de prestador y tipo de oferta (RF-65)")
    void matriz(TipoPrestador tipoPrestador, boolean puedeActividad, boolean puedePaquete, boolean puedeAlojamiento,
                boolean puedeRestaurante) {
        verificarPermiso(tipoPrestador, actividadCompletaDeGuia(), puedeActividad);
        verificarPermiso(tipoPrestador, paquete("a", "b"), puedePaquete);
        verificarPermiso(tipoPrestador, alojamiento(), puedeAlojamiento);
        verificarPermiso(tipoPrestador, restaurante(), puedeRestaurante);
    }

    @Test
    @DisplayName("una agencia puede publicar una actividad sin los datos propios del guía")
    void agenciaSinDatosDeGuia() {
        assertThatCode(() -> ReglaTipoOferta.verificar(TipoPrestador.AGENCIA_OPERADORA, actividad()))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("el guía independiente debe indicar punto de encuentro, duración, modalidad e idiomas (RF-67)")
    void guiaExigeSusDatos() {
        assertThatThrownBy(() -> ReglaTipoOferta.verificar(TipoPrestador.GUIA_INDEPENDIENTE, actividad()))
                .isInstanceOf(DatoInvalidoException.class)
                .hasMessageContaining("RF-67");

        VersionActividad sinIdiomas = new VersionActividad(contenido("Recorrido"), TipoActividad.SENDERISMO,
                NivelEsfuerzo.BAJO, false, List.of(LocalTime.of(6, 0)), "Plaza de Minca", 120,
                ModalidadRecorrido.CAMINATA, List.of());
        assertThatThrownBy(() -> ReglaTipoOferta.verificar(TipoPrestador.GUIA_INDEPENDIENTE, sinIdiomas))
                .isInstanceOf(DatoInvalidoException.class)
                .hasMessageContaining("idiomasAtencion");

        assertThatCode(() -> ReglaTipoOferta.verificar(TipoPrestador.GUIA_INDEPENDIENTE, actividadCompletaDeGuia()))
                .doesNotThrowAnyException();
    }

    private static void verificarPermiso(TipoPrestador tipoPrestador, VersionOferta version, boolean permitido) {
        if (permitido) {
            assertThatCode(() -> ReglaTipoOferta.verificar(tipoPrestador, version)).doesNotThrowAnyException();
        } else {
            assertThatThrownBy(() -> ReglaTipoOferta.verificar(tipoPrestador, version))
                    .isInstanceOf(TipoOfertaNoPermitidoException.class);
        }
    }
}
