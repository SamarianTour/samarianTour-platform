package co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo;

/** Lo que responde el turista a las condiciones de una solicitud atendida (RF-43). */
public enum DecisionTurista {
    /** Acepta: avanza al pago. */
    CONFIRMAR,
    /** Rechaza: la solicitud se cierra como Declinada y el cupo se libera (RF-56). */
    DECLINAR
}
