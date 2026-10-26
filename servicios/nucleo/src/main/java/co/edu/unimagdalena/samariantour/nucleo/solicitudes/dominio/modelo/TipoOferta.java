package co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo;

/** Los tipos de oferta del catálogo (glosario del SRS). Definen qué datos pide la solicitud (RF-30). */
public enum TipoOferta {
    ACTIVIDAD,
    PAQUETE,
    ALOJAMIENTO,
    RESTAURANTE;

    /** Solo la actividad y el paquete tienen horarios de salida para elegir (RF-30). */
    public boolean tieneHorarioSalida() {
        return this == ACTIVIDAD || this == PAQUETE;
    }
}
