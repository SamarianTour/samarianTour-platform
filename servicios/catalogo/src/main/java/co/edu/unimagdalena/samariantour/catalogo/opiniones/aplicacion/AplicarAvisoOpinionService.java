package co.edu.unimagdalena.samariantour.catalogo.opiniones.aplicacion;

import co.edu.unimagdalena.samariantour.catalogo.compartido.concurrencia.EjecutorConReintento;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.modelo.HabilitacionOpinion;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.modelo.Opinion;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.puerto.entrada.AplicarAvisoOpinionUseCase;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.puerto.entrada.comando.AvisoHabilitacionOpinion;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.puerto.entrada.comando.AvisoModeracionOpinion;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.puerto.entrada.comando.AvisoOpinionCommand;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.puerto.entrada.comando.AvisoSupresionTurista;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.puerto.entrada.comando.ResultadoAviso;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.puerto.salida.AvisoOpinionProcesadoPort;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.puerto.salida.HabilitacionOpinionRepositoryPort;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.puerto.salida.OfertaCatalogoPort;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.puerto.salida.OpinionRepositoryPort;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Aplica los avisos del núcleo sobre opiniones, que llegan por la misma bandeja de salida que los de ofertas (DC-04):
 * la habilitación cuando una solicitud queda pagada (DC-40), la decisión del administrador (DC-43) y la supresión de
 * un turista (DC-44).
 *
 * <p>Es idempotente como el aviso de ofertas: el aviso se marca como procesado al final, así que si el proceso se cae
 * a mitad, el reintento del núcleo deja todo correcto. Si otra escritura se cruza con la moderación, el aviso completo
 * se vuelve a aplicar (ADR-05); si se agotan los intentos, no queda marcado y el núcleo lo reenvía.
 */
public class AplicarAvisoOpinionService implements AplicarAvisoOpinionUseCase {

    private final OpinionRepositoryPort opiniones;
    private final HabilitacionOpinionRepositoryPort habilitaciones;
    private final OfertaCatalogoPort ofertas;
    private final AvisoOpinionProcesadoPort avisosProcesados;
    private final Clock reloj;
    private final EjecutorConReintento reintento;

    public AplicarAvisoOpinionService(OpinionRepositoryPort opiniones,
                                      HabilitacionOpinionRepositoryPort habilitaciones, OfertaCatalogoPort ofertas,
                                      AvisoOpinionProcesadoPort avisosProcesados, Clock reloj,
                                      EjecutorConReintento reintento) {
        this.opiniones = opiniones;
        this.habilitaciones = habilitaciones;
        this.ofertas = ofertas;
        this.avisosProcesados = avisosProcesados;
        this.reloj = reloj;
        this.reintento = reintento;
    }

    @Override
    public ResultadoAviso aplicar(AvisoOpinionCommand aviso) {
        return reintento.ejecutar(() -> aplicarUnaVez(aviso));
    }

    private ResultadoAviso aplicarUnaVez(AvisoOpinionCommand aviso) {
        if (avisosProcesados.fueProcesado(aviso.idAviso())) {
            return ResultadoAviso.DUPLICADO;
        }
        ResultadoAviso resultado = switch (aviso) {
            case AvisoHabilitacionOpinion habilitacion -> habilitar(habilitacion);
            case AvisoModeracionOpinion moderacion -> moderar(moderacion);
            case AvisoSupresionTurista supresion -> suprimir(supresion);
        };
        avisosProcesados.marcarProcesado(aviso.idAviso());
        return resultado;
    }

    /** Una oferta que el catálogo no tiene no se puede calificar: el aviso se ignora. */
    private ResultadoAviso habilitar(AvisoHabilitacionOpinion aviso) {
        if (!ofertas.existe(aviso.ofertaId())) {
            return ResultadoAviso.IGNORADO;
        }
        boolean nueva = habilitaciones.registrar(new HabilitacionOpinion(aviso.turistaId(), aviso.ofertaId()));
        return nueva ? ResultadoAviso.APLICADO : ResultadoAviso.IGNORADO;
    }

    /** Una opinión que ya no existe (se suprimió) o que ya se moderó ignora el aviso (DC-08). */
    private ResultadoAviso moderar(AvisoModeracionOpinion aviso) {
        Opinion opinion = opiniones.buscarPorId(aviso.opinionId()).orElse(null);
        if (opinion == null) {
            return ResultadoAviso.IGNORADO;
        }
        Instant ahora = Instant.now(reloj).truncatedTo(ChronoUnit.MILLIS);
        boolean cambio = switch (aviso.decision()) {
            case APROBAR -> opinion.aprobar(ahora);
            case RECHAZAR -> opinion.rechazar(aviso.motivo(), ahora);
        };
        if (!cambio) {
            return ResultadoAviso.IGNORADO;
        }
        opiniones.guardar(opinion);
        return ResultadoAviso.APLICADO;
    }

    /**
     * Borra las opiniones y las habilitaciones del turista (Ley 1581, DC-44). Repetirla no cambia nada: el segundo
     * aviso no encuentra qué borrar y se ignora.
     */
    private ResultadoAviso suprimir(AvisoSupresionTurista aviso) {
        long borradas = opiniones.eliminarDelTurista(aviso.turistaId())
                + habilitaciones.eliminarDelTurista(aviso.turistaId());
        return borradas > 0 ? ResultadoAviso.APLICADO : ResultadoAviso.IGNORADO;
    }
}
