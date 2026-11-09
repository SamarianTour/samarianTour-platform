package co.edu.unimagdalena.samariantour.nucleo.bandeja.aplicacion;

import co.edu.unimagdalena.samariantour.nucleo.bandeja.dominio.excepcion.AvisoNoReenviableException;
import co.edu.unimagdalena.samariantour.nucleo.bandeja.dominio.modelo.AvisoSaliente;
import co.edu.unimagdalena.samariantour.nucleo.bandeja.dominio.modelo.EstadoAviso;
import co.edu.unimagdalena.samariantour.nucleo.bandeja.dominio.puerto.entrada.AdministrarBandejaUseCase;
import co.edu.unimagdalena.samariantour.nucleo.bandeja.dominio.puerto.entrada.LimpiarBandejaUseCase;
import co.edu.unimagdalena.samariantour.nucleo.bandeja.dominio.puerto.salida.BandejaSalidaRepositoryPort;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Los rechazados y su reenvío, y la limpieza de los enviados (DN-44). Un rechazado es un aviso que el catálogo
 * respondió con un 4xx: no se reintenta solo, porque volvería a fallar igual. El administrador lo reenvía después de
 * corregir la causa, por ejemplo una zona que no existía en el catálogo.
 */
public class AdministrarBandejaService implements AdministrarBandejaUseCase, LimpiarBandejaUseCase {

    /** Cuánto se conservan los enviados como registro [SUPUESTO]. */
    static final Duration CONSERVAR_ENVIADOS = Duration.ofDays(30);

    private final BandejaSalidaRepositoryPort bandeja;
    private final Clock reloj;

    public AdministrarBandejaService(BandejaSalidaRepositoryPort bandeja, Clock reloj) {
        this.bandeja = bandeja;
        this.reloj = reloj;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AvisoSaliente> rechazados() {
        return bandeja.rechazados();
    }

    @Override
    @Transactional
    public AvisoSaliente reenviar(String idAviso) {
        AvisoSaliente aviso = bandeja.buscarParaCambiar(idAviso)
                .orElseThrow(() -> new AvisoNoReenviableException(AvisoNoReenviableException.NO_ENCONTRADO,
                        "No existe el aviso " + idAviso));
        if (aviso.estado() != EstadoAviso.RECHAZADO) {
            throw new AvisoNoReenviableException(AvisoNoReenviableException.NO_RECHAZADO,
                    "El aviso " + idAviso + " no está rechazado: " + aviso.estado());
        }
        aviso.reenviar(reloj.instant());
        bandeja.guardar(aviso);
        return aviso;
    }

    @Override
    @Transactional
    public int limpiar() {
        Instant limite = reloj.instant().minus(CONSERVAR_ENVIADOS);
        return bandeja.borrarEnviadosAntesDe(limite);
    }
}
