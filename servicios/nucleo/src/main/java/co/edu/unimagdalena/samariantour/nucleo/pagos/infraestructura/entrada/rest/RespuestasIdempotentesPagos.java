package co.edu.unimagdalena.samariantour.nucleo.pagos.infraestructura.entrada.rest;

import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.excepcion.DatoInvalidoException;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo.PeticionIdempotente;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo.RespuestaGuardada;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo.ResultadoIdempotente;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * El lado HTTP de la idempotencia de pagos, como el de las solicitudes (ADR-04, DN-12, DN-19): lee el header
 * {@code Idempotency-Key} y responde la respuesta guardada tal cual, con {@code Idempotency-Replayed: true} si es
 * repetida.
 */
@Component
class RespuestasIdempotentesPagos {

    static final String HEADER_CLAVE = "Idempotency-Key";
    static final String HEADER_REPETIDA = "Idempotency-Replayed";

    private final ObjectMapper json;

    RespuestasIdempotentesPagos(ObjectMapper json) {
        this.json = json;
    }

    /** @throws DatoInvalidoException si falta el header o no es un UUID */
    PeticionIdempotente peticion(String clave, UUID usuarioId, String operacion, String recurso) {
        return PeticionIdempotente.de(clave(clave), usuarioId, operacion, recurso);
    }

    RespuestaGuardada guardar(int codigo, Object cuerpo) {
        try {
            return new RespuestaGuardada(codigo, json.writeValueAsString(cuerpo));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo convertir a JSON: " + cuerpo.getClass().getSimpleName(), e);
        }
    }

    ResponseEntity<String> responder(ResultadoIdempotente resultado) {
        ResponseEntity.BodyBuilder respuesta = ResponseEntity.status(resultado.respuesta().codigo())
                .contentType(MediaType.APPLICATION_JSON);
        if (resultado.repetida()) {
            respuesta.header(HEADER_REPETIDA, "true");
        }
        return respuesta.body(resultado.respuesta().cuerpo());
    }

    private static UUID clave(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new DatoInvalidoException(HEADER_CLAVE, "es obligatorio: un UUID que la aplicación genera antes del "
                    + "primer intento y repite en cada reintento");
        }
        try {
            return UUID.fromString(valor.strip());
        } catch (IllegalArgumentException e) {
            throw new DatoInvalidoException(HEADER_CLAVE, "debe ser un UUID");
        }
    }
}
