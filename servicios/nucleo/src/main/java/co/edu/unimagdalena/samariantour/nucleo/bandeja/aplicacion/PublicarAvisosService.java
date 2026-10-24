package co.edu.unimagdalena.samariantour.nucleo.bandeja.aplicacion;

import co.edu.unimagdalena.samariantour.nucleo.bandeja.dominio.modelo.AvisoSaliente;
import co.edu.unimagdalena.samariantour.nucleo.bandeja.dominio.puerto.entrada.PublicarAvisosUseCase;
import co.edu.unimagdalena.samariantour.nucleo.bandeja.dominio.puerto.salida.BandejaSalidaRepositoryPort;
import co.edu.unimagdalena.samariantour.nucleo.bandeja.dominio.puerto.salida.CatalogoAvisosPort;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;

/**
 * Envía un lote de avisos pendientes y registra lo que respondió el catálogo (DN-06). El lote se toma y se actualiza
 * en una sola transacción: mientras dura, la otra réplica salta esos avisos.
 */
public class PublicarAvisosService implements PublicarAvisosUseCase {

    /** Pocos avisos por lote, para que la transacción no retenga los avisos mucho tiempo: cada envío espera 3 s. */
    public static final int TAMANO_LOTE = 10;

    private final BandejaSalidaRepositoryPort bandeja;
    private final CatalogoAvisosPort catalogo;
    private final Clock reloj;

    public PublicarAvisosService(BandejaSalidaRepositoryPort bandeja, CatalogoAvisosPort catalogo, Clock reloj) {
        this.bandeja = bandeja;
        this.catalogo = catalogo;
        this.reloj = reloj;
    }

    @Override
    @Transactional
    public int publicarLote() {
        List<AvisoSaliente> lote = bandeja.tomarPendientes(reloj.instant(), TAMANO_LOTE);
        for (AvisoSaliente aviso : lote) {
            aviso.registrar(catalogo.enviar(aviso.destino(), aviso.cuerpo()), reloj.instant());
            bandeja.guardar(aviso);
        }
        return lote.size();
    }
}
