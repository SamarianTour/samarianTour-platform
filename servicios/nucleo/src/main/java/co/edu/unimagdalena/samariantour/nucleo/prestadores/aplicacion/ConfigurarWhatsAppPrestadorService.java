package co.edu.unimagdalena.samariantour.nucleo.prestadores.aplicacion;

import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.excepcion.CodigoIncorrectoException;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.excepcion.PrestadorNoRegistradoException;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.modelo.Prestador;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.politica.PoliticaCodigoWhatsapp;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.puerto.entrada.ConfigurarWhatsAppPrestadorUseCase;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.puerto.salida.HasherPort;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.puerto.salida.NotificacionPrestadorPort;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.puerto.salida.PrestadorRepositoryPort;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import java.util.random.RandomGenerator;

/** El WhatsApp del prestador y su verificación (RF-71, DN-33). Solo se guarda el hash del código. */
public class ConfigurarWhatsAppPrestadorService implements ConfigurarWhatsAppPrestadorUseCase {

    private final PrestadorRepositoryPort prestadores;
    private final HasherPort hasher;
    private final NotificacionPrestadorPort notificacion;
    private final RandomGenerator azar;
    private final Clock reloj;

    public ConfigurarWhatsAppPrestadorService(PrestadorRepositoryPort prestadores, HasherPort hasher,
                                              NotificacionPrestadorPort notificacion, RandomGenerator azar,
                                              Clock reloj) {
        this.prestadores = prestadores;
        this.hasher = hasher;
        this.notificacion = notificacion;
        this.azar = azar;
        this.reloj = reloj;
    }

    @Override
    @Transactional
    public Prestador configurar(UUID prestadorId, String whatsapp) {
        Prestador prestador = prestador(prestadorId);
        if (prestador.cambiarWhatsapp(whatsapp)) {
            emitir(prestador, reloj.instant());
        }
        prestadores.actualizar(prestador);
        return prestador;
    }

    @Override
    @Transactional
    public void enviarCodigo(UUID prestadorId) {
        Prestador prestador = prestador(prestadorId);
        emitir(prestador, reloj.instant());
        prestadores.actualizar(prestador);
    }

    /** Un código equivocado no deshace la transacción: el intento tiene que quedar contado. */
    @Override
    @Transactional(noRollbackFor = CodigoIncorrectoException.class)
    public Prestador verificar(UUID prestadorId, String codigo) {
        Prestador prestador = prestador(prestadorId);
        Instant ahora = reloj.instant();
        boolean coincide = prestador.codigoVigente(ahora) && codigo != null
                && hasher.coincide(codigo.strip(), prestador.hashCodigo());
        try {
            prestador.verificarCodigo(coincide, ahora);
        } finally {
            prestadores.actualizar(prestador);
        }
        return prestador;
    }

    private void emitir(Prestador prestador, Instant ahora) {
        String codigo = PoliticaCodigoWhatsapp.nuevoCodigo(azar);
        if (prestador.emitirCodigo(hasher.hash(codigo), ahora)) {
            notificacion.enviarCodigo(prestador.whatsapp(), codigo);
        }
    }

    private Prestador prestador(UUID prestadorId) {
        return prestadores.porId(prestadorId).orElseThrow(() -> new PrestadorNoRegistradoException(prestadorId));
    }
}
