package co.edu.unimagdalena.samariantour.catalogo.catalogo.aplicacion;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.DatoInvalidoException;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.DestinoFoto;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Foto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.AHORA;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.PRESTADOR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SubirFotoServiceTest {

    private static final byte[] IMAGEN = {1, 2, 3};

    private final EscenarioAplicacion escenario = new EscenarioAplicacion();
    private final SubirFotoService servicio = escenario.subirFoto;
    /** Las dos fotos que el escenario ya trae registradas. */
    private final int registradasAntes = escenario.registroFotos.cantidad();

    @Test
    @DisplayName("guarda la foto y la registra como del prestador, para sus ofertas y usada ahora (DC-47)")
    void subir() {
        Foto foto = servicio.subir(PRESTADOR, IMAGEN);

        assertThat(escenario.fotos.tiene(foto)).isTrue();
        assertThat(escenario.registroFotos.de(foto)).hasValueSatisfying(subida -> {
            assertThat(subida.prestadorId()).isEqualTo(PRESTADOR);
            assertThat(subida.destino()).isEqualTo(DestinoFoto.OFERTAS);
            assertThat(subida.ultimoUso()).isEqualTo(AHORA);
        });
    }

    @Test
    @DisplayName("un archivo vacío o de más de 5 MB se rechaza sin guardar ni registrar nada")
    void tamano() {
        assertThatThrownBy(() -> servicio.subir(PRESTADOR, new byte[0]))
                .isInstanceOf(DatoInvalidoException.class)
                .hasMessageContaining("foto: no llegó ninguna imagen");
        assertThatThrownBy(() -> servicio.subir(PRESTADOR, new byte[Foto.TAMANO_MAXIMO_BYTES + 1]))
                .isInstanceOf(DatoInvalidoException.class)
                .hasMessageContaining("foto: pesa más de 5 MB");

        assertThat(escenario.fotos.cantidad()).isZero();
        assertThat(escenario.registroFotos.cantidad()).isEqualTo(registradasAntes);
    }

    @Test
    @DisplayName("si el almacenamiento rechaza la imagen, no queda ni el registro ni el archivo")
    void noEsImagen() {
        assertThatThrownBy(() -> servicio.subir(PRESTADOR, new byte[]{FileStorageEnMemoria.NO_ES_IMAGEN}))
                .isInstanceOf(DatoInvalidoException.class);

        assertThat(escenario.fotos.cantidad()).isZero();
        assertThat(escenario.registroFotos.cantidad()).isEqualTo(registradasAntes);
    }
}
