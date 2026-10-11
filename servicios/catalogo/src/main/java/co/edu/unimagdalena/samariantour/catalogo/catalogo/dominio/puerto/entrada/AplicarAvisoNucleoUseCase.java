package co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.comando.AvisoNucleoCommand;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.comando.ResultadoAviso;

/**
 * Aplica los avisos que el núcleo envía por la bandeja de salida (RF-77, RF-79). Es idempotente:
 * un aviso repetido o desactualizado no cambia nada.
 */
public interface AplicarAvisoNucleoUseCase {

    ResultadoAviso aplicar(AvisoNucleoCommand aviso);
}
