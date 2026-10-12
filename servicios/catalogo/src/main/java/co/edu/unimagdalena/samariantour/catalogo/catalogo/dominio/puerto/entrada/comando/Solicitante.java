package co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.comando;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.TipoPrestador;

import java.util.Objects;
import java.util.UUID;

/**
 * Prestador que hace la petición, según el token de acceso (en el prototipo, según los headers que lo sustituyen).
 */
public record Solicitante(UUID prestadorId, TipoPrestador tipoPrestador) {

    public Solicitante {
        Objects.requireNonNull(prestadorId, "prestadorId");
        Objects.requireNonNull(tipoPrestador, "tipoPrestador");
    }
}
