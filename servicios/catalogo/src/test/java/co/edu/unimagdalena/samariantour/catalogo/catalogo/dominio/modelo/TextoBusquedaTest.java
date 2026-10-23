package co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.DatoInvalidoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TextoBusquedaTest {

    @Test
    @DisplayName("separa las palabras y las normaliza: sin tildes, en minúsculas y sin espacios de sobra")
    void palabrasNormalizadas() {
        assertThat(TextoBusqueda.de("  Café   NIÑO  ").palabras()).containsExactly("cafe", "nino");
    }

    @Test
    @DisplayName("sin texto no hay filtro")
    void sinTexto() {
        assertThat(TextoBusqueda.de(null)).isNull();
        assertThat(TextoBusqueda.de("   ")).isNull();
    }

    @Test
    @DisplayName("sin los espacios de los extremos, el texto tiene entre 2 y 100 caracteres")
    void longitud() {
        assertThat(TextoBusqueda.de(" ab ").palabras()).containsExactly("ab");
        assertThat(TextoBusqueda.de("a".repeat(100))).isNotNull();
        assertThatThrownBy(() -> TextoBusqueda.de(" a "))
                .isInstanceOf(DatoInvalidoException.class)
                .hasMessageContaining("texto");
        assertThatThrownBy(() -> TextoBusqueda.de("a".repeat(101)))
                .isInstanceOf(DatoInvalidoException.class);
    }

    @Test
    @DisplayName("no distingue mayúsculas ni tildes, ni en el texto buscado ni en el de la oferta")
    void sinMayusculasNiTildes() {
        assertThat(TextoBusqueda.de("cafe").coincideCon("Ruta del CAFÉ", "")).isTrue();
        assertThat(TextoBusqueda.de("CAFÉ").coincideCon("Ruta del cafe", "")).isTrue();
        assertThat(TextoBusqueda.de("mañana").coincideCon("", "salida en la manana")).isTrue();
        assertThat(TextoBusqueda.de("Rio").coincideCon("Tubing por el RÍO", "")).isTrue();
    }

    @Test
    @DisplayName("acepta palabras incompletas, y cada una puede estar en el nombre o en la descripción")
    void palabrasIncompletasEnCualquierCampo() {
        TextoBusqueda texto = TextoBusqueda.de("buce tagan");

        assertThat(texto.coincideCon("Bautizo de buceo", "Salida desde Taganga")).isTrue();
        assertThat(texto.coincideCon("Bautizo de buceo", "Salida desde la marina")).isFalse();
    }

    @Test
    @DisplayName("una palabra no se arma con el final del nombre y el comienzo de la descripción")
    void sinUnirCampos() {
        assertThat(TextoBusqueda.de("marsal").coincideCon("Frente al mar", "Salida temprano")).isFalse();
    }

    @Test
    @DisplayName("los caracteres especiales se buscan tal cual")
    void caracteresEspeciales() {
        assertThat(TextoBusqueda.de("c++").coincideCon("Curso de c++", "")).isTrue();
        assertThat(TextoBusqueda.de(".*").coincideCon("Cualquier cosa", "")).isFalse();
        assertThat(TextoBusqueda.de("(mar)").expresiones()).containsExactly("\\(m[aáàäâAÁÀÄÂ]r\\)");
    }
}
