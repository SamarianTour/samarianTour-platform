package co.edu.unimagdalena.samariantour.nucleo.solicitudes.infraestructura.salida.persistencia;

import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.OperacionRegistrada;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.PeticionIdempotente;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.RespuestaGuardada;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.salida.IdempotenciaPort;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;

/**
 * Las claves de idempotencia en {@code claves_idempotencia} (ADR-04, DN-12). Dos réplicas con la misma clave a la vez
 * se ordenan con {@code INSERT ... ON CONFLICT}, como las decisiones de moderación (DN-08).
 */
@Component
public class IdempotenciaJpaAdapter implements IdempotenciaPort {

    private final ClaveIdempotenciaJpaRepository claves;

    IdempotenciaJpaAdapter(ClaveIdempotenciaJpaRepository claves) {
        this.claves = claves;
    }

    @Override
    public Optional<OperacionRegistrada> reservar(PeticionIdempotente peticion, Instant ahora) {
        claves.borrarSiVencio(peticion.usuarioId(), peticion.clave(), ahora);
        int reservadas = claves.reservar(peticion.usuarioId(), peticion.clave(), peticion.operacion(),
                peticion.huella(), ahora, ahora.plus(PeticionIdempotente.VIGENCIA));
        if (reservadas == 1) {
            return Optional.empty();
        }
        // Otra transacción la usó y confirmó: su respuesta ya está guardada.
        ClaveIdempotenciaEntity fila = claves.leer(peticion.usuarioId(), peticion.clave()).orElseThrow(() ->
                new IllegalStateException("La clave " + peticion.clave() + " chocó pero no está guardada"));
        return Optional.of(new OperacionRegistrada(fila.operacion(), fila.huella(),
                new RespuestaGuardada(fila.codigoRespuesta(), fila.cuerpoRespuesta())));
    }

    @Override
    public void completar(PeticionIdempotente peticion, RespuestaGuardada respuesta) {
        claves.completar(peticion.usuarioId(), peticion.clave(), respuesta.codigo(), respuesta.cuerpo());
    }

    @Override
    public int purgarVencidas(Instant ahora) {
        return claves.purgarVencidas(ahora);
    }
}
