package co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.modelo;

import java.util.Objects;

/**
 * Lo que dice el RNT de un número (RF-62, RF-63): la fuente solo publica los registros activos, así que un registro
 * encontrado es un RNT activo (DN-33).
 *
 * @param personaNatural si el titular es persona natural ("No Comerciante" en el RNT); el guía lo es (RF-70)
 * @param empleados      los que declaró al RNT; vacío si no los publica
 */
public record RegistroRnt(String numero, String razonSocial, String municipio, String categoria, String subCategoria,
                          boolean personaNatural, Integer empleados) {

    public RegistroRnt {
        Objects.requireNonNull(numero, "numero");
        Objects.requireNonNull(razonSocial, "razonSocial");
        Objects.requireNonNull(municipio, "municipio");
        Objects.requireNonNull(categoria, "categoria");
    }
}
