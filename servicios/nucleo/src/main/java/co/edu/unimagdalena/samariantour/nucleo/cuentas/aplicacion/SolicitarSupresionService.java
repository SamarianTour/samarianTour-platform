package co.edu.unimagdalena.samariantour.nucleo.cuentas.aplicacion;

import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.excepcion.ContrasenaIncorrectaException;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.excepcion.CuentaBloqueadaException;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.excepcion.CuentaNoEncontradaException;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.Turista;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.entrada.SolicitarSupresionUseCase;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.salida.AvisosCatalogoPort;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.salida.PasswordHasherPort;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.salida.SesionRepositoryPort;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.salida.UsuarioRepositoryPort;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * La supresión inmediata de los datos del turista (RF-13, DN-21). La anonimización, el cierre de las sesiones y el
 * aviso al catálogo van en una sola transacción: o se cumple todo, o nada.
 */
public class SolicitarSupresionService implements SolicitarSupresionUseCase {

    private final UsuarioRepositoryPort usuarios;
    private final SesionRepositoryPort sesiones;
    private final AvisosCatalogoPort avisos;
    private final PasswordHasherPort hasher;
    private final Clock reloj;

    public SolicitarSupresionService(UsuarioRepositoryPort usuarios, SesionRepositoryPort sesiones,
                                     AvisosCatalogoPort avisos, PasswordHasherPort hasher, Clock reloj) {
        this.usuarios = usuarios;
        this.sesiones = sesiones;
        this.avisos = avisos;
        this.hasher = hasher;
        this.reloj = reloj;
    }

    /** Una contraseña equivocada no deshace la transacción: el fallo tiene que quedar contado (RF-05). */
    @Override
    @Transactional(noRollbackFor = {ContrasenaIncorrectaException.class, CuentaBloqueadaException.class})
    public void suprimir(UUID turistaId, String contrasena) {
        Instant ahora = reloj.instant();
        Turista turista = usuarios.porId(turistaId)
                .filter(Turista.class::isInstance).map(Turista.class::cast)
                .filter(Turista::estaActiva)
                .orElseThrow(() -> new CuentaNoEncontradaException(turistaId));
        if (turista.estaBloqueada(ahora)) {
            throw new CuentaBloqueadaException(turista.bloqueadaHasta());
        }
        if (!turista.autenticar(contrasena, hasher)) {
            turista.registrarIntentoFallido(ahora);
            usuarios.actualizar(turista);
            if (turista.estaBloqueada(ahora)) {
                throw new CuentaBloqueadaException(turista.bloqueadaHasta());
            }
            throw new ContrasenaIncorrectaException();
        }
        turista.suprimir(UUID.randomUUID(), ahora);
        usuarios.actualizar(turista);
        sesiones.revocarTodas(turistaId, ahora);
        avisos.avisarSupresion(turistaId);
    }
}
