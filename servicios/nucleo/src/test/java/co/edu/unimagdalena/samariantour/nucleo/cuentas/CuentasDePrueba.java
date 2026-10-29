package co.edu.unimagdalena.samariantour.nucleo.cuentas;

import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.DatosCuenta;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.PoliticaDatos;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.Rol;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.Turista;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.Usuario;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.salida.PasswordHasherPort;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Datos y dobles comunes de las pruebas de cuentas. */
public final class CuentasDePrueba {

    public static final Instant AHORA = Instant.parse("2026-10-28T15:00:00Z");
    public static final PoliticaDatos V1 = new PoliticaDatos("v1", LocalDate.parse("2026-10-28"),
            "/api/nucleo/politica-datos/v1");
    public static final PoliticaDatos V2 = new PoliticaDatos("v2", LocalDate.parse("2026-11-15"),
            "/api/nucleo/politica-datos/v2");

    /** Un hash reconocible y sin costo: "h:" y el valor. */
    public static final PasswordHasherPort HASHER = new PasswordHasherPort() {
        @Override
        public String hash(String valor) {
            return "h:" + valor;
        }

        @Override
        public boolean coincide(String valor, String hash) {
            return hash.equals("h:" + valor);
        }
    };

    private CuentasDePrueba() {
    }

    public static Turista turista(String correo, String whatsapp, boolean verificado) {
        return (Turista) Usuario.reconstruir(Rol.TURISTA, DatosCuenta.nueva(UUID.randomUUID(), "Laura Gómez", correo,
                HASHER.hash("Clave1234"), AHORA), whatsapp, verificado);
    }

    public static Usuario cuenta(Rol rol, String correo) {
        return Usuario.reconstruir(rol, DatosCuenta.nueva(UUID.randomUUID(), "Cuenta de prueba", correo,
                HASHER.hash("Clave1234"), AHORA), null, false);
    }
}
