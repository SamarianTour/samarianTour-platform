package co.edu.unimagdalena.samariantour.recomendacion.recomendacion.aplicacion;

import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.excepcion.FuenteAlertasNoDisponibleException;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.excepcion.RecomendacionNoDisponibleException;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.AlertaClimatica;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.LecturaAlertas;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.ResultadoFuenteAlertas;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.ZonaCatalogo;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.puerto.entrada.ActualizarAlertasUseCase;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.puerto.salida.AlertaCachePort;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.puerto.salida.CatalogoConsultaPort;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.puerto.salida.WeatherAlertPort;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Lee la fuente de alertas para las zonas del catálogo y guarda la lectura con su hora (RF-28, DR-16).
 * <ul>
 *   <li>Si la fuente falla del todo, la caché no se toca: se queda la última lectura buena con su hora.</li>
 *   <li>Si fallan solo algunas zonas, esas conservan sus alertas anteriores con la hora en que se leyeron; las demás
 *   quedan con la hora de esta lectura.</li>
 *   <li>El reintento solo llama a la fuente mientras la caché no tenga ninguna lectura buena.</li>
 * </ul>
 */
public class ActualizarAlertasService implements ActualizarAlertasUseCase {

    private final WeatherAlertPort fuente;
    private final AlertaCachePort cache;
    private final CatalogoConsultaPort catalogo;
    private final Clock reloj;

    /**
     * @param catalogo las zonas que se consultan; si no responde, la fuente recibe la lista vacía
     */
    public ActualizarAlertasService(WeatherAlertPort fuente, AlertaCachePort cache, CatalogoConsultaPort catalogo,
                                    Clock reloj) {
        this.fuente = fuente;
        this.cache = cache;
        this.catalogo = catalogo;
        this.reloj = reloj;
    }

    @Override
    public boolean actualizar() {
        ResultadoFuenteAlertas resultado;
        try {
            resultado = fuente.consultar(zonas());
        } catch (FuenteAlertasNoDisponibleException e) {
            // El adaptador ya registró la causa; la última lectura buena sigue en la caché.
            return false;
        }
        Optional<LecturaAlertas> anterior = cache.ultima();
        List<AlertaClimatica> alertas = new ArrayList<>(resultado.alertas());
        Map<String, Instant> horasConservadas = new HashMap<>();
        anterior.ifPresent(lectura -> resultado.zonasFallidas().forEach(zonaId -> {
            lectura.alertas().stream().filter(alerta -> alerta.zonaId().equals(zonaId)).forEach(alertas::add);
            horasConservadas.put(zonaId, lectura.actualizadoEn(zonaId));
        }));
        cache.guardar(new LecturaAlertas(alertas, reloj.instant(), horasConservadas));
        return true;
    }

    @Override
    public Reintento reintentar() {
        if (cache.ultima().isPresent()) {
            return Reintento.NO_HACE_FALTA;
        }
        return actualizar() ? Reintento.ACTUALIZADA : Reintento.FALLIDA;
    }

    private List<ZonaCatalogo> zonas() {
        try {
            return catalogo.zonas();
        } catch (RecomendacionNoDisponibleException e) {
            return List.of();
        }
    }
}
