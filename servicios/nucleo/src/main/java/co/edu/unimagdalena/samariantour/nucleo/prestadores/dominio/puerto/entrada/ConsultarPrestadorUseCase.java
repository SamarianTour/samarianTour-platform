package co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.puerto.entrada;

import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.modelo.CuentaCobro;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.modelo.Prestador;

import java.util.Optional;
import java.util.UUID;

/** Las consultas de los demás módulos (RF-32, RF-54, RF-57, RF-79, RF-89, DN-35). */
public interface ConsultarPrestadorUseCase {

    /** Vacío si todavía no registró su RNT. */
    Optional<Prestador> consultar(UUID prestadorId);

    /** La cuenta vigente, completa: solo para la dispersión (RF-54, RF-75). */
    Optional<CuentaCobro> cuentaCobroVigente(UUID prestadorId);
}
