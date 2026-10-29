package co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.entrada;

import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.Turista;

import java.util.Optional;
import java.util.UUID;

/** El contacto del turista para los demás módulos (RF-44, RF-57, DN-22). */
public interface ConsultarContactoUseCase {

    /** El turista, si está activo y verificó su WhatsApp; si no, vacío (RF-30). */
    Optional<Turista> turistaContactable(UUID turistaId);
}
