package co.edu.unimagdalena.samariantour.nucleo.pagos.infraestructura.salida.persistencia;

import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo.OperacionRegistrada;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo.PeticionIdempotente;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo.RespuestaGuardada;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.salida.IdempotenciaPort;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;

/**
 * Las claves de idempotencia de pagos en {@code claves_idempotencia_pagos} (DN-19), como las de las solicitudes:
 * dos réplicas con la misma clave a la vez se ordenan con {@code INSERT ... ON CONFLICT}.
 */
@Component
public class IdempotenciaPagosJpaAdapter implements IdempotenciaPort {

    private final ClaveIdempotenciaPagoJpaRepository claves;

    IdempotenciaPagosJpaAdapter(ClaveIdempotenciaPagoJpaRepository claves) {
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
        ClaveIdempotenciaPagoEntity fila = claves.leer(peticion.usuarioId(), peticion.clave()).orElseThrow(() ->
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
