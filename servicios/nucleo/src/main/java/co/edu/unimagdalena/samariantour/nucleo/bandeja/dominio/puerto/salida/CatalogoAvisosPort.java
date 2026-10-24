package co.edu.unimagdalena.samariantour.nucleo.bandeja.dominio.puerto.salida;

import co.edu.unimagdalena.samariantour.nucleo.bandeja.dominio.modelo.Destino;
import co.edu.unimagdalena.samariantour.nucleo.bandeja.dominio.modelo.ResultadoEnvio;

/** Las entradas de avisos internos del catálogo. */
public interface CatalogoAvisosPort {

    /** Nunca lanza por un fallo de red o del catálogo: lo devuelve como {@link ResultadoEnvio.Fallido}. */
    ResultadoEnvio enviar(Destino destino, String cuerpo);
}
