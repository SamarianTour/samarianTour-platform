package co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.entrada.rest;

import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.puerto.entrada.AplicarAvisoOpinionUseCase;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.entrada.rest.dto.AvisoOpinionRequest;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.entrada.rest.dto.AvisoOpinionResponse;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.entrada.rest.mapper.AvisoOpinionWebMapper;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Avisos del núcleo sobre opiniones: habilitación por solicitud pagada (DC-40), decisión del administrador (DC-43) y
 * supresión de un turista (DC-44). Nginx no expone {@code /internal/**} hacia afuera (DC-04).
 *
 * <p>Los tres resultados (APLICADO, IGNORADO y DUPLICADO) responden 200, para que el núcleo no reintente el aviso.
 */
@RestController
@RequestMapping("/internal/opiniones/avisos")
public class AvisoOpinionesController {

    private final AplicarAvisoOpinionUseCase aplicarAviso;

    public AvisoOpinionesController(AplicarAvisoOpinionUseCase aplicarAviso) {
        this.aplicarAviso = aplicarAviso;
    }

    @PostMapping
    public AvisoOpinionResponse recibir(@RequestBody AvisoOpinionRequest aviso) {
        return new AvisoOpinionResponse(aplicarAviso.aplicar(AvisoOpinionWebMapper.aComando(aviso)));
    }
}
