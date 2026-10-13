package co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.PRESTADOR;
import static org.assertj.core.api.Assertions.assertThat;

class PrestadorCatalogoTest {

    @Test
    @DisplayName("un prestador sin avisos no está habilitado")
    void sinAvisos() {
        PrestadorCatalogo prestador = PrestadorCatalogo.sinAvisos(PRESTADOR);

        assertThat(prestador.rntActivo()).isFalse();
        assertThat(prestador.version()).isZero();
    }

    @Test
    @DisplayName("aplica un aviso con versión mayor a la conocida")
    void avisoNuevo() {
        PrestadorCatalogo prestador = PrestadorCatalogo.sinAvisos(PRESTADOR);

        boolean cambio = prestador.aplicarHabilitacion(true, 5);

        assertThat(cambio).isTrue();
        assertThat(prestador.rntActivo()).isTrue();
        assertThat(prestador.version()).isEqualTo(5);
    }

    @Test
    @DisplayName("descarta un aviso viejo que llega en desorden")
    void avisoViejo() {
        PrestadorCatalogo prestador = new PrestadorCatalogo(PRESTADOR, false, 7);

        boolean cambio = prestador.aplicarHabilitacion(true, 6);

        assertThat(cambio).isFalse();
        assertThat(prestador.rntActivo()).isFalse();
        assertThat(prestador.version()).isEqualTo(7);
    }

    @Test
    @DisplayName("un aviso repetido no cambia nada")
    void avisoRepetido() {
        PrestadorCatalogo prestador = new PrestadorCatalogo(PRESTADOR, true, 3);

        assertThat(prestador.aplicarHabilitacion(true, 3)).isFalse();
    }
}
