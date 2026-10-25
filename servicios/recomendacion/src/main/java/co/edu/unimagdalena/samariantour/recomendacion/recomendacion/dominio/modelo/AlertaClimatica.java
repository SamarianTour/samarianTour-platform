package co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo;

import java.time.Instant;
import java.util.Comparator;
import java.util.Objects;

/**
 * Una condición del destino asociada a una zona (RF-28): el estado de los ríos, el clima, sus efectos o un cierre.
 *
 * @param fuente       quién la emite, como la ve el turista; una alerta simulada lo dice ("IDEAM (simulado)")
 * @param emitidaEn    null si la fuente no da la fecha: no se inventa (DR-15)
 * @param vigenteHasta null si no tiene fin anunciado
 */
public record AlertaClimatica(String id, String zonaId, TipoAlerta tipo, NivelAlerta nivel, String titulo,
                              String descripcion, String fuente, Instant emitidaEn, Instant vigenteHasta) {

    /**
     * Para mostrar: de mayor a menor nivel; en el mismo nivel, la más reciente primero y las que no tienen fecha de
     * emisión al final.
     */
    public static final Comparator<AlertaClimatica> ORDEN_PARA_MOSTRAR = Comparator
            .comparing(AlertaClimatica::nivel, Comparator.reverseOrder())
            .thenComparing(AlertaClimatica::emitidaEn, Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(AlertaClimatica::id);

    public AlertaClimatica {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(zonaId, "zonaId");
        Objects.requireNonNull(tipo, "tipo");
        Objects.requireNonNull(nivel, "nivel");
        Objects.requireNonNull(titulo, "titulo");
        Objects.requireNonNull(fuente, "fuente");
        descripcion = descripcion == null ? "" : descripcion;
    }

    /** Activa si no tiene fin o si su fin es posterior a ahora. */
    public boolean activa(Instant ahora) {
        return vigenteHasta == null || vigenteHasta.isAfter(ahora);
    }
}
