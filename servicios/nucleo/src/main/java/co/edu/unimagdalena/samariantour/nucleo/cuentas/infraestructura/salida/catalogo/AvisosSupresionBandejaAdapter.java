package co.edu.unimagdalena.samariantour.nucleo.cuentas.infraestructura.salida.catalogo;

import co.edu.unimagdalena.samariantour.nucleo.bandeja.api.BandejaSalidaApi;
import co.edu.unimagdalena.samariantour.nucleo.bandeja.api.DestinoAviso;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.salida.AvisosCatalogoPort;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Deja en la bandeja de salida los avisos de las cuentas para el catálogo (DN-06), con la forma del contrato del
 * catálogo ({@code POST /internal/opiniones/avisos}).
 */
@Component
public class AvisosSupresionBandejaAdapter implements AvisosCatalogoPort {

    private final BandejaSalidaApi bandeja;

    public AvisosSupresionBandejaAdapter(BandejaSalidaApi bandeja) {
        this.bandeja = bandeja;
    }

    @Override
    public void avisarSupresion(UUID turistaId) {
        // Estable: un turista se suprime una sola vez, y reenviar el mismo cambio da el mismo identificador.
        String idAviso = "supresion-" + turistaId;
        Map<String, Object> aviso = new LinkedHashMap<>();
        aviso.put("idAviso", idAviso);
        aviso.put("tipo", "SUPRESION_TURISTA");
        aviso.put("turistaId", turistaId.toString());
        bandeja.encolar(idAviso, DestinoAviso.OPINIONES, aviso);
    }
}
