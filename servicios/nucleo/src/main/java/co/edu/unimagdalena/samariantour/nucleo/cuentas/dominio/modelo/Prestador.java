package co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo;

import java.time.Instant;
import java.util.UUID;

/**
 * La cuenta de un prestador. Su RNT, su tipo y su WhatsApp de contacto son del módulo de prestadores (RF-62 a RF-71);
 * aquí solo están los datos para iniciar sesión.
 */
public final class Prestador extends Usuario {

    Prestador(DatosCuenta datos) {
        super(datos);
    }

    @Override
    public Rol rol() {
        return Rol.PRESTADOR;
    }

    /**
     * Suprime sus datos personales (RF-13, DN-45), como los del turista: el nombre, el correo y la contraseña. Su
     * WhatsApp y su RNT son del módulo de prestadores, que se encarga de ellos.
     */
    public void suprimir(UUID supresionId, Instant ahora) {
        anonimizar(supresionId, ahora);
    }
}
