package co.edu.unimagdalena.samariantour.nucleo.cuentas.infraestructura.entrada.modulos;

import co.edu.unimagdalena.samariantour.nucleo.cuentas.api.CuentaInternaRechazadaException;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.api.CuentaInternaRegistrada;

import co.edu.unimagdalena.samariantour.nucleo.cuentas.api.CuentasAdministracionApi;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.excepcion.ExcepcionDominio;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.Rol;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.Usuario;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.entrada.ConsultarCuentasInternasUseCase;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.entrada.CrearCuentaInternaUseCase;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.entrada.DesactivarCuentaInternaUseCase;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.entrada.comando.CrearCuentaInternaCommand;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * La entrada de la administración a las cuentas internas (RF-81), como un controlador: traduce a los casos de uso y
 * entrega los tipos del {@code api}, también en los errores (regla 11 de {@code ArquitecturaTest}).
 */
@Component
public class CuentasAdministracionApiAdapter implements CuentasAdministracionApi {

    private final CrearCuentaInternaUseCase crear;
    private final DesactivarCuentaInternaUseCase desactivacion;
    private final ConsultarCuentasInternasUseCase consultas;

    public CuentasAdministracionApiAdapter(CrearCuentaInternaUseCase crear,
                                           DesactivarCuentaInternaUseCase desactivacion,
                                           ConsultarCuentasInternasUseCase consultas) {
        this.crear = crear;
        this.desactivacion = desactivacion;
        this.consultas = consultas;
    }

    @Override
    public List<CuentaInternaRegistrada> cuentasInternas() {
        return consultas.internas().stream().map(CuentasAdministracionApiAdapter::aApi).toList();
    }

    @Override
    public Optional<CuentaInternaRegistrada> cuentaInterna(UUID usuarioId) {
        return consultas.interna(usuarioId).map(CuentasAdministracionApiAdapter::aApi);
    }

    @Override
    public CuentaInternaRegistrada crearCuentaInterna(String rol, String nombre, String correo) {
        // Un rol desconocido llega vacío, y el dominio responde qué roles se aceptan.
        Rol valor = Arrays.stream(Rol.values()).filter(candidato -> candidato.name().equals(rol)).findFirst()
                .orElse(null);
        return traducir(() -> crear.crear(new CrearCuentaInternaCommand(valor, nombre, correo)));
    }

    @Override
    public CuentaInternaRegistrada desactivar(UUID usuarioId) {
        return traducir(() -> desactivacion.desactivar(usuarioId));
    }

    @Override
    public CuentaInternaRegistrada reactivar(UUID usuarioId) {
        return traducir(() -> desactivacion.reactivar(usuarioId));
    }

    @Override
    public boolean esOperadorActivo(UUID usuarioId) {
        return consultas.esOperadorActivo(usuarioId);
    }

    private static CuentaInternaRegistrada traducir(Supplier<Usuario> operacion) {
        try {
            return aApi(operacion.get());
        } catch (ExcepcionDominio e) {
            throw new CuentaInternaRechazadaException(e.codigo(), e.getMessage());
        }
    }

    private static CuentaInternaRegistrada aApi(Usuario usuario) {
        return new CuentaInternaRegistrada(usuario.id(), usuario.rol().name(), usuario.nombre(), usuario.correo(),
                usuario.estado().name(), usuario.tieneContrasena(), usuario.fechaRegistro());
    }
}
