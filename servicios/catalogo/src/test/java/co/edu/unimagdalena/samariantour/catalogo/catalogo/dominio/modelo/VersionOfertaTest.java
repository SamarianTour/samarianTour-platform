package co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.DatoInvalidoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.ZONA_ACTIVA;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.actividad;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.alojamiento;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.contenido;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VersionOfertaTest {

    private static final List<LocalTime> SALIDAS = List.of(LocalTime.of(8, 0));

    @Test
    @DisplayName("cada tipo de versión define su unidad de precio (RF-20)")
    void unidadPrecio() {
        assertThat(actividad().unidadPrecio()).isEqualTo("por persona");
        assertThat(alojamiento().unidadPrecio()).isEqualTo("por noche y unidad");
    }

    @Test
    @DisplayName("una versión nueva es un borrador sin número hasta que la oferta la envía a moderación")
    void borrador() {
        VersionActividad borrador = actividad();

        assertThat(borrador.estaEnviada()).isFalse();
        assertThatThrownBy(borrador::numero).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("el nombre y la descripción son obligatorios")
    void nombreObligatorio() {
        assertThatThrownBy(() -> new ContenidoComun(" ", "Descripción", BigDecimal.TEN, null, ZONA_ACTIVA, null, null, 5))
                .isInstanceOf(DatoInvalidoException.class)
                .hasMessageContaining("nombre");
        assertThatThrownBy(() -> new ContenidoComun("Nombre", null, BigDecimal.TEN, null, ZONA_ACTIVA, null, null, 5))
                .isInstanceOf(DatoInvalidoException.class)
                .hasMessageContaining("descripcion");
    }

    @Test
    @DisplayName("el precio de referencia es obligatorio y no puede ser negativo")
    void precio() {
        assertThatThrownBy(() -> new ContenidoComun("N", "D", null, null, ZONA_ACTIVA, null, null, 5))
                .isInstanceOf(DatoInvalidoException.class);
        assertThatThrownBy(() -> new ContenidoComun("N", "D", new BigDecimal("-1"), null, ZONA_ACTIVA, null, null, 5))
                .isInstanceOf(DatoInvalidoException.class);
    }

    @Test
    @DisplayName("la capacidad máxima debe ser mayor que cero")
    void capacidad() {
        assertThatThrownBy(() -> new ContenidoComun("N", "D", BigDecimal.TEN, null, ZONA_ACTIVA, null, null, 0))
                .isInstanceOf(DatoInvalidoException.class)
                .hasMessageContaining("capacidadMaxima");
    }

    @Test
    @DisplayName("los textos opcionales en blanco se tratan como ausentes")
    void textosOpcionales() {
        ContenidoComun contenido = new ContenidoComun("N", "D", BigDecimal.ZERO, "  ", ZONA_ACTIVA, null, "", 1);

        assertThat(contenido.horarios()).isNull();
        assertThat(contenido.comoLlegar()).isNull();
    }

    @Test
    @DisplayName("una actividad necesita al menos un horario de salida (RF-30)")
    void horariosDeSalida() {
        assertThatThrownBy(() -> new VersionActividad(contenido("A"), TipoActividad.SENDERISMO, NivelEsfuerzo.BAJO,
                false, List.of(), null, null, null, null))
                .isInstanceOf(DatoInvalidoException.class)
                .hasMessageContaining("horariosSalida");
    }

    @Test
    @DisplayName("tipo de actividad y nivel de esfuerzo son obligatorios")
    void tipoYEsfuerzo() {
        assertThatThrownBy(() -> new VersionActividad(contenido("A"), null, NivelEsfuerzo.BAJO,
                false, SALIDAS, null, null, null, null))
                .isInstanceOf(DatoInvalidoException.class);
        assertThatThrownBy(() -> new VersionActividad(contenido("A"), TipoActividad.SENDERISMO, null,
                false, SALIDAS, null, null, null, null))
                .isInstanceOf(DatoInvalidoException.class);
    }

    @Test
    @DisplayName("la duración, si se indica, debe ser mayor que cero")
    void duracion() {
        assertThatThrownBy(() -> new VersionActividad(contenido("A"), TipoActividad.SENDERISMO, NivelEsfuerzo.BAJO,
                false, SALIDAS, null, 0, null, null))
                .isInstanceOf(DatoInvalidoException.class)
                .hasMessageContaining("duracionMinutos");
    }

    @Test
    @DisplayName("los idiomas se indican con códigos de dos letras")
    void idiomas() {
        assertThatThrownBy(() -> new VersionActividad(contenido("A"), TipoActividad.SENDERISMO, NivelEsfuerzo.BAJO,
                false, SALIDAS, null, null, null, List.of("español")))
                .isInstanceOf(DatoInvalidoException.class)
                .hasMessageContaining("idiomasAtencion");
    }

    @Test
    @DisplayName("las listas de la versión no se pueden modificar desde fuera")
    void listasInmutables() {
        VersionActividad version = actividad();

        assertThatThrownBy(() -> version.horariosSalida().add(LocalTime.NOON))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
