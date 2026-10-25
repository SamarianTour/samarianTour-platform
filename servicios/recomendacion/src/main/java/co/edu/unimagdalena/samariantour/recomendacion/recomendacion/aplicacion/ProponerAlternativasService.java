package co.edu.unimagdalena.samariantour.recomendacion.recomendacion.aplicacion;

import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.excepcion.OfertaNoEncontradaException;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.AvisoRecomendacion;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.Idioma;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.OfertaCandidata;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.Recomendacion;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.RegistroExposicion;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.ResultadoAlternativas;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.politica.ReglaAlternativas;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.puerto.entrada.ProponerAlternativasUseCase;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.puerto.salida.CatalogoConsultaPort;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.puerto.salida.ExposicionPort;

import java.time.Clock;
import java.util.List;

/**
 * Propone alternativas a una oferta de una zona con alta afluencia (RF-29):
 * <ol>
 *   <li>busca la oferta entre las visibles del catálogo (o su copia en caché); si no está, 404;</li>
 *   <li>si su zona no es de alta afluencia, no aplica y no se registra nada: la ficha no muestra la sección;</li>
 *   <li>elige y explica con {@link ReglaAlternativas}, sin IA (DR-19);</li>
 *   <li>registra la exposición con el origen ALTERNATIVAS (RET-02, DR-20).</li>
 * </ol>
 * Si menos de 2 ofertas cumplen, devuelve las que haya con el aviso POCAS_COINCIDENCIAS.
 */
public class ProponerAlternativasService implements ProponerAlternativasUseCase {

    private final CatalogoConsultaPort catalogo;
    private final ExposicionPort exposicion;
    private final ReglaAlternativas regla;
    private final Clock reloj;

    public ProponerAlternativasService(CatalogoConsultaPort catalogo, ExposicionPort exposicion,
                                       ReglaAlternativas regla, Clock reloj) {
        this.catalogo = catalogo;
        this.exposicion = exposicion;
        this.regla = regla;
        this.reloj = reloj;
    }

    @Override
    public ResultadoAlternativas proponer(String ofertaId, Idioma idioma) {
        List<OfertaCandidata> visibles = catalogo.ofertasVisibles();
        OfertaCandidata origen = visibles.stream()
                .filter(oferta -> oferta.id().equals(ofertaId))
                .findFirst()
                .orElseThrow(() -> new OfertaNoEncontradaException(ofertaId));
        if (!origen.zonaAltaAfluencia()) {
            return ResultadoAlternativas.noAplica();
        }
        List<Recomendacion> alternativas = regla.proponer(origen, visibles, idioma);
        ResultadoAlternativas resultado = new ResultadoAlternativas(true,
                alternativas.size() < ResultadoAlternativas.MINIMO ? AvisoRecomendacion.POCAS_COINCIDENCIAS : null,
                new ResultadoAlternativas.ZonaOrigen(origen.zonaId(), origen.zonaNombre()), alternativas);
        exposicion.registrar(RegistroExposicion.de(reloj.instant(), resultado));
        return resultado;
    }
}
