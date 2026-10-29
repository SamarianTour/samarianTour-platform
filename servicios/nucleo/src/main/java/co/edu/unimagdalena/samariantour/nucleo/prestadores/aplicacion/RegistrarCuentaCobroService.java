package co.edu.unimagdalena.samariantour.nucleo.prestadores.aplicacion;

import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.excepcion.CuentaCobroNoRegistradaException;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.excepcion.PrestadorNoRegistradoException;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.modelo.CuentaCobro;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.modelo.TipoCuenta;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.puerto.entrada.RegistrarCuentaCobroUseCase;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.puerto.salida.AuditoriaPort;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.puerto.salida.CuentaCobroRepositoryPort;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.puerto.salida.PrestadorRepositoryPort;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** La cuenta de cobro con su historial y su auditoría, en una sola transacción (RF-74, RF-75, DN-34). */
public class RegistrarCuentaCobroService implements RegistrarCuentaCobroUseCase {

    private final PrestadorRepositoryPort prestadores;
    private final CuentaCobroRepositoryPort cuentas;
    private final AuditoriaPort auditoria;
    private final Clock reloj;

    public RegistrarCuentaCobroService(PrestadorRepositoryPort prestadores, CuentaCobroRepositoryPort cuentas,
                                       AuditoriaPort auditoria, Clock reloj) {
        this.prestadores = prestadores;
        this.cuentas = cuentas;
        this.auditoria = auditoria;
        this.reloj = reloj;
    }

    @Override
    @Transactional
    public CuentaCobro registrar(UUID prestadorId, TipoCuenta tipo, String entidad, String numero, String titular) {
        if (prestadores.porId(prestadorId).isEmpty()) {
            throw new PrestadorNoRegistradoException(prestadorId);
        }
        Instant ahora = reloj.instant();
        CuentaCobro nueva = CuentaCobro.nueva(UUID.randomUUID(), prestadorId, tipo, entidad, numero, titular, ahora);
        Optional<CuentaCobro> anterior = cuentas.vigente(prestadorId);
        if (anterior.isPresent() && anterior.get().mismosDatos(nueva)) {
            return anterior.get();
        }
        cuentas.reemplazar(anterior.map(cuenta -> cuenta.cerrar(ahora)).orElse(null), nueva);
        auditoria.registrar(prestadorId, anterior.isPresent() ? "CUENTA_COBRO_CAMBIADA" : "CUENTA_COBRO_REGISTRADA",
                "%s %s %s".formatted(nueva.tipo(), nueva.entidad(), nueva.numeroEnmascarado()), ahora);
        return nueva;
    }

    @Override
    public CuentaCobro vigente(UUID prestadorId) {
        return cuentas.vigente(prestadorId).orElseThrow(() -> new CuentaCobroNoRegistradaException(prestadorId));
    }
}
