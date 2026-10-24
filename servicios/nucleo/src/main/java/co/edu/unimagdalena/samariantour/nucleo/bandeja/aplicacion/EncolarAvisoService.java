package co.edu.unimagdalena.samariantour.nucleo.bandeja.aplicacion;

import co.edu.unimagdalena.samariantour.nucleo.bandeja.dominio.modelo.AvisoSaliente;
import co.edu.unimagdalena.samariantour.nucleo.bandeja.dominio.modelo.Destino;
import co.edu.unimagdalena.samariantour.nucleo.bandeja.dominio.puerto.entrada.EncolarAvisoUseCase;
import co.edu.unimagdalena.samariantour.nucleo.bandeja.dominio.puerto.salida.BandejaSalidaRepositoryPort;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/**
 * Encola un aviso en la transacción de quien lo origina (DN-06). {@code MANDATORY} hace que falle de inmediato si no
 * hay una: un aviso fuera de la transacción del cambio podría quedar aunque el cambio se deshaga.
 */
public class EncolarAvisoService implements EncolarAvisoUseCase {

    private final BandejaSalidaRepositoryPort bandeja;
    private final Clock reloj;

    public EncolarAvisoService(BandejaSalidaRepositoryPort bandeja, Clock reloj) {
        this.bandeja = bandeja;
        this.reloj = reloj;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean encolar(String idAviso, Destino destino, String cuerpo) {
        return bandeja.agregar(AvisoSaliente.nuevo(idAviso, destino, cuerpo, reloj.instant()));
    }
}
