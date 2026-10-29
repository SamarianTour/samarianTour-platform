package co.edu.unimagdalena.samariantour.nucleo.prestadores.infraestructura.entrada.rest.dto;

/** El código de 6 dígitos que llegó por WhatsApp (RF-71). */
public record CodigoWhatsappRequest(String codigo) {

    @Override
    public String toString() {
        return "CodigoWhatsappRequest[codigo=******]";
    }
}
