package co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.puerto.entrada;

import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.excepcion.CatalogoNoDisponibleException;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.excepcion.ModeracionYaDecididaException;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.excepcion.NoEstaEnModeracionException;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.modelo.DecisionModeracion;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.puerto.entrada.comando.ModerarOpinionCommand;

/**
 * El administrador aprueba o rechaza una opinión en moderación (RF-80, DN-08), como las ofertas: la decisión llega al
 * catálogo por la bandeja de salida y un rechazo se notifica al turista con su motivo.
 */
public interface ModerarOpinionUseCase {

    /**
     * @return la decisión guardada; si ya se había tomado la misma, la de entonces
     * @throws ModeracionYaDecididaException si ya se tomó la contraria
     * @throws NoEstaEnModeracionException si el catálogo no la tiene en moderación
     * @throws CatalogoNoDisponibleException si el catálogo no responde
     */
    DecisionModeracion moderar(ModerarOpinionCommand comando);
}
