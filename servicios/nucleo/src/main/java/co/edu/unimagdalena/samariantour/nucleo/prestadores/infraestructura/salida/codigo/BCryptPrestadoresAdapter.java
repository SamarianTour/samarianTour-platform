package co.edu.unimagdalena.samariantour.nucleo.prestadores.infraestructura.salida.codigo;

import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.puerto.salida.HasherPort;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/** BCrypt para los códigos del WhatsApp del prestador (RNF-06), como en las cuentas (DN-28). */
@Component
public class BCryptPrestadoresAdapter implements HasherPort {

    private final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder();

    @Override
    public String hash(String valor) {
        return bcrypt.encode(valor);
    }

    @Override
    public boolean coincide(String valor, String hash) {
        return bcrypt.matches(valor, hash);
    }
}
