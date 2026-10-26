package co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo;

import java.time.LocalTime;
import java.util.List;
import java.util.Objects;

/**
 * Una oferta visible del catálogo, como la entrega su consulta interna al crear la solicitud (RF-30): lo que se copia
 * y lo que se valida.
 *
 * @param horariosSalida  las horas entre las que el turista elige; vacío salvo en actividades y paquetes
 * @param capacidadMaxima el máximo de personas por solicitud (RF-65)
 */
public record OfertaSolicitable(String id, DatosOfertaCopiados datos, List<LocalTime> horariosSalida,
                                int capacidadMaxima) {

    public OfertaSolicitable {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(datos, "datos");
        horariosSalida = horariosSalida == null ? List.of() : List.copyOf(horariosSalida);
    }
}
