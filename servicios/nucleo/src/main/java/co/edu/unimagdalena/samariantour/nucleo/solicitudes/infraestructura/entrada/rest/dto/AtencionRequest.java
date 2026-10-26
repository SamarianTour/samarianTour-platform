package co.edu.unimagdalena.samariantour.nucleo.solicitudes.infraestructura.entrada.rest.dto;

/**
 * Lo que registra el operador (RF-37 a RF-39).
 *
 * @param resultado           ATENDIDA o NO_DISPONIBLE
 * @param precioBase          obligatorio para ATENDIDA: el total en pesos, entero
 * @param politicaCancelacion obligatoria para ATENDIDA
 * @param aparteAcordado      debe ser true para ATENDIDA: el prestador aceptó apartar el cupo durante la ventana
 * @param nota                otras condiciones (ATENDIDA) o el motivo (NO_DISPONIBLE); opcional
 */
public record AtencionRequest(String resultado, Long precioBase, String politicaCancelacion, Boolean aparteAcordado,
                              String nota) {
}
