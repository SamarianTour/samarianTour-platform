package co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.puerto.entrada;

import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.modelo.Prestador;

import java.util.Optional;
import java.util.UUID;

/** El administrador asigna el operador de disponibilidad que atiende al prestador (RF-32, RF-81). */
public interface AsignarOperadorUseCase {

    /** @return vacío si el prestador todavía no registró su RNT */
    Optional<Prestador> asignar(UUID prestadorId, UUID operadorId);
}
