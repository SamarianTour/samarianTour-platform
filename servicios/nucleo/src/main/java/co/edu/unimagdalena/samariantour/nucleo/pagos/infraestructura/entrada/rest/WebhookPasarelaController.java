package co.edu.unimagdalena.samariantour.nucleo.pagos.infraestructura.entrada.rest;

import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.entrada.RecibirEventoPasarelaUseCase;
import co.edu.unimagdalena.samariantour.nucleo.pagos.infraestructura.entrada.rest.dto.EventoRecibidoResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * El webhook de la pasarela (RF-50, RT-07, DN-21). Es público y sin rol, porque lo llama la pasarela, y no está bajo
 * {@code /internal}: Nginx lo deja pasar. Su autenticidad es la firma del evento: sin una válida responde 401 y no
 * guarda nada. Con una válida guarda el evento y responde 200 de inmediato, también si es un duplicado; Wompi
 * reintenta mientras no reciba 200. El procesamiento corre después, en una tarea programada.
 */
@RestController
@RequestMapping("/pagos/eventos")
public class WebhookPasarelaController {

    static final String HEADER_FIRMA = "X-Event-Checksum";

    private final RecibirEventoPasarelaUseCase eventos;

    public WebhookPasarelaController(RecibirEventoPasarelaUseCase eventos) {
        this.eventos = eventos;
    }

    @PostMapping
    public EventoRecibidoResponse recibir(@RequestBody(required = false) String cuerpo,
                                          @RequestHeader(value = HEADER_FIRMA, required = false) String firma) {
        return new EventoRecibidoResponse(!eventos.recibir(cuerpo, firma));
    }
}
