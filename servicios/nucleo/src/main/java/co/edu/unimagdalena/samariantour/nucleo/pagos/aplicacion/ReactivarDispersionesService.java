package co.edu.unimagdalena.samariantour.nucleo.pagos.aplicacion;

import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo.Dispersion;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.entrada.ReactivarDispersionesUseCase;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.salida.DispersionRepositoryPort;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Reactiva las dispersiones fallidas de un prestador cuando cambia su cuenta de cobro (RF-54, DN-44): la falla más
 * común es una cuenta equivocada, y el aviso de RF-54 le pide justamente que la actualice. Va en la transacción del
 * cambio, así que si este se deshace ninguna queda reactivada. La tarea de dispersiones las envía en los siguientes
 * 15 s con la cuenta nueva.
 */
public class ReactivarDispersionesService implements ReactivarDispersionesUseCase {

    private final DispersionRepositoryPort dispersiones;
    private final Clock reloj;

    public ReactivarDispersionesService(DispersionRepositoryPort dispersiones, Clock reloj) {
        this.dispersiones = dispersiones;
        this.reloj = reloj;
    }

    @Override
    public int reactivar(UUID prestadorId) {
        Instant ahora = reloj.instant();
        List<Dispersion> fallidas = dispersiones.fallidasDelPrestador(prestadorId);
        for (Dispersion dispersion : fallidas) {
            dispersion.reactivar(ahora);
            dispersiones.actualizar(dispersion);
        }
        return fallidas.size();
    }
}
