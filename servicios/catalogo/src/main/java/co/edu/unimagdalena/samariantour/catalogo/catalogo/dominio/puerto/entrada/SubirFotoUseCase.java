package co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Foto;

import java.util.UUID;

/**
 * El prestador sube una foto para sus ofertas (RF-65, DC-47). La foto queda suelta: se asigna al publicar o editar una
 * oferta, dentro del plazo de {@link co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.FotoSubida#PLAZO_SIN_USO};
 * si no se usa, la limpieza la borra.
 */
public interface SubirFotoUseCase {

    /**
     * @param imagen el archivo que subió el prestador: JPG, PNG o WebP de hasta 5 MB
     * @throws co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.DatoInvalidoException si no es una imagen aceptada o pesa más de 5 MB
     */
    Foto subir(UUID prestadorId, byte[] imagen);
}
