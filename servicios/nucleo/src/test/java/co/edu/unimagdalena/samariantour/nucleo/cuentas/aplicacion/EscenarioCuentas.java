package co.edu.unimagdalena.samariantour.nucleo.cuentas.aplicacion;

import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.excepcion.CorreoYaRegistradoException;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.CodigoUnUso;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.PoliticaDatos;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.PropositoCodigo;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.Sesion;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.TokenAcceso;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.Usuario;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.salida.CodigoRepositoryPort;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.salida.EmailPort;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.salida.SesionRepositoryPort;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.salida.TokenPort;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.salida.UsuarioRepositoryPort;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.random.RandomGenerator;

import static co.edu.unimagdalena.samariantour.nucleo.cuentas.CuentasDePrueba.AHORA;
import static co.edu.unimagdalena.samariantour.nucleo.cuentas.CuentasDePrueba.HASHER;
import static co.edu.unimagdalena.samariantour.nucleo.cuentas.CuentasDePrueba.V1;

/**
 * Los casos de uso de las cuentas con dobles en memoria (DN-03): las cuentas, los códigos y los WhatsApp enviados. El
 * generador de códigos da siempre 48213, así que el código es "048213".
 */
class EscenarioCuentas {

    static final String CODIGO = "048213";

    final Map<UUID, Usuario> usuarios = new LinkedHashMap<>();
    final List<CodigoUnUso> codigos = new ArrayList<>();
    final List<String> whatsappsEnviados = new ArrayList<>();
    PoliticaDatos vigente = V1;
    Instant ahora = AHORA;

    final UsuarioRepositoryPort repositorio = new UsuarioRepositoryPort() {
        @Override
        public void insertar(Usuario usuario) {
            if (porCorreo(usuario.correo()).isPresent()) {
                throw new CorreoYaRegistradoException(usuario.correo());
            }
            usuarios.put(usuario.id(), usuario);
        }

        @Override
        public void actualizar(Usuario usuario) {
            usuarios.put(usuario.id(), usuario);
        }

        @Override
        public Optional<Usuario> porId(UUID id) {
            return Optional.ofNullable(usuarios.get(id));
        }

        @Override
        public Optional<Usuario> porCorreo(String correo) {
            return usuarios.values().stream().filter(usuario -> usuario.correo().equals(correo)).findFirst();
        }

        @Override
        public Optional<Usuario> porCorreoParaActualizar(String correo) {
            return porCorreo(correo);
        }
    };

    final CodigoRepositoryPort repositorioCodigos = new CodigoRepositoryPort() {
        @Override
        public void insertar(CodigoUnUso codigo) {
            codigos.add(codigo);
        }

        @Override
        public void actualizar(CodigoUnUso codigo) {
            // Es el mismo objeto de la lista.
        }

        @Override
        public Optional<CodigoUnUso> porId(UUID id) {
            return codigos.stream().filter(codigo -> codigo.id().equals(id)).findFirst();
        }

        @Override
        public Optional<CodigoUnUso> ultimo(UUID usuarioId, PropositoCodigo proposito) {
            return codigos.stream()
                    .filter(codigo -> codigo.usuarioId().equals(usuarioId) && codigo.proposito() == proposito)
                    .max(Comparator.comparing(CodigoUnUso::emitidoEn));
        }
    };

    private final Clock reloj = new Clock() {
        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zona) {
            return this;
        }

        @Override
        public Instant instant() {
            return ahora;
        }
    };

    final EmisionCodigoWhatsapp emision = new EmisionCodigoWhatsapp(repositorioCodigos, HASHER,
            (whatsapp, codigo) -> whatsappsEnviados.add(whatsapp + ": " + codigo), new RandomGenerator() {
                @Override
                public long nextLong() {
                    return 48213L;
                }

                @Override
                public int nextInt(int limite) {
                    return 48213;
                }
            });

    final RegistrarUsuarioService registrar = new RegistrarUsuarioService(repositorio, () -> vigente, HASHER,
            emision, reloj);

    final VerificarWhatsAppService verificarWhatsApp = new VerificarWhatsAppService(repositorio, repositorioCodigos,
            HASHER, emision, reloj);

    final Map<UUID, Sesion> sesiones = new LinkedHashMap<>();

    final SesionRepositoryPort repositorioSesiones = new SesionRepositoryPort() {
        @Override
        public void insertar(Sesion sesion) {
            sesiones.put(sesion.id(), sesion);
        }

        @Override
        public void actualizar(Sesion sesion) {
            sesiones.put(sesion.id(), sesion);
        }

        @Override
        public Optional<Sesion> porId(UUID id) {
            return Optional.ofNullable(sesiones.get(id));
        }

        @Override
        public void revocarTodas(UUID usuarioId, Instant ahora) {
            sesiones.values().stream().filter(sesion -> sesion.usuarioId().equals(usuarioId))
                    .forEach(sesion -> sesion.revocar(ahora));
        }
    };

    /** Un token de acceso falso que deja ver a quién y a qué sesión se emitió. */
    final TokenPort tokens = (usuario, sesionId, emitido) ->
            new TokenAcceso("jwt:" + usuario.id() + ":" + sesionId, emitido.plus(Sesion.VIGENCIA_ACCESO));

    final EmisionSesion emisionSesion = new EmisionSesion(repositorioSesiones, HASHER, tokens, new Random(7));

    final IniciarSesionService iniciarSesion = new IniciarSesionService(repositorio, HASHER, emisionSesion, reloj);

    final RenovarSesionService renovarSesion = new RenovarSesionService(repositorioSesiones, repositorio, HASHER,
            emisionSesion, reloj);

    final CerrarSesionService cerrarSesion = new CerrarSesionService(repositorioSesiones, HASHER, reloj);

    /** Los enlaces enviados por correo: el correo y el token, separados por un espacio. */
    final List<String> correosEnviados = new ArrayList<>();

    final EmailPort correo = (direccion, nombre, token) -> correosEnviados.add(direccion + " " + token);

    final SolicitarRestablecimientoService solicitarRestablecimiento = new SolicitarRestablecimientoService(
            repositorio, repositorioCodigos, HASHER, correo, new Random(11), reloj);

    final RestablecerContrasenaService restablecerContrasena = new RestablecerContrasenaService(repositorio,
            repositorioCodigos, repositorioSesiones, HASHER, reloj);

    /** Los turistas cuya supresión se avisó al catálogo. */
    final List<UUID> supresionesAvisadas = new ArrayList<>();

    final GestionarDatosPersonalesService datosPersonales = new GestionarDatosPersonalesService(repositorio, emision,
            reloj);

    final SolicitarSupresionService supresion = new SolicitarSupresionService(repositorio, repositorioSesiones,
            supresionesAvisadas::add, HASHER, reloj);

    /** El token del último enlace enviado. */
    String ultimoEnlace() {
        String ultimo = correosEnviados.getLast();
        return ultimo.substring(ultimo.indexOf(' ') + 1);
    }
}
