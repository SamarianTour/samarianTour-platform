package co.edu.unimagdalena.samariantour.nucleo.cuentas.infraestructura.salida.contrasena;

import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.salida.PasswordHasherPort;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/** BCrypt con el costo por defecto de Spring Security, 10 (RNF-06, DN-18). */
@Component
public class BCryptAdapter implements PasswordHasherPort {

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
