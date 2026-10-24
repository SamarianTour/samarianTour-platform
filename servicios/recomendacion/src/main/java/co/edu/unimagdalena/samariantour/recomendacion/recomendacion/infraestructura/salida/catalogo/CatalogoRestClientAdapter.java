package co.edu.unimagdalena.samariantour.recomendacion.recomendacion.infraestructura.salida.catalogo;

import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.excepcion.RecomendacionNoDisponibleException;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.NivelEsfuerzo;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.OfertaCandidata;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.TipoActividad;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.TipoOferta;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

/**
 * Lee las ofertas visibles del catálogo ({@code GET /internal/ofertas}, de a 100 hasta el final) y los nombres de sus
 * zonas ({@code GET /zonas}), y los guarda en una caché en memoria (DR-07).
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
    private final LoadingCache<String, List<OfertaCandidata>> cache;

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
        try {
            return cache.get(CLAVE);
        } catch (RuntimeException e) {
            LOG.warn("catalogo no disponible y sin copia en cache: causa={}", causa(e));
            throw new RecomendacionNoDisponibleException(
                    "El catálogo no responde y no hay ofertas en caché; intenta de nuevo en unos minutos");
        }
    }

    private List<OfertaCandidata> leer() {
        long inicio = System.nanoTime();
        Map<String, String> nombresZonas = new HashMap<>();
        List<ZonaJson> zonas = catalogo.get().uri("/zonas").retrieve()
                .body(new ParameterizedTypeReference<List<ZonaJson>>() { });
        Objects.requireNonNullElse(zonas, List.<ZonaJson>of())
                .forEach(zona -> nombresZonas.put(zona.id(), zona.nombre()));

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
                OfertaCandidata oferta = convertir(json, zonaNombre(json.zonaId(), nombresZonas));
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
        LOG.info("catalogo leido: ofertas={} descartadas={} latenciaMs={}", ofertas.size(), descartadas,
                (System.nanoTime() - inicio) / 1_000_000);
        return List.copyOf(ofertas);
    }

    /**
     * {@code GET /zonas} solo trae las activas, pero las ofertas de una zona inactiva siguen visibles (DC-49): su
     * nombre se pide a {@code GET /zonas/{id}}. Si tampoco se puede, la oferta queda sin nombre de zona.
     */
    private String zonaNombre(String zonaId, Map<String, String> nombres) {
        if (zonaId == null) {
            return null;
        }
        if (!nombres.containsKey(zonaId)) {
            // Se guarda aunque no tenga nombre, para no volver a pedirla por cada oferta de esa zona.
            nombres.put(zonaId, leerZona(zonaId));
        }
        return nombres.get(zonaId);
    }

    private String leerZona(String zonaId) {
        try {
            ZonaJson zona = catalogo.get().uri("/zonas/{id}", zonaId).retrieve().body(ZonaJson.class);
            return zona == null ? null : zona.nombre();
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
                json.zonaId(), zonaNombre, json.precioReferencia(), json.unidadPrecio(), tiposActividad, nivel,
                json.accesibleSillaRuedas());
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

    private class Cargador implements CacheLoader<String, List<OfertaCandidata>> {

        @Override
        public List<OfertaCandidata> load(String clave) {
            return leer();
        }

        /** Si el catálogo falla al refrescar, la copia anterior sigue en uso hasta la próxima vigencia. */
        @Override
        public List<OfertaCandidata> reload(String clave, List<OfertaCandidata> anterior) {
            try {
                return leer();
            } catch (RestClientException e) {
                LOG.warn("catalogo no disponible: se sigue usando la copia anterior ({} ofertas); causa={}",
                        anterior.size(), causa(e));
                return anterior;
            }
        }
    }
}
