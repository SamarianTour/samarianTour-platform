package co.edu.unimagdalena.samariantour.nucleo.cuentas.infraestructura.salida.persistencia;

import co.edu.unimagdalena.samariantour.nucleo.compartido.cifrado.CifradorDatos;
import co.edu.unimagdalena.samariantour.nucleo.compartido.concurrencia.ConflictoConcurrenciaException;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.excepcion.CorreoYaRegistradoException;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.AceptacionPolitica;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.DatosCuenta;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.EstadoCuenta;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.EstadoSupresion;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.Rol;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.SolicitudSupresion;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.Turista;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.Usuario;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.salida.UsuarioRepositoryPort;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Las cuentas en {@code usuarios}, {@code aceptaciones_politica} y {@code solicitudes_supresion} (V8, DN-02). Cifra el
 * nombre y el WhatsApp al guardar y los descifra al leer (DN-18): el dominio nunca ve un valor cifrado.
 */
@Component
public class UsuarioJpaAdapter implements UsuarioRepositoryPort {

    /** La restricción única del correo (V8). */
    static final String CORREO_UNICO = "usuarios_correo_unico";

    private final UsuarioJpaRepository usuarios;
    private final AceptacionPoliticaJpaRepository aceptaciones;
    private final SolicitudSupresionJpaRepository supresiones;
    private final CifradorDatos cifrador;

    UsuarioJpaAdapter(UsuarioJpaRepository usuarios, AceptacionPoliticaJpaRepository aceptaciones,
                      SolicitudSupresionJpaRepository supresiones, CifradorDatos cifrador) {
        this.usuarios = usuarios;
        this.aceptaciones = aceptaciones;
        this.supresiones = supresiones;
        this.cifrador = cifrador;
    }

    @Override
    public void insertar(Usuario usuario) {
        UsuarioEntity fila = new UsuarioEntity(usuario.id(), usuario.rol().name(), usuario.fechaRegistro());
        copiar(usuario, fila);
        try {
            usuarios.saveAndFlush(fila);
        } catch (DataIntegrityViolationException e) {
            if (String.valueOf(e.getMostSpecificCause().getMessage()).contains(CORREO_UNICO)) {
                throw new CorreoYaRegistradoException(usuario.correo());
            }
            throw e;
        }
        guardarAceptacionesYSupresion(usuario);
    }

    @Override
    public void actualizar(Usuario usuario) {
        UsuarioEntity fila = usuarios.findById(usuario.id())
                .orElseThrow(() -> new ConflictoConcurrenciaException(
                        "La cuenta %s ya no existe".formatted(usuario.id())));
        if (!Objects.equals(fila.revision(), usuario.revision())) {
            throw new ConflictoConcurrenciaException("La cuenta %s cambió desde que se leyó".formatted(usuario.id()));
        }
        copiar(usuario, fila);
        try {
            usuarios.saveAndFlush(fila);
        } catch (OptimisticLockingFailureException e) {
            throw new ConflictoConcurrenciaException("La cuenta %s cambió desde que se leyó".formatted(usuario.id()));
        } catch (DataIntegrityViolationException e) {
            if (String.valueOf(e.getMostSpecificCause().getMessage()).contains(CORREO_UNICO)) {
                throw new CorreoYaRegistradoException(usuario.correo());
            }
            throw e;
        }
        guardarAceptacionesYSupresion(usuario);
    }

    @Override
    public Optional<Usuario> porId(UUID id) {
        return usuarios.findById(id).map(this::aDominio);
    }

    @Override
    public Optional<Usuario> porCorreo(String correo) {
        return usuarios.findByCorreo(correo).map(this::aDominio);
    }

    @Override
    public Optional<Usuario> porCorreoParaActualizar(String correo) {
        return usuarios.bloquearPorCorreo(correo).map(this::aDominio);
    }

    private void copiar(Usuario usuario, UsuarioEntity fila) {
        String whatsapp = usuario instanceof Turista turista ? turista.whatsapp() : null;
        boolean verificado = usuario instanceof Turista turista && turista.whatsappVerificado();
        fila.copiar(cifrador.cifrar(usuario.nombre()), usuario.correo(), usuario.hashContrasena(),
                usuario.estado().name(), usuario.intentosFallidos(), usuario.bloqueadaHasta(),
                cifrador.cifrar(whatsapp), verificado);
    }

    /** Las aceptaciones solo se agregan: inserta las que la cuenta tiene y la tabla todavía no. */
    private void guardarAceptacionesYSupresion(Usuario usuario) {
        Set<String> guardadas = aceptaciones.findByUsuarioIdOrderByFechaAceptacionAsc(usuario.id()).stream()
                .map(AceptacionPoliticaEntity::version).collect(Collectors.toSet());
        usuario.aceptaciones().stream()
                .filter(aceptacion -> !guardadas.contains(aceptacion.version()))
                .forEach(aceptacion -> aceptaciones.save(new AceptacionPoliticaEntity(usuario.id(),
                        aceptacion.version(), aceptacion.fechaAceptacion())));
        usuario.supresion().ifPresent(supresion -> {
            SolicitudSupresionEntity fila = supresiones.findById(supresion.id()).orElseGet(() ->
                    new SolicitudSupresionEntity(supresion.id(), usuario.id(), supresion.fechaSolicitud()));
            fila.copiar(supresion.estado().name(), supresion.fechaEjecucion());
            supresiones.save(fila);
        });
    }

    private Usuario aDominio(UsuarioEntity fila) {
        DatosCuenta datos = new DatosCuenta(fila.id(), cifrador.descifrar(fila.nombreCifrado()), fila.correo(),
                fila.hashContrasena(), EstadoCuenta.valueOf(fila.estado()), fila.intentosFallidos(),
                fila.bloqueadaHasta(), fila.fechaRegistro(), fila.revision(),
                aceptaciones.findByUsuarioIdOrderByFechaAceptacionAsc(fila.id()).stream()
                        .map(aceptacion -> new AceptacionPolitica(aceptacion.version(), aceptacion.fechaAceptacion()))
                        .toList(),
                supresiones.findByUsuarioId(fila.id())
                        .map(supresion -> new SolicitudSupresion(supresion.id(), supresion.fechaSolicitud(),
                                EstadoSupresion.valueOf(supresion.estado()), supresion.fechaEjecucion()))
                        .orElse(null));
        return Usuario.reconstruir(Rol.valueOf(fila.rol()), datos, cifrador.descifrar(fila.whatsappCifrado()),
                fila.whatsappVerificado());
    }
}
