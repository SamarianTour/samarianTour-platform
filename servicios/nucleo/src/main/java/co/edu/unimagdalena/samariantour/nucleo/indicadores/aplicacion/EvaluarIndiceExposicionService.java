package co.edu.unimagdalena.samariantour.nucleo.indicadores.aplicacion;

import co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.excepcion.CatalogoNoDisponibleException;
import co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.modelo.AlertaSesgo;
import co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.modelo.MedicionExposicion;
import co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.modelo.OfertasElegibles;
import co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.modelo.Periodo;
import co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.modelo.RevisionSesgo;
import co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.modelo.RevisionSesgo.Resultado;
import co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.politica.IndiceExposicion;
import co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.puerto.entrada.EvaluarIndiceExposicionUseCase;
import co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.puerto.salida.AlertaSesgoRepositoryPort;
import co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.puerto.salida.ExposicionRepositoryPort;
import co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.puerto.salida.OfertaElegibleConsultaPort;
import co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.puerto.salida.TransaccionPort;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * La revisión diaria del sesgo (RET-02, DN-43). Pide las ofertas elegibles al catálogo antes de abrir la transacción;
 * dentro, reserva el día, borra las exposiciones de más de 90 días, mide a cada prestador y abre, actualiza o cierra
 * sus alertas. Si el catálogo no responde, no reserva el día y se intenta a la hora siguiente.
 */
public class EvaluarIndiceExposicionService implements EvaluarIndiceExposicionUseCase {

    /** Cuánto se guarda la exposición [SUPUESTO]: el índice solo mira 30 días. */
    public static final Duration RETENCION = Duration.ofDays(90);

    private final ExposicionRepositoryPort exposiciones;
    private final AlertaSesgoRepositoryPort alertas;
    private final OfertaElegibleConsultaPort catalogo;
    private final TransaccionPort transaccion;
    private final Clock reloj;

    public EvaluarIndiceExposicionService(ExposicionRepositoryPort exposiciones, AlertaSesgoRepositoryPort alertas,
                                          OfertaElegibleConsultaPort catalogo, TransaccionPort transaccion,
                                          Clock reloj) {
        this.exposiciones = exposiciones;
        this.alertas = alertas;
        this.catalogo = catalogo;
        this.transaccion = transaccion;
        this.reloj = reloj;
    }

    @Override
    public RevisionSesgo evaluar() {
        Instant ahora = reloj.instant();
        LocalDate dia = LocalDate.ofInstant(ahora, Periodo.ZONA);
        if (alertas.revisado(dia)) {
            return RevisionSesgo.sinMedir(dia, Resultado.YA_REVISADA);
        }
        List<OfertasElegibles> elegibles;
        try {
            elegibles = catalogo.porPrestador();
        } catch (CatalogoNoDisponibleException e) {
            return RevisionSesgo.sinMedir(dia, Resultado.CATALOGO_NO_DISPONIBLE);
        }
        return transaccion.ejecutar(() -> revisar(dia, ahora, elegibles));
    }

    private RevisionSesgo revisar(LocalDate dia, Instant ahora, List<OfertasElegibles> elegibles) {
        if (!alertas.reservarRevision(dia, ahora)) {
            return RevisionSesgo.sinMedir(dia, Resultado.YA_REVISADA);
        }
        exposiciones.borrarAnteriores(ahora.minus(RETENCION));
        Instant desde = ahora.minus(IndiceExposicion.VENTANA);
        long recomendaciones = exposiciones.recomendacionesDesde(desde);
        if (!IndiceExposicion.hayDatosSuficientes(recomendaciones)) {
            return RevisionSesgo.sinMedir(dia, Resultado.POCAS_RECOMENDACIONES);
        }
        Map<UUID, AlertaSesgo> abiertas = alertas.abiertas().stream()
                .collect(Collectors.toMap(AlertaSesgo::prestadorId, Function.identity()));
        int nuevas = 0;
        int actualizadas = 0;
        for (MedicionExposicion medicion : IndiceExposicion.medir(exposiciones.aparicionesDesde(desde), elegibles)) {
            if (!IndiceExposicion.superaUmbral(medicion)) {
                continue;
            }
            AlertaSesgo abierta = abiertas.remove(medicion.prestadorId());
            if (abierta == null) {
                alertas.guardar(AlertaSesgo.abrir(UUID.randomUUID(), medicion, recomendaciones, ahora));
                nuevas++;
            } else {
                alertas.guardar(abierta.actualizada(medicion, recomendaciones, ahora));
                actualizadas++;
            }
        }
        // Las que quedan ya no superan el umbral, o su prestador no tiene ofertas elegibles.
        abiertas.values().forEach(alerta -> alertas.guardar(alerta.cerrada(ahora)));
        return new RevisionSesgo(dia, Resultado.EVALUADA, nuevas, actualizadas, abiertas.size());
    }
}
