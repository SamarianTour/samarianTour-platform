package co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo;

/** Perfil institucional de solo lectura que consulta los indicadores del destino (RF-85 a RF-89). Sin datos propios. */
public final class GestorDestino extends Usuario {

    GestorDestino(DatosCuenta datos) {
        super(datos);
    }

    @Override
    public Rol rol() {
        return Rol.GESTOR_DESTINO;
    }
}
