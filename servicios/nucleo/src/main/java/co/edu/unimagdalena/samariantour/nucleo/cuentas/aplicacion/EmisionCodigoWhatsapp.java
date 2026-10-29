package co.edu.unimagdalena.samariantour.nucleo.cuentas.aplicacion;

import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.CodigoUnUso;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.PropositoCodigo;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.Turista;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.politica.PoliticaCodigoUnUso;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.salida.CodigoRepositoryPort;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.salida.NotificationPort;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.salida.PasswordHasherPort;

import java.time.Instant;
import java.util.UUID;
import java.util.random.RandomGenerator;

/**
 * Emite el código que verifica el WhatsApp del turista y se lo envía (RF-03). Lo usan el registro y el reenvío; corre
 * dentro de la transacción de quien lo llama. Solo se guarda el hash del código.
 */
public class EmisionCodigoWhatsapp {

    private final CodigoRepositoryPort codigos;
    private final PasswordHasherPort hasher;
    private final NotificationPort notificacion;
    private final RandomGenerator azar;

    public EmisionCodigoWhatsapp(CodigoRepositoryPort codigos, PasswordHasherPort hasher,
                                 NotificationPort notificacion, RandomGenerator azar) {
        this.codigos = codigos;
        this.hasher = hasher;
        this.notificacion = notificacion;
        this.azar = azar;
    }

    void emitir(Turista turista, Instant ahora) {
        String codigo = PoliticaCodigoUnUso.nuevoCodigo(azar);
        codigos.insertar(CodigoUnUso.emitir(UUID.randomUUID(), turista.id(), PropositoCodigo.VERIFICACION_WHATSAPP,
                hasher.hash(codigo), ahora));
        notificacion.enviarCodigoWhatsapp(turista.whatsapp(), codigo);
    }
}
