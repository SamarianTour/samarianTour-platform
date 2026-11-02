package co.edu.unimagdalena.samariantour.nucleo.cuentas.api;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Lo que la administración usa de las cuentas: las cuentas internas (RF-81, DN-40). Java puro, como
 * {@code pagos.api.PagosAdministracionApi}.
 */
public interface CuentasAdministracionApi {

    List<CuentaInternaRegistrada> cuentasInternas();

    /** Vacío si no existe o no es interna. */
    Optional<CuentaInternaRegistrada> cuentaInterna(UUID usuarioId);

    /**
     * @param rol ADMINISTRADOR, OPERADOR_DISPONIBILIDAD o GESTOR_DESTINO
     * @throws CuentaInternaRechazadaException con el código del error de las cuentas
     */
    CuentaInternaRegistrada crearCuentaInterna(String rol, String nombre, String correo);

    /** @throws CuentaInternaRechazadaException con {@code CUENTA_NO_ENCONTRADA} */
    CuentaInternaRegistrada desactivar(UUID usuarioId);

    /** @throws CuentaInternaRechazadaException con {@code CUENTA_NO_ENCONTRADA} */
    CuentaInternaRegistrada reactivar(UUID usuarioId);

    boolean esOperadorActivo(UUID usuarioId);
}
