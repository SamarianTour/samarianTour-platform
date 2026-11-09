package co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.entrada;

import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.CompromisosConTuristas;

import java.util.UUID;

/** Lo que el prestador todavía le debe a los turistas, para la supresión de sus datos (RF-13, DN-45). */
public interface ConsultarCompromisosUseCase {

    CompromisosConTuristas delPrestador(UUID prestadorId);
}
