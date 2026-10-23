package co.edu.unimagdalena.samariantour.catalogo.catalogo.aplicacion;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.DestinoFoto;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Foto;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.FotoSubida;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Oferta;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.comando.EditarOfertaCommand;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.comando.GuardarPerfilCommand;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.comando.RetirarOfertaCommand;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.stream.IntStream;

import static co.edu.unimagdalena.samariantour.catalogo.catalogo.aplicacion.EscenarioAplicacion.AGENCIA;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.aplicacion.EscenarioAplicacion.actividad;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.FOTO;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.PRESTADOR;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.ZONA_ACTIVA;
import static org.assertj.core.api.Assertions.assertThat;

class LimpiarFotosSinUsoServiceTest {

    private static final byte[] IMAGEN = {1, 2, 3};
    private static final Duration PASADO_EL_PLAZO = FotoSubida.PLAZO_SIN_USO.plusMinutes(1);

    private final EscenarioAplicacion escenario = new EscenarioAplicacion();
    private final LimpiarFotosSinUsoService servicio = escenario.limpiezaFotos;

    @Test
    @DisplayName("una foto que nunca se usó se conserva 24 horas; después se borran su registro y su archivo")
    void fotoSuelta() {
        Foto suelta = escenario.subirFoto.subir(PRESTADOR, IMAGEN);

        escenario.reloj.adelantar(FotoSubida.PLAZO_SIN_USO.minusMinutes(1));
        servicio.limpiar();
        assertThat(escenario.fotos.tiene(suelta)).isTrue();

        escenario.reloj.adelantar(Duration.ofMinutes(2));
        servicio.limpiar();
        assertThat(escenario.fotos.tiene(suelta)).isFalse();
        assertThat(escenario.registroFotos.de(suelta)).isEmpty();
    }

    @Test
    @DisplayName("una foto que usa una oferta no se borra aunque pase el plazo, y no se vuelve a revisar hasta otro plazo")
    void enUsoPorOferta() {
        Foto foto = escenario.subirFoto.subir(PRESTADOR, IMAGEN);
        escenario.ofertaPublicada(actividad("Tour", "50000", ZONA_ACTIVA, foto));
        escenario.reloj.adelantar(PASADO_EL_PLAZO);

        servicio.limpiar();

        assertThat(escenario.fotos.tiene(foto)).isTrue();
        assertThat(escenario.registroFotos.de(foto).orElseThrow().ultimoUso()).isEqualTo(escenario.reloj.instant());
    }

    @Test
    @DisplayName("las fotos de una versión reemplazada se borran; las de la versión publicada se conservan")
    void versionReemplazada() {
        Oferta oferta = escenario.ofertaPublicada("Tour", "50000", ZONA_ACTIVA);
        Foto primerIntento = escenario.subirFoto.subir(PRESTADOR, IMAGEN);
        escenario.gestion.editar(new EditarOfertaCommand(oferta.id(), AGENCIA,
                actividad("Tour editado", "50000", ZONA_ACTIVA, primerIntento)));
        escenario.gestion.editar(new EditarOfertaCommand(oferta.id(), AGENCIA,
                actividad("Tour editado otra vez", "50000", ZONA_ACTIVA, FOTO)));
        escenario.reloj.adelantar(PASADO_EL_PLAZO);

        servicio.limpiar();

        assertThat(escenario.registroFotos.de(primerIntento)).isEmpty();
        assertThat(escenario.registroFotos.de(FOTO)).isPresent();
    }

    @Test
    @DisplayName("las fotos de una oferta retirada se borran, salvo las que otra oferta todavía usa")
    void ofertaRetirada() {
        Foto soloDeLaRetirada = escenario.subirFoto.subir(PRESTADOR, IMAGEN);
        Oferta retirada = escenario.ofertaPublicada(actividad("Retirada", "50000", ZONA_ACTIVA, soloDeLaRetirada,
                FOTO));
        escenario.publicar(AGENCIA, actividad("Sigue", "50000", ZONA_ACTIVA, FOTO));
        escenario.gestion.retirar(new RetirarOfertaCommand(retirada.id(), PRESTADOR));
        escenario.reloj.adelantar(PASADO_EL_PLAZO);

        servicio.limpiar();

        assertThat(escenario.fotos.tiene(soloDeLaRetirada)).isFalse();
        assertThat(escenario.registroFotos.de(FOTO)).isPresent();
    }

    @Test
    @DisplayName("una foto del perfil se conserva mientras el perfil la tenga; una que quedó sin perfil se borra")
    void fotosDelPerfil() {
        escenario.perfilPublico.guardar(new GuardarPerfilCommand(PRESTADOR, null, "Guías", null, null, List.of()));
        Foto delPerfil = escenario.perfilPublico.agregarFoto(PRESTADOR, IMAGEN).fotos().getFirst();
        Foto perdida = Foto.nueva();
        escenario.registroFotos.registrar(new FotoSubida(perdida, PRESTADOR, DestinoFoto.PERFIL,
                escenario.reloj.instant()));
        escenario.reloj.adelantar(PASADO_EL_PLAZO);

        servicio.limpiar();

        assertThat(escenario.fotos.tiene(delPerfil)).isTrue();
        assertThat(escenario.registroFotos.de(perdida)).isEmpty();
    }

    @Test
    @DisplayName("revisa por lotes: borra todas aunque sean más que un lote")
    void porLotes() {
        List<Foto> sueltas = IntStream.range(0, LimpiarFotosSinUsoService.LOTE + 20)
                .mapToObj(i -> escenario.subirFoto.subir(PRESTADOR, IMAGEN))
                .toList();
        escenario.reloj.adelantar(PASADO_EL_PLAZO);

        int borradas = servicio.limpiar();

        // También borra las dos fotos del escenario, que no usa ninguna oferta.
        assertThat(borradas).isEqualTo(sueltas.size() + 2);
        assertThat(escenario.fotos.cantidad()).isZero();
    }
}
