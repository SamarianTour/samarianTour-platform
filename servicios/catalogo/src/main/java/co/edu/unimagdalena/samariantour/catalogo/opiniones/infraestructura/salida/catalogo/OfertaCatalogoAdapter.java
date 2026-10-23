package co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.salida.catalogo;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.api.ConsultaOfertaApi;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.puerto.salida.OfertaCatalogoPort;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;

/**
 * Pregunta al catálogo por sus ofertas a través de {@link ConsultaOfertaApi}, sin conocer su modelo (DC-39). Si el
 * catálogo se separa en otro servicio, solo cambia este adaptador.
 */
@Component
public class OfertaCatalogoAdapter implements OfertaCatalogoPort {

    private final ConsultaOfertaApi catalogo;

    public OfertaCatalogoAdapter(ConsultaOfertaApi catalogo) {
        this.catalogo = catalogo;
    }

    @Override
    public boolean existe(String ofertaId) {
        return catalogo.existe(ofertaId);
    }

    @Override
    public boolean esVisible(String ofertaId) {
        return catalogo.esVisible(ofertaId);
    }

    @Override
    public Map<String, String> nombres(Collection<String> ofertaIds) {
        return catalogo.nombres(ofertaIds);
    }
}
