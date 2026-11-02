package co.edu.unimagdalena.samariantour.nucleo.cuentas.aplicacion;

import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.Rol;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.Usuario;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.entrada.ConsultarCuentasInternasUseCase;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.salida.UsuarioRepositoryPort;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Las cuentas internas para la administración (RF-32, RF-81). */
public class ConsultarCuentasInternasService implements ConsultarCuentasInternasUseCase {

    private final UsuarioRepositoryPort usuarios;

    public ConsultarCuentasInternasService(UsuarioRepositoryPort usuarios) {
        this.usuarios = usuarios;
    }

    @Override
    public List<Usuario> internas() {
        return usuarios.internas();
    }

    @Override
    public Optional<Usuario> interna(UUID usuarioId) {
        return usuarios.porId(usuarioId).filter(usuario -> usuario.rol().esInterno());
    }

    @Override
    public boolean esOperadorActivo(UUID usuarioId) {
        return usuarios.porId(usuarioId)
                .filter(usuario -> usuario.rol() == Rol.OPERADOR_DISPONIBILIDAD)
                .filter(Usuario::estaActiva)
                .isPresent();
    }
}
