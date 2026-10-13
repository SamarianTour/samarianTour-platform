package co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo;

import java.math.BigDecimal;

/**
 * Atributos comunes a toda versión de oferta (VersionOferta en el diagrama de clases),
 * agrupados en un objeto de valor para no repetirlos en cada subclase.
 *
 * @param precioReferencia precio informativo y no vinculante, en pesos colombianos (RF-20, RF-65)
 * @param zonaId           zona en la que se ubica la oferta (referencia por identificador)
 * @param capacidadMaxima  máximo de personas por solicitud (RF-65)
 */
public record ContenidoComun(
        String nombre,
        String descripcion,
        BigDecimal precioReferencia,
        String horarios,
        String zonaId,
        Coordenada ubicacion,
        String comoLlegar,
        int capacidadMaxima) {

    public ContenidoComun {
        nombre = Validaciones.texto(nombre, "nombre");
        descripcion = Validaciones.texto(descripcion, "descripcion");
        Validaciones.noNegativo(precioReferencia, "precioReferencia");
        horarios = Validaciones.textoOpcional(horarios);
        zonaId = Validaciones.texto(zonaId, "zonaId");
        comoLlegar = Validaciones.textoOpcional(comoLlegar);
        Validaciones.positivo(capacidadMaxima, "capacidadMaxima");
    }
}
