package co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion;

/** Ya hay otra zona con ese nombre, sin distinguir mayúsculas ni tildes (DC-49). */
public class ZonaDuplicadaException extends ExcepcionDominio {

    public ZonaDuplicadaException(String nombre) {
        super("ZONA_DUPLICADA", "Ya existe una zona con el nombre %s".formatted(nombre));
    }
}
