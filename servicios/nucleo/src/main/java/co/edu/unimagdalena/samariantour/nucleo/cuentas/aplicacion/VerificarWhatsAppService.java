package co.edu.unimagdalena.samariantour.nucleo.cuentas.aplicacion;

import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.excepcion.CodigoIncorrectoException;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.excepcion.CodigoNoVigenteException;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.excepcion.CuentaNoEncontradaException;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.CodigoUnUso;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.PropositoCodigo;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.Turista;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.entrada.VerificarWhatsAppUseCase;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.salida.CodigoRepositoryPort;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.salida.PasswordHasherPort;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.salida.UsuarioRepositoryPort;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/** Verifica el WhatsApp del turista con el último código emitido (RF-03, RF-08). */
public class VerificarWhatsAppService implements VerificarWhatsAppUseCase {

    private final UsuarioRepositoryPort usuarios;
    private final CodigoRepositoryPort codigos;
    private final PasswordHasherPort hasher;
    private final EmisionCodigoWhatsapp emision;
    private final Clock reloj;

    public VerificarWhatsAppService(UsuarioRepositoryPort usuarios, CodigoRepositoryPort codigos,
                                    PasswordHasherPort hasher, EmisionCodigoWhatsapp emision, Clock reloj) {
        this.usuarios = usuarios;
        this.codigos = codigos;
        this.hasher = hasher;
        this.emision = emision;
        this.reloj = reloj;
    }

    @Override
    @Transactional
    public void enviarCodigo(UUID turistaId) {
        Turista turista = turista(turistaId);
        if (!turista.whatsappVerificado()) {
            emision.emitir(turista, reloj.instant());
        }
    }

    /** Un código equivocado no deshace la transacción: el intento tiene que quedar contado. */
    @Override
    @Transactional(noRollbackFor = CodigoIncorrectoException.class)
    public Turista verificar(UUID turistaId, String codigo) {
        Turista turista = turista(turistaId);
        if (turista.whatsappVerificado()) {
            return turista;
        }
        Instant ahora = reloj.instant();
        CodigoUnUso vigente = codigos.ultimo(turistaId, PropositoCodigo.VERIFICACION_WHATSAPP)
                .filter(emitido -> emitido.esValido(ahora))
                .orElseThrow(CodigoNoVigenteException::new);
        if (codigo == null || !hasher.coincide(codigo.strip(), vigente.hashCodigo())) {
            vigente.registrarIntentoFallido();
            codigos.actualizar(vigente);
            throw new CodigoIncorrectoException(vigente.intentosRestantes());
        }
        vigente.usar(ahora);
        codigos.actualizar(vigente);
        turista.verificarWhatsapp();
        usuarios.actualizar(turista);
        return turista;
    }

    private Turista turista(UUID turistaId) {
        return usuarios.porId(turistaId)
                .filter(Turista.class::isInstance).map(Turista.class::cast)
                .orElseThrow(() -> new CuentaNoEncontradaException(turistaId));
    }
}
