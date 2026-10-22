package co.edu.unimagdalena.samariantour.catalogo.compartido.seguridad;

/**
 * Roles que el catálogo distingue. El núcleo emite el rol en el token; en el prototipo llega en un header (DC-01).
 */
public enum Rol {
    TURISTA,
    PRESTADOR,
    ADMINISTRADOR
}
