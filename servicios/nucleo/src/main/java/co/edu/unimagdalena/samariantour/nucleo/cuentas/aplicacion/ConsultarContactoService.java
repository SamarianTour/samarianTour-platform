package co.edu.unimagdalena.samariantour.nucleo.cuentas.aplicacion;

import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.Turista;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.entrada.ConsultarContactoUseCase;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.salida.UsuarioRepositoryPort;

import java.util.Optional;
import java.util.UUID;

/** El turista contactable: activo y con el WhatsApp verificado (RF-03, RF-30, RF-44). */
public class ConsultarContactoService implements ConsultarContactoUseCase {

    private final UsuarioRepositoryPort usuarios;

    public ConsultarContactoService(UsuarioRepositoryPort usuarios) {
        this.usuarios = usuarios;
    }

    @Override
    public Optional<Turista> turistaContactable(UUID turistaId) {
        return usuarios.porId(turistaId)
                .filter(Turista.class::isInstance).map(Turista.class::cast)
                .filter(Turista::puedeEnviarSolicitudes);
    }
}
