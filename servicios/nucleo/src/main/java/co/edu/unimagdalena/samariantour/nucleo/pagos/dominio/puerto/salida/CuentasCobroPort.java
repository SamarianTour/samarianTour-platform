package co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.salida;

import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo.CuentaCobro;

import java.util.Optional;
import java.util.UUID;

/**
 * La cuenta de cobro vigente de cada prestador (RF-74). Es del módulo de prestadores, que todavía está vacío: por ahora
 * la lee un adaptador provisional de la demo [SUPUESTO] (DN-26).
 */
public interface CuentasCobroPort {

    Optional<CuentaCobro> cuentaVigente(UUID prestadorId);
}
