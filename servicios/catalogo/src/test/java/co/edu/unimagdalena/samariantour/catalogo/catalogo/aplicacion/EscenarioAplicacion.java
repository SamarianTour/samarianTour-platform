package co.edu.unimagdalena.samariantour.catalogo.catalogo.aplicacion;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.ContenidoComun;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.CriteriosBusqueda;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.NivelEsfuerzo;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Oferta;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Pagina;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Paginacion;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.TipoActividad;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.TipoPrestador;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.VersionActividad;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.VersionOferta;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Zona;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.comando.AvisoHabilitacion;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.comando.AvisoModeracion;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.comando.DecisionModeracion;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.comando.PublicarOfertaCommand;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.comando.ResultadoAviso;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.comando.Solicitante;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.AHORA;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.OTRO_PRESTADOR;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.PRESTADOR;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.zonaActiva;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.zonaInactiva;

/**
 * Los cinco servicios de aplicación conectados a dobles en memoria de los puertos de salida, con la hora fija.
 * Reúne las acciones que se repiten en las pruebas (habilitar, publicar, moderar).
 */
class EscenarioAplicacion {

    static final String ZONA_TAGANGA = "taganga";
    static final Solicitante AGENCIA = new Solicitante(PRESTADOR, TipoPrestador.AGENCIA_OPERADORA);
    static final Solicitante OTRA_AGENCIA = new Solicitante(OTRO_PRESTADOR, TipoPrestador.AGENCIA_OPERADORA);

    final OfertaRepositoryEnMemoria ofertas = new OfertaRepositoryEnMemoria();
    final ZonaRepositoryEnMemoria zonas = new ZonaRepositoryEnMemoria(zonaActiva(), zonaInactiva(),
            new Zona(ZONA_TAGANGA, "Taganga", null, null, false, true, true, List.of(), null));
    final PrestadorCatalogoRepositoryEnMemoria prestadores = new PrestadorCatalogoRepositoryEnMemoria();
    final AvisoProcesadoEnMemoria avisosProcesados = new AvisoProcesadoEnMemoria();
    private final Clock reloj = Clock.fixed(AHORA, ZoneOffset.UTC);

    final GestionarOfertaService gestion = new GestionarOfertaService(ofertas, zonas, prestadores, reloj);
    final BuscarOfertasService busqueda = new BuscarOfertasService(ofertas);
    final ConsultarFichaOfertaService ficha = new ConsultarFichaOfertaService(ofertas);
    final ConsultarZonasService consultaZonas = new ConsultarZonasService(zonas);
    final AplicarAvisoNucleoService avisos = new AplicarAvisoNucleoService(ofertas, prestadores, avisosProcesados,
            reloj);

    private int secuenciaAvisos;

    ResultadoAviso habilitar(UUID prestadorId, boolean rntActivo, long version) {
        return avisos.aplicar(new AvisoHabilitacion(siguienteIdAviso(), prestadorId, rntActivo, version));
    }

    ResultadoAviso moderar(String ofertaId, int numeroVersion, DecisionModeracion decision) {
        String motivo = decision == DecisionModeracion.RECHAZAR ? "Motivo de prueba" : null;
        return avisos.aplicar(new AvisoModeracion(siguienteIdAviso(), ofertaId, numeroVersion, decision, motivo));
    }

    Oferta publicar(Solicitante solicitante, VersionOferta contenido) {
        return gestion.publicar(new PublicarOfertaCommand(solicitante, contenido));
    }

    /** Publica una actividad de la agencia de prueba (habilitándola si hace falta) y aprueba su versión 1. */
    Oferta ofertaPublicada(String nombre, String precio, String zonaId) {
        if (prestadores.buscarPorId(PRESTADOR).isEmpty()) {
            habilitar(PRESTADOR, true, 1);
        }
        Oferta oferta = publicar(AGENCIA, actividad(nombre, precio, zonaId));
        moderar(oferta.id(), 1, DecisionModeracion.APROBAR);
        return oferta;
    }

    Pagina<Oferta> buscarTodas() {
        return busqueda.buscar(new CriteriosBusqueda(null, null, null, null, Paginacion.de(null, null)));
    }

    Oferta guardada(String ofertaId) {
        return ofertas.buscarPorId(ofertaId).orElseThrow();
    }

    String siguienteIdAviso() {
        return "aviso-" + ++secuenciaAvisos;
    }

    static String nombrePublicado(Oferta oferta) {
        return oferta.publicada().orElseThrow().contenido().nombre();
    }

    static VersionActividad actividad(String nombre, String precio, String zonaId) {
        ContenidoComun contenido = new ContenidoComun(nombre, "Descripción de " + nombre, new BigDecimal(precio),
                null, zonaId, null, null, 10);
        return new VersionActividad(contenido, TipoActividad.SENDERISMO, NivelEsfuerzo.MODERADO, false,
                List.of(LocalTime.of(7, 0)), null, null, null, List.of());
    }
}
