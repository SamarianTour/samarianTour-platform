package co.edu.unimagdalena.samariantour.nucleo.pagos.infraestructura.salida.persistencia;

import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo.MotivoPagoTardio;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo.PagoTardio;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.salida.PagoTardioRepositoryPort;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Los pagos tardíos en {@code pagos_tardios} (DN-22): uno por intento. */
@Component
public class PagoTardioJpaAdapter implements PagoTardioRepositoryPort {

    private final PagoTardioJpaRepository tardios;

    PagoTardioJpaAdapter(PagoTardioJpaRepository tardios) {
        this.tardios = tardios;
    }

    @Override
    public void guardar(PagoTardio pago) {
        tardios.saveAndFlush(new PagoTardioEntity(pago.id(), pago.solicitudId(), pago.intentoId(), pago.referencia(),
                pago.transaccionId(), pago.montoCentavos(), pago.motivo().name(), pago.registradoEn(),
                pago.avisoEnviadoEn()));
    }

    @Override
    public List<PagoTardio> todos() {
        return tardios.findAllByOrderByRegistradoEnDescIdAsc().stream().map(PagoTardioJpaAdapter::aDominio).toList();
    }

    @Override
    public Optional<PagoTardio> tomarSinAviso(Instant inicio) {
        return tardios.tomarSinAviso(inicio).map(PagoTardioJpaAdapter::aDominio);
    }

    @Override
    public void marcarAvisado(PagoTardio pago) {
        tardios.findById(pago.id()).ifPresent(fila -> fila.marcarAvisado(pago.avisoEnviadoEn()));
    }

    @Override
    public void marcarAvisoIntentado(UUID id, Instant ahora) {
        tardios.marcarAvisoIntentado(id, ahora);
    }

    private static PagoTardio aDominio(PagoTardioEntity fila) {
        return new PagoTardio(fila.id(), fila.solicitudId(), fila.intentoId(), fila.referencia(),
                fila.transaccionId(), fila.montoCentavos(), MotivoPagoTardio.valueOf(fila.motivo()),
                fila.registradoEn(), fila.avisoEnviadoEn());
    }
}
