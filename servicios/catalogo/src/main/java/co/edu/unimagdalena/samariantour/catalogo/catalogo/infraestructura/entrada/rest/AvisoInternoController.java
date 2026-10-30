package co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.AplicarAvisoNucleoUseCase;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest.dto.AvisoRequest;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest.dto.AvisoResponse;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest.mapper.AvisoWebMapper;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Avisos del núcleo: moderación y habilitación RNT (RF-77, RF-79). Nginx no expone {@code /internal/**} hacia afuera,
 * y solo el núcleo entra, con su token de servicio (DC-04, DN-38).
 *
 * <p>Los tres resultados (APLICADO, IGNORADO y DUPLICADO) responden 200, para que el núcleo no reintente el aviso.
 */
@RestController
@RequestMapping("/internal/avisos")
public class AvisoInternoController {

    private final AplicarAvisoNucleoUseCase aplicarAviso;

    public AvisoInternoController(AplicarAvisoNucleoUseCase aplicarAviso) {
        this.aplicarAviso = aplicarAviso;
    }

    @PostMapping
    public AvisoResponse recibir(@RequestBody AvisoRequest aviso) {
        return new AvisoResponse(aplicarAviso.aplicar(AvisoWebMapper.aComando(aviso)));
    }
}
