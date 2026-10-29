package co.edu.unimagdalena.samariantour.nucleo.pagos.infraestructura.entrada.rest;

import co.edu.unimagdalena.samariantour.nucleo.compartido.seguridad.Identidad;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo.IntentoPago;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo.PeticionIdempotente;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo.SesionCobro;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo.SesionGenerada;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.entrada.GenerarSesionCobroUseCase;
import co.edu.unimagdalena.samariantour.nucleo.pagos.infraestructura.entrada.rest.dto.CheckoutResponse;
import co.edu.unimagdalena.samariantour.nucleo.pagos.infraestructura.entrada.rest.dto.IntentoPagoResponse;
import co.edu.unimagdalena.samariantour.nucleo.pagos.infraestructura.entrada.rest.dto.SesionCobroResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * El pago del turista (RF-46 a RF-49, ADR-04), con el rol TURISTA (DN-05). La sesión de cobro lleva su
 * {@code Idempotency-Key}; la consulta de los intentos sirve para reconsultar al reconectar.
 */
@RestController
@RequestMapping("/turista/solicitudes/{solicitudId}")
public class PagoController {

    private final GenerarSesionCobroUseCase cobros;
    private final RespuestasIdempotentesPagos idempotentes;

    public PagoController(GenerarSesionCobroUseCase cobros, RespuestasIdempotentesPagos idempotentes) {
        this.cobros = cobros;
        this.idempotentes = idempotentes;
    }

    @PostMapping("/pago")
    public ResponseEntity<String> generar(@PathVariable UUID solicitudId,
                                          @RequestHeader(value = RespuestasIdempotentesPagos.HEADER_CLAVE,
                                                  required = false) String clave,
                                          HttpServletRequest http) {
        UUID turistaId = Identidad.de(http).usuarioId();
        PeticionIdempotente peticion = idempotentes.peticion(clave, turistaId, "GENERAR_SESION_COBRO",
                "/turista/solicitudes/" + solicitudId + "/pago");
        return idempotentes.responder(cobros.generar(solicitudId, turistaId, peticion, sesion ->
                idempotentes.guardar(HttpStatus.CREATED.value(), aRespuesta(sesion))));
    }

    @GetMapping("/pagos")
    public List<IntentoPagoResponse> intentos(@PathVariable UUID solicitudId, HttpServletRequest http) {
        return cobros.intentos(solicitudId, Identidad.de(http).usuarioId()).stream()
                .map(PagoController::aRespuesta).toList();
    }

    private static SesionCobroResponse aRespuesta(SesionGenerada generada) {
        IntentoPago intento = generada.intento();
        SesionCobro sesion = generada.sesion();
        return new SesionCobroResponse(intento.id(), intento.solicitudId(), intento.numero(), intento.estado(),
                intento.referencia(), intento.montoCentavos(), IntentoPago.MONEDA, intento.creadoEn(),
                new CheckoutResponse(sesion.pasarela(), sesion.urlCheckout(), sesion.llavePublica(),
                        sesion.firmaIntegridad(), sesion.urlRedireccion()));
    }

    private static IntentoPagoResponse aRespuesta(IntentoPago intento) {
        return new IntentoPagoResponse(intento.id(), intento.numero(), intento.estado(), intento.referencia(),
                intento.montoCentavos(), intento.transaccionId(), intento.creadoEn(), intento.resueltoEn());
    }
}
