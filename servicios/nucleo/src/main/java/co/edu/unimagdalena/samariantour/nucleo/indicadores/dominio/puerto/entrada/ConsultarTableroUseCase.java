package co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.puerto.entrada;

import co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.excepcion.DatoInvalidoException;
import co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.modelo.Tablero;

import java.time.LocalDate;

/** El gestor del destino consulta la demanda por zona y tipo de oferta (RF-85, RF-86). */
public interface ConsultarTableroUseCase {

    /**
     * @param desde vacío: 29 días antes de {@code hasta}
     * @param hasta vacío: hoy
     * @throws DatoInvalidoException si el periodo está al revés o pasa de 366 días
     */
    Tablero consultarTablero(LocalDate desde, LocalDate hasta);
}
