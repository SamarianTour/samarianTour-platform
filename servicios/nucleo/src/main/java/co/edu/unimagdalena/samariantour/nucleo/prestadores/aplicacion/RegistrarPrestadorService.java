package co.edu.unimagdalena.samariantour.nucleo.prestadores.aplicacion;

import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.excepcion.CuentaNoEncontradaException;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.excepcion.PrestadorNoRegistradoException;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.excepcion.PrestadorYaRegistradoException;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.modelo.ConsultaRnt;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.modelo.Prestador;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.puerto.entrada.RegistrarPrestadorUseCase;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.puerto.entrada.comando.RegistrarPrestadorCommand;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.puerto.salida.CuentasPort;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.puerto.salida.EventoSalidaPort;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.puerto.salida.PrestadorRepositoryPort;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.puerto.salida.RntVerificationPort;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.puerto.salida.TransaccionPort;

import java.time.Clock;
import java.util.UUID;

/**
 * El registro del prestador con su RNT y el cambio de número (RF-62, RF-63, RF-70, DN-33). El RNT se consulta antes de
 * abrir la transacción, porque puede tardar hasta 3 s; el prestador y el aviso al catálogo se guardan juntos.
 */
public class RegistrarPrestadorService implements RegistrarPrestadorUseCase {

    private final PrestadorRepositoryPort prestadores;
    private final RntVerificationPort rnt;
    private final CuentasPort cuentas;
    private final EventoSalidaPort eventos;
    private final TransaccionPort transaccion;
    private final Clock reloj;

    public RegistrarPrestadorService(PrestadorRepositoryPort prestadores, RntVerificationPort rnt, CuentasPort cuentas,
                                     EventoSalidaPort eventos, TransaccionPort transaccion, Clock reloj) {
        this.prestadores = prestadores;
        this.rnt = rnt;
        this.cuentas = cuentas;
        this.eventos = eventos;
        this.transaccion = transaccion;
        this.reloj = reloj;
    }

    @Override
    public Prestador registrar(RegistrarPrestadorCommand comando) {
        UUID id = comando.prestadorId();
        if (!cuentas.esPrestadorActivo(id)) {
            throw new CuentaNoEncontradaException(id);
        }
        if (prestadores.porId(id).isPresent()) {
            throw new PrestadorYaRegistradoException(id);
        }
        String numero = comando.numeroRnt() == null ? "" : comando.numeroRnt().strip();
        ConsultaRnt consulta = rnt.consultar(numero);
        Prestador prestador = Prestador.registrar(id, numero, comando.datosTipo(), consulta, reloj.instant());
        return transaccion.ejecutar(() -> {
            prestadores.insertar(prestador);
            if (prestador.habilitadoEnCatalogo()) {
                eventos.avisarHabilitacion(id, true, prestador.versionHabilitacion());
            }
            return prestador;
        });
    }

    @Override
    public Prestador cambiarRnt(UUID prestadorId, String numeroRnt) {
        String numero = numeroRnt == null ? "" : numeroRnt.strip();
        ConsultaRnt consulta = rnt.consultar(numero);
        return transaccion.ejecutar(() -> {
            Prestador prestador = prestadores.porId(prestadorId)
                    .orElseThrow(() -> new PrestadorNoRegistradoException(prestadorId));
            if (prestador.cambiarRnt(numero, consulta, reloj.instant())) {
                eventos.avisarHabilitacion(prestadorId, prestador.estaHabilitado(), prestador.versionHabilitacion());
            }
            prestadores.actualizar(prestador);
            return prestador;
        });
    }
}
