package co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.entrada;

import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.Usuario;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Las cuentas internas, para la administración (RF-32, RF-81). */
public interface ConsultarCuentasInternasUseCase {

    /** De la más antigua a la más reciente. */
    List<Usuario> internas();

    /** Vacío si no existe o no es interna. */
    Optional<Usuario> interna(UUID usuarioId);

    boolean esOperadorActivo(UUID usuarioId);
}
