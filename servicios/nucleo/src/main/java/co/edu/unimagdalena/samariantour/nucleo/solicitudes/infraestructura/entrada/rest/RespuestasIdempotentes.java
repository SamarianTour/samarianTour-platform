package co.edu.unimagdalena.samariantour.nucleo.solicitudes.infraestructura.entrada.rest;

import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.excepcion.DatoInvalidoException;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.PeticionIdempotente;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.RespuestaGuardada;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.ResultadoIdempotente;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * El lado HTTP de la idempotencia (ADR-04, DN-12): lee el header {@code Idempotency-Key}, calcula la huella de la
 * petición con el cuerpo convertido de nuevo a JSON (así no importan los espacios) y responde la respuesta guardada tal
 * cual. Una respuesta repetida lleva {@code Idempotency-Replayed: true}.
 */
@Component
class RespuestasIdempotentes {

    static final String HEADER_CLAVE = "Idempotency-Key";
    static final String HEADER_REPETIDA = "Idempotency-Replayed";

    private final ObjectMapper json;

    RespuestasIdempotentes(ObjectMapper json) {
        this.json = json;
    }

    /**
     * @param recurso lo que identifica el destino de la operación además del cuerpo, por ejemplo la solicitud
     * @throws DatoInvalidoException si falta el header o no es un UUID
     */
    PeticionIdempotente peticion(String clave, UUID usuarioId, String operacion, String recurso, Object cuerpo) {
        return PeticionIdempotente.de(clave(clave), usuarioId, operacion, recurso + "\n" + aJson(cuerpo));
    }

    RespuestaGuardada guardar(int codigo, Object cuerpo) {
        return new RespuestaGuardada(codigo, aJson(cuerpo));
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
            throw new DatoInvalidoException(HEADER_CLAVE, ("es obligatorio: un UUID que la aplicación genera antes del "
                    + "primer intento y repite en cada reintento"));
        }
        try {
            return UUID.fromString(valor.strip());
        } catch (IllegalArgumentException e) {
            throw new DatoInvalidoException(HEADER_CLAVE, "debe ser un UUID");
        }
    }

    private String aJson(Object cuerpo) {
        try {
            return json.writeValueAsString(cuerpo);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo convertir a JSON: " + cuerpo.getClass().getSimpleName(), e);
        }
    }
}
