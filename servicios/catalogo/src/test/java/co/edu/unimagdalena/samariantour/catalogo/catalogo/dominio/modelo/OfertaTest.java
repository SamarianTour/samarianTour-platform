package co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.DatoInvalidoException;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.OfertaNoEditableException;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.PrestadorNoAutorizadoException;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.RntInactivoException;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.TipoOfertaNoPermitidoException;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.ZonaNoDisponibleException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.AHORA;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.DESPUES;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.OTRO_PRESTADOR;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.PRESTADOR;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.ZONA_INACTIVA;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.actividad;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.actividadPublicada;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.alojamiento;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.contenido;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.habilitado;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.inhabilitado;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.ofertaEnModeracion;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.ofertaPublicada;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.paquete;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.restaurante;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.zonaActiva;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.zonaInactiva;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OfertaTest {

    @Nested
    @DisplayName("Publicar")
    class Publicar {

        @Test
        @DisplayName("una oferta nueva nace con su versión 1 en moderación, sin versión publicada y sin ser visible")
        void naceEnModeracion() {
            Oferta oferta = ofertaEnModeracion();

            assertThat(oferta.tipo()).isEqualTo(TipoOferta.ACTIVIDAD);
            assertThat(oferta.prestadorId()).isEqualTo(PRESTADOR);
            assertThat(oferta.publicada()).isEmpty();
            assertThat(oferta.pendiente()).hasValueSatisfying(version -> {
                assertThat(version.numero()).isEqualTo(1);
                assertThat(version.estadoModeracion()).isEqualTo(EstadoModeracion.EN_MODERACION);
                assertThat(version.moderacion().enviadaEn()).isEqualTo(AHORA);
            });
            assertThat(oferta.estaVisible()).isFalse();
        }

        @Test
        @DisplayName("con el RNT inactivo se rechaza con el código RNT_INACTIVO")
        void rntInactivo() {
            assertThatThrownBy(() -> Oferta.publicar("o", inhabilitado(), TipoPrestador.AGENCIA_OPERADORA,
                    actividad(), zonaActiva(), AHORA))
                    .isInstanceOfSatisfying(RntInactivoException.class,
                            excepcion -> assertThat(excepcion.codigo()).isEqualTo("RNT_INACTIVO"));
        }

        @Test
        @DisplayName("un prestador sin avisos del núcleo se trata como no habilitado")
        void prestadorSinAvisos() {
            assertThatThrownBy(() -> Oferta.publicar("o", PrestadorCatalogo.sinAvisos(PRESTADOR),
                    TipoPrestador.AGENCIA_OPERADORA, actividad(), zonaActiva(), AHORA))
                    .isInstanceOf(RntInactivoException.class);
        }

        @Test
        @DisplayName("el tipo de prestador debe poder publicar ese tipo de oferta")
        void tipoNoPermitido() {
            assertThatThrownBy(() -> Oferta.publicar("o", habilitado(), TipoPrestador.ALOJAMIENTO,
                    actividad(), zonaActiva(), AHORA))
                    .isInstanceOf(TipoOfertaNoPermitidoException.class);
        }

        @Test
        @DisplayName("la zona debe estar activa")
        void zonaInactivaSeRechaza() {
            VersionActividad enZonaInactiva = new VersionActividad(contenido("Tour", ZONA_INACTIVA),
                    TipoActividad.CULTURAL_HISTORICO, NivelEsfuerzo.BAJO, false,
                    actividad().horariosSalida(), null, null, null, null);

            assertThatThrownBy(() -> Oferta.publicar("o", habilitado(), TipoPrestador.AGENCIA_OPERADORA,
                    enZonaInactiva, zonaInactiva(), AHORA))
                    .isInstanceOf(ZonaNoDisponibleException.class);
        }

        @Test
        @DisplayName("un prestador de alojamiento publica un alojamiento")
        void prestadorDeAlojamientoPublicaAlojamiento() {
            Oferta oferta = Oferta.publicar("o", habilitado(), TipoPrestador.ALOJAMIENTO, alojamiento(),
                    zonaActiva(), AHORA);

            assertThat(oferta.tipo()).isEqualTo(TipoOferta.ALOJAMIENTO);
        }
    }

    @Nested
    @DisplayName("Editar")
    class Editar {

        @Test
        @DisplayName("[Sustentación] editar una oferta publicada crea una versión en moderación; la publicada sigue "
                + "visible hasta que se apruebe la nueva")
        void editarPublicadaCreaVersionPendienteYSigueVisible() {
            Oferta oferta = ofertaPublicada();

            oferta.editar(habilitado(), TipoPrestador.AGENCIA_OPERADORA, actividad("Caminata editada"),
                    zonaActiva(), DESPUES);

            assertThat(oferta.pendiente()).hasValueSatisfying(pendiente -> {
                assertThat(pendiente.numero()).isEqualTo(2);
                assertThat(pendiente.estadoModeracion()).isEqualTo(EstadoModeracion.EN_MODERACION);
            });
            assertThat(oferta.publicada()).hasValueSatisfying(publicada -> {
                assertThat(publicada.numero()).isEqualTo(1);
                assertThat(publicada.contenido().nombre()).isEqualTo("Caminata a Cabo San Juan");
            });
            assertThat(oferta.estaVisible()).isTrue();

            oferta.aprobar(2, habilitado(), DESPUES);

            assertThat(oferta.pendiente()).isEmpty();
            assertThat(oferta.publicada()).hasValueSatisfying(publicada ->
                    assertThat(publicada.contenido().nombre()).isEqualTo("Caminata editada"));
        }

        @Test
        @DisplayName("una edición reemplaza a la pendiente con un número nuevo, también si estaba rechazada")
        void reemplazaPendienteConNumeroNuevo() {
            Oferta oferta = ofertaEnModeracion();
            oferta.rechazar(1, "Fotos poco claras", AHORA);

            oferta.editar(habilitado(), TipoPrestador.AGENCIA_OPERADORA, actividad("Segundo intento"),
                    zonaActiva(), DESPUES);

            assertThat(oferta.pendiente()).hasValueSatisfying(pendiente -> {
                assertThat(pendiente.numero()).isEqualTo(2);
                assertThat(pendiente.estadoModeracion()).isEqualTo(EstadoModeracion.EN_MODERACION);
                assertThat(pendiente.moderacion().motivoRechazo()).isNull();
            });
        }

        @Test
        @DisplayName("el número de versión nunca se reutiliza: el aviso de una versión reemplazada no aprueba la nueva")
        void avisoDeVersionReemplazadaSeIgnora() {
            Oferta oferta = ofertaEnModeracion();
            oferta.editar(habilitado(), TipoPrestador.AGENCIA_OPERADORA, actividad("Versión 2"), zonaActiva(), DESPUES);

            boolean aplicado = oferta.aprobar(1, habilitado(), DESPUES);

            assertThat(aplicado).isFalse();
            assertThat(oferta.publicada()).isEmpty();
            assertThat(oferta.pendiente()).hasValueSatisfying(pendiente ->
                    assertThat(pendiente.numero()).isEqualTo(2));
        }

        @Test
        @DisplayName("solo el prestador dueño puede editar")
        void soloElDueno() {
            Oferta oferta = ofertaPublicada();

            assertThatThrownBy(() -> oferta.editar(new PrestadorCatalogo(OTRO_PRESTADOR, true, 1),
                    TipoPrestador.AGENCIA_OPERADORA, actividad(), zonaActiva(), DESPUES))
                    .isInstanceOf(PrestadorNoAutorizadoException.class);
        }

        @Test
        @DisplayName("una oferta retirada no se puede editar")
        void retiradaNoEditable() {
            Oferta oferta = ofertaPublicada();
            oferta.retirar(PRESTADOR);

            assertThatThrownBy(() -> oferta.editar(habilitado(), TipoPrestador.AGENCIA_OPERADORA, actividad(),
                    zonaActiva(), DESPUES))
                    .isInstanceOf(OfertaNoEditableException.class);
        }

        @Test
        @DisplayName("editar con el RNT inactivo se rechaza, porque crea una versión nueva para moderar")
        void rntInactivo() {
            Oferta oferta = ofertaPublicada();

            assertThatThrownBy(() -> oferta.editar(inhabilitado(), TipoPrestador.AGENCIA_OPERADORA, actividad(),
                    zonaActiva(), DESPUES))
                    .isInstanceOf(RntInactivoException.class);
        }

        @Test
        @DisplayName("el tipo de la oferta no cambia después de creada")
        void tipoNoCambia() {
            Oferta oferta = ofertaPublicada();

            assertThatThrownBy(() -> oferta.editar(habilitado(), TipoPrestador.AGENCIA_OPERADORA, alojamiento(),
                    zonaActiva(), DESPUES))
                    .isInstanceOf(DatoInvalidoException.class);
        }
    }

    @Nested
    @DisplayName("Retirar")
    class Retirar {

        @Test
        @DisplayName("retirar es un cambio de estado, no un borrado: conserva sus versiones y deja de ser visible")
        void retirarConservaVersiones() {
            Oferta oferta = ofertaPublicada();

            oferta.retirar(PRESTADOR);

            assertThat(oferta.retirada()).isTrue();
            assertThat(oferta.publicada()).isPresent();
            assertThat(oferta.estaVisible()).isFalse();
        }

        @Test
        @DisplayName("se puede retirar aunque el RNT esté inactivo")
        void retirarSinRnt() {
            Oferta oferta = Oferta.reconstituir("o", TipoOferta.ACTIVIDAD, PRESTADOR, false, false, 2,
                    Oferta.REVISION_NUEVA, null, ofertaEnModeracion().pendiente().orElseThrow());

            oferta.retirar(PRESTADOR);

            assertThat(oferta.retirada()).isTrue();
        }

        @Test
        @DisplayName("solo el prestador dueño puede retirar")
        void soloElDueno() {
            Oferta oferta = ofertaPublicada();

            assertThatThrownBy(() -> oferta.retirar(OTRO_PRESTADOR))
                    .isInstanceOf(PrestadorNoAutorizadoException.class);
        }

        @Test
        @DisplayName("una oferta retirada ignora los avisos de moderación")
        void retiradaIgnoraModeracion() {
            Oferta oferta = ofertaEnModeracion();
            oferta.retirar(PRESTADOR);

            assertThat(oferta.aprobar(1, habilitado(), DESPUES)).isFalse();
            assertThat(oferta.publicada()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Moderar")
    class Moderar {

        @Test
        @DisplayName("aprobar publica la versión pendiente y la oferta pasa a ser visible")
        void aprobar() {
            Oferta oferta = ofertaEnModeracion();

            boolean aplicado = oferta.aprobar(1, habilitado(), DESPUES);

            assertThat(aplicado).isTrue();
            assertThat(oferta.pendiente()).isEmpty();
            assertThat(oferta.publicada()).hasValueSatisfying(publicada -> {
                assertThat(publicada.estadoModeracion()).isEqualTo(EstadoModeracion.APROBADA);
                assertThat(publicada.moderacion().revisadaEn()).isEqualTo(DESPUES);
            });
            assertThat(oferta.estaVisible()).isTrue();
        }

        @Test
        @DisplayName("aprobar dos veces la misma versión no falla ni cambia nada")
        void aprobarEsIdempotente() {
            Oferta oferta = ofertaEnModeracion();
            oferta.aprobar(1, habilitado(), DESPUES);

            boolean segundaVez = oferta.aprobar(1, habilitado(), DESPUES.plusSeconds(60));

            assertThat(segundaVez).isFalse();
            assertThat(oferta.publicada().orElseThrow().moderacion().revisadaEn()).isEqualTo(DESPUES);
        }

        @Test
        @DisplayName("aprobar copia la habilitación vigente: con el RNT inactivo la oferta queda publicada pero no visible")
        void aprobarCopiaHabilitacion() {
            Oferta oferta = ofertaEnModeracion();

            oferta.aprobar(1, inhabilitado(), DESPUES);

            assertThat(oferta.publicada()).isPresent();
            assertThat(oferta.rntPrestadorActivo()).isFalse();
            assertThat(oferta.estaVisible()).isFalse();
        }

        @Test
        @DisplayName("rechazar deja la versión como pendiente, con su motivo, y la publicada no cambia")
        void rechazar() {
            Oferta oferta = ofertaPublicada();
            oferta.editar(habilitado(), TipoPrestador.AGENCIA_OPERADORA, actividad("Edición"), zonaActiva(), DESPUES);

            boolean aplicado = oferta.rechazar(2, "Precio inconsistente", DESPUES);

            assertThat(aplicado).isTrue();
            assertThat(oferta.pendiente()).hasValueSatisfying(pendiente -> {
                assertThat(pendiente.estadoModeracion()).isEqualTo(EstadoModeracion.RECHAZADA);
                assertThat(pendiente.moderacion().motivoRechazo()).isEqualTo("Precio inconsistente");
            });
            assertThat(oferta.publicada().orElseThrow().numero()).isEqualTo(1);
            assertThat(oferta.estaVisible()).isTrue();
        }

        @Test
        @DisplayName("rechazar dos veces la misma versión no falla ni cambia nada")
        void rechazarEsIdempotente() {
            Oferta oferta = ofertaEnModeracion();
            oferta.rechazar(1, "Primer motivo", AHORA);

            boolean segundaVez = oferta.rechazar(1, "Otro motivo", DESPUES);

            assertThat(segundaVez).isFalse();
            assertThat(oferta.pendiente().orElseThrow().moderacion().motivoRechazo()).isEqualTo("Primer motivo");
        }

        @Test
        @DisplayName("rechazar exige un motivo")
        void rechazarExigeMotivo() {
            Oferta oferta = ofertaEnModeracion();

            assertThatThrownBy(() -> oferta.rechazar(1, " ", DESPUES))
                    .isInstanceOf(DatoInvalidoException.class);
        }
    }

    @Nested
    @DisplayName("Paquetes y restaurantes")
    class PaquetesYRestaurantes {

        private final Oferta caminata = actividadPublicada("caminata", NivelEsfuerzo.BAJO, true);
        private final Oferta buceo = actividadPublicada("buceo", NivelEsfuerzo.ALTA_EXIGENCIA, false);

        @Test
        @DisplayName("un paquete se publica con dos o más actividades del mismo prestador (RF-22)")
        void publicarPaquete() {
            Oferta oferta = Oferta.publicar("p", habilitado(), TipoPrestador.GUIA_INDEPENDIENTE,
                    paquete("caminata", "buceo"), zonaActiva(), List.of(caminata, buceo), AHORA);

            assertThat(oferta.tipo()).isEqualTo(TipoOferta.PAQUETE);
            assertThat(oferta.pendiente().orElseThrow().numero()).isEqualTo(1);
        }

        @Test
        @DisplayName("un paquete que referencia una actividad que no es del prestador se rechaza")
        void publicarPaqueteSinSusActividades() {
            assertThatThrownBy(() -> Oferta.publicar("p", habilitado(), TipoPrestador.AGENCIA_OPERADORA,
                    paquete("caminata", "buceo"), zonaActiva(), List.of(caminata), AHORA))
                    .isInstanceOf(DatoInvalidoException.class)
                    .hasMessageContaining("actividadIds");
            assertThatThrownBy(() -> Oferta.publicar("p", habilitado(), TipoPrestador.AGENCIA_OPERADORA,
                    paquete("caminata", "buceo"), zonaActiva(), AHORA))
                    .isInstanceOf(DatoInvalidoException.class);
        }

        @Test
        @DisplayName("editar un paquete vuelve a verificar sus actividades")
        void editarPaquete() {
            Oferta oferta = Oferta.publicar("p", habilitado(), TipoPrestador.AGENCIA_OPERADORA,
                    paquete("caminata", "buceo"), zonaActiva(), List.of(caminata, buceo), AHORA);

            assertThatThrownBy(() -> oferta.editar(habilitado(), TipoPrestador.AGENCIA_OPERADORA,
                    paquete("caminata", "otra"), zonaActiva(), List.of(caminata, buceo), DESPUES))
                    .isInstanceOf(DatoInvalidoException.class)
                    .hasMessageContaining("otra");

            oferta.editar(habilitado(), TipoPrestador.AGENCIA_OPERADORA, paquete("buceo", "caminata"), zonaActiva(),
                    List.of(caminata, buceo), DESPUES);
            assertThat(oferta.pendiente().orElseThrow().numero()).isEqualTo(2);
        }

        @Test
        @DisplayName("un prestador de alojamiento no puede publicar un paquete")
        void paqueteSoloParaAgenciasYGuias() {
            assertThatThrownBy(() -> Oferta.publicar("p", habilitado(), TipoPrestador.ALOJAMIENTO,
                    paquete("caminata", "buceo"), zonaActiva(), List.of(caminata, buceo), AHORA))
                    .isInstanceOf(TipoOfertaNoPermitidoException.class);
        }

        @Test
        @DisplayName("un restaurante publica una oferta de restaurante")
        void publicarRestaurante() {
            Oferta oferta = Oferta.publicar("r", habilitado(), TipoPrestador.RESTAURANTE, restaurante(),
                    zonaActiva(), AHORA);

            assertThat(oferta.tipo()).isEqualTo(TipoOferta.RESTAURANTE);
        }
    }

    @Test
    @DisplayName("una oferta tiene al menos una versión publicada o pendiente")
    void alMenosUnaVersion() {
        assertThatThrownBy(() -> Oferta.reconstituir("o", TipoOferta.ACTIVIDAD, PRESTADOR, false, true, 1,
                Oferta.REVISION_NUEVA, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Nested
    @DisplayName("Habilitación y revisión (ADR-05)")
    class HabilitacionYRevision {

        @Test
        @DisplayName("una oferta nueva copia la versión de la habilitación y nace sin revisión")
        void publicarCopiaLaVersion() {
            Oferta oferta = Oferta.publicar("o", new PrestadorCatalogo(PRESTADOR, true, 4),
                    TipoPrestador.AGENCIA_OPERADORA, actividad(), zonaActiva(), AHORA);

            assertThat(oferta.versionHabilitacion()).isEqualTo(4);
            assertThat(oferta.revision()).isEqualTo(Oferta.REVISION_NUEVA);
        }

        @Test
        @DisplayName("editar copia el RNT y su versión juntos")
        void editarCopiaLaHabilitacion() {
            Oferta oferta = ofertaPublicada();

            oferta.editar(new PrestadorCatalogo(PRESTADOR, true, 7), TipoPrestador.AGENCIA_OPERADORA,
                    actividad("Edición"), zonaActiva(), DESPUES);

            assertThat(oferta.rntPrestadorActivo()).isTrue();
            assertThat(oferta.versionHabilitacion()).isEqualTo(7);
        }

        @Test
        @DisplayName("aprobar copia el RNT y su versión desde la réplica del dueño")
        void aprobarCopiaLaHabilitacion() {
            Oferta oferta = ofertaEnModeracion();

            oferta.aprobar(1, new PrestadorCatalogo(PRESTADOR, false, 9), DESPUES);

            assertThat(oferta.rntPrestadorActivo()).isFalse();
            assertThat(oferta.versionHabilitacion()).isEqualTo(9);
        }

        @Test
        @DisplayName("aprobar con la habilitación de otro prestador es un error de programación")
        void aprobarConOtraHabilitacion() {
            Oferta oferta = ofertaEnModeracion();

            assertThatThrownBy(() -> oferta.aprobar(1, new PrestadorCatalogo(OTRO_PRESTADOR, true, 1), DESPUES))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("publicar y editar no modifican el borrador recibido, así el caso de uso se puede reintentar")
        void borradorReutilizable() {
            VersionActividad borrador = actividad("Borrador");

            Oferta oferta = Oferta.publicar("o", habilitado(), TipoPrestador.AGENCIA_OPERADORA, borrador,
                    zonaActiva(), AHORA);
            oferta.editar(habilitado(), TipoPrestador.AGENCIA_OPERADORA, borrador, zonaActiva(), DESPUES);

            assertThat(borrador.estaEnviada()).isFalse();
            assertThat(oferta.pendiente().orElseThrow().numero()).isEqualTo(2);
        }

        @Test
        @DisplayName("la revisión es opaca: reconstituir la conserva y ninguna regla la cambia")
        void revisionOpaca() {
            Oferta guardada = Oferta.reconstituir("o", TipoOferta.ACTIVIDAD, PRESTADOR, false, true, 1, 12,
                    null, ofertaEnModeracion().pendiente().orElseThrow());

            guardada.aprobar(1, habilitado(), DESPUES);

            assertThat(guardada.revision()).isEqualTo(12);
        }
    }
}
