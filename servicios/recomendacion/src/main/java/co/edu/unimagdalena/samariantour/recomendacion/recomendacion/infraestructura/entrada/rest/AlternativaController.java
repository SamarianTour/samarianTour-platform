package co.edu.unimagdalena.samariantour.recomendacion.recomendacion.infraestructura.entrada.rest;

import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.Idioma;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.puerto.entrada.ProponerAlternativasUseCase;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.infraestructura.entrada.rest.dto.AlternativasResponse;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.infraestructura.entrada.rest.mapper.RecomendacionWebMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code GET /api/recomendacion/ofertas/{ofertaId}/alternativas?idioma=es|en} (RF-29). Pública, como la ficha de la
 * oferta que la muestra.
 */
@RestController
public class AlternativaController {

    private final ProponerAlternativasUseCase proponerAlternativas;

    public AlternativaController(ProponerAlternativasUseCase proponerAlternativas) {
        this.proponerAlternativas = proponerAlternativas;
    }

    @GetMapping("/ofertas/{ofertaId}/alternativas")
    public AlternativasResponse alternativas(@PathVariable String ofertaId,
                                             @RequestParam(required = false) String idioma) {
        return RecomendacionWebMapper.aRespuesta(proponerAlternativas.proponer(ofertaId, Idioma.deCodigo(idioma)));
    }
}
