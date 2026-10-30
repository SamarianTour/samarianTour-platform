/**
 * Seguridad: la identidad del usuario (id, rol y, para el prestador, su tipo), que sale del token de acceso del núcleo
 * validado por Spring Security con {@code libs/seguridad-jwt} (DC-01), y los errores 401 y 403 de los controladores.
 * Las reglas por prefijo de ruta están en {@code compartido/config/SeguridadConfig}.
 */
package co.edu.unimagdalena.samariantour.catalogo.compartido.seguridad;
