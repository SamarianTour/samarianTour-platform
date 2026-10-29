package co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo;

import java.time.Instant;
import java.util.UUID;

/**
 * Persona que consulta el catálogo y envía solicitudes. Su WhatsApp es el único canal por el que se le notifica
 * (RF-03, RF-44), y solo cuenta cuando lo confirmó con el código.
 */
public final class Turista extends Usuario {

    private String whatsapp;
    private boolean whatsappVerificado;

    Turista(DatosCuenta datos, String whatsapp, boolean whatsappVerificado) {
        super(datos);
        this.whatsapp = whatsapp;
        this.whatsappVerificado = whatsapp != null && whatsappVerificado;
    }

    @Override
    public Rol rol() {
        return Rol.TURISTA;
    }

    /** Solo envía solicitudes con el WhatsApp verificado (RF-30). */
    public boolean puedeEnviarSolicitudes() {
        return estaActiva() && whatsapp != null && whatsappVerificado;
    }

    /** Confirmó su número con el código (RF-03): desde ahora puede enviar solicitudes. */
    public void verificarWhatsapp() {
        if (whatsapp == null) {
            throw new IllegalStateException("El turista %s no tiene WhatsApp que verificar".formatted(id()));
        }
        whatsappVerificado = true;
    }

    /**
     * Cambia el número (RF-12). Un número distinto queda sin verificar hasta que confirme el código nuevo (RF-03).
     *
     * @return si el número cambió
     */
    public boolean cambiarWhatsapp(String nuevo) {
        String limpio = Validaciones.whatsapp(nuevo);
        if (limpio.equals(whatsapp)) {
            return false;
        }
        whatsapp = limpio;
        whatsappVerificado = false;
        return true;
    }

    /** Suprime sus datos personales de inmediato, también el WhatsApp (RF-13, DN-21). */
    public void suprimir(UUID supresionId, Instant ahora) {
        anonimizar(supresionId, ahora);
        whatsapp = null;
        whatsappVerificado = false;
    }

    public String whatsapp() {
        return whatsapp;
    }

    public boolean whatsappVerificado() {
        return whatsappVerificado;
    }
}
