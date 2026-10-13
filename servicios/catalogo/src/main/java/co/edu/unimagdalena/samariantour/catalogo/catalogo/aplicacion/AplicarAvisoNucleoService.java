package co.edu.unimagdalena.samariantour.catalogo.catalogo.aplicacion;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Oferta;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.PrestadorCatalogo;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.AplicarAvisoNucleoUseCase;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.comando.AvisoHabilitacion;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.comando.AvisoModeracion;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.comando.AvisoNucleoCommand;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.comando.ResultadoAviso;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.salida.AvisoProcesadoPort;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.salida.OfertaRepositoryPort;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.salida.PrestadorCatalogoRepositoryPort;

import co.edu.unimagdalena.samariantour.catalogo.compartido.concurrencia.EjecutorConReintento;

import java.time.Clock;
import java.time.Instant;

/**
 * Aplica los avisos que el núcleo envía por la bandeja de salida (RF-77, RF-79). Es idempotente (DC-04): el aviso
 * se marca como procesado al final, así que si el proceso se cae a mitad, el reintento del núcleo deja todo correcto.
 * Si otra escritura se cruza, el aviso completo se vuelve a aplicar (ADR-05); si se agotan los intentos, no queda
 * marcado como procesado y el núcleo lo reenvía.
 */
public class AplicarAvisoNucleoService implements AplicarAvisoNucleoUseCase {

    private final OfertaRepositoryPort ofertas;
    private final PrestadorCatalogoRepositoryPort prestadores;
    private final AvisoProcesadoPort avisosProcesados;
    private final Clock reloj;
    private final EjecutorConReintento reintento;

    public AplicarAvisoNucleoService(OfertaRepositoryPort ofertas, PrestadorCatalogoRepositoryPort prestadores,
                                     AvisoProcesadoPort avisosProcesados, Clock reloj,
                                     EjecutorConReintento reintento) {
        this.ofertas = ofertas;
        this.prestadores = prestadores;
        this.avisosProcesados = avisosProcesados;
        this.reloj = reloj;
        this.reintento = reintento;
    }

    @Override
    public ResultadoAviso aplicar(AvisoNucleoCommand aviso) {
        return reintento.ejecutar(() -> aplicarUnaVez(aviso));
    }

    private ResultadoAviso aplicarUnaVez(AvisoNucleoCommand aviso) {
        if (avisosProcesados.fueProcesado(aviso.idAviso())) {
            return ResultadoAviso.DUPLICADO;
        }
        ResultadoAviso resultado = switch (aviso) {
            case AvisoModeracion moderacion -> aplicarModeracion(moderacion);
            case AvisoHabilitacion habilitacion -> aplicarHabilitacion(habilitacion);
        };
        avisosProcesados.marcarProcesado(aviso.idAviso());
        return resultado;
    }

    private ResultadoAviso aplicarModeracion(AvisoModeracion aviso) {
        Oferta oferta = ofertas.buscarPorId(aviso.ofertaId()).orElse(null);
        if (oferta == null) {
            return ResultadoAviso.IGNORADO;
        }
        Instant ahora = Instant.now(reloj);
        boolean cambio = switch (aviso.decision()) {
            case APROBAR -> oferta.aprobar(aviso.numeroVersion(), prestadores.habilitacionDe(oferta.prestadorId()),
                    ahora);
            case RECHAZAR -> oferta.rechazar(aviso.numeroVersion(), aviso.motivo(), ahora);
        };
        if (!cambio) {
            return ResultadoAviso.IGNORADO;
        }
        ofertas.guardar(oferta);
        return ResultadoAviso.APLICADO;
    }

    /**
     * Orden acordado (DC-03): réplica, luego copia en las ofertas, luego (en {@link #aplicar}) el registro del aviso.
     * La copia se sincroniza siempre con la réplica, aunque el aviso sea viejo: así un reintento después de una
     * caída entre los dos pasos completa la actualización de las ofertas. La réplica y las ofertas solo avanzan de
     * versión, así que dos avisos cruzados no pueden dejar el RNT viejo (ADR-05).
     */
    private ResultadoAviso aplicarHabilitacion(AvisoHabilitacion aviso) {
        PrestadorCatalogo prestador = prestadores.habilitacionDe(aviso.prestadorId());
        boolean cambio = prestador.aplicarHabilitacion(aviso.rntActivo(), aviso.version());
        if (cambio) {
            prestadores.guardar(prestador);
        }
        ofertas.actualizarRntPrestador(prestador.prestadorId(), prestador.rntActivo(), prestador.version());
        return cambio ? ResultadoAviso.APLICADO : ResultadoAviso.IGNORADO;
    }
}
