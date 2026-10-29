package co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.salida;

import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.CodigoUnUso;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.PropositoCodigo;

import java.util.Optional;
import java.util.UUID;

/** Los códigos de un solo uso (RF-03, RF-08). */
public interface CodigoRepositoryPort {

    void insertar(CodigoUnUso codigo);

    /** Guarda el uso o un intento fallido, con bloqueo optimista: un código no se gasta dos veces a la vez. */
    void actualizar(CodigoUnUso codigo);

    Optional<CodigoUnUso> porId(UUID id);

    /** El emitido más recientemente para ese usuario y ese propósito, válido o no. */
    Optional<CodigoUnUso> ultimo(UUID usuarioId, PropositoCodigo proposito);
}
