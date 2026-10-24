package co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo;

/**
 * Tipo de actividad, con los mismos valores que el contrato del catálogo. Son provisionales: un valor que el catálogo
 * agregue y este servicio no conozca se ignora al leer las ofertas (DR-07).
 */
public enum TipoActividad {
    SENDERISMO,
    BUCEO_SNORKEL,
    DEPORTES_ACUATICOS,
    NAUTICO,
    AVISTAMIENTO_FAUNA,
    CULTURAL_HISTORICO,
    ETNOTURISMO,
    GASTRONOMICO,
    CICLISMO,
    AVENTURA,
    PLAYA_RELAX,
    OTRO
}
