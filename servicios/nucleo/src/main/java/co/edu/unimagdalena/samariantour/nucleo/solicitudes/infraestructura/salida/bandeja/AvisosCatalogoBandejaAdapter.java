package co.edu.unimagdalena.samariantour.nucleo.solicitudes.infraestructura.salida.bandeja;

import co.edu.unimagdalena.samariantour.nucleo.bandeja.api.BandejaSalidaApi;
import co.edu.unimagdalena.samariantour.nucleo.bandeja.api.DestinoAviso;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.SolicitudDisponibilidad;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.salida.AvisosCatalogoPort;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Encola en la bandeja de salida el aviso {@code HABILITACION_OPINION {idAviso, turistaId, ofertaId}} que espera el
 * catálogo en {@code POST /internal/opiniones/avisos} (DC-41, DN-27). El {@code idAviso} sale de la solicitud, así que
 * el mismo pago nunca lo encola dos veces.
 */
@Component
public class AvisosCatalogoBandejaAdapter implements AvisosCatalogoPort {

    static final String PREFIJO = "habilitacion-opinion-";

    private final BandejaSalidaApi bandeja;

    public AvisosCatalogoBandejaAdapter(BandejaSalidaApi bandeja) {
        this.bandeja = bandeja;
    }

    @Override
    public void habilitarOpinion(SolicitudDisponibilidad pagada) {
        String idAviso = PREFIJO + pagada.id();
        Map<String, Object> aviso = new LinkedHashMap<>();
        aviso.put("idAviso", idAviso);
        aviso.put("tipo", "HABILITACION_OPINION");
        aviso.put("turistaId", pagada.turistaId().toString());
        aviso.put("ofertaId", pagada.ofertaId());
        bandeja.encolar(idAviso, DestinoAviso.OPINIONES, aviso);
    }
}
