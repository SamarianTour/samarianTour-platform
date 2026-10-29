package co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.puerto.salida;

import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.modelo.CuentaCobro;

import java.util.Optional;
import java.util.UUID;

/** Las cuentas de cobro con su historial, el número y el titular cifrados (RF-74, RF-75, RNF-08). */
public interface CuentaCobroRepositoryPort {

    Optional<CuentaCobro> vigente(UUID prestadorId);

    /**
     * Cierra la vigente, si hay, y guarda la nueva. Hay una sola vigente por prestador, aunque dos réplicas registren a
     * la vez: la segunda falla con {@code ConflictoConcurrenciaException} (ADR-05).
     */
    void reemplazar(CuentaCobro anterior, CuentaCobro nueva);
}
