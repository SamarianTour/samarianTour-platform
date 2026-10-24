package co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.puerto.entrada;

import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.excepcion.CatalogoNoDisponibleException;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.excepcion.ModeracionYaDecididaException;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.excepcion.NoEstaEnModeracionException;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.excepcion.VersionNoVigenteException;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.modelo.DecisionModeracion;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.puerto.entrada.comando.ModerarOfertaCommand;

/**
 * El administrador aprueba o rechaza la versión en moderación de una oferta (RF-79, DN-08). La decisión llega al
 * catálogo como aviso de moderación por la bandeja de salida; un rechazo se notifica al prestador con su motivo.
 */
public interface ModerarOfertaUseCase {

    /**
     * @return la decisión guardada; si ya se había tomado la misma, la de entonces
     * @throws ModeracionYaDecididaException si ya se tomó la contraria
     * @throws NoEstaEnModeracionException si el catálogo no la tiene en moderación
     * @throws VersionNoVigenteException si la versión en moderación es otra
     * @throws CatalogoNoDisponibleException si el catálogo no responde
     */
    DecisionModeracion moderar(ModerarOfertaCommand comando);
}
