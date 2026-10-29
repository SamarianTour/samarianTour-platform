package co.edu.unimagdalena.samariantour.nucleo.cuentas.aplicacion;

import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.TokenOpaco;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.entrada.CerrarSesionUseCase;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.salida.PasswordHasherPort;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.salida.SesionRepositoryPort;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/**
 * El cierre de sesión (RF-06): revoca la sesión del token de renovación. Solo quien tiene el token completo la
 * cierra; con el identificador de la sesión no basta.
 */
public class CerrarSesionService implements CerrarSesionUseCase {

    private final SesionRepositoryPort sesiones;
    private final PasswordHasherPort hasher;
    private final Clock reloj;

    public CerrarSesionService(SesionRepositoryPort sesiones, PasswordHasherPort hasher, Clock reloj) {
        this.sesiones = sesiones;
        this.hasher = hasher;
        this.reloj = reloj;
    }

    @Override
    @Transactional
    public void cerrar(String tokenRenovacion) {
        TokenOpaco.leer(tokenRenovacion).ifPresent(token -> sesiones.porId(token.id())
                .filter(sesion -> sesion.revocadaEn() == null)
                .filter(sesion -> hasher.coincide(token.secreto(), sesion.hashRenovacion()))
                .ifPresent(sesion -> {
                    sesion.revocar(reloj.instant());
                    sesiones.actualizar(sesion);
                }));
    }
}
