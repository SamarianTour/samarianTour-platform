package co.edu.unimagdalena.samariantour.nucleo.indicadores.infraestructura.entrada.rest;

import co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.excepcion.DatoInvalidoException;
import co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.modelo.OfertaExpuesta;
import co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.modelo.OrigenExposicion;
import co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.modelo.RegistroExposicion;
import co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.puerto.entrada.RegistrarExposicionUseCase;
import co.edu.unimagdalena.samariantour.nucleo.indicadores.infraestructura.entrada.rest.dto.ExposicionRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * El {@code ExposicionController} del diagrama de paquetes: la recomendación envía qué ofertas mostró en cada consulta
 * (RET-02, DN-42). Solo con su token de servicio (DN-38); Nginx no expone {@code /internal/**}.
 */
@RestController
@RequestMapping("/internal/exposiciones")
public class ExposicionController {

    private final RegistrarExposicionUseCase exposicion;

    public ExposicionController(RegistrarExposicionUseCase exposicion) {
        this.exposicion = exposicion;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void registrar(@RequestBody ExposicionRequest peticion) {
        List<OfertaExpuesta> ofertas = peticion.ofertas() == null ? List.of() : peticion.ofertas().stream()
                .map(oferta -> new OfertaExpuesta(oferta.ofertaId(), oferta.prestadorId(),
                        oferta.posicion() == null ? 0 : oferta.posicion()))
                .toList();
        exposicion.registrar(new RegistroExposicion(UUID.randomUUID(), peticion.fecha(), origen(peticion.origen()),
                peticion.motor(), ofertas));
    }

    private static OrigenExposicion origen(String valor) {
        return Arrays.stream(OrigenExposicion.values())
                .filter(origen -> origen.name().equals(valor))
                .findFirst()
                .orElseThrow(() -> new DatoInvalidoException("origen", "debe ser RECOMENDACION o ALTERNATIVAS"));
    }
}
