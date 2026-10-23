package co.edu.unimagdalena.samariantour.catalogo.catalogo.aplicacion;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.CriteriosBusqueda;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Oferta;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.OfertaVisible;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Pagina;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Paginacion;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.TipoOferta;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Zona;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.ConsultarOfertasVisiblesUseCase;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.salida.OfertaRepositoryPort;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.salida.ZonaRepositoryPort;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Ofertas visibles para la recomendación (RF-26, RF-29, RET-02). Reutiliza la búsqueda pública, así que la
 * visibilidad, el orden y la paginación son los mismos; a cada oferta le agrega la marca de alta afluencia de su
 * zona y, si es un paquete, sus actividades.
 */
public class ConsultarOfertasVisiblesService implements ConsultarOfertasVisiblesUseCase {

    private final OfertaRepositoryPort ofertas;
    private final ZonaRepositoryPort zonas;
    private final ActividadesDePaquetes actividades;

    public ConsultarOfertasVisiblesService(OfertaRepositoryPort ofertas, ZonaRepositoryPort zonas) {
        this.ofertas = ofertas;
        this.zonas = zonas;
        this.actividades = new ActividadesDePaquetes(ofertas);
    }

    @Override
    public Pagina<OfertaVisible> consultar(TipoOferta tipo, String zonaId, Paginacion paginacion) {
        Pagina<Oferta> pagina = ofertas.buscarVisibles(
                new CriteriosBusqueda(tipo, zonaId, null, null, null, null, false, paginacion));
        Map<String, List<Oferta>> incluidas = actividades.de(pagina.contenido());
        Map<String, Boolean> altaAfluencia = altaAfluenciaPorZona(pagina.contenido());
        return pagina.map(oferta -> new OfertaVisible(oferta,
                altaAfluencia.get(zonaDe(oferta)), incluidas.get(oferta.id())));
    }

    /** Una consulta por zona distinta de la página; una zona que ya no existe no se marca (RF-29). */
    private Map<String, Boolean> altaAfluenciaPorZona(List<Oferta> lote) {
        return lote.stream().map(ConsultarOfertasVisiblesService::zonaDe).distinct()
                .collect(Collectors.toMap(Function.identity(),
                        id -> zonas.buscarPorId(id).map(Zona::altaAfluencia).orElse(false)));
    }

    private static String zonaDe(Oferta oferta) {
        return oferta.publicada().orElseThrow().contenido().zonaId();
    }
}
