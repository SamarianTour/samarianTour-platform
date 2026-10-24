package co.edu.unimagdalena.samariantour.nucleo.administracion.infraestructura.salida.bandeja;

import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.modelo.DecisionModeracion;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.modelo.TipoModerado;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.puerto.salida.AvisosModeracionPort;
import co.edu.unimagdalena.samariantour.nucleo.bandeja.api.BandejaSalidaApi;
import co.edu.unimagdalena.samariantour.nucleo.bandeja.api.DestinoAviso;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Encola la decisión en la bandeja de salida como el aviso que espera el catálogo (DN-06, DN-08):
 * {@code MODERACION} para una oferta y {@code MODERACION_OPINION} para una opinión, con el {@code idAviso} de la
 * decisión.
 */
@Component
public class AvisosModeracionBandejaAdapter implements AvisosModeracionPort {

    private final BandejaSalidaApi bandeja;

    public AvisosModeracionBandejaAdapter(BandejaSalidaApi bandeja) {
        this.bandeja = bandeja;
    }

    @Override
    public void avisar(DecisionModeracion decision) {
        Map<String, Object> aviso = new LinkedHashMap<>();
        aviso.put("idAviso", decision.idAviso());
        DestinoAviso destino;
        if (decision.tipo() == TipoModerado.OFERTA) {
            aviso.put("tipo", "MODERACION");
            aviso.put("ofertaId", decision.moderadoId());
            aviso.put("numeroVersion", decision.numeroVersion());
            destino = DestinoAviso.OFERTAS;
        } else {
            aviso.put("tipo", "MODERACION_OPINION");
            aviso.put("opinionId", decision.moderadoId());
            destino = DestinoAviso.OPINIONES;
        }
        aviso.put("decision", decision.decision().name());
        if (decision.motivo() != null) {
            aviso.put("motivo", decision.motivo());
        }
        bandeja.encolar(decision.idAviso(), destino, aviso);
    }
}
