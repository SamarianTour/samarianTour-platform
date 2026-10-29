package co.edu.unimagdalena.samariantour.nucleo.cuentas.infraestructura.salida.demo;

import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.excepcion.CorreoYaRegistradoException;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.DatosCuenta;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.PoliticaDatos;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.Rol;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.Usuario;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.salida.PasswordHasherPort;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.salida.PoliticaDatosRepositoryPort;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.salida.UsuarioRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Las cuentas de la demostración, con los mismos identificadores que usan el catálogo, el frontend y la demo de
 * solicitudes (DN-15, DN-28). Solo con el perfil demo. Se crean al arrancar si no existen, todas con la contraseña
 * {@value #CONTRASENA} y la política vigente aceptada; los turistas, con su WhatsApp ficticio verificado. Si las
 * dos réplicas arrancan a la vez, la que llega segunda encuentra la cuenta y la salta.
 */
@Component
@Profile("demo")
public class PrecargaCuentasDemo implements ApplicationRunner {

    public static final String CONTRASENA = "Demo1234";

    private static final Logger LOG = LoggerFactory.getLogger(PrecargaCuentasDemo.class);
    private static final String DOMINIO = "@demo.samariantour.co";

    /** Una cuenta de la demo; el WhatsApp solo para los turistas. */
    record CuentaDemo(String id, Rol rol, String nombre, String usuarioCorreo, String whatsapp) {
    }

    static final List<CuentaDemo> CUENTAS = List.of(
            // Prestadores del catálogo y del selector de usuario del frontend
            new CuentaDemo("10000000-0000-0000-0000-000000000001", Rol.PRESTADOR, "Agencia demo", "agencia", null),
            new CuentaDemo("20000000-0000-0000-0000-000000000002", Rol.PRESTADOR, "Guía demo", "guia", null),
            new CuentaDemo("30000000-0000-0000-0000-000000000003", Rol.PRESTADOR, "Hotel demo", "hotel", null),
            new CuentaDemo("40000000-0000-0000-0000-000000000004", Rol.PRESTADOR, "Restaurante demo", "restaurante",
                    null),
            new CuentaDemo("50000000-0000-0000-0000-000000000005", Rol.PRESTADOR, "Agencia sin RNT demo",
                    "agencia-sin-rnt", null),
            // Operadores de la demo de solicitudes (DN-15) y un gestor del destino
            new CuentaDemo("60000000-0000-0000-0000-000000000001", Rol.OPERADOR_DISPONIBILIDAD, "Operador 1",
                    "operador1", null),
            new CuentaDemo("60000000-0000-0000-0000-000000000002", Rol.OPERADOR_DISPONIBILIDAD, "Operador 2",
                    "operador2", null),
            new CuentaDemo("60000000-0000-0000-0000-000000000003", Rol.GESTOR_DESTINO, "Gestor del destino",
                    "gestor", null),
            // Turistas del catálogo y del frontend
            new CuentaDemo("70000000-0000-0000-0000-000000000007", Rol.TURISTA, "Laura", "laura", "+573105550007"),
            new CuentaDemo("80000000-0000-0000-0000-000000000008", Rol.TURISTA, "Andrés", "andres", "+573105550008"),
            // Turistas de la demo de solicitudes (DN-15)
            new CuentaDemo("90000000-0000-0000-0000-000000000001", Rol.TURISTA, "Laura Gómez", "laura.gomez",
                    "+573105550001"),
            new CuentaDemo("90000000-0000-0000-0000-000000000002", Rol.TURISTA, "Carlos Pérez", "carlos.perez",
                    "+573105550002"),
            new CuentaDemo("90000000-0000-0000-0000-000000000003", Rol.TURISTA, "Ana Torres", "ana.torres",
                    "+573105550003"));

    private final UsuarioRepositoryPort usuarios;
    private final PoliticaDatosRepositoryPort politicas;
    private final PasswordHasherPort hasher;
    private final TransactionTemplate transaccion;
    private final Clock reloj;

    PrecargaCuentasDemo(UsuarioRepositoryPort usuarios, PoliticaDatosRepositoryPort politicas,
                        PasswordHasherPort hasher, TransactionTemplate transaccion, Clock reloj) {
        this.usuarios = usuarios;
        this.politicas = politicas;
        this.hasher = hasher;
        this.transaccion = transaccion;
        this.reloj = reloj;
    }

    @Override
    public void run(ApplicationArguments argumentos) {
        String hash = hasher.hash(CONTRASENA);
        PoliticaDatos vigente = politicas.vigente();
        Instant ahora = reloj.instant();
        int creadas = 0;
        for (CuentaDemo cuenta : CUENTAS) {
            UUID id = UUID.fromString(cuenta.id());
            try {
                Boolean creada = transaccion.execute(estado -> {
                    if (usuarios.porId(id).isPresent()) {
                        return false;
                    }
                    Usuario usuario = Usuario.reconstruir(cuenta.rol(),
                            DatosCuenta.nueva(id, cuenta.nombre(), cuenta.usuarioCorreo() + DOMINIO, hash, ahora),
                            cuenta.whatsapp(), cuenta.whatsapp() != null);
                    usuario.aceptarPolitica(vigente, ahora);
                    usuarios.insertar(usuario);
                    return true;
                });
                creadas += Boolean.TRUE.equals(creada) ? 1 : 0;
            } catch (CorreoYaRegistradoException | DataIntegrityViolationException e) {
                // La otra réplica la creó entre la consulta y la inserción.
            }
        }
        LOG.info("Cuentas de la demo: {} creadas, {} ya estaban", creadas, CUENTAS.size() - creadas);
    }
}
