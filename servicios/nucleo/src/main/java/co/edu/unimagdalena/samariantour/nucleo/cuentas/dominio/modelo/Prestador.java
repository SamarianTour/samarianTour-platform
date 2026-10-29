package co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo;

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
}
