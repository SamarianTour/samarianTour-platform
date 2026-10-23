package co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.ContenidoComun;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Coordenada;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.CriteriosBusqueda;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.ModalidadRecorrido;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.NivelEsfuerzo;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Oferta;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Pagina;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Paginacion;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.PrestadorCatalogo;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.TextoBusqueda;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.TipoActividad;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.TipoOferta;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.TipoPrestador;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.VersionActividad;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Zona;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.salida.OfertaRepositoryPort;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.demo.DatosSantaMarta;
import co.edu.unimagdalena.samariantour.catalogo.compartido.concurrencia.ConflictoConcurrenciaException;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Stream;

import static co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.demo.DatosSantaMarta.AGENCIA;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.demo.DatosSantaMarta.APARTAMENTO_POZOS_COLORADOS;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.demo.DatosSantaMarta.AVISTAMIENTO_AVES_MINCA;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.demo.DatosSantaMarta.BAUTIZO_BUCEO_TAGANGA;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.demo.DatosSantaMarta.CABANA_FRENTE_AL_MAR_BURITACA;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.demo.DatosSantaMarta.CAMINATA_CABO_SAN_JUAN;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.demo.DatosSantaMarta.CAMINATA_POZO_AZUL_MARINKA;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.demo.DatosSantaMarta.COCINA_CARIBE_CENTRO_HISTORICO;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.demo.DatosSantaMarta.DESCENSO_BICICLETA_SIERRA;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.demo.DatosSantaMarta.EXPEDICION_CIUDAD_PERDIDA;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.demo.DatosSantaMarta.GUIA;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.demo.DatosSantaMarta.HABITACION_DOBLE_RODADERO;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.demo.DatosSantaMarta.MINCA;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.demo.DatosSantaMarta.PAQUETE_MINCA_AVES_Y_CAFE;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.demo.DatosSantaMarta.PAQUETE_TAGANGA_BAJO_EL_AGUA;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.demo.DatosSantaMarta.PAQUETE_TAYRONA_MAR_Y_SELVA;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.demo.DatosSantaMarta.PARRILLA_RODADERO;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.demo.DatosSantaMarta.PASEO_LANCHA_PLAYA_BLANCA;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.demo.DatosSantaMarta.PESCADOS_FRENTE_AL_MAR_TAGANGA;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.demo.DatosSantaMarta.RECORRIDO_CENTRO_HISTORICO;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.demo.DatosSantaMarta.RUTA_CAFE_CACAO_MINCA;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.demo.DatosSantaMarta.SNORKEL_PLAYA_CRISTAL;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.demo.DatosSantaMarta.TAGANGA;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.demo.DatosSantaMarta.TAYRONA;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.demo.DatosSantaMarta.TUBING_RIO_BURITACA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Contrato de OfertaRepositoryPort, común a los adaptadores en memoria y de MongoDB: los dos deben comportarse igual
 * con los datos de demostración de Santa Marta. Cada prueba recibe un repositorio que solo tiene esos datos.
 */
public interface ContratoOfertaRepository {

    /** El repositorio de la prueba, cargado con DatosSantaMarta.ofertas() y nada más. */
    OfertaRepositoryPort ofertas();

    @Test
    @DisplayName("genera identificadores nuevos con formato ObjectId (DC-30)")
    default void nuevoId() {
        String uno = ofertas().nuevoId();
        String otro = ofertas().nuevoId();

        assertThat(ObjectId.isValid(uno)).isTrue();
        assertThat(uno).isNotEqualTo(otro);
    }

    @Test
    @DisplayName("devuelve cada oferta como se guardó, con sus versiones, su habilitación y los campos de su tipo")
    default void idaYVuelta() {
        for (Oferta original : DatosSantaMarta.ofertas()) {
            assertThat(ofertas().buscarPorId(original.id())).get()
                    .usingRecursiveComparison()
                    .ignoringFields("revision")
                    .isEqualTo(original);
        }
    }

    @Test
    @DisplayName("guarda una oferta nueva con todos sus campos opcionales, incluida la ubicación")
    default void ofertaNuevaCompleta() {
        Zona tayrona = DatosSantaMarta.zonas().stream().filter(zona -> zona.id().equals(TAYRONA)).findFirst()
                .orElseThrow();
        ContenidoComun contenido = new ContenidoComun("Snorkel en La Piscina", "Arrecife de aguas tranquilas",
                new BigDecimal("95000.50"), "Todos los días", TAYRONA, new Coordenada(11.3301, -74.0389),
                "Desde Cañaveral, 40 minutos a pie", 8);
        VersionActividad actividad = new VersionActividad(contenido, TipoActividad.BUCEO_SNORKEL,
                NivelEsfuerzo.BAJO, true, List.of(LocalTime.of(8, 30), LocalTime.of(13, 0)), "Entrada de Cañaveral",
                240, ModalidadRecorrido.CAMINATA, List.of("es", "en"));
        Oferta nueva = Oferta.publicar(ofertas().nuevoId(), new PrestadorCatalogo(GUIA, true, 4),
                TipoPrestador.GUIA_INDEPENDIENTE, actividad, tayrona, Instant.parse("2026-10-06T12:00:00Z"));

        ofertas().guardar(nueva);

        assertThat(ofertas().buscarPorId(nueva.id())).get()
                .usingRecursiveComparison()
                .ignoringFields("revision")
                .isEqualTo(nueva);
    }

    @Test
    @DisplayName("una versión rechazada conserva su motivo y la fecha de revisión")
    default void versionRechazada() {
        Oferta oferta = ofertas().buscarPorId(PASEO_LANCHA_PLAYA_BLANCA).orElseThrow();
        Instant revisadaEn = Instant.parse("2026-10-05T14:30:00Z");
        String motivo = "Falta el permiso de la capitanía de puerto";
        oferta.rechazar(oferta.pendiente().orElseThrow().numero(), motivo, revisadaEn);

        ofertas().guardar(oferta);

        assertThat(ofertas().buscarPorId(PASEO_LANCHA_PLAYA_BLANCA).orElseThrow().pendiente()).hasValueSatisfying(
                version -> {
                    assertThat(version.moderacion().motivoRechazo()).isEqualTo(motivo);
                    assertThat(version.moderacion().revisadaEn()).isEqualTo(revisadaEn);
                });
    }

    @Test
    @DisplayName("un id que no existe o que no tiene formato ObjectId se trata como inexistente (DC-30)")
    default void idInexistente() {
        assertThat(ofertas().buscarPorId(new ObjectId().toHexString())).isEmpty();
        assertThat(ofertas().buscarPorId("no-es-un-objectid")).isEmpty();
        assertThat(ofertas().buscarPorIds(List.of(CAMINATA_CABO_SAN_JUAN, "no-es-un-objectid",
                new ObjectId().toHexString()))).extracting(Oferta::id).containsExactly(CAMINATA_CABO_SAN_JUAN);
    }

    @Test
    @DisplayName("entrega copias: un cambio que no se guarda no queda registrado")
    default void guardaYEntregaCopias() {
        Oferta leida = ofertas().buscarPorId(RECORRIDO_CENTRO_HISTORICO).orElseThrow();

        leida.retirar(AGENCIA);
        assertThat(ofertas().buscarPorId(RECORRIDO_CENTRO_HISTORICO).orElseThrow().retirada()).isFalse();

        ofertas().guardar(leida);
        assertThat(ofertas().buscarPorId(RECORRIDO_CENTRO_HISTORICO).orElseThrow().retirada()).isTrue();
    }

    @Test
    @DisplayName("la búsqueda solo trae ofertas visibles, ordenadas por precio")
    default void soloVisiblesOrdenadasPorPrecio() {
        Pagina<Oferta> todas = buscar(null, null, null, null, false, 100);

        assertThat(todas.contenido()).extracting(Oferta::id)
                .doesNotContain(PASEO_LANCHA_PLAYA_BLANCA, EXPEDICION_CIUDAD_PERDIDA);
        assertThat(todas.totalElementos()).isEqualTo(DatosSantaMarta.ofertas().size() - 2);
        assertThat(todas.contenido())
                .extracting(oferta -> oferta.publicada().orElseThrow().contenido().precioReferencia())
                .isSorted();
    }

    @Test
    @DisplayName("filtra por tipo, zona y precio")
    default void filtrosBasicos() {
        assertThat(ids(buscar(TipoOferta.RESTAURANTE, TAGANGA, null, null, false, 100)))
                .containsExactly(PESCADOS_FRENTE_AL_MAR_TAGANGA);

        CriteriosBusqueda hastaCienMil = new CriteriosBusqueda(null, TipoOferta.ACTIVIDAD, MINCA, null,
                new BigDecimal("100000"), null, null, false, Paginacion.de(null, null));
        assertThat(ids(ofertas().buscarVisibles(hastaCienMil)))
                .containsExactly(RUTA_CAFE_CACAO_MINCA, CAMINATA_POZO_AZUL_MARINKA);

        CriteriosBusqueda desdeCienMil = new CriteriosBusqueda(null, null, TAYRONA, new BigDecimal("100000"), null, null,
                null, false, Paginacion.de(null, null));
        assertThat(ofertas().buscarVisibles(desdeCienMil).contenido())
                .isNotEmpty()
                .allSatisfy(oferta -> assertThat(oferta.publicada().orElseThrow().contenido().precioReferencia())
                        .isGreaterThanOrEqualTo(new BigDecimal("100000")));
    }

    @Test
    @DisplayName("una zona que no existe o que no tiene formato ObjectId no trae resultados")
    default void zonaInexistente() {
        assertThat(buscar(null, new ObjectId().toHexString(), null, null, false, 100).contenido()).isEmpty();
        assertThat(buscar(null, "no-es-un-objectid", null, null, false, 100).totalElementos()).isZero();
    }

    @Test
    @DisplayName("el filtro por tipo de actividad incluye los paquetes que tienen una de ese tipo (DC-24)")
    default void porTipoDeActividad() {
        assertThat(ids(buscar(null, null, TipoActividad.BUCEO_SNORKEL, null, false, 100)))
                .containsExactlyInAnyOrder(SNORKEL_PLAYA_CRISTAL, BAUTIZO_BUCEO_TAGANGA,
                        PAQUETE_TAYRONA_MAR_Y_SELVA, PAQUETE_TAGANGA_BAJO_EL_AGUA);
    }

    @Test
    @DisplayName("el filtro por nivel de esfuerzo es hasta el indicado y deja fuera alojamientos y restaurantes (DC-25)")
    default void hastaElNivelDeEsfuerzo() {
        Pagina<Oferta> bajas = buscar(null, null, null, NivelEsfuerzo.BAJO, false, 100);

        assertThat(bajas.contenido()).extracting(Oferta::tipo)
                .containsOnly(TipoOferta.ACTIVIDAD, TipoOferta.PAQUETE);
        assertThat(ids(bajas)).contains(SNORKEL_PLAYA_CRISTAL, PAQUETE_MINCA_AVES_Y_CAFE)
                .doesNotContain(CAMINATA_CABO_SAN_JUAN, PAQUETE_TAYRONA_MAR_Y_SELVA,
                        DESCENSO_BICICLETA_SIERRA);
        assertThat(ids(buscar(null, null, null, NivelEsfuerzo.ALTA_EXIGENCIA, false, 100)))
                .contains(DESCENSO_BICICLETA_SIERRA, PAQUETE_TAYRONA_MAR_Y_SELVA);
    }

    @Test
    @DisplayName("filtra las ofertas accesibles en silla de ruedas (RF-17)")
    default void soloAccesibles() {
        assertThat(ids(buscar(null, null, null, null, true, 100)))
                .containsExactlyInAnyOrder(RECORRIDO_CENTRO_HISTORICO, HABITACION_DOBLE_RODADERO,
                        APARTAMENTO_POZOS_COLORADOS, COCINA_CARIBE_CENTRO_HISTORICO, PARRILLA_RODADERO);
    }

    @Test
    @DisplayName("un paquete deja de ser visible si se retira una de sus actividades (RF-22)")
    default void paqueteConActividadRetirada() {
        Oferta caminata = ofertas().buscarPorId(CAMINATA_CABO_SAN_JUAN).orElseThrow();
        caminata.retirar(AGENCIA);
        ofertas().guardar(caminata);

        assertThat(ids(buscar(null, null, null, null, false, 100)))
                .doesNotContain(CAMINATA_CABO_SAN_JUAN, PAQUETE_TAYRONA_MAR_Y_SELVA)
                .contains(PAQUETE_TAGANGA_BAJO_EL_AGUA);
    }

    @Test
    @DisplayName("pagina los resultados sin repetir ni saltar ofertas")
    default void paginacion() {
        Pagina<Oferta> primera = ofertas().buscarVisibles(criterios(null, null, null, null, false,
                new Paginacion(0, 5)));
        Pagina<Oferta> segunda = ofertas().buscarVisibles(criterios(null, null, null, null, false,
                new Paginacion(1, 5)));
        Pagina<Oferta> todas = buscar(null, null, null, null, false, 100);

        assertThat(primera.contenido()).hasSize(5);
        assertThat(ids(segunda)).hasSize(5).doesNotContainAnyElementsOf(ids(primera));
        assertThat(primera.totalElementos()).isEqualTo(segunda.totalElementos()).isEqualTo(todas.totalElementos());
        assertThat(ids(todas).subList(0, 10)).isEqualTo(Stream.concat(ids(primera).stream(),
                ids(segunda).stream()).toList());
    }

    @Test
    @DisplayName("una página más allá del final llega vacía, pero con el total")
    default void paginaVacia() {
        Pagina<Oferta> lejana = ofertas().buscarVisibles(criterios(null, null, null, null, false,
                new Paginacion(50, 20)));

        assertThat(lejana.contenido()).isEmpty();
        assertThat(lejana.totalElementos()).isEqualTo(DatosSantaMarta.ofertas().size() - 2);
    }

    @Test
    @DisplayName("al cambiar el RNT de un prestador, sus ofertas y paquetes dejan de ser visibles y vuelven (RF-77)")
    default void actualizarRntPrestador() {
        ofertas().actualizarRntPrestador(AGENCIA, false, 2);
        assertThat(buscar(null, null, null, null, false, 100).contenido())
                .noneMatch(oferta -> oferta.esDe(AGENCIA));

        ofertas().actualizarRntPrestador(AGENCIA, true, 3);
        assertThat(ids(buscar(null, null, null, null, false, 100)))
                .contains(CAMINATA_CABO_SAN_JUAN, PAQUETE_TAYRONA_MAR_Y_SELVA);
    }

    @Test
    @DisplayName("la actualización del RNT solo avanza: una versión atrasada no cambia nada (ADR-05)")
    default void actualizarRntSoloAvanza() {
        ofertas().actualizarRntPrestador(AGENCIA, false, 3);

        ofertas().actualizarRntPrestador(AGENCIA, true, 2);

        Oferta caminata = ofertas().buscarPorId(CAMINATA_CABO_SAN_JUAN).orElseThrow();
        assertThat(caminata.rntPrestadorActivo()).isFalse();
        assertThat(caminata.versionHabilitacion()).isEqualTo(3);
    }

    @Test
    @DisplayName("guardar sube la revisión, y guardar con una revisión vieja lanza un conflicto (ADR-05)")
    default void bloqueoOptimista() {
        Oferta primeraLectura = ofertas().buscarPorId(CAMINATA_CABO_SAN_JUAN).orElseThrow();
        Oferta segundaLectura = ofertas().buscarPorId(CAMINATA_CABO_SAN_JUAN).orElseThrow();

        ofertas().guardar(primeraLectura);

        assertThat(ofertas().buscarPorId(CAMINATA_CABO_SAN_JUAN).orElseThrow().revision())
                .isEqualTo(primeraLectura.revision() + 1);
        assertThatThrownBy(() -> ofertas().guardar(segundaLectura))
                .isInstanceOf(ConflictoConcurrenciaException.class);
    }

    @Test
    @DisplayName("crear una oferta con un id que ya existe lanza un conflicto y no reemplaza la guardada")
    default void crearConIdExistente() {
        Oferta guardada = ofertas().buscarPorId(CAMINATA_CABO_SAN_JUAN).orElseThrow();
        Oferta nueva = Oferta.reconstituir(guardada.id(), guardada.tipo(), guardada.prestadorId(), true,
                guardada.rntPrestadorActivo(), guardada.versionHabilitacion(), Oferta.REVISION_NUEVA,
                guardada.publicada().orElse(null), guardada.pendiente().orElse(null));

        assertThatThrownBy(() -> ofertas().guardar(nueva)).isInstanceOf(ConflictoConcurrenciaException.class);
        assertThat(ofertas().buscarPorId(CAMINATA_CABO_SAN_JUAN).orElseThrow().retirada()).isFalse();
    }

    @Test
    @DisplayName("la actualización masiva del RNT también sube la revisión: una edición cruzada se reintenta")
    default void actualizarRntSubeLaRevision() {
        Oferta leida = ofertas().buscarPorId(CAMINATA_CABO_SAN_JUAN).orElseThrow();

        ofertas().actualizarRntPrestador(AGENCIA, false, 5);

        assertThatThrownBy(() -> ofertas().guardar(leida)).isInstanceOf(ConflictoConcurrenciaException.class);
    }

    @Test
    @DisplayName("lista las ofertas de un prestador en cualquier estado, ordenadas por id y paginadas")
    default void porPrestador() {
        Pagina<Oferta> delGuia = ofertas().buscarPorPrestador(GUIA, Paginacion.de(null, null));

        assertThat(ids(delGuia)).containsExactly(AVISTAMIENTO_AVES_MINCA,
                RUTA_CAFE_CACAO_MINCA, CAMINATA_POZO_AZUL_MARINKA,
                DESCENSO_BICICLETA_SIERRA, PAQUETE_MINCA_AVES_Y_CAFE);
        assertThat(ids(ofertas().buscarPorPrestador(AGENCIA, Paginacion.de(null, null))))
                .contains(PASEO_LANCHA_PLAYA_BLANCA);

        Pagina<Oferta> segunda = ofertas().buscarPorPrestador(GUIA, new Paginacion(1, 2));
        assertThat(ids(segunda)).containsExactly(CAMINATA_POZO_AZUL_MARINKA, DESCENSO_BICICLETA_SIERRA);
        assertThat(segunda.totalElementos()).isEqualTo(5);
    }

    @Test
    @DisplayName("el texto no distingue mayúsculas ni tildes, en ninguno de los dos sentidos (DC-37)")
    default void textoSinMayusculasNiTildes() {
        assertThat(buscarTexto("CAFE", null)).containsExactly(RUTA_CAFE_CACAO_MINCA, PAQUETE_MINCA_AVES_Y_CAFE);
        assertThat(buscarTexto("río", null)).containsExactly(TUBING_RIO_BURITACA, CABANA_FRENTE_AL_MAR_BURITACA);
        assertThat(buscarTexto("RIO", null)).containsExactly(TUBING_RIO_BURITACA, CABANA_FRENTE_AL_MAR_BURITACA);
    }

    @Test
    @DisplayName("el texto busca palabras incompletas en el nombre y en la descripción, incluida la de un paquete")
    default void textoIncompletoEnNombreYDescripcion() {
        assertThat(buscarTexto("buce", null)).containsExactly(BAUTIZO_BUCEO_TAGANGA, PAQUETE_TAGANGA_BAJO_EL_AGUA);
    }

    @Test
    @DisplayName("con varias palabras deben aparecer todas, aunque unas estén en el nombre y otras en la descripción")
    default void textoConVariasPalabras() {
        assertThat(buscarTexto("kayak manana", null)).containsExactly(PAQUETE_TAGANGA_BAJO_EL_AGUA);
        assertThat(buscarTexto("Taganga   arroz", null)).containsExactly(PESCADOS_FRENTE_AL_MAR_TAGANGA);
    }

    @Test
    @DisplayName("el texto se combina con los demás filtros y nunca trae ofertas que no son visibles")
    default void textoConFiltrosYVisibilidad() {
        assertThat(buscarTexto("caminata", TipoOferta.PAQUETE)).containsExactly(PAQUETE_TAYRONA_MAR_Y_SELVA);
        // El paseo a Playa Blanca también dice "traslado", pero no tiene versión aprobada.
        assertThat(buscarTexto("traslado", null)).containsExactly(SNORKEL_PLAYA_CRISTAL);
        assertThat(buscarTexto("Ciudad Perdida", null)).isEmpty();
    }

    @Test
    @DisplayName("los caracteres especiales del texto se buscan tal cual, sin romper la consulta")
    default void textoConCaracteresEspeciales() {
        assertThat(buscarTexto("(c++ [mar]", null)).isEmpty();
        assertThat(buscarTexto(".*", null)).isEmpty();
    }

    private List<String> buscarTexto(String texto, TipoOferta tipo) {
        return ids(ofertas().buscarVisibles(new CriteriosBusqueda(TextoBusqueda.de(texto), tipo, null, null, null,
                null, null, false, new Paginacion(0, 100))));
    }

    private Pagina<Oferta> buscar(TipoOferta tipo, String zonaId, TipoActividad tipoActividad,
                                  NivelEsfuerzo nivelEsfuerzoMaximo, boolean soloAccesibles, int tamano) {
        return ofertas().buscarVisibles(criterios(tipo, zonaId, tipoActividad, nivelEsfuerzoMaximo, soloAccesibles,
                new Paginacion(0, tamano)));
    }

    private static CriteriosBusqueda criterios(TipoOferta tipo, String zonaId, TipoActividad tipoActividad,
                                               NivelEsfuerzo nivelEsfuerzoMaximo, boolean soloAccesibles,
                                               Paginacion paginacion) {
        return new CriteriosBusqueda(null, tipo, zonaId, null, null, tipoActividad, nivelEsfuerzoMaximo,
                soloAccesibles, paginacion);
    }

    private static List<String> ids(Pagina<Oferta> pagina) {
        return pagina.contenido().stream().map(Oferta::id).toList();
    }
}
