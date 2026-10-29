package co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo;

/**
 * Los cinco roles del SRS (DN-04), con los mismos nombres que lleva el token. Cada uno es una subclase de
 * {@link Usuario}.
 */
public enum Rol {
    TURISTA,
    PRESTADOR,
    ADMINISTRADOR,
    OPERADOR_DISPONIBILIDAD,
    GESTOR_DESTINO;

    /**
     * Solo el turista y el prestador se registran solos; las cuentas internas las crea el administrador (RF-81) y la
     * del administrador viene con la instalación (RF-01).
     */
    public boolean seRegistraSolo() {
        return this == TURISTA || this == PRESTADOR;
    }
}
