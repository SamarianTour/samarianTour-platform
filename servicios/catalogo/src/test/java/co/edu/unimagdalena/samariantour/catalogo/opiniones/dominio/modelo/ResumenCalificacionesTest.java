package co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.modelo;

import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.excepcion.DatoInvalidoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ResumenCalificacionesTest {

    @Test
    @DisplayName("el promedio tiene un decimal y se redondea hacia arriba desde la mitad")
    void promedioConUnDecimal() {
        assertThat(new ResumenCalificaciones(4, 17).promedio()).contains(new BigDecimal("4.3"));
        assertThat(new ResumenCalificaciones(3, 13).promedio()).contains(new BigDecimal("4.3"));
        assertThat(new ResumenCalificaciones(3, 14).promedio()).contains(new BigDecimal("4.7"));
        assertThat(new ResumenCalificaciones(2, 10).promedio()).contains(new BigDecimal("5.0"));
    }

    @Test
    @DisplayName("sin opiniones aprobadas no hay promedio")
    void sinOpiniones() {
        assertThat(ResumenCalificaciones.SIN_OPINIONES.total()).isZero();
        assertThat(ResumenCalificaciones.SIN_OPINIONES.promedio()).isEmpty();
    }

    @Test
    @DisplayName("una suma imposible para ese número de calificaciones no se acepta")
    void sumaImposible() {
        assertThatThrownBy(() -> new ResumenCalificaciones(2, 11)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ResumenCalificaciones(2, 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ResumenCalificaciones(-1, 0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("la paginación sigue las reglas de DC-14")
    void paginacion() {
        assertThat(Paginacion.de(null, null)).isEqualTo(new Paginacion(0, 20));
        assertThatThrownBy(() -> new Paginacion(-1, 10)).isInstanceOf(DatoInvalidoException.class)
                .hasMessage("page: no puede ser negativa");
        assertThatThrownBy(() -> new Paginacion(0, 101)).isInstanceOf(DatoInvalidoException.class)
                .hasMessage("size: debe estar entre 1 y 100");
        assertThat(new Pagina<>(java.util.List.of("a"), new Paginacion(0, 2), 5).totalPaginas()).isEqualTo(3);
    }
}
