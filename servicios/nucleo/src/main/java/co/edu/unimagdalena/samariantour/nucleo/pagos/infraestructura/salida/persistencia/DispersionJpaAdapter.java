package co.edu.unimagdalena.samariantour.nucleo.pagos.infraestructura.salida.persistencia;

import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo.Dispersion;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo.EstadoDispersion;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo.Liquidacion;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.salida.DispersionRepositoryPort;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Las dispersiones en {@code dispersiones} (DN-24): una por solicitud, tomadas con SKIP LOCKED. */
@Component
public class DispersionJpaAdapter implements DispersionRepositoryPort {

    private final DispersionJpaRepository dispersiones;

    DispersionJpaAdapter(DispersionJpaRepository dispersiones) {
        this.dispersiones = dispersiones;
    }

    @Override
    public void insertar(Dispersion dispersion) {
        Liquidacion liquidacion = dispersion.liquidacion();
        DispersionEntity fila = new DispersionEntity(dispersion.id(), dispersion.solicitudId(),
                dispersion.prestadorId(), liquidacion.montoBruto(), liquidacion.porcentajeComision(),
                liquidacion.comision(), liquidacion.costoTransaccional(), liquidacion.montoNeto(),
                dispersion.creadaEn());
        copiarEstado(dispersion, fila);
        dispersiones.saveAndFlush(fila);
    }

    @Override
    public void actualizar(Dispersion dispersion) {
        DispersionEntity fila = dispersiones.findById(dispersion.id()).orElseThrow(() ->
                new IllegalStateException("La dispersión " + dispersion.id() + " no existe"));
        copiarEstado(dispersion, fila);
        dispersiones.saveAndFlush(fila);
    }

    @Override
    public Optional<Dispersion> deLaSolicitud(UUID solicitudId) {
        return dispersiones.findBySolicitudId(solicitudId).map(DispersionJpaAdapter::aDominio);
    }

    @Override
    public Optional<Dispersion> tomarPendiente(Instant ahora) {
        return dispersiones.tomarPendiente(ahora).map(DispersionJpaAdapter::aDominio);
    }

    @Override
    public Optional<Dispersion> pendientePorId(UUID id) {
        return dispersiones.pendientePorId(id).map(DispersionJpaAdapter::aDominio);
    }

    @Override
    public Optional<Dispersion> tomarFallidaSinAviso(Instant inicio) {
        return dispersiones.tomarFallidaSinAviso(inicio).map(DispersionJpaAdapter::aDominio);
    }

    @Override
    public void marcarAvisoIntentado(UUID id, Instant ahora) {
        dispersiones.marcarAvisoIntentado(id, ahora);
    }

    private static void copiarEstado(Dispersion dispersion, DispersionEntity fila) {
        fila.cambiarEstado(dispersion.estado().name(), dispersion.intentos(), dispersion.ultimaCausa(),
                dispersion.cuentaDestino(), dispersion.idPasarela(), dispersion.proximoIntento(),
                dispersion.ejecutadaEn(), dispersion.avisoEnviadoEn());
    }

    private static Dispersion aDominio(DispersionEntity fila) {
        return Dispersion.reconstituir(fila.id(), fila.solicitudId(), fila.prestadorId(),
                new Liquidacion(fila.montoBruto(), fila.porcentajeComision(), fila.comision(),
                        fila.costoTransaccional(), fila.montoNeto()),
                fila.creadaEn(), EstadoDispersion.valueOf(fila.estado()), fila.intentos(), fila.ultimaCausa(),
                fila.cuentaDestino(), fila.idPasarela(), fila.proximoIntento(), fila.ejecutadaEn(),
                fila.avisoEnviadoEn());
    }
}
