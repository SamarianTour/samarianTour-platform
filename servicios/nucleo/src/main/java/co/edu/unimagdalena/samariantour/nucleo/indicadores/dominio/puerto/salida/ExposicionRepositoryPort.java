package co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.puerto.salida;

import co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.modelo.AparicionesPrestador;
import co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.modelo.RegistroExposicion;

import java.time.Instant;
import java.util.List;

/** El registro de exposición (RET-02, DN-42). Lo que cuenta para el sesgo es solo el origen RECOMENDACION. */
public interface ExposicionRepositoryPort {

    void guardar(RegistroExposicion registro);

    /** Cuántas recomendaciones, es decir consultas, se registraron desde {@code desde}. */
    long recomendacionesDesde(Instant desde);

    List<AparicionesPrestador> aparicionesDesde(Instant desde);

    /** @return cuántas filas borró */
    int borrarAnteriores(Instant limite);
}
