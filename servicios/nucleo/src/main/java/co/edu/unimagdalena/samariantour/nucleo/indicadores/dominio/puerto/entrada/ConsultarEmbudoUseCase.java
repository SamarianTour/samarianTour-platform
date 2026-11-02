package co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.puerto.entrada;

import co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.excepcion.DatoInvalidoException;
import co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.modelo.EmbudoConversion;

import java.time.LocalDate;

/** El gestor del destino consulta el embudo de conversión de las solicitudes (RF-87, RF-88). */
public interface ConsultarEmbudoUseCase {

    /** @throws DatoInvalidoException si el periodo está al revés o pasa de 366 días */
    EmbudoConversion consultarEmbudo(LocalDate desde, LocalDate hasta);
}
