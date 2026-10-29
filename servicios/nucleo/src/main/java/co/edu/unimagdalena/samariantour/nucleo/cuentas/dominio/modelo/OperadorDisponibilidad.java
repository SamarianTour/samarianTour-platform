package co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo;

/** Personal interno que atiende las solicitudes de los prestadores asignados (RF-32 a RF-40). Sin datos propios. */
public final class OperadorDisponibilidad extends Usuario {

    OperadorDisponibilidad(DatosCuenta datos) {
        super(datos);
    }

    @Override
    public Rol rol() {
        return Rol.OPERADOR_DISPONIBILIDAD;
    }
}
