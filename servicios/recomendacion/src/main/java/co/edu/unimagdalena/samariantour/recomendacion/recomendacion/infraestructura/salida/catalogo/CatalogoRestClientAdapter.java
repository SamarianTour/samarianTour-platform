package co.edu.unimagdalena.samariantour.recomendacion.recomendacion.infraestructura.salida.catalogo;

import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.excepcion.RecomendacionNoDisponibleException;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.NivelEsfuerzo;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.OfertaCandidata;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.TipoActividad;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.TipoOferta;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.ZonaCatalogo;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.puerto.salida.CatalogoConsultaPort;
import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.github.benmanes.caffeine.cache.Ticker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

/**
 * Lee las ofertas visibles del catálogo ({@code GET /internal/ofertas}, de a 100 hasta el final) y sus zonas, con su
 * nombre y su ubicación ({@code GET /zonas}), y los guarda en una caché en memoria (DR-07).
 * <ul>
 *   <li>Pasada la vigencia (60 s), la siguiente consulta dispara una lectura nueva en segundo plano y recibe la copia
 *   anterior; las siguientes, la nueva.</li>
 *   <li>Si esa lectura falla, se sigue usando la copia anterior aunque esté vencida, y se reintenta al cumplirse otra
 *   vigencia: el catálogo caído no recibe una petición por cada consulta.</li>
 *   <li>Si no hay ninguna copia y el catálogo no responde, 503 RECOMENDACION_NO_DISPONIBLE.</li>
 * </ul>
 */
public class CatalogoRestClientAdapter implements CatalogoConsultaPort {

    static final int TAMANO_PAGINA = 100;
    /** Tope de seguridad: 20.000 ofertas. */
    private static final int PAGINAS_MAXIMAS = 200;
    private static final String CLAVE = "ofertas-visibles";
    private static final Logger LOG = LoggerFactory.getLogger(CatalogoRestClientAdapter.class);

    private final RestClient catalogo;
    private final LoadingCache<String, Instantanea> cache;

    public CatalogoRestClientAdapter(RestClient catalogo, Duration vigencia) {
        this(catalogo, vigencia, Ticker.systemTicker(), ForkJoinPool.commonPool());
    }

    /** Con el reloj y el ejecutor de la caché a la mano, para las pruebas. */
    CatalogoRestClientAdapter(RestClient catalogo, Duration vigencia, Ticker reloj, Executor ejecutor) {
        this.catalogo = catalogo;
        this.cache = Caffeine.newBuilder()
                .refreshAfterWrite(vigencia)
                .ticker(reloj)
                .executor(ejecutor)
                .build(new Cargador());
    }

    @Override
    public List<OfertaCandidata> ofertasVisibles() {
        return instantanea().ofertas();
    }

    @Override
    public List<ZonaCatalogo> zonas() {
        return instantanea().zonas();
    }

    private Instantanea instantanea() {
        try {
            return cache.get(CLAVE);
        } catch (RuntimeException e) {
            LOG.warn("catalogo no disponible y sin copia en cache: causa={}", causa(e));
            throw new RecomendacionNoDisponibleException(
                    "El catálogo no responde y no hay ofertas en caché; intenta de nuevo en unos minutos");
        }
    }

    private Instantanea leer() {
        long inicio = System.nanoTime();
        Map<String, ZonaJson> zonas = new LinkedHashMap<>();
        List<ZonaJson> activas = catalogo.get().uri("/zonas").retrieve()
                .body(new ParameterizedTypeReference<List<ZonaJson>>() { });
        Objects.requireNonNullElse(activas, List.<ZonaJson>of()).forEach(zona -> zonas.put(zona.id(), zona));

        List<OfertaCandidata> ofertas = new ArrayList<>();
        int descartadas = 0;
        for (int pagina = 0; pagina < PAGINAS_MAXIMAS; pagina++) {
            int numero = pagina;
            PaginaOfertasJson leida = catalogo.get()
                    .uri(uri -> uri.path("/internal/ofertas").queryParam("page", numero)
                            .queryParam("size", TAMANO_PAGINA).build())
                    .retrieve().body(PaginaOfertasJson.class);
            if (leida == null || leida.contenido() == null) {
                break;
            }
            for (OfertaVisibleJson json : leida.contenido()) {
                ZonaJson zona = zona(json.zonaId(), zonas);
                OfertaCandidata oferta = convertir(json, zona == null ? null : zona.nombre());
                if (oferta == null) {
                    descartadas++;
                } else {
                    ofertas.add(oferta);
                }
            }
            if (pagina + 1 >= leida.totalPaginas()) {
                break;
            }
        }
        LOG.info("catalogo leido: ofertas={} descartadas={} zonas={} latenciaMs={}", ofertas.size(), descartadas,
                zonas.size(), (System.nanoTime() - inicio) / 1_000_000);
        return new Instantanea(List.copyOf(ofertas), zonas.values().stream()
                .filter(Objects::nonNull)
                .map(zona -> new ZonaCatalogo(zona.id(), zona.nombre(),
                        zona.ubicacion() == null ? null : zona.ubicacion().latitud(),
                        zona.ubicacion() == null ? null : zona.ubicacion().longitud()))
                .toList());
    }

    /**
     * {@code GET /zonas} solo trae las activas, pero las ofertas de una zona inactiva siguen visibles (DC-49): esa zona
     * se pide a {@code GET /zonas/{id}}. Si tampoco se puede, la oferta queda sin nombre de zona.
     */
    private ZonaJson zona(String zonaId, Map<String, ZonaJson> zonas) {
        if (zonaId == null) {
            return null;
        }
        if (!zonas.containsKey(zonaId)) {
            // Se guarda aunque no exista, para no volver a pedirla por cada oferta de esa zona.
            zonas.put(zonaId, leerZona(zonaId));
        }
        return zonas.get(zonaId);
    }

    private ZonaJson leerZona(String zonaId) {
        try {
            return catalogo.get().uri("/zonas/{id}", zonaId).retrieve().body(ZonaJson.class);
        } catch (HttpClientErrorException e) {
            return null;
        }
    }

    /** null si la oferta trae un tipo o un nivel que este servicio no conoce: no se puede filtrar bien. */
    private static OfertaCandidata convertir(OfertaVisibleJson json, String zonaNombre) {
        TipoOferta tipo = valor(TipoOferta.class, json.tipo());
        NivelEsfuerzo nivel = valor(NivelEsfuerzo.class, json.nivelEsfuerzo());
        if (json.id() == null || tipo == null || json.precioReferencia() == null
                || (json.nivelEsfuerzo() != null && nivel == null)) {
            return null;
        }
        Set<TipoActividad> tiposActividad = EnumSet.noneOf(TipoActividad.class);
        Objects.requireNonNullElse(json.tiposActividad(), List.<String>of()).stream()
                .map(texto -> valor(TipoActividad.class, texto)).filter(Objects::nonNull)
                .forEach(tiposActividad::add);
        return new OfertaCandidata(json.id(), tipo, json.nombre(), json.descripcion(), json.prestadorId(),
                json.zonaId(), zonaNombre, json.zonaAltaAfluencia(), json.precioReferencia(), json.unidadPrecio(),
                tiposActividad, nivel, json.accesibleSillaRuedas());
    }

    private static <E extends Enum<E>> E valor(Class<E> tipo, String texto) {
        if (texto == null) {
            return null;
        }
        try {
            return Enum.valueOf(tipo, texto);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static String causa(Throwable error) {
        Throwable raiz = error;
        while (raiz.getCause() != null && raiz.getCause() != raiz) {
            raiz = raiz.getCause();
        }
        return raiz.getClass().getSimpleName();
    }

    /** Lo que se guarda en la caché: una lectura completa del catálogo. */
    private record Instantanea(List<OfertaCandidata> ofertas, List<ZonaCatalogo> zonas) {
    }

    private class Cargador implements CacheLoader<String, Instantanea> {

        @Override
        public Instantanea load(String clave) {
            return leer();
        }

        /** Si el catálogo falla al refrescar, la copia anterior sigue en uso hasta la próxima vigencia. */
        @Override
        public Instantanea reload(String clave, Instantanea anterior) {
            try {
                return leer();
            } catch (RestClientException e) {
                LOG.warn("catalogo no disponible: se sigue usando la copia anterior ({} ofertas); causa={}",
                        anterior.ofertas().size(), causa(e));
                return anterior;
            }
        }
    }
}
