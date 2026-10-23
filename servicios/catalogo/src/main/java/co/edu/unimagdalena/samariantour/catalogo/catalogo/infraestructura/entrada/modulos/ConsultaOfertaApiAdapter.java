package co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.modulos;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.api.ConsultaOfertaApi;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.ConsultarOfertaParaModulosUseCase;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;

/**
 * Implementa {@link ConsultaOfertaApi} con el caso de uso del catálogo. Los otros módulos dependen solo de la
 * interfaz; este adaptador queda dentro del catálogo (DC-39).
 */
@Component
public class ConsultaOfertaApiAdapter implements ConsultaOfertaApi {

    private final ConsultarOfertaParaModulosUseCase consulta;

    public ConsultaOfertaApiAdapter(ConsultarOfertaParaModulosUseCase consulta) {
        this.consulta = consulta;
    }

    @Override
    public boolean existe(String ofertaId) {
        return consulta.existe(ofertaId);
    }

    @Override
    public boolean esVisible(String ofertaId) {
        return consulta.esVisible(ofertaId);
    }

    @Override
    public Map<String, String> nombres(Collection<String> ofertaIds) {
        return consulta.nombres(ofertaIds);
    }
}
