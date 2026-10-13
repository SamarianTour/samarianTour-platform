package co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.memoria;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.PrestadorCatalogo;
import co.edu.unimagdalena.samariantour.catalogo.compartido.concurrencia.ConflictoConcurrenciaException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PrestadorMemoriaAdapterTest {

    private static final UUID PRESTADOR = UUID.fromString("00000000-0000-0000-0000-0000000000aa");

    private final PrestadorMemoriaAdapter prestadores =
            new PrestadorMemoriaAdapter(List.of(new PrestadorCatalogo(PRESTADOR, true, 2)));

    @Test
    @DisplayName("guarda la réplica cuando su versión es más nueva que la guardada")
    void guardaVersionNueva() {
        prestadores.guardar(new PrestadorCatalogo(PRESTADOR, false, 3));

        assertThat(prestadores.buscarPorId(PRESTADOR)).hasValueSatisfying(replica -> {
            assertThat(replica.rntActivo()).isFalse();
            assertThat(replica.version()).isEqualTo(3);
        });
    }

    @Test
    @DisplayName("la habilitación solo avanza: guardar una versión igual o anterior lanza un conflicto (ADR-05)")
    void noRetrocede() {
        assertThatThrownBy(() -> prestadores.guardar(new PrestadorCatalogo(PRESTADOR, false, 2)))
                .isInstanceOf(ConflictoConcurrenciaException.class);
        assertThatThrownBy(() -> prestadores.guardar(new PrestadorCatalogo(PRESTADOR, false, 1)))
                .isInstanceOf(ConflictoConcurrenciaException.class);
        assertThat(prestadores.buscarPorId(PRESTADOR).orElseThrow().rntActivo()).isTrue();
    }

    @Test
    @DisplayName("un prestador nuevo se guarda con cualquier versión")
    void prestadorNuevo() {
        UUID nuevo = UUID.randomUUID();

        prestadores.guardar(new PrestadorCatalogo(nuevo, true, 1));

        assertThat(prestadores.buscarPorId(nuevo)).isPresent();
    }
}
