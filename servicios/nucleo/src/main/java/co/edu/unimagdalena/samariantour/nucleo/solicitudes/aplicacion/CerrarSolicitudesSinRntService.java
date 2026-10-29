package co.edu.unimagdalena.samariantour.nucleo.solicitudes.aplicacion;

import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.SolicitudDisponibilidad;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.entrada.CerrarSolicitudesSinRntUseCase;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.salida.NotificacionRepositoryPort;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.salida.PrestadoresPort;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.salida.SolicitudRepositoryPort;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.salida.TransaccionPort;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Cierra las solicitudes no pagadas de los prestadores sin RNT activo (RF-77, HU-ADM-02, DN-36). Corre con los
 * vencimientos, cada minuto: no depende de quién le quitó la habilitación al prestador, y si una réplica cae a mitad
 * la siguiente vuelta termina. Los lotes se toman con {@code FOR UPDATE SKIP LOCKED}, como los vencimientos; cada
 * cierre deja su transición y su notificación en la misma transacción.
 */
public class CerrarSolicitudesSinRntService implements CerrarSolicitudesSinRntUseCase {

    private final SolicitudRepositoryPort solicitudes;
    private final NotificacionRepositoryPort notificaciones;
    private final PrestadoresPort prestadores;
    private final TransaccionPort transaccion;
    private final Clock reloj;
    private final int tamanoLote;

    public CerrarSolicitudesSinRntService(SolicitudRepositoryPort solicitudes,
                                          NotificacionRepositoryPort notificaciones, PrestadoresPort prestadores,
                                          TransaccionPort transaccion, Clock reloj, int tamanoLote) {
        this.solicitudes = solicitudes;
        this.notificaciones = notificaciones;
        this.prestadores = prestadores;
        this.transaccion = transaccion;
        this.reloj = reloj;
        this.tamanoLote = tamanoLote;
    }

    @Override
    public int cerrar() {
        int cerradas = 0;
        for (UUID prestadorId : solicitudes.prestadoresConAbiertas()) {
            if (prestadores.estaHabilitado(prestadorId)) {
                continue;
            }
            int tomadas;
            do {
                int[] lote = transaccion.ejecutar(() -> cerrarLote(prestadorId));
                tomadas = lote[0];
                cerradas += lote[1];
            } while (tomadas == tamanoLote);
        }
        return cerradas;
    }

    /** @return cuántas tomó y cuántas cerró */
    private int[] cerrarLote(UUID prestadorId) {
        Instant ahora = reloj.instant();
        List<SolicitudDisponibilidad> abiertas = solicitudes.tomarAbiertasDelPrestador(prestadorId, tamanoLote);
        int cerradas = 0;
        for (SolicitudDisponibilidad solicitud : abiertas) {
            if (solicitud.cerrarPorRntInactivo(ahora)) {
                solicitudes.actualizar(solicitud);
                notificaciones.registrar(solicitud.extraerNotificaciones());
                cerradas++;
            }
        }
        return new int[] {abiertas.size(), cerradas};
    }
}
