package co.edu.unimagdalena.samariantour.nucleo.cuentas.infraestructura.entrada.rest.dto;

/** El código de 6 dígitos que llegó por WhatsApp (RF-03). */
public record VerificacionWhatsappRequest(String codigo) {

    @Override
    public String toString() {
        return "VerificacionWhatsappRequest[codigo=******]";
    }
}
