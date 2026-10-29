package co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.puerto.salida;

import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.modelo.ConsultaRnt;

/**
 * La fuente del RNT (RF-62, RF-63, RF-70, RF-76): la API SODA de datos.gov.co o una tabla simulada para la demo,
 * según {@code nucleo.rnt.fuente} (DN-33). Espera como máximo 3 s (RNF-05).
 */
public interface RntVerificationPort {

    /** Nunca lanza por la fuente: si no responde, devuelve {@link ConsultaRnt.NoDisponible} (QAS-03). */
    ConsultaRnt consultar(String numeroRnt);
}
