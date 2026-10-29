package co.edu.unimagdalena.samariantour.nucleo.cuentas.infraestructura.salida.persistencia;

import co.edu.unimagdalena.samariantour.nucleo.compartido.concurrencia.ConflictoConcurrenciaException;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.CodigoUnUso;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.PropositoCodigo;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.salida.CodigoRepositoryPort;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Los códigos de un solo uso en {@code codigos_un_uso} (V8, DN-02). */
@Component
public class CodigoJpaAdapter implements CodigoRepositoryPort {

    private final CodigoUnUsoJpaRepository codigos;

    CodigoJpaAdapter(CodigoUnUsoJpaRepository codigos) {
        this.codigos = codigos;
    }

    @Override
    public void insertar(CodigoUnUso codigo) {
        CodigoUnUsoEntity fila = new CodigoUnUsoEntity(codigo.id(), codigo.usuarioId(), codigo.proposito().name(),
                codigo.hashCodigo(), codigo.emitidoEn(), codigo.expiraEn());
        fila.copiar(codigo.usadoEn(), codigo.intentosFallidos());
        codigos.saveAndFlush(fila);
    }

    @Override
    public void actualizar(CodigoUnUso codigo) {
        CodigoUnUsoEntity fila = codigos.findById(codigo.id())
                .orElseThrow(() -> new ConflictoConcurrenciaException(
                        "El código %s ya no existe".formatted(codigo.id())));
        if (!Objects.equals(fila.revision(), codigo.revision())) {
            throw new ConflictoConcurrenciaException("El código %s cambió desde que se leyó".formatted(codigo.id()));
        }
        fila.copiar(codigo.usadoEn(), codigo.intentosFallidos());
        try {
            codigos.saveAndFlush(fila);
        } catch (OptimisticLockingFailureException e) {
            throw new ConflictoConcurrenciaException("El código %s cambió desde que se leyó".formatted(codigo.id()));
        }
    }

    @Override
    public Optional<CodigoUnUso> porId(UUID id) {
        return codigos.findById(id).map(CodigoJpaAdapter::aDominio);
    }

    @Override
    public Optional<CodigoUnUso> ultimo(UUID usuarioId, PropositoCodigo proposito) {
        return codigos.findFirstByUsuarioIdAndPropositoOrderByEmitidoEnDesc(usuarioId, proposito.name())
                .map(CodigoJpaAdapter::aDominio);
    }

    private static CodigoUnUso aDominio(CodigoUnUsoEntity fila) {
        return new CodigoUnUso(fila.id(), fila.usuarioId(), PropositoCodigo.valueOf(fila.proposito()),
                fila.hashCodigo(), fila.emitidoEn(), fila.expiraEn(), fila.usadoEn(), fila.intentosFallidos(),
                fila.revision());
    }
}
