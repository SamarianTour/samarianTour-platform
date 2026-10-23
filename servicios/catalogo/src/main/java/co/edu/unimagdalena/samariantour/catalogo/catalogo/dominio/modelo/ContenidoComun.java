package co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.DatoInvalidoException;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;

/**
 * Atributos comunes a toda versión de oferta (VersionOferta en el diagrama de clases),
 * agrupados en un objeto de valor para no repetirlos en cada subclase.
 *
 * <p>Las fotos son parte de la versión, como el nombre o el precio: cambiarlas es editar la oferta, y pasa por
 * moderación (RF-66, DC-47). Una versión guardada antes de DC-47 puede no tener fotos; publicar o editar exige al
 * menos una ({@link Oferta}).
 *
 * @param precioReferencia precio informativo y no vinculante, en pesos colombianos (RF-20, RF-65)
 * @param zonaId           zona en la que se ubica la oferta (referencia por identificador)
 * @param capacidadMaxima  máximo de personas por solicitud (RF-65)
 * @param fotos            fotos de la oferta en el orden en que se muestran; la primera es la portada. Como máximo
 *                         {@link #MAXIMO_FOTOS}, sin repetir
 */
public record ContenidoComun(
        String nombre,
        String descripcion,
        BigDecimal precioReferencia,
        String horarios,
        String zonaId,
        Coordenada ubicacion,
        String comoLlegar,
        int capacidadMaxima,
        List<Foto> fotos) {

    /** Las mismas que un perfil (DC-39). */
    public static final int MAXIMO_FOTOS = 6;

    public ContenidoComun {
        nombre = Validaciones.texto(nombre, "nombre");
        descripcion = Validaciones.texto(descripcion, "descripcion");
        Validaciones.noNegativo(precioReferencia, "precioReferencia");
        horarios = Validaciones.textoOpcional(horarios);
        zonaId = Validaciones.texto(zonaId, "zonaId");
        comoLlegar = Validaciones.textoOpcional(comoLlegar);
        Validaciones.positivo(capacidadMaxima, "capacidadMaxima");
        fotos = Validaciones.lista(fotos, "fotos");
        if (fotos.size() > MAXIMO_FOTOS) {
            throw new DatoInvalidoException("fotos", "una oferta tiene como máximo %d fotos".formatted(MAXIMO_FOTOS));
        }
        if (new HashSet<>(fotos).size() != fotos.size()) {
            throw new DatoInvalidoException("fotos", "no puede repetir una foto");
        }
    }
}
