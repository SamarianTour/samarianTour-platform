package co.edu.unimagdalena.samariantour.nucleo.prestadores.aplicacion;

import co.edu.unimagdalena.samariantour.nucleo.compartido.concurrencia.ConflictoConcurrenciaException;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.modelo.ConsultaRnt;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.modelo.Prestador;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.modelo.ResultadoSincronizacion;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.puerto.entrada.SincronizarRntUseCase;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.puerto.salida.EventoSalidaPort;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.puerto.salida.NotificacionPrestadorPort;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.puerto.salida.PrestadorRepositoryPort;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.puerto.salida.RntVerificationPort;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.puerto.salida.TransaccionPort;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * La sincronización con el RNT (RF-76 a RF-78, DN-36). Toma a los prestadores en lotes, en transacciones cortas, y así
 * dos réplicas se reparten el trabajo sin repetirlo. Cada RNT se consulta fuera de una transacción, porque puede tardar
 * hasta 3 s; el estado nuevo y el aviso al catálogo se guardan juntos. Si el RNT deja de responder, se detiene y cada
 * prestador conserva su último estado conocido (QAS-03).
 */
public class SincronizarRntService implements SincronizarRntUseCase {

    /** Los prestadores que toma cada transacción [SUPUESTO]. */
    static final int LOTE = 20;
    /** Las consultas fallidas seguidas tras las que se considera caído el RNT [SUPUESTO]. */
    static final int FALLOS_SEGUIDOS = 3;

    private final PrestadorRepositoryPort prestadores;
    private final RntVerificationPort rnt;
    private final EventoSalidaPort eventos;
    private final NotificacionPrestadorPort notificacion;
    private final TransaccionPort transaccion;
    private final Clock reloj;

    public SincronizarRntService(PrestadorRepositoryPort prestadores, RntVerificationPort rnt,
                                 EventoSalidaPort eventos, NotificacionPrestadorPort notificacion,
                                 TransaccionPort transaccion, Clock reloj) {
        this.prestadores = prestadores;
        this.rnt = rnt;
        this.eventos = eventos;
        this.notificacion = notificacion;
        this.transaccion = transaccion;
        this.reloj = reloj;
    }

    @Override
    public ResultadoSincronizacion sincronizar() {
        Instant inicio = reloj.instant();
        Conteo conteo = new Conteo();
        List<UUID> lote;
        while (!(lote = transaccion.ejecutar(() -> prestadores.tomarParaSincronizar(inicio, LOTE))).isEmpty()) {
            for (UUID id : lote) {
                revisar(id, conteo);
                if (conteo.fallosSeguidos >= FALLOS_SEGUIDOS) {
                    return conteo.resultado(true);
                }
            }
        }
        return conteo.resultado(false);
    }

    private void revisar(UUID id, Conteo conteo) {
        Optional<String> numero = prestadores.porId(id).map(Prestador::numeroRnt);
        if (numero.isEmpty()) {
            return;
        }
        conteo.revisados++;
        ConsultaRnt consulta = rnt.consultar(numero.get());
        if (consulta instanceof ConsultaRnt.NoDisponible) {
            conteo.sinRespuesta++;
            conteo.fallosSeguidos++;
            return;
        }
        conteo.fallosSeguidos = 0;
        Optional<Prestador> cambiado;
        try {
            cambiado = transaccion.ejecutar(() -> aplicar(id, numero.get(), consulta));
        } catch (ConflictoConcurrenciaException e) {
            // Lo cambió otra réplica o el propio prestador mientras se consultaba; la próxima sincronización lo revisa.
            return;
        }
        cambiado.ifPresent(prestador -> {
            if (prestador.estaHabilitado()) {
                conteo.restituidos++;
                return;
            }
            conteo.retirados++;
            if (prestador.whatsappContactable() != null) {
                notificacion.avisarRetiroCatalogo(prestador.whatsappContactable(), prestador.numeroRnt());
            }
        });
    }

    /**
     * @return el prestador si cambió su habilitación; vacío si no cambió, o si cambió de número mientras se consultaba
     *         el anterior
     */
    private Optional<Prestador> aplicar(UUID id, String numero, ConsultaRnt consulta) {
        Optional<Prestador> leido = prestadores.porId(id).filter(prestador -> prestador.numeroRnt().equals(numero));
        if (leido.isEmpty()) {
            return Optional.empty();
        }
        Prestador prestador = leido.get();
        boolean cambio = prestador.sincronizar(consulta, reloj.instant());
        prestadores.actualizar(prestador);
        if (!cambio) {
            return Optional.empty();
        }
        eventos.avisarHabilitacion(id, prestador.estaHabilitado(), prestador.versionHabilitacion());
        return leido;
    }

    private static final class Conteo {
        private int revisados;
        private int retirados;
        private int restituidos;
        private int sinRespuesta;
        private int fallosSeguidos;

        ResultadoSincronizacion resultado(boolean interrumpida) {
            return new ResultadoSincronizacion(revisados, retirados, restituidos, sinRespuesta, interrumpida);
        }
    }
}
