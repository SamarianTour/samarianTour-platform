package co.edu.unimagdalena.samariantour.nucleo.prestadores.api;

import java.util.List;
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

    /** Para asignarles el operador (RF-81); con {@code soloSinOperador}, solo los que no tienen. */
    List<PrestadorAdministrado> prestadores(boolean soloSinOperador);

    /**
     * Quien llama ya verificó que el operador es una cuenta activa (DN-39).
     *
     * @return vacío si el prestador todavía no registró su RNT
     */
    Optional<PrestadorAdministrado> asignarOperador(UUID prestadorId, UUID operadorId);

    long prestadoresAsignados(UUID operadorId);
}
