package co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.salida;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.PrestadorCatalogo;

import java.util.Optional;
import java.util.UUID;

/**
 * Almacenamiento de la réplica de habilitación RNT de los prestadores (RF-77).
 */
public interface PrestadorCatalogoRepositoryPort {

    Optional<PrestadorCatalogo> buscarPorId(UUID prestadorId);

    /**
     * Guarda la réplica solo si su versión es mayor que la guardada: la habilitación solo avanza (ADR-05).
     * Si no, lanza ConflictoConcurrenciaException y no guarda nada.
     */
    void guardar(PrestadorCatalogo prestador);

    /** Habilitación del prestador; si aún no llega ningún aviso del núcleo, se trata como no habilitado (DC-02). */
    default PrestadorCatalogo habilitacionDe(UUID prestadorId) {
        return buscarPorId(prestadorId).orElseGet(() -> PrestadorCatalogo.sinAvisos(prestadorId));
    }
}
