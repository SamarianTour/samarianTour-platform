package co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.puerto.salida;

/** Hash adaptativo de los códigos de un solo uso (RNF-06): nunca se guarda el código. */
public interface HasherPort {

    String hash(String valor);

    boolean coincide(String valor, String hash);
}
