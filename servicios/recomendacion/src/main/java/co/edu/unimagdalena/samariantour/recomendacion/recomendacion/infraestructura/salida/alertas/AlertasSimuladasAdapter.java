package co.edu.unimagdalena.samariantour.recomendacion.recomendacion.infraestructura.salida.alertas;

import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.excepcion.FuenteAlertasNoDisponibleException;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.AlertaClimatica;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.NivelAlerta;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.ResultadoFuenteAlertas;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.TextoTraducido;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.TipoAlerta;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.ZonaCatalogo;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.puerto.salida.WeatherAlertPort;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Fuente simulada de alertas para la demostración (DR-17): lee {@code alertas/alertas-simuladas.json}. Las fechas van
 * relativas ("emitida hace 2 horas, vigente por 1 día") y se calculan con el reloj en cada lectura, para que la demo
 * nunca quede vencida. La fuente de cada alerta dice "(simulado)": un dato inventado no se presenta como oficial.
 * Ya trae la zona de cada alerta, así que no usa las zonas del catálogo. El título y la descripción vienen en español e
 * inglés (DR-21); si a alguno le falta un idioma, el archivo es ilegible.
 */
public class AlertasSimuladasAdapter implements WeatherAlertPort {

    private final Resource archivo;
    private final ObjectMapper json;
    private final Clock reloj;

    public AlertasSimuladasAdapter(Resource archivo, ObjectMapper json, Clock reloj) {
        this.archivo = archivo;
        this.json = json;
        this.reloj = reloj;
    }

    @Override
    public ResultadoFuenteAlertas consultar(List<ZonaCatalogo> zonas) {
        Instant ahora = reloj.instant();
        try (InputStream entrada = archivo.getInputStream()) {
            List<AlertaSimulada> simuladas = json.readValue(entrada, new TypeReference<>() { });
            return ResultadoFuenteAlertas.completo(
                    simuladas.stream().map(simulada -> simulada.aAlerta(ahora)).toList());
        } catch (IOException | RuntimeException e) {
            throw new FuenteAlertasNoDisponibleException("SIMULADA_ILEGIBLE (" + e.getClass().getSimpleName() + ")");
        }
    }

    /**
     * Una alerta del archivo.
     *
     * @param emitidaHace duración ISO-8601 antes de ahora, por ejemplo PT2H
     * @param titulo      {@code {"es": ..., "en": ...}}
     * @param descripcion igual que el título; opcional
     * @param vigentePor  duración ISO-8601 desde la emisión; sin ella, la alerta no tiene fin anunciado
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record AlertaSimulada(String id, String zonaId, TipoAlerta tipo, NivelAlerta nivel, Texto titulo,
                          Texto descripcion, String fuente, Duration emitidaHace, Duration vigentePor) {

        AlertaClimatica aAlerta(Instant ahora) {
            Instant emitida = ahora.minus(emitidaHace == null ? Duration.ZERO : emitidaHace);
            return new AlertaClimatica(id, zonaId, tipo, nivel, titulo.aDominio(),
                    descripcion == null ? null : descripcion.aDominio(), fuente, emitida,
                    vigentePor == null ? null : emitida.plus(vigentePor));
        }
    }

    record Texto(String es, String en) {

        TextoTraducido aDominio() {
            return new TextoTraducido(es, en);
        }
    }
}
