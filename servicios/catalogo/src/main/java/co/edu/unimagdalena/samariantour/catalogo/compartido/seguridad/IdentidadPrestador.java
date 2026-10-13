package co.edu.unimagdalena.samariantour.catalogo.compartido.seguridad;

import java.util.UUID;

/**
 * Prestador que hace la petición. En la versión final sale del JWT que emite el núcleo. El tipo llega como texto
 * porque el paquete compartido no conoce los tipos de los módulos: cada módulo lo convierte al suyo.
 */
public record IdentidadPrestador(UUID prestadorId, String tipoPrestador) {
}
