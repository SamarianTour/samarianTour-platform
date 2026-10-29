package co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.salida;

import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo.PagoTardio;

import java.util.List;

/** Los pagos tardíos en PostgreSQL (DN-22). */
public interface PagoTardioRepositoryPort {

    void guardar(PagoTardio pago);

    /** Todos, del más reciente al más antiguo, para que el administrador los reembolse (RF-50). */
    List<PagoTardio> todos();

    /** Los que todavía no se avisaron al administrador, bloqueados y saltando los de otra transacción. */
    List<PagoTardio> tomarSinAviso(int cantidad);

    void marcarAvisado(PagoTardio pago);
}
