package co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.puerto.entrada;

import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.puerto.entrada.comando.AvisoOpinionCommand;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.puerto.entrada.comando.ResultadoAviso;

/**
 * Aplica los avisos del núcleo sobre opiniones: habilitación por solicitud pagada (DC-40), decisión de moderación
 * (DC-43) y supresión de un turista (DC-44). Es idempotente: un aviso repetido o que ya no aplica no cambia nada.
 */
public interface AplicarAvisoOpinionUseCase {

    ResultadoAviso aplicar(AvisoOpinionCommand aviso);
}
