package co.edu.unimagdalena.samariantour.nucleo.prestadores.infraestructura.salida.persistencia;

import co.edu.unimagdalena.samariantour.nucleo.compartido.cifrado.CifradorDatos;
import co.edu.unimagdalena.samariantour.nucleo.compartido.concurrencia.ConflictoConcurrenciaException;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.modelo.CuentaCobro;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.modelo.TipoCuenta;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.puerto.salida.CuentaCobroRepositoryPort;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Las cuentas de cobro en {@code cuentas_cobro} (V10, DN-02). Cifra el número y el titular al guardar y los descifra
 * al leer (RNF-08).
 */
@Component
public class CuentaCobroJpaAdapter implements CuentaCobroRepositoryPort {

    /** El índice único parcial de la cuenta vigente (V10). */
    static final String UNA_VIGENTE = "cuentas_cobro_una_vigente";

    private final CuentaCobroJpaRepository cuentas;
    private final CifradorDatos cifrador;

    CuentaCobroJpaAdapter(CuentaCobroJpaRepository cuentas, CifradorDatos cifrador) {
        this.cuentas = cuentas;
        this.cifrador = cifrador;
    }

    @Override
    public Optional<CuentaCobro> vigente(UUID prestadorId) {
        return cuentas.findByPrestadorIdAndVigenteHastaIsNull(prestadorId).map(this::aDominio);
    }

    @Override
    public void reemplazar(CuentaCobro anterior, CuentaCobro nueva) {
        if (anterior != null) {
            CuentaCobroEntity fila = cuentas.findById(anterior.id())
                    .filter(encontrada -> encontrada.vigenteHasta() == null)
                    .orElseThrow(() -> new ConflictoConcurrenciaException(
                            "La cuenta de cobro de %s cambió a la vez".formatted(nueva.prestadorId())));
            fila.cerrar(anterior.vigenteHasta());
            cuentas.saveAndFlush(fila);
        }
        try {
            cuentas.saveAndFlush(new CuentaCobroEntity(nueva.id(), nueva.prestadorId(), nueva.tipo().name(),
                    nueva.entidad(), cifrador.cifrar(nueva.numero()), cifrador.cifrar(nueva.titular()),
                    nueva.vigenteDesde(), null));
        } catch (DataIntegrityViolationException e) {
            if (String.valueOf(e.getMostSpecificCause().getMessage()).contains(UNA_VIGENTE)) {
                throw new ConflictoConcurrenciaException(
                        "La cuenta de cobro de %s cambió a la vez".formatted(nueva.prestadorId()));
            }
            throw e;
        }
    }

    private CuentaCobro aDominio(CuentaCobroEntity fila) {
        return new CuentaCobro(fila.id(), fila.prestadorId(), TipoCuenta.valueOf(fila.tipo()), fila.entidad(),
                cifrador.descifrar(fila.numeroCifrado()), cifrador.descifrar(fila.titularCifrado()),
                fila.vigenteDesde(), fila.vigenteHasta());
    }
}
