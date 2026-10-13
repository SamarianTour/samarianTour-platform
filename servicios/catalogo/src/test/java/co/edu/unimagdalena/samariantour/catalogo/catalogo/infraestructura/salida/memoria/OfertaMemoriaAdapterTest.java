package co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.memoria;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.CriteriosBusqueda;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.NivelEsfuerzo;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Oferta;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Pagina;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Paginacion;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.TipoActividad;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.TipoOferta;
import co.edu.unimagdalena.samariantour.catalogo.compartido.concurrencia.ConflictoConcurrenciaException;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.memoria.DatosSantaMarta.AGENCIA;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.memoria.DatosSantaMarta.APARTAMENTO_POZOS_COLORADOS;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.memoria.DatosSantaMarta.AVISTAMIENTO_AVES_MINCA;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.memoria.DatosSantaMarta.BAUTIZO_BUCEO_TAGANGA;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.memoria.DatosSantaMarta.CAMINATA_CABO_SAN_JUAN;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.memoria.DatosSantaMarta.CAMINATA_POZO_AZUL_MARINKA;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.memoria.DatosSantaMarta.COCINA_CARIBE_CENTRO_HISTORICO;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.memoria.DatosSantaMarta.DESCENSO_BICICLETA_SIERRA;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.memoria.DatosSantaMarta.EXPEDICION_CIUDAD_PERDIDA;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.memoria.DatosSantaMarta.GUIA;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.memoria.DatosSantaMarta.HABITACION_DOBLE_RODADERO;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.memoria.DatosSantaMarta.MINCA;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.memoria.DatosSantaMarta.PAQUETE_MINCA_AVES_Y_CAFE;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.memoria.DatosSantaMarta.PAQUETE_TAGANGA_BAJO_EL_AGUA;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.memoria.DatosSantaMarta.PAQUETE_TAYRONA_MAR_Y_SELVA;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.memoria.DatosSantaMarta.PARRILLA_RODADERO;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.memoria.DatosSantaMarta.PASEO_LANCHA_PLAYA_BLANCA;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.memoria.DatosSantaMarta.PESCADOS_FRENTE_AL_MAR_TAGANGA;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.memoria.DatosSantaMarta.RECORRIDO_CENTRO_HISTORICO;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.memoria.DatosSantaMarta.RUTA_CAFE_CACAO_MINCA;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.memoria.DatosSantaMarta.SNORKEL_PLAYA_CRISTAL;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.memoria.DatosSantaMarta.TAGANGA;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** El adaptador en memoria con los datos de demostración de Santa Marta. */
class OfertaMemoriaAdapterTest {

    private final OfertaMemoriaAdapter ofertas = new OfertaMemoriaAdapter();

    @Test
    @DisplayName("genera identificadores nuevos con formato ObjectId, como MongoDB (DC-30)")
    void nuevoId() {
        String uno = ofertas.nuevoId();
        String otro = ofertas.nuevoId();

        assertThat(ObjectId.isValid(uno)).isTrue();
        assertThat(uno).isNotEqualTo(otro);
    }

    @Test
    @DisplayName("entrega copias: un cambio que no se guarda no queda registrado")
    void guardaYEntregaCopias() {
        Oferta leida = ofertas.buscarPorId(RECORRIDO_CENTRO_HISTORICO).orElseThrow();

        leida.retirar(AGENCIA);
        assertThat(ofertas.buscarPorId(RECORRIDO_CENTRO_HISTORICO).orElseThrow().retirada()).isFalse();

        ofertas.guardar(leida);
        assertThat(ofertas.buscarPorId(RECORRIDO_CENTRO_HISTORICO).orElseThrow().retirada()).isTrue();
    }

    @Test
    @DisplayName("la búsqueda solo trae ofertas visibles, ordenadas por precio")
    void soloVisiblesOrdenadasPorPrecio() {
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
    void filtrosBasicos() {
        assertThat(ids(buscar(TipoOferta.RESTAURANTE, TAGANGA, null, null, false, 100)))
                .containsExactly(PESCADOS_FRENTE_AL_MAR_TAGANGA);

        CriteriosBusqueda hastaCienMil = new CriteriosBusqueda(TipoOferta.ACTIVIDAD, MINCA, null,
                new BigDecimal("100000"), null, null, false, Paginacion.de(null, null));
        assertThat(ids(ofertas.buscarVisibles(hastaCienMil)))
                .containsExactly(RUTA_CAFE_CACAO_MINCA, CAMINATA_POZO_AZUL_MARINKA);
    }

    @Test
    @DisplayName("el filtro por tipo de actividad incluye los paquetes que tienen una de ese tipo (DC-24)")
    void porTipoDeActividad() {
        assertThat(ids(buscar(null, null, TipoActividad.BUCEO_SNORKEL, null, false, 100)))
                .containsExactlyInAnyOrder(SNORKEL_PLAYA_CRISTAL, BAUTIZO_BUCEO_TAGANGA,
                        PAQUETE_TAYRONA_MAR_Y_SELVA, PAQUETE_TAGANGA_BAJO_EL_AGUA);
    }

    @Test
    @DisplayName("el filtro por nivel de esfuerzo es hasta el indicado y deja fuera alojamientos y restaurantes (DC-25)")
    void hastaElNivelDeEsfuerzo() {
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
    void soloAccesibles() {
        assertThat(ids(buscar(null, null, null, null, true, 100)))
                .containsExactlyInAnyOrder(RECORRIDO_CENTRO_HISTORICO, HABITACION_DOBLE_RODADERO,
                        APARTAMENTO_POZOS_COLORADOS, COCINA_CARIBE_CENTRO_HISTORICO, PARRILLA_RODADERO);
    }

    @Test
    @DisplayName("pagina los resultados sin repetir ni saltar ofertas")
    void paginacion() {
        Pagina<Oferta> primera = ofertas.buscarVisibles(criterios(null, null, null, null, false, new Paginacion(0, 5)));
        Pagina<Oferta> segunda = ofertas.buscarVisibles(criterios(null, null, null, null, false, new Paginacion(1, 5)));

        assertThat(primera.contenido()).hasSize(5);
        assertThat(ids(segunda)).hasSize(5).doesNotContainAnyElementsOf(ids(primera));
        assertThat(primera.totalElementos()).isEqualTo(segunda.totalElementos());
    }

    @Test
    @DisplayName("al cambiar el RNT de un prestador, sus ofertas y paquetes dejan de ser visibles y vuelven (RF-77)")
    void actualizarRntPrestador() {
        ofertas.actualizarRntPrestador(AGENCIA, false, 2);
        assertThat(buscar(null, null, null, null, false, 100).contenido())
                .noneMatch(oferta -> oferta.esDe(AGENCIA));

        ofertas.actualizarRntPrestador(AGENCIA, true, 3);
        assertThat(ids(buscar(null, null, null, null, false, 100)))
                .contains(CAMINATA_CABO_SAN_JUAN, PAQUETE_TAYRONA_MAR_Y_SELVA);
    }

    @Test
    @DisplayName("la actualización del RNT solo avanza: una versión atrasada no cambia nada (ADR-05)")
    void actualizarRntSoloAvanza() {
        ofertas.actualizarRntPrestador(AGENCIA, false, 3);

        ofertas.actualizarRntPrestador(AGENCIA, true, 2);

        Oferta caminata = ofertas.buscarPorId(CAMINATA_CABO_SAN_JUAN).orElseThrow();
        assertThat(caminata.rntPrestadorActivo()).isFalse();
        assertThat(caminata.versionHabilitacion()).isEqualTo(3);
    }

    @Test
    @DisplayName("guardar sube la revisión, y guardar con una revisión vieja lanza un conflicto (ADR-05)")
    void bloqueoOptimista() {
        Oferta primeraLectura = ofertas.buscarPorId(CAMINATA_CABO_SAN_JUAN).orElseThrow();
        Oferta segundaLectura = ofertas.buscarPorId(CAMINATA_CABO_SAN_JUAN).orElseThrow();

        ofertas.guardar(primeraLectura);

        assertThat(ofertas.buscarPorId(CAMINATA_CABO_SAN_JUAN).orElseThrow().revision())
                .isEqualTo(primeraLectura.revision() + 1);
        assertThatThrownBy(() -> ofertas.guardar(segundaLectura))
                .isInstanceOf(ConflictoConcurrenciaException.class);
    }

    @Test
    @DisplayName("la actualización masiva del RNT también sube la revisión: una edición cruzada se reintenta")
    void actualizarRntSubeLaRevision() {
        Oferta leida = ofertas.buscarPorId(CAMINATA_CABO_SAN_JUAN).orElseThrow();

        ofertas.actualizarRntPrestador(AGENCIA, false, 5);

        assertThatThrownBy(() -> ofertas.guardar(leida)).isInstanceOf(ConflictoConcurrenciaException.class);
    }

    @Test
    @DisplayName("lista las ofertas de un prestador en cualquier estado, ordenadas por id")
    void porPrestador() {
        Pagina<Oferta> delGuia = ofertas.buscarPorPrestador(GUIA, Paginacion.de(null, null));

        assertThat(ids(delGuia)).containsExactly(AVISTAMIENTO_AVES_MINCA,
                RUTA_CAFE_CACAO_MINCA, CAMINATA_POZO_AZUL_MARINKA,
                DESCENSO_BICICLETA_SIERRA, PAQUETE_MINCA_AVES_Y_CAFE);
        assertThat(ids(ofertas.buscarPorPrestador(AGENCIA, Paginacion.de(null, null))))
                .contains(PASEO_LANCHA_PLAYA_BLANCA);
    }

    private Pagina<Oferta> buscar(TipoOferta tipo, String zonaId, TipoActividad tipoActividad,
                                  NivelEsfuerzo nivelEsfuerzoMaximo, boolean soloAccesibles, int tamano) {
        return ofertas.buscarVisibles(criterios(tipo, zonaId, tipoActividad, nivelEsfuerzoMaximo, soloAccesibles,
                new Paginacion(0, tamano)));
    }

    private static CriteriosBusqueda criterios(TipoOferta tipo, String zonaId, TipoActividad tipoActividad,
                                               NivelEsfuerzo nivelEsfuerzoMaximo, boolean soloAccesibles,
                                               Paginacion paginacion) {
        return new CriteriosBusqueda(tipo, zonaId, null, null, tipoActividad, nivelEsfuerzoMaximo, soloAccesibles,
                paginacion);
    }

    private static List<String> ids(Pagina<Oferta> pagina) {
        return pagina.contenido().stream().map(Oferta::id).toList();
    }
}
