package co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.salida;

/**
 * Hash adaptativo de las contraseñas (RNF-06) y de los secretos de un solo uso: códigos y tokens de renovación.
 * Nunca se guarda el valor en claro.
 */
public interface PasswordHasherPort {

    String hash(String valor);

    boolean coincide(String valor, String hash);
}
