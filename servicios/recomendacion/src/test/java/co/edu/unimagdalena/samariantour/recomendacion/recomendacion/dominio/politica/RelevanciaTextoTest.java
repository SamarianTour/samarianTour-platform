package co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.politica;

import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.OfertaCandidata;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static co.edu.unimagdalena.samariantour.recomendacion.recomendacion.OfertasDePrueba.oferta;
import static co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.TipoActividad.AVISTAMIENTO_FAUNA;
import static co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.TipoActividad.BUCEO_SNORKEL;
import static co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.TipoActividad.SENDERISMO;
import static co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.TipoOferta.ALOJAMIENTO;
import static co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.TipoOferta.RESTAURANTE;
import static org.assertj.core.api.Assertions.assertThat;

class RelevanciaTextoTest {

    private final RelevanciaTexto relevancia = new RelevanciaTexto();

    private final OfertaCandidata snorkel = oferta("1").nombre("Snorkel en Playa Cristal").actividades(BUCEO_SNORKEL)
            .descripcion("Arrecife de aguas cristalinas con traslado en lancha").zona("a1", "Parque Tayrona").crear();
    private final OfertaCandidata aves = oferta("2").nombre("Avistamiento de aves en Minca")
            .actividades(AVISTAMIENTO_FAUNA).descripcion("Recorrido al amanecer por la Sierra").crear();

    @Test
    @DisplayName("busca en el nombre, la descripción y la zona, sin mayúsculas ni tildes")
    void nombreDescripcionZona() {
        assertThat(relevancia.coincidencias("Quiero hacer SNORKEL en el Tayrona", snorkel))
                .containsExactly("snorkel", "tayrona");
        assertThat(relevancia.coincidencias("algo en la sierra al amanecer", aves)).containsExactly("sierra", "amanecer");
        assertThat(relevancia.coincidencias("algo en la montaña", oferta("3").descripcion("Paseo por la montana").crear()))
                .as("sin tildes de ningún lado").containsExactly("montaña");
    }

    @Test
    @DisplayName("una palabra que empieza como otra coincide: snorkeling con snorkel, playas con playa")
    void porElComienzo() {
        assertThat(relevancia.coincidencias("snorkeling", snorkel)).containsExactly("snorkeling");
        assertThat(relevancia.coincidencias("playas", snorkel)).containsExactly("playas");
    }

    @Test
    @DisplayName("los tipos se reconocen con las palabras de la gente: aves, birds, diving, comer, hotel")
    void palabrasDeLosTipos() {
        assertThat(relevancia.coincidencias("quiero ver pajaros", aves)).containsExactly("pajaros");
        assertThat(relevancia.coincidencias("birds", aves)).containsExactly("birds");
        assertThat(relevancia.coincidencias("diving", snorkel)).containsExactly("diving");
        assertThat(relevancia.coincidencias("donde comer rico",
                oferta("4").tipo(RESTAURANTE).actividades().crear())).containsExactly("comer");
        assertThat(relevancia.coincidencias("un hotel para dormir",
                oferta("5").tipo(ALOJAMIENTO).actividades().crear())).containsExactly("hotel", "dormir");
    }

    @Test
    @DisplayName("las palabras vacías y las cortas no cuentan, y nada sin texto")
    void vaciasYCortas() {
        assertThat(relevancia.coincidencias("quiero algo con el de la para", snorkel)).isEmpty();
        assertThat(relevancia.coincidencias("en la Sierra", aves)).containsExactly("sierra");
        assertThat(relevancia.coincidencias(null, aves)).isEmpty();
        assertThat(relevancia.coincidencias("  ", aves)).isEmpty();
    }

    @Test
    @DisplayName("sin relación con la oferta, ninguna coincidencia")
    void sinRelacion() {
        assertThat(relevancia.coincidencias("snorkel en el arrecife",
                oferta("6").actividades(SENDERISMO).crear())).isEmpty();
    }
}
