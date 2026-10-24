package co.edu.unimagdalena.samariantour.nucleo.bandeja.infraestructura.salida.persistencia;

import co.edu.unimagdalena.samariantour.nucleo.bandeja.dominio.modelo.AvisoSaliente;
import co.edu.unimagdalena.samariantour.nucleo.bandeja.dominio.modelo.Destino;
import co.edu.unimagdalena.samariantour.nucleo.bandeja.dominio.modelo.EstadoAviso;
import co.edu.unimagdalena.samariantour.nucleo.bandeja.dominio.puerto.salida.BandejaSalidaRepositoryPort;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** La bandeja de salida en la tabla {@code bandeja_salida} de PostgreSQL (DN-06). */
@Component
public class BandejaSalidaJpaAdapter implements BandejaSalidaRepositoryPort {

    private final AvisoSalienteJpaRepository repositorio;

    BandejaSalidaJpaAdapter(AvisoSalienteJpaRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public boolean agregar(AvisoSaliente aviso) {
        return repositorio.insertarSiNoExiste(aviso.idAviso(), aviso.destino().name(), aviso.cuerpo(),
                aviso.creadoEn()) == 1;
    }

    @Override
    public List<AvisoSaliente> tomarPendientes(Instant ahora, int limite) {
        return repositorio.tomarPendientes(ahora, limite).stream().map(BandejaSalidaJpaAdapter::aDominio).toList();
    }

    @Override
    public void guardar(AvisoSaliente aviso) {
        repositorio.save(new AvisoSalienteEntity(aviso.idAviso(), aviso.destino().name(), aviso.cuerpo(),
                aviso.estado().name(), aviso.creadoEn(), aviso.proximoIntento(), aviso.intentos(), aviso.enviadoEn(),
                aviso.ultimoError()));
    }

    @Override
    public Optional<AvisoSaliente> buscar(String idAviso) {
        return repositorio.findById(idAviso).map(BandejaSalidaJpaAdapter::aDominio);
    }

    private static AvisoSaliente aDominio(AvisoSalienteEntity fila) {
        return AvisoSaliente.reconstituir(fila.idAviso(), Destino.valueOf(fila.destino()), fila.cuerpo(),
                fila.creadoEn(), EstadoAviso.valueOf(fila.estado()), fila.proximoIntento(), fila.intentos(),
                fila.enviadoEn(), fila.ultimoError());
    }
}
