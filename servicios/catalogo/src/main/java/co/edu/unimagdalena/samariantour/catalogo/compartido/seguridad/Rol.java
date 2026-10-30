package co.edu.unimagdalena.samariantour.catalogo.compartido.seguridad;

/**
 * Roles que el catálogo distingue. El núcleo emite el rol en el token de acceso (DC-01); los otros dos roles del
 * núcleo, el operador y el gestor, no tienen rutas aquí.
 */
public enum Rol {
    TURISTA,
    PRESTADOR,
    ADMINISTRADOR
}
