package co.edu.unimagdalena.samariantour.catalogo.catalogo.aplicacion;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.DatoInvalidoException;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.FotoNoEncontradaException;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.PerfilNoEncontradoException;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.DestinoFoto;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Foto;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.PerfilPublico;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.TamanoFoto;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.comando.GuardarPerfilCommand;
import co.edu.unimagdalena.samariantour.catalogo.compartido.concurrencia.ConflictoConcurrenciaException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.OTRO_PRESTADOR;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.PRESTADOR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GestionarPerfilPublicoServiceTest {

    private static final byte[] IMAGEN = {1, 2, 3};

    private final EscenarioAplicacion escenario = new EscenarioAplicacion();
    private final GestionarPerfilPublicoService servicio = escenario.perfilPublico;

    @Nested
    @DisplayName("Datos del perfil (RF-64)")
    class Datos {

        @Test
        @DisplayName("crea el perfil la primera vez y lo publica de inmediato, sin moderación (DC-39)")
        void crear() {
            PerfilPublico creado = servicio.guardar(datos("Aves de Minca", "Guías de aves"));

            assertThat(creado.descripcion()).isEqualTo("Guías de aves");
            assertThat(servicio.consultar(PRESTADOR).nombreComercial()).isEqualTo("Aves de Minca");
        }

        @Test
        @DisplayName("cambiar los datos conserva las fotos")
        void actualizarConservaFotos() {
            servicio.guardar(datos("Antes", "Primera descripción"));
            PerfilPublico conFoto = servicio.agregarFoto(PRESTADOR, IMAGEN);

            servicio.guardar(datos("Después", "Otra descripción"));

            PerfilPublico actual = servicio.consultar(PRESTADOR);
            assertThat(actual.nombreComercial()).isEqualTo("Después");
            assertThat(actual.fotos()).isEqualTo(conFoto.fotos());
        }

        @Test
        @DisplayName("sin descripción no se guarda")
        void sinDescripcion() {
            assertThatThrownBy(() -> servicio.guardar(datos("Sin descripción", " ")))
                    .isInstanceOf(DatoInvalidoException.class)
                    .hasMessageContaining("descripcion");
        }

        @Test
        @DisplayName("un prestador sin perfil recibe PERFIL_NO_ENCONTRADO, y nunca ve el de otro")
        void sinPerfil() {
            servicio.guardar(datos("De otro", "Perfil de otro prestador"));

            assertThatThrownBy(() -> servicio.consultar(OTRO_PRESTADOR))
                    .isInstanceOf(PerfilNoEncontradoException.class);
        }

        @Test
        @DisplayName("si otra escritura se cruza, vuelve a leer el perfil y guarda (ADR-05)")
        void reintenta() {
            servicio.guardar(datos("Antes", "Primera descripción"));
            escenario.perfiles.simularConflictos(2);

            servicio.guardar(datos("Después", "Otra descripción"));

            assertThat(servicio.consultar(PRESTADOR).nombreComercial()).isEqualTo("Después");
        }
    }

    @Nested
    @DisplayName("Fotos (RF-64, RF-72, DC-39)")
    class Fotos {

        @Test
        @DisplayName("guarda la foto en el almacenamiento y la agrega al final del perfil")
        void agregar() {
            servicio.guardar(datos(null, "Guías de aves"));

            PerfilPublico primera = servicio.agregarFoto(PRESTADOR, IMAGEN);
            PerfilPublico segunda = servicio.agregarFoto(PRESTADOR, IMAGEN);

            assertThat(segunda.fotos()).hasSize(2).startsWith(primera.fotos().getFirst());
            assertThat(escenario.fotos.tiene(segunda.fotos().get(1))).isTrue();
            assertThat(escenario.registroFotos.de(segunda.fotos().get(1))).hasValueSatisfying(subida -> {
                assertThat(subida.prestadorId()).isEqualTo(PRESTADOR);
                assertThat(subida.destino()).isEqualTo(DestinoFoto.PERFIL);
            });
            assertThat(escenario.consultaFoto.consultar(segunda.fotos().get(1).id(), TamanoFoto.GALERIA))
                    .isEqualTo(IMAGEN);
        }

        @Test
        @DisplayName("sin perfil, con 6 fotos o con una imagen de más de 5 MB, falla antes de guardar la imagen")
        void fallaAntesDeGuardar() {
            assertThatThrownBy(() -> servicio.agregarFoto(PRESTADOR, IMAGEN))
                    .isInstanceOf(PerfilNoEncontradoException.class);

            servicio.guardar(datos(null, "Guías de aves"));
            assertThatThrownBy(() -> servicio.agregarFoto(PRESTADOR, new byte[Foto.TAMANO_MAXIMO_BYTES + 1]))
                    .isInstanceOf(DatoInvalidoException.class)
                    .hasMessageContaining("5 MB");
            assertThatThrownBy(() -> servicio.agregarFoto(PRESTADOR, new byte[0]))
                    .isInstanceOf(DatoInvalidoException.class);

            for (int i = 0; i < PerfilPublico.MAXIMO_FOTOS; i++) {
                servicio.agregarFoto(PRESTADOR, IMAGEN);
            }
            assertThatThrownBy(() -> servicio.agregarFoto(PRESTADOR, IMAGEN))
                    .isInstanceOf(DatoInvalidoException.class)
                    .hasMessageContaining("6 fotos");
            assertThat(escenario.fotos.cantidad()).isEqualTo(PerfilPublico.MAXIMO_FOTOS);
        }

        @Test
        @DisplayName("una imagen que el almacenamiento rechaza no cambia el perfil")
        void imagenRechazada() {
            servicio.guardar(datos(null, "Guías de aves"));

            assertThatThrownBy(() -> servicio.agregarFoto(PRESTADOR, new byte[] {FileStorageEnMemoria.NO_ES_IMAGEN}))
                    .isInstanceOf(DatoInvalidoException.class);
            assertThat(servicio.consultar(PRESTADOR).fotos()).isEmpty();
        }

        @Test
        @DisplayName("si el perfil no se puede guardar, borra la foto del almacenamiento para no dejarla huérfana")
        void sinHuerfanas() {
            servicio.guardar(datos(null, "Guías de aves"));
            escenario.perfiles.simularConflictos(10);

            int registradasAntes = escenario.registroFotos.cantidad();

            assertThatThrownBy(() -> servicio.agregarFoto(PRESTADOR, IMAGEN))
                    .isInstanceOf(ConflictoConcurrenciaException.class);
            assertThat(escenario.fotos.cantidad()).isZero();
            assertThat(escenario.registroFotos.cantidad()).isEqualTo(registradasAntes);
        }

        @Test
        @DisplayName("quitar una foto la saca del perfil y del almacenamiento; quitarla otra vez no es un error")
        void quitar() {
            servicio.guardar(datos(null, "Guías de aves"));
            Foto foto = servicio.agregarFoto(PRESTADOR, IMAGEN).fotos().getFirst();

            servicio.quitarFoto(PRESTADOR, foto.id());
            servicio.quitarFoto(PRESTADOR, foto.id());
            servicio.quitarFoto(PRESTADOR, "no-es-un-id");

            assertThat(servicio.consultar(PRESTADOR).fotos()).isEmpty();
            assertThat(escenario.fotos.tiene(foto)).isFalse();
            assertThat(escenario.registroFotos.de(foto)).isEmpty();
            assertThatThrownBy(() -> escenario.consultaFoto.consultar(foto.id(), TamanoFoto.GALERIA))
                    .isInstanceOf(FotoNoEncontradaException.class);
        }

        @Test
        @DisplayName("quitar una foto subida antes de DC-47, que no tiene registro, también borra su archivo")
        void quitarFotoSinRegistro() {
            Foto anterior = Foto.nueva();
            escenario.fotos.guardar(anterior, IMAGEN);
            escenario.perfiles.guardar(new PerfilPublico(PRESTADOR, null, "Guías de aves", List.of(anterior), null,
                    null, List.of()));

            servicio.quitarFoto(PRESTADOR, anterior.id());

            assertThat(escenario.fotos.tiene(anterior)).isFalse();
        }

        @Test
        @DisplayName("un prestador no puede quitar la foto de otro: solo busca en su propio perfil")
        void fotoDeOtro() {
            servicio.guardar(datos(null, "Guías de aves"));
            Foto ajena = servicio.agregarFoto(PRESTADOR, IMAGEN).fotos().getFirst();
            servicio.guardar(new GuardarPerfilCommand(OTRO_PRESTADOR, null, "Otro", null, null, List.of()));

            servicio.quitarFoto(OTRO_PRESTADOR, ajena.id());

            assertThat(servicio.consultar(PRESTADOR).fotos()).containsExactly(ajena);
            assertThat(escenario.fotos.tiene(ajena)).isTrue();
        }

        @Test
        @DisplayName("una foto con un identificador mal formado responde como una que no existe")
        void identificadorMalFormado() {
            assertThatThrownBy(() -> escenario.consultaFoto.consultar("../../secreto", TamanoFoto.GALERIA))
                    .isInstanceOf(FotoNoEncontradaException.class);
        }
    }

    private static GuardarPerfilCommand datos(String nombreComercial, String descripcion) {
        return new GuardarPerfilCommand(PRESTADOR, nombreComercial, descripcion, "Lunes a sábado", null, List.of());
    }
}
