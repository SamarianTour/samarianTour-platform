package co.edu.unimagdalena.samariantour.nucleo.cuentas.infraestructura.salida.correo;

import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.excepcion.CorreoNoDisponibleException;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.politica.PoliticaCodigoUnUso;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.salida.EmailPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Envía los correos de las cuentas por SMTP (RF-07, DN-20). En el piloto el servidor es Mailpit, que los recibe todos
 * y los muestra en {@code http://localhost:8025} sin entregarlos a nadie. La conexión espera como máximo 3 s
 * (RNF-05, {@code spring.mail.properties}).
 */
@Component
public class SmtpEmailAdapter implements EmailPort {

    private final JavaMailSender smtp;
    private final String remitente;
    private final String enlace;

    /**
     * @param enlace la pantalla del frontend que pide la contraseña nueva, con {@code {token}} donde va el secreto
     */
    public SmtpEmailAdapter(JavaMailSender smtp, @Value("${nucleo.correo.remitente}") String remitente,
                            @Value("${nucleo.cuentas.enlace-restablecimiento}") String enlace) {
        this.smtp = smtp;
        this.remitente = remitente;
        this.enlace = enlace;
    }

    @Override
    public void enviarRestablecimiento(String correo, String nombre, String token) {
        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setFrom(remitente);
        mensaje.setTo(correo);
        mensaje.setSubject("Restablece tu contraseña de Samarian Tour");
        mensaje.setText("""
                Hola, %s:

                Recibimos una solicitud para restablecer la contraseña de tu cuenta. Para poner una nueva, entra a:

                %s

                El enlace vence en %d minutos y sirve una sola vez. Si no lo pediste, ignora este correo: tu contraseña
                sigue igual.

                Samarian Tour
                """.formatted(nombre, enlace.replace("{token}", URLEncoder.encode(token, StandardCharsets.UTF_8)),
                PoliticaCodigoUnUso.VIGENCIA.toMinutes()));
        try {
            smtp.send(mensaje);
        } catch (MailException e) {
            throw new CorreoNoDisponibleException(e);
        }
    }
}
