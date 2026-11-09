package co.edu.unimagdalena.samariantour.nucleo.solicitudes.aplicacion;

import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.CompromisosConTuristas;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.entrada.ConsultarCompromisosUseCase;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.salida.CompromisosSolicitudesPort;

import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;

/** Cuenta los compromisos del prestador con la fecha de hoy en Colombia, la del reloj del núcleo (DN-45). */
public class ConsultarCompromisosService implements ConsultarCompromisosUseCase {

    private final CompromisosSolicitudesPort compromisos;
    private final Clock reloj;

    public ConsultarCompromisosService(CompromisosSolicitudesPort compromisos, Clock reloj) {
        this.compromisos = compromisos;
        this.reloj = reloj;
    }

    @Override
    public CompromisosConTuristas delPrestador(UUID prestadorId) {
        return compromisos.delPrestador(prestadorId, LocalDate.now(reloj));
    }
}
