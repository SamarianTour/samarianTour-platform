package co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Foto;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.PerfilPublico;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.salida.PerfilRepositoryPort;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.demo.DatosSantaMarta;
import co.edu.unimagdalena.samariantour.catalogo.compartido.concurrencia.ConflictoConcurrenciaException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.demo.DatosSantaMarta.GUIA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Contrato de PerfilRepositoryPort, común a los adaptadores en memoria y de MongoDB. Cada prueba recibe un repositorio
 * que solo tiene los perfiles de DatosSantaMarta, guardados una vez.
 */
public interface ContratoPerfilRepository {

    PerfilRepositoryPort perfiles();

    @Test
    @DisplayName("devuelve cada perfil como se guardó, y nada para un prestador sin perfil")
    default void idaYVuelta() {
        for (PerfilPublico original : DatosSantaMarta.perfiles()) {
            assertThat(perfiles().buscarPorPrestador(original.prestadorId())).get()
                    .usingRecursiveComparison().ignoringFields("revision").isEqualTo(original);
        }
        assertThat(perfiles().buscarPorPrestador(UUID.randomUUID())).isEmpty();
    }

    @Test
    @DisplayName("crea un perfil nuevo con sus fotos, y cada guardado sube la revisión en uno (ADR-05)")
    default void crearYActualizar() {
        UUID prestador = UUID.randomUUID();
        Foto foto = Foto.nueva();
        perfiles().guardar(new PerfilPublico(prestador, "Nuevo", "Recién llegado", List.of(foto), null, null,
                List.of()));

        PerfilPublico creado = perfiles().buscarPorPrestador(prestador).orElseThrow();
        assertThat(creado.fotos()).containsExactly(foto);
        assertThat(creado.revision()).isEqualTo(1);

        Foto otra = Foto.nueva();
        perfiles().guardar(creado.conDatos(null, "Con más fotos", "Todos los días", "https://ejemplo.co",
                List.of("https://ejemplo.co/red")).agregarFoto(otra));

        PerfilPublico actualizado = perfiles().buscarPorPrestador(prestador).orElseThrow();
        assertThat(actualizado.nombreComercial()).isNull();
        assertThat(actualizado.descripcion()).isEqualTo("Con más fotos");
        assertThat(actualizado.horariosAtencion()).isEqualTo("Todos los días");
        assertThat(actualizado.sitioWeb()).isEqualTo("https://ejemplo.co");
        assertThat(actualizado.redesSociales()).containsExactly("https://ejemplo.co/red");
        assertThat(actualizado.fotos()).containsExactly(foto, otra);
        assertThat(actualizado.revision()).isEqualTo(2);
    }

    @Test
    @DisplayName("guardar con una revisión vieja lanza un conflicto y no cambia nada (ADR-05)")
    default void revisionVieja() {
        PerfilPublico leido = perfiles().buscarPorPrestador(GUIA).orElseThrow();
        perfiles().guardar(leido.agregarFoto(Foto.nueva()));

        assertThatThrownBy(() -> perfiles().guardar(leido.conDatos(null, "Pisaría la foto", null, null, null)))
                .isInstanceOf(ConflictoConcurrenciaException.class);
        assertThat(perfiles().buscarPorPrestador(GUIA).orElseThrow().fotos()).hasSize(1);
    }

    @Test
    @DisplayName("crear un segundo perfil para el mismo prestador lanza un conflicto y conserva el primero")
    default void unPerfilPorPrestador() {
        assertThatThrownBy(() -> perfiles().guardar(new PerfilPublico(GUIA, null, "Otro perfil", List.of(), null,
                null, List.of()))).isInstanceOf(ConflictoConcurrenciaException.class);
        assertThat(perfiles().buscarPorPrestador(GUIA).orElseThrow().descripcion()).contains("Minca");
    }
}
