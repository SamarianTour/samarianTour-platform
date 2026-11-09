package co.edu.unimagdalena.samariantour.nucleo.prestadores.aplicacion;

import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.excepcion.PrestadorConCompromisosException;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.modelo.Prestador;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.puerto.entrada.SuprimirPrestadorUseCase;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.puerto.salida.AuditoriaPort;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.puerto.salida.CompromisosPort;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.puerto.salida.CuentasPort;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.puerto.salida.EventoSalidaPort;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.puerto.salida.PrestadorRepositoryPort;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.puerto.salida.TransaccionPort;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * La supresión de los datos del prestador (RF-13, DN-45), que orquesta este módulo porque las ofertas y el perfil del
 * prestador viven en el catálogo y su RNT, aquí:
 * <ol>
 *   <li>confirma la contraseña, en su propia transacción, para que un fallo quede contado (RF-05);</li>
 *   <li>si tiene compromisos con turistas o dinero por recibir, se niega (409): primero los cumple;</li>
 *   <li>en una sola transacción: el prestador queda sin WhatsApp ni habilitación, el catálogo recibe la nueva
 *       habilitación y el aviso de supresión, queda la auditoría, y la cuenta se anonimiza y pierde sus sesiones.</li>
 * </ol>
 * Un prestador que todavía no registró su RNT también puede suprimir sus datos: solo tiene la cuenta y, quizá, el
 * perfil en el catálogo. Sus solicitudes abiertas sin pagar las cierra solicitudes en el minuto siguiente, como cuando
 * pierde el RNT (DN-36).
 */
public class SuprimirPrestadorService implements SuprimirPrestadorUseCase {

    private final PrestadorRepositoryPort prestadores;
    private final CuentasPort cuentas;
    private final CompromisosPort compromisos;
    private final EventoSalidaPort eventos;
    private final AuditoriaPort auditoria;
    private final TransaccionPort transaccion;
    private final Clock reloj;

    public SuprimirPrestadorService(PrestadorRepositoryPort prestadores, CuentasPort cuentas,
                                    CompromisosPort compromisos, EventoSalidaPort eventos, AuditoriaPort auditoria,
                                    TransaccionPort transaccion, Clock reloj) {
        this.prestadores = prestadores;
        this.cuentas = cuentas;
        this.compromisos = compromisos;
        this.eventos = eventos;
        this.auditoria = auditoria;
        this.transaccion = transaccion;
        this.reloj = reloj;
    }

    @Override
    public void suprimir(UUID prestadorId, String contrasena) {
        cuentas.verificarContrasena(prestadorId, contrasena);
        transaccion.ejecutar(() -> {
            List<String> pendientes = compromisos.pendientes(prestadorId);
            if (!pendientes.isEmpty()) {
                throw new PrestadorConCompromisosException(pendientes);
            }
            Instant ahora = reloj.instant();
            Optional<Prestador> registrado = prestadores.porId(prestadorId);
            registrado.ifPresent(prestador -> {
                if (prestador.suprimir(ahora)) {
                    eventos.avisarHabilitacion(prestadorId, false, prestador.versionHabilitacion());
                }
                prestadores.actualizar(prestador);
                auditoria.registrar(prestadorId, "PRESTADOR_SUPRIMIDO", "RF-13", ahora);
            });
            eventos.avisarSupresion(prestadorId);
            cuentas.suprimirCuenta(prestadorId);
            return null;
        });
    }
}
