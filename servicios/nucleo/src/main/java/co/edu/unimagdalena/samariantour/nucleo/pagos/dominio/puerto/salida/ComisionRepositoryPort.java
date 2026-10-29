package co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.salida;

import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo.ConfiguracionComision;

import java.util.List;

/** El historial de la comisión en PostgreSQL (DN-23): solo se agregan filas. */
public interface ComisionRepositoryPort {

    /** Del cambio más reciente al más antiguo; incluye el inicial de la migración. */
    List<ConfiguracionComision> historial();

    void agregar(ConfiguracionComision configuracion);
}
