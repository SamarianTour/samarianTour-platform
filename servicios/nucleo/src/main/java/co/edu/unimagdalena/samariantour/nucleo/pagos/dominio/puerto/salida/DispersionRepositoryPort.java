package co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.salida;

import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo.Dispersion;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Las dispersiones en PostgreSQL (DN-24): una por solicitud. */
public interface DispersionRepositoryPort {

    void insertar(Dispersion dispersion);

    void actualizar(Dispersion dispersion);

    Optional<Dispersion> deLaSolicitud(UUID solicitudId);

    /**
     * Las pendientes cuyo próximo intento ya toca, bloqueadas hasta el final de la transacción; las de otra transacción
     * se saltan, así dos réplicas nunca envían la misma.
     */
    List<Dispersion> tomarPendientes(Instant ahora, int cantidad);

    /** Las fallidas que todavía no se avisaron al prestador, bloqueadas y saltando las de otra transacción. */
    List<Dispersion> tomarFallidasSinAviso(int cantidad);
}
