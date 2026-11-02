package co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.puerto.salida;

import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.modelo.PrestadorRegistrado;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** El operador de cada prestador, que guardan los prestadores (RF-32, RF-81, DN-35). */
public interface OperadoresPrestadoresPort {

    List<PrestadorRegistrado> prestadores(boolean soloSinOperador);

    /** @return vacío si el prestador todavía no registró su RNT */
    Optional<PrestadorRegistrado> asignarOperador(UUID prestadorId, UUID operadorId);

    long asignadosA(UUID operadorId);
}
