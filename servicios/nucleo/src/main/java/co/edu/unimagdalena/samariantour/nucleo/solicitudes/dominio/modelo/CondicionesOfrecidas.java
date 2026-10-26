package co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo;

import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.excepcion.DatoInvalidoException;

/**
 * Lo que el operador registra al atender la solicitud y el turista ve antes de confirmar (RF-37 a RF-39, RF-42). Una
 * vez atendida no cambia (RF-39).
 *
 * @param precioBase          precio total en pesos colombianos, sin cargos adicionales: es lo que se cobra (RF-37,
 *                            RF-49)
 * @param politicaCancelacion la del prestador, obligatoria (RF-39)
 * @param nota                otras condiciones transmitidas; opcional
 */
public record CondicionesOfrecidas(long precioBase, String politicaCancelacion, String nota) {

    public static final int LONGITUD_MAXIMA_POLITICA = 1000;
    public static final int LONGITUD_MAXIMA_NOTA = 1000;

    public CondicionesOfrecidas {
        if (precioBase <= 0) {
            throw new DatoInvalidoException("precioBase", "debe ser un valor entero positivo en pesos");
        }
        politicaCancelacion = Textos.limpio(politicaCancelacion);
        if (politicaCancelacion == null) {
            throw new DatoInvalidoException("politicaCancelacion",
                    "es obligatoria: el turista la ve antes de confirmar (RF-39, RF-42)");
        }
        Textos.verificarLongitud("politicaCancelacion", politicaCancelacion, LONGITUD_MAXIMA_POLITICA);
        nota = Textos.limpio(nota);
        Textos.verificarLongitud("nota", nota, LONGITUD_MAXIMA_NOTA);
    }
}
