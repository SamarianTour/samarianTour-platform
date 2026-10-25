package co.edu.unimagdalena.samariantour.recomendacion.recomendacion.aplicacion;

import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.CatalogoDePrueba;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.excepcion.OfertaNoEncontradaException;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.excepcion.RecomendacionNoDisponibleException;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.Idioma;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.MotorRecomendacion;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.OfertaCandidata;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.OrigenExposicion;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.RegistroExposicion;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.ResultadoAlternativas;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.politica.PoliticaRotacion;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.politica.ReglaAlternativas;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.puerto.salida.CatalogoConsultaPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static co.edu.unimagdalena.samariantour.recomendacion.recomendacion.OfertasDePrueba.oferta;
import static co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.AvisoRecomendacion.POCAS_COINCIDENCIAS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProponerAlternativasServiceTest {

    private static final Instant AHORA = Instant.parse("2026-10-24T15:00:00Z");
    private static final String TAYRONA = "a00000000000000000000005";

    private final List<RegistroExposicion> exposiciones = new ArrayList<>();
    private final OfertaCandidata enTayrona = oferta("tayrona").zona(TAYRONA, "Parque Tayrona").altaAfluencia().crear();
    private final OfertaCandidata enMinca = oferta("minca").crear();

    @Test
    @DisplayName("zona con alta afluencia: aplica, con la zona de origen, las alternativas y la exposición ALTERNATIVAS")
    void aplica() {
        ResultadoAlternativas resultado = servicio(CatalogoDePrueba.de(List.of(enTayrona, enMinca,
                oferta("taganga").zona("a3", "Taganga").crear()))).proponer("tayrona", Idioma.ES);

        assertThat(resultado.aplica()).isTrue();
        assertThat(resultado.aviso()).isNull();
        assertThat(resultado.zonaOrigen()).isEqualTo(new ResultadoAlternativas.ZonaOrigen(TAYRONA, "Parque Tayrona"));
        assertThat(resultado.alternativas()).hasSize(2);
        assertThat(exposiciones).singleElement().satisfies(registro -> {
            assertThat(registro.fecha()).isEqualTo(AHORA);
            assertThat(registro.origen()).isEqualTo(OrigenExposicion.ALTERNATIVAS);
            assertThat(registro.motor()).isEqualTo(MotorRecomendacion.REGLAS);
            assertThat(registro.ofertas()).extracting(RegistroExposicion.OfertaExpuesta::posicion)
                    .containsExactly(1, 2);
        });
    }

    @Test
    @DisplayName("menos de 2 alternativas: las que haya con POCAS_COINCIDENCIAS")
    void pocas() {
        ResultadoAlternativas resultado = servicio(CatalogoDePrueba.de(List.of(enTayrona, enMinca)))
                .proponer("tayrona", Idioma.ES);

        assertThat(resultado.aviso()).isEqualTo(POCAS_COINCIDENCIAS);
        assertThat(resultado.alternativas()).hasSize(1);
    }

    @Test
    @DisplayName("zona sin alta afluencia: no aplica, lista vacía, sin zona de origen y sin registrar exposición")
    void noAplica() {
        ResultadoAlternativas resultado = servicio(CatalogoDePrueba.de(List.of(enTayrona, enMinca)))
                .proponer("minca", Idioma.ES);

        assertThat(resultado).isEqualTo(ResultadoAlternativas.noAplica());
        assertThat(exposiciones).isEmpty();
    }

    @Test
    @DisplayName("una oferta que no está entre las visibles: OFERTA_NO_ENCONTRADA (404)")
    void noEncontrada() {
        assertThatThrownBy(() -> servicio(CatalogoDePrueba.de(List.of(enTayrona))).proponer("no-existe", Idioma.ES))
                .isInstanceOf(OfertaNoEncontradaException.class)
                .extracting(e -> ((OfertaNoEncontradaException) e).codigo()).isEqualTo("OFERTA_NO_ENCONTRADA");
        assertThat(exposiciones).isEmpty();
    }

    @Test
    @DisplayName("catálogo caído sin caché: RECOMENDACION_NO_DISPONIBLE (503)")
    void catalogoCaido() {
        assertThatThrownBy(() -> servicio(CatalogoDePrueba.caido()).proponer("tayrona", Idioma.ES))
                .isInstanceOf(RecomendacionNoDisponibleException.class);
    }

    private ProponerAlternativasService servicio(CatalogoConsultaPort catalogo) {
        return new ProponerAlternativasService(catalogo, exposiciones::add,
                new ReglaAlternativas(new PoliticaRotacion(new Random(7))), Clock.fixed(AHORA, ZoneOffset.UTC));
    }
}
