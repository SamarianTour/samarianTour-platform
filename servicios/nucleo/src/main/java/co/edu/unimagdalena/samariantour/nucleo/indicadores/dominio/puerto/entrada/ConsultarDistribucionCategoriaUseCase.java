package co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.puerto.entrada;

import co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.excepcion.DatoInvalidoException;
import co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.modelo.DistribucionCategoria;

import java.time.LocalDate;

/** El gestor del destino consulta cómo se reparte la demanda entre las categorías de prestador (RF-89). */
public interface ConsultarDistribucionCategoriaUseCase {

    /** @throws DatoInvalidoException si el periodo está al revés o pasa de 366 días */
    DistribucionCategoria consultarDistribucion(LocalDate desde, LocalDate hasta);
}
