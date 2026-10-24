/**
 * Bandeja de salida hacia el catálogo (DN-06). Los demás módulos encolan sus avisos por {@code bandeja.api}, en la
 * misma transacción que el cambio que los origina, y una tarea programada los envía al catálogo hasta que los recibe.
 */
package co.edu.unimagdalena.samariantour.nucleo.bandeja;
