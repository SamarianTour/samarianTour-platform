package co.edu.unimagdalena.samariantour.recomendacion.recomendacion.aplicacion;

import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.excepcion.AlertasNoDisponiblesException;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.excepcion.RecomendacionNoDisponibleException;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.AlertaClimatica;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.AlertasDeZona;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.LecturaAlertas;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.ResumenAlertas;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.ZonaCatalogo;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.puerto.entrada.ConsultarAlertasZonaUseCase;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.puerto.salida.AlertaCachePort;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.puerto.salida.CatalogoConsultaPort;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Alertas activas del destino (RF-28). Lee solo la caché: la petición del turista nunca llama a la fuente, así que
 * una fuente lenta o caída no la demora (DR-16). Sin ninguna lectura, 503 ALERTAS_NO_DISPONIBLES.
 */
public class ConsultarAlertasService implements ConsultarAlertasZonaUseCase {

    private final AlertaCachePort cache;
    private final CatalogoConsultaPort catalogo;
    private final Clock reloj;

    /**
     * @param catalogo solo para el nombre de las zonas en {@link #todas()}; si no responde, van sin nombre
     */
    public ConsultarAlertasService(AlertaCachePort cache, CatalogoConsultaPort catalogo, Clock reloj) {
        this.cache = cache;
        this.catalogo = catalogo;
        this.reloj = reloj;
    }

    @Override
    public AlertasDeZona deZona(String zonaId) {
        LecturaAlertas lectura = ultimaLectura();
        return new AlertasDeZona(zonaId, lectura.actualizadoEn(zonaId), activas(lectura).stream()
                .filter(alerta -> alerta.zonaId().equals(zonaId))
                .toList());
    }

    @Override
    public ResumenAlertas todas() {
        LecturaAlertas lectura = ultimaLectura();
        Map<String, List<AlertaClimatica>> porZona = new TreeMap<>();
        for (AlertaClimatica alerta : activas(lectura)) {
            porZona.computeIfAbsent(alerta.zonaId(), id -> new ArrayList<>()).add(alerta);
        }
        Map<String, String> nombres = porZona.isEmpty() ? Map.of() : nombresDeZonas();
        return new ResumenAlertas(lectura.actualizadoEn(), porZona.entrySet().stream()
                .map(zona -> new ResumenAlertas.Zona(zona.getKey(), nombres.get(zona.getKey()), zona.getValue()))
                .toList());
    }

    private LecturaAlertas ultimaLectura() {
        return cache.ultima().orElseThrow(AlertasNoDisponiblesException::new);
    }

    /** Las activas ahora, en el orden para mostrar. */
    private List<AlertaClimatica> activas(LecturaAlertas lectura) {
        Instant ahora = reloj.instant();
        return lectura.alertas().stream()
                .filter(alerta -> alerta.activa(ahora))
                .sorted(AlertaClimatica.ORDEN_PARA_MOSTRAR)
                .toList();
    }

    /** Los nombres que trae la caché del catálogo; sin catálogo, ninguno: las alertas no dependen de él. */
    private Map<String, String> nombresDeZonas() {
        Map<String, String> nombres = new HashMap<>();
        try {
            for (ZonaCatalogo zona : catalogo.zonas()) {
                if (zona.nombre() != null) {
                    nombres.put(zona.id(), zona.nombre());
                }
            }
        } catch (RecomendacionNoDisponibleException e) {
            return Map.of();
        }
        return nombres;
    }
}
