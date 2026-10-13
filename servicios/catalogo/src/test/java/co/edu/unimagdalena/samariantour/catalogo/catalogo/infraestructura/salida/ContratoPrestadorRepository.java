package co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.PrestadorCatalogo;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.salida.PrestadorCatalogoRepositoryPort;
import co.edu.unimagdalena.samariantour.catalogo.compartido.concurrencia.ConflictoConcurrenciaException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Contrato de PrestadorCatalogoRepositoryPort, común a los adaptadores en memoria y de MongoDB.
 */
public interface ContratoPrestadorRepository {

    UUID PRESTADOR = UUID.fromString("00000000-0000-0000-0000-0000000000aa");

    /** El repositorio de la prueba, que solo tiene a {@link #PRESTADOR} habilitado con la versión 2. */
    PrestadorCatalogoRepositoryPort prestadores();

    @Test
    @DisplayName("guarda la réplica cuando su versión es más nueva que la guardada")
    default void guardaVersionNueva() {
        prestadores().guardar(new PrestadorCatalogo(PRESTADOR, false, 3));

        assertThat(prestadores().buscarPorId(PRESTADOR)).hasValueSatisfying(replica -> {
            assertThat(replica.rntActivo()).isFalse();
            assertThat(replica.version()).isEqualTo(3);
        });
    }

    @Test
    @DisplayName("la habilitación solo avanza: guardar una versión igual o anterior lanza un conflicto (ADR-05)")
    default void noRetrocede() {
        assertThatThrownBy(() -> prestadores().guardar(new PrestadorCatalogo(PRESTADOR, false, 2)))
                .isInstanceOf(ConflictoConcurrenciaException.class);
        assertThatThrownBy(() -> prestadores().guardar(new PrestadorCatalogo(PRESTADOR, false, 1)))
                .isInstanceOf(ConflictoConcurrenciaException.class);
        assertThat(prestadores().buscarPorId(PRESTADOR)).hasValueSatisfying(replica -> {
            assertThat(replica.rntActivo()).isTrue();
            assertThat(replica.version()).isEqualTo(2);
        });
    }

    @Test
    @DisplayName("un prestador nuevo se guarda con cualquier versión; uno sin avisos no existe")
    default void prestadorNuevo() {
        UUID nuevo = UUID.randomUUID();
        assertThat(prestadores().buscarPorId(nuevo)).isEmpty();

        prestadores().guardar(new PrestadorCatalogo(nuevo, true, 1));

        assertThat(prestadores().buscarPorId(nuevo)).hasValueSatisfying(replica -> {
            assertThat(replica.prestadorId()).isEqualTo(nuevo);
            assertThat(replica.rntActivo()).isTrue();
        });
    }
}
