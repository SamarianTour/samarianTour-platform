package co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo;

/**
 * Personal interno que modera, sincroniza el RNT y gestiona zonas, cuentas internas y la comisión. Sin datos
 * propios.
 */
public final class Administrador extends Usuario {

    Administrador(DatosCuenta datos) {
        super(datos);
    }

    @Override
    public Rol rol() {
        return Rol.ADMINISTRADOR;
    }
}
