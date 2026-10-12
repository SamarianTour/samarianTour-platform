package co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.DatoInvalidoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BusquedaTest {

    @Test
    @DisplayName("sin parámetros, la paginación empieza en la página 0 con 20 elementos")
    void valoresPorDefecto() {
        assertThat(Paginacion.de(null, null)).isEqualTo(new Paginacion(0, 20));
    }

    @Test
    @DisplayName("la página no puede ser negativa y el tamaño va de 1 a 100")
    void limites() {
        assertThatThrownBy(() -> Paginacion.de(-1, 10)).isInstanceOf(DatoInvalidoException.class);
        assertThatThrownBy(() -> Paginacion.de(0, 0)).isInstanceOf(DatoInvalidoException.class);
        assertThatThrownBy(() -> Paginacion.de(0, 101)).isInstanceOf(DatoInvalidoException.class);
        assertThat(Paginacion.de(0, 100).tamano()).isEqualTo(100);
    }

    @Test
    @DisplayName("el total de páginas redondea hacia arriba")
    void totalPaginas() {
        Pagina<String> pagina = new Pagina<>(List.of("a", "b"), new Paginacion(0, 2), 5);

        assertThat(pagina.totalPaginas()).isEqualTo(3);
    }

    @Test
    @DisplayName("el precio mínimo no puede ser mayor que el máximo")
    void rangoDePrecio() {
        assertThatThrownBy(() -> new CriteriosBusqueda(null, null, new BigDecimal("100"), new BigDecimal("50"),
                Paginacion.de(null, null)))
                .isInstanceOf(DatoInvalidoException.class)
                .hasMessageContaining("precioMin");
    }
}
