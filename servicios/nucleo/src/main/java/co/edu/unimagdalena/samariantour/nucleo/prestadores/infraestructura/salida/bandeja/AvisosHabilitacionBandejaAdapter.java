package co.edu.unimagdalena.samariantour.nucleo.prestadores.infraestructura.salida.bandeja;

import co.edu.unimagdalena.samariantour.nucleo.bandeja.api.BandejaSalidaApi;
import co.edu.unimagdalena.samariantour.nucleo.bandeja.api.DestinoAviso;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.puerto.salida.EventoSalidaPort;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Deja en la bandeja el aviso {@code HABILITACION} del contrato del catálogo (DC-02, DN-06): con el RNT inactivo, el
 * catálogo retira de la vista pública las ofertas del prestador (RF-77); activo, las restituye.
 */
@Component
public class AvisosHabilitacionBandejaAdapter implements EventoSalidaPort {

    private final BandejaSalidaApi bandeja;

    public AvisosHabilitacionBandejaAdapter(BandejaSalidaApi bandeja) {
        this.bandeja = bandeja;
    }

    @Override
    public void avisarHabilitacion(UUID prestadorId, boolean rntActivo, long version) {
        // Estable: la misma versión del mismo prestador da el mismo identificador.
        String idAviso = "habilitacion-%s-v%d".formatted(prestadorId, version);
        Map<String, Object> aviso = new LinkedHashMap<>();
        aviso.put("idAviso", idAviso);
        aviso.put("tipo", "HABILITACION");
        aviso.put("prestadorId", prestadorId.toString());
        aviso.put("rntActivo", rntActivo);
        aviso.put("version", version);
        bandeja.encolar(idAviso, DestinoAviso.OFERTAS, aviso);
    }
}
