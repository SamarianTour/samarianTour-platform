package co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.salida;

import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.SolicitudDisponibilidad;

/**
 * Los avisos de las solicitudes al catálogo, por la bandeja de salida (DN-06), en la transacción del cambio.
 */
public interface AvisosCatalogoPort {

    /**
     * La solicitud quedó Pagada: el turista ya puede opinar de la oferta (DC-41, RF-60). El aviso es uno por
     * solicitud, así que repetirlo no lo duplica.
     */
    void habilitarOpinion(SolicitudDisponibilidad pagada);
}
