package co.edu.unimagdalena.samariantour.nucleo.prestadores.api;

import java.util.Optional;
import java.util.UUID;

/**
 * Lo único que los demás módulos usan de los prestadores (DN-17, DN-35): solicitudes, pagos y administración. Los
 * prestadores solo dependen de la API de cuentas.
 */
public interface PrestadoresApiInterna {

    /** El prestador registrado (RF-32, RF-57, RF-79, RF-89); vacío si todavía no registró su RNT. */
    Optional<PrestadorConsultado> consultar(UUID prestadorId);

    /** La cuenta donde recibe la dispersión, completa: solo la usa la dispersión de pagos (RF-54, RF-75). */
    Optional<CuentaCobroVigente> cuentaCobroVigente(UUID prestadorId);

    /** Compara a todos los prestadores con el RNT; la administración la ejecuta (RF-76 a RF-78, DN-36). */
    SincronizacionRnt sincronizarRnt();
}
