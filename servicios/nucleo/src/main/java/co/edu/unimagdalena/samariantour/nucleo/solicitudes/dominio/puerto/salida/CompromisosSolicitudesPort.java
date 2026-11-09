package co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.salida;

import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.CompromisosConTuristas;

import java.time.LocalDate;
import java.util.UUID;

/** Cuenta los compromisos del prestador con los turistas, en la base (DN-45). */
public interface CompromisosSolicitudesPort {

    /** @param hoy la fecha de hoy en Colombia: una solicitud cuyo servicio termina hoy todavía cuenta */
    CompromisosConTuristas delPrestador(UUID prestadorId, LocalDate hoy);
}
