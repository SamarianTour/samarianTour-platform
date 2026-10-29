package co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo;

import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.excepcion.DatoInvalidoException;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Las reglas de los datos de una cuenta (RF-02, RF-03). Los topes de longitud y los formatos son [SUPUESTO] hasta
 * cerrar el SRS, como DC-19 en el catálogo (DN-18).
 */
public final class Validaciones {

    public static final int LONGITUD_MAXIMA_NOMBRE = 100;
    public static final int LONGITUD_MAXIMA_CORREO = 254;
    public static final int LONGITUD_MINIMA_CONTRASENA = 8;
    /** BCrypt solo usa los primeros 72 bytes: una contraseña más larga se compararía sin su final. */
    public static final int LONGITUD_MAXIMA_CONTRASENA = 72;

    private static final Pattern CORREO = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    /** Formato internacional E.164: + y de 8 a 15 dígitos, sin espacios. */
    private static final Pattern WHATSAPP = Pattern.compile("^\\+[1-9]\\d{7,14}$");
    private static final Pattern LETRA = Pattern.compile("\\p{L}");
    private static final Pattern DIGITO = Pattern.compile("\\d");

    private Validaciones() {
    }

    /** @return el nombre sin espacios en los extremos */
    public static String nombre(String nombre) {
        String limpio = nombre == null ? "" : nombre.strip();
        if (limpio.isEmpty()) {
            throw new DatoInvalidoException("nombre", "es obligatorio");
        }
        if (limpio.length() > LONGITUD_MAXIMA_NOMBRE) {
            throw new DatoInvalidoException("nombre", "tiene más de %d caracteres".formatted(LONGITUD_MAXIMA_NOMBRE));
        }
        return limpio;
    }

    /** @return el correo en minúsculas: dos cuentas no pueden tener el mismo correo con otras mayúsculas */
    public static String correo(String correo) {
        String limpio = correo == null ? "" : correo.strip().toLowerCase(Locale.ROOT);
        if (limpio.isEmpty()) {
            throw new DatoInvalidoException("correo", "es obligatorio");
        }
        if (limpio.length() > LONGITUD_MAXIMA_CORREO || !CORREO.matcher(limpio).matches()) {
            throw new DatoInvalidoException("correo", "no es un correo válido");
        }
        return limpio;
    }

    /** @return el número sin espacios ni guiones, en formato E.164 */
    public static String whatsapp(String whatsapp) {
        String limpio = whatsapp == null ? "" : whatsapp.replaceAll("[\\s-]", "");
        if (limpio.isEmpty()) {
            throw new DatoInvalidoException("whatsapp", "es obligatorio para el turista");
        }
        if (!WHATSAPP.matcher(limpio).matches()) {
            throw new DatoInvalidoException("whatsapp", "debe tener el indicativo del país, por ejemplo +573001234567");
        }
        return limpio;
    }

    /** Una contraseña nueva: de 8 a 72 caracteres, con al menos una letra y un número. */
    public static void contrasena(String contrasena) {
        if (contrasena == null || contrasena.length() < LONGITUD_MINIMA_CONTRASENA
                || contrasena.length() > LONGITUD_MAXIMA_CONTRASENA
                || !LETRA.matcher(contrasena).find() || !DIGITO.matcher(contrasena).find()) {
            throw new DatoInvalidoException("contrasena",
                    "debe tener de %d a %d caracteres, con al menos una letra y un número"
                            .formatted(LONGITUD_MINIMA_CONTRASENA, LONGITUD_MAXIMA_CONTRASENA));
        }
    }
}
