package co.edu.unimagdalena.samariantour.catalogo.catalogo.aplicacion;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.DatoInvalidoException;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.OfertaNoEncontradaException;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.PrestadorNoAutorizadoException;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.RntInactivoException;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.TipoOfertaNoPermitidoException;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.ZonaNoDisponibleException;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.EstadoModeracion;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Foto;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Oferta;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Pagina;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Paginacion;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.TipoOferta;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.TipoPrestador;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.comando.DecisionModeracion;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.comando.EditarOfertaCommand;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.comando.GuardarPerfilCommand;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.comando.RetirarOfertaCommand;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.comando.Solicitante;
import co.edu.unimagdalena.samariantour.catalogo.compartido.concurrencia.ConflictoConcurrenciaException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static co.edu.unimagdalena.samariantour.catalogo.catalogo.aplicacion.EscenarioAplicacion.AGENCIA;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.aplicacion.EscenarioAplicacion.OTRA_AGENCIA;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.aplicacion.EscenarioAplicacion.actividad;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.AHORA;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.FOTO;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.FOTO_OTRO_PRESTADOR;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.OTRO_PRESTADOR;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.PRESTADOR;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.ZONA_ACTIVA;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.ZONA_INACTIVA;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.alojamiento;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.paquete;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.restaurante;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GestionarOfertaServiceTest {

    private final EscenarioAplicacion escenario = new EscenarioAplicacion();
    private final GestionarOfertaService servicio = escenario.gestion;

    @Nested
    @DisplayName("Publicar")
    class Publicar {

        @Test
        @DisplayName("guarda la oferta con un id nuevo y su versión 1 en moderación")
        void guarda() {
            escenario.habilitar(PRESTADOR, true, 1);

            Oferta oferta = escenario.publicar(AGENCIA, actividad("Tour", "50000", ZONA_ACTIVA));

            assertThat(oferta.id()).isNotBlank();
            assertThat(escenario.ofertas.vecesGuardada(oferta.id())).isEqualTo(1);
            assertThat(oferta.pendiente().orElseThrow().moderacion().enviadaEn()).isEqualTo(AHORA);
        }

        @Test
        @DisplayName("un prestador del que no ha llegado aviso de habilitación no puede publicar")
        void sinAvisoDeHabilitacion() {
            assertThatThrownBy(() -> escenario.publicar(AGENCIA, actividad("Tour", "50000", ZONA_ACTIVA)))
                    .isInstanceOf(RntInactivoException.class);
            assertThat(escenario.ofertas.total()).isZero();
        }

        @Test
        @DisplayName("la zona debe existir")
        void zonaInexistente() {
            escenario.habilitar(PRESTADOR, true, 1);

            assertThatThrownBy(() -> escenario.publicar(AGENCIA, actividad("Tour", "50000", "no-existe")))
                    .isInstanceOf(ZonaNoDisponibleException.class);
        }

        @Test
        @DisplayName("la zona debe estar activa")
        void zonaDebeEstarActiva() {
            escenario.habilitar(PRESTADOR, true, 1);

            assertThatThrownBy(() -> escenario.publicar(AGENCIA, actividad("Tour", "50000", ZONA_INACTIVA)))
                    .isInstanceOf(ZonaNoDisponibleException.class);
        }

        @Test
        @DisplayName("el tipo de prestador del token decide qué puede publicar")
        void tipoDePrestador() {
            escenario.habilitar(PRESTADOR, true, 1);

            assertThatThrownBy(() -> escenario.publicar(AGENCIA, alojamiento()))
                    .isInstanceOf(TipoOfertaNoPermitidoException.class);
        }

        @Test
        @DisplayName("un paquete se publica con actividades del mismo prestador (RF-22)")
        void publicarPaquete() {
            Oferta caminata = escenario.ofertaPublicada("Caminata", "50000", ZONA_ACTIVA);
            Oferta kayak = escenario.ofertaPublicada("Kayak", "70000", ZONA_ACTIVA);

            Oferta oferta = escenario.publicar(AGENCIA, paquete(caminata.id(), kayak.id()));

            assertThat(oferta.tipo()).isEqualTo(TipoOferta.PAQUETE);
            assertThat(escenario.ofertas.vecesGuardada(oferta.id())).isEqualTo(1);
        }

        @Test
        @DisplayName("un paquete con una actividad inexistente o de otro prestador se rechaza y no se guarda")
        void paqueteConActividadAjena() {
            Oferta caminata = escenario.ofertaPublicada("Caminata", "50000", ZONA_ACTIVA);
            escenario.habilitar(OTRO_PRESTADOR, true, 1);
            Oferta ajena = escenario.publicar(OTRA_AGENCIA,
                    actividad("De otro", "70000", ZONA_ACTIVA, FOTO_OTRO_PRESTADOR));
            int totalAntes = escenario.ofertas.total();

            assertThatThrownBy(() -> escenario.publicar(AGENCIA, paquete(caminata.id(), "no-existe")))
                    .isInstanceOf(DatoInvalidoException.class);
            assertThatThrownBy(() -> escenario.publicar(AGENCIA, paquete(caminata.id(), ajena.id())))
                    .isInstanceOf(DatoInvalidoException.class);
            assertThat(escenario.ofertas.total()).isEqualTo(totalAntes);
        }

        @Test
        @DisplayName("un paquete no puede incluir una actividad retirada, pero sí una aún sin aprobar (DC-23)")
        void paqueteConActividadRetiradaOSinAprobar() {
            Oferta caminata = escenario.ofertaPublicada("Caminata", "50000", ZONA_ACTIVA);
            Oferta retirada = escenario.ofertaPublicada("Kayak", "70000", ZONA_ACTIVA);
            servicio.retirar(new RetirarOfertaCommand(retirada.id(), PRESTADOR));
            Oferta sinAprobar = escenario.publicar(AGENCIA, actividad("Buceo", "90000", ZONA_ACTIVA));

            assertThatThrownBy(() -> escenario.publicar(AGENCIA, paquete(caminata.id(), retirada.id())))
                    .isInstanceOf(DatoInvalidoException.class)
                    .hasMessageContaining("retirada");

            Oferta oferta = escenario.publicar(AGENCIA, paquete(caminata.id(), sinAprobar.id()));
            assertThat(oferta.tipo()).isEqualTo(TipoOferta.PAQUETE);
        }

        @Test
        @DisplayName("un restaurante publica una oferta de restaurante")
        void publicarRestaurante() {
            escenario.habilitar(PRESTADOR, true, 1);

            Oferta oferta = escenario.publicar(new Solicitante(PRESTADOR, TipoPrestador.RESTAURANTE), restaurante());

            assertThat(oferta.tipo()).isEqualTo(TipoOferta.RESTAURANTE);
        }
    }

    @Nested
    @DisplayName("Editar y retirar")
    class EditarYRetirar {

        @Test
        @DisplayName("editar guarda la nueva versión pendiente")
        void editarGuarda() {
            Oferta oferta = escenario.ofertaPublicada("Tour", "50000", ZONA_ACTIVA);
            int guardadasAntes = escenario.ofertas.vecesGuardada(oferta.id());

            Oferta editada = servicio.editar(new EditarOfertaCommand(oferta.id(), AGENCIA,
                    actividad("Tour editado", "55000", ZONA_ACTIVA)));

            assertThat(editada.pendiente().orElseThrow().numero()).isEqualTo(2);
            assertThat(escenario.ofertas.vecesGuardada(oferta.id())).isEqualTo(guardadasAntes + 1);
        }

        @Test
        @DisplayName("editar una oferta que no existe responde no encontrada")
        void editarInexistente() {
            assertThatThrownBy(() -> servicio.editar(new EditarOfertaCommand("no-existe", AGENCIA,
                    actividad("Tour", "50000", ZONA_ACTIVA))))
                    .isInstanceOf(OfertaNoEncontradaException.class);
        }

        @Test
        @DisplayName("otro prestador no puede editar ni retirar la oferta")
        void otroPrestador() {
            Oferta oferta = escenario.ofertaPublicada("Tour", "50000", ZONA_ACTIVA);
            escenario.habilitar(OTRO_PRESTADOR, true, 1);

            assertThatThrownBy(() -> servicio.editar(new EditarOfertaCommand(oferta.id(), OTRA_AGENCIA,
                    actividad("Tour", "50000", ZONA_ACTIVA))))
                    .isInstanceOf(PrestadorNoAutorizadoException.class);
            assertThatThrownBy(() -> servicio.retirar(new RetirarOfertaCommand(oferta.id(), OTRO_PRESTADOR)))
                    .isInstanceOf(PrestadorNoAutorizadoException.class);
        }

        @Test
        @DisplayName("retirar guarda la oferta como retirada, sin borrarla")
        void retirarGuarda() {
            Oferta oferta = escenario.ofertaPublicada("Tour", "50000", ZONA_ACTIVA);

            servicio.retirar(new RetirarOfertaCommand(oferta.id(), PRESTADOR));

            assertThat(escenario.guardada(oferta.id()).retirada()).isTrue();
        }

        @Test
        @DisplayName("el prestador ve sus ofertas en cualquier estado, y no las de otros")
        void listarMias() {
            Oferta publicada = escenario.ofertaPublicada("Publicada", "50000", ZONA_ACTIVA);
            Oferta enModeracion = escenario.publicar(AGENCIA, actividad("En moderación", "60000", ZONA_ACTIVA));
            escenario.habilitar(OTRO_PRESTADOR, true, 1);
            escenario.publicar(OTRA_AGENCIA, actividad("De otro", "70000", ZONA_ACTIVA, FOTO_OTRO_PRESTADOR));

            Pagina<Oferta> mias = servicio.listarMias(PRESTADOR, Paginacion.de(null, null));

            assertThat(mias.contenido()).extracting(Oferta::id)
                    .containsExactlyInAnyOrder(publicada.id(), enModeracion.id());
        }
    }

    @Nested
    @DisplayName("Concurrencia (ADR-05)")
    class Concurrencia {

        @Test
        @DisplayName("si otra escritura se cruza, la edición se reintenta completa y se aplica una sola vez")
        void edicionSeReintenta() {
            Oferta oferta = escenario.ofertaPublicada("Tour", "50000", ZONA_ACTIVA);
            escenario.ofertas.simularConflictos(2);

            servicio.editar(new EditarOfertaCommand(oferta.id(), AGENCIA,
                    actividad("Tour editado", "55000", ZONA_ACTIVA)));

            // Si el reintento reutilizara la oferta del intento fallido, la versión pendiente sería la 4, no la 2.
            assertThat(escenario.guardada(oferta.id()).pendiente().orElseThrow().numero()).isEqualTo(2);
        }

        @Test
        @DisplayName("si el conflicto sigue después de 3 intentos, la edición falla sin cambiar nada")
        void edicionAgotaLosIntentos() {
            Oferta oferta = escenario.ofertaPublicada("Tour", "50000", ZONA_ACTIVA);
            escenario.ofertas.simularConflictos(3);

            assertThatThrownBy(() -> servicio.editar(new EditarOfertaCommand(oferta.id(), AGENCIA,
                    actividad("Tour editado", "55000", ZONA_ACTIVA))))
                    .isInstanceOf(ConflictoConcurrenciaException.class);
            assertThat(escenario.guardada(oferta.id()).pendiente()).isEmpty();
        }

        @Test
        @DisplayName("una edición basada en una lectura vieja no pisa el cambio de otra réplica")
        void lecturaViejaNoPisa() {
            Oferta oferta = escenario.ofertaPublicada("Tour", "50000", ZONA_ACTIVA);
            Oferta leidaAntes = escenario.guardada(oferta.id());
            servicio.retirar(new RetirarOfertaCommand(oferta.id(), PRESTADOR));

            leidaAntes.editar(escenario.prestadores.habilitacionDe(PRESTADOR), TipoPrestador.AGENCIA_OPERADORA,
                    actividad("Edición tardía", "60000", ZONA_ACTIVA), escenario.zonas.buscarPorId(ZONA_ACTIVA)
                            .orElseThrow(), AHORA);

            assertThatThrownBy(() -> escenario.ofertas.guardar(leidaAntes))
                    .isInstanceOf(ConflictoConcurrenciaException.class);
            assertThat(escenario.guardada(oferta.id()).retirada()).isTrue();
        }

        @Test
        @DisplayName("retirar también se reintenta ante un conflicto")
        void retiroSeReintenta() {
            Oferta oferta = escenario.ofertaPublicada("Tour", "50000", ZONA_ACTIVA);
            escenario.ofertas.simularConflictos(1);

            servicio.retirar(new RetirarOfertaCommand(oferta.id(), PRESTADOR));

            assertThat(escenario.guardada(oferta.id()).retirada()).isTrue();
        }
    }

    @Nested
    @DisplayName("Fotos (DC-47)")
    class Fotos {

        private static final byte[] IMAGEN = {1, 2, 3};

        @Test
        @DisplayName("publicar con una foto que el prestador subió la marca como usada en ese momento")
        void marcaEnUso() {
            escenario.habilitar(PRESTADOR, true, 1);
            Foto subida = escenario.subirFoto.subir(PRESTADOR, IMAGEN);
            escenario.reloj.adelantar(Duration.ofHours(23));

            Oferta oferta = escenario.publicar(AGENCIA, actividad("Tour", "50000", ZONA_ACTIVA, subida));

            assertThat(oferta.pendiente().orElseThrow().contenido().fotos()).containsExactly(subida);
            assertThat(escenario.registroFotos.de(subida).orElseThrow().ultimoUso())
                    .isEqualTo(AHORA.plus(Duration.ofHours(23)));
        }

        @Test
        @DisplayName("una foto de otro prestador, una que no existe o una del perfil no sirven, y no se guarda nada")
        void fotosQueNoSirven() {
            escenario.habilitar(PRESTADOR, true, 1);
            escenario.perfilPublico.guardar(new GuardarPerfilCommand(PRESTADOR, null, "Guías", null, null, null));
            Foto delPerfil = escenario.perfilPublico.agregarFoto(PRESTADOR, IMAGEN).fotos().getFirst();

            for (Foto foto : new Foto[]{FOTO_OTRO_PRESTADOR, Foto.nueva(), delPerfil}) {
                assertThatThrownBy(() -> escenario.publicar(AGENCIA, actividad("Tour", "50000", ZONA_ACTIVA, foto)))
                        .isInstanceOf(DatoInvalidoException.class)
                        .hasMessageContaining("fotos: incluye una foto que no existe o no es tuya");
            }
            assertThat(escenario.ofertas.total()).isZero();
        }

        @Test
        @DisplayName("editar también comprueba que las fotos sean del prestador")
        void editarConFotoAjena() {
            Oferta oferta = escenario.ofertaPublicada("Tour", "50000", ZONA_ACTIVA);

            assertThatThrownBy(() -> servicio.editar(new EditarOfertaCommand(oferta.id(), AGENCIA,
                    actividad("Tour editado", "60000", ZONA_ACTIVA, FOTO_OTRO_PRESTADOR))))
                    .isInstanceOf(DatoInvalidoException.class);
            assertThat(escenario.guardada(oferta.id()).pendiente()).isEmpty();
        }

        @Test
        @DisplayName("una misma foto puede ir en varias ofertas del prestador")
        void variasOfertas() {
            Oferta primera = escenario.ofertaPublicada("Tour", "50000", ZONA_ACTIVA);

            Oferta segunda = escenario.publicar(AGENCIA, actividad("Otro tour", "60000", ZONA_ACTIVA, FOTO));

            assertThat(primera.pendiente().or(primera::publicada).orElseThrow().contenido().fotos())
                    .containsExactly(FOTO);
            assertThat(segunda.pendiente().orElseThrow().contenido().fotos()).containsExactly(FOTO);
        }
    }

    @Nested
    @DisplayName("Consultar una oferta propia (DC-50)")
    class ConsultarMia {

        @Test
        @DisplayName("devuelve una oferta rechazada que nunca se publicó, con su motivo, para corregirla")
        void rechazadaSinPublicar() {
            escenario.habilitar(PRESTADOR, true, 1);
            Oferta nueva = escenario.publicar(AGENCIA, actividad("Tour", "50000", ZONA_ACTIVA));
            escenario.moderar(nueva.id(), 1, DecisionModeracion.RECHAZAR);

            Oferta mia = servicio.consultarMia(nueva.id(), PRESTADOR);

            assertThat(mia.publicada()).isEmpty();
            assertThat(mia.pendiente()).hasValueSatisfying(pendiente -> {
                assertThat(pendiente.estadoModeracion()).isEqualTo(EstadoModeracion.RECHAZADA);
                assertThat(pendiente.moderacion().motivoRechazo()).isEqualTo("Motivo de prueba");
                assertThat(pendiente.contenido().nombre()).isEqualTo("Tour");
            });
        }

        @Test
        @DisplayName("también devuelve una oferta retirada")
        void retirada() {
            Oferta oferta = escenario.ofertaPublicada("Tour", "50000", ZONA_ACTIVA);
            servicio.retirar(new RetirarOfertaCommand(oferta.id(), PRESTADOR));

            assertThat(servicio.consultarMia(oferta.id(), PRESTADOR).retirada()).isTrue();
        }

        @Test
        @DisplayName("la oferta de otro prestador no se muestra, y una que no existe responde no encontrada")
        void ajenaOInexistente() {
            Oferta oferta = escenario.ofertaPublicada("Tour", "50000", ZONA_ACTIVA);

            assertThatThrownBy(() -> servicio.consultarMia(oferta.id(), OTRO_PRESTADOR))
                    .isInstanceOf(PrestadorNoAutorizadoException.class);
            assertThatThrownBy(() -> servicio.consultarMia("no-existe", PRESTADOR))
                    .isInstanceOf(OfertaNoEncontradaException.class);
        }
    }
}
