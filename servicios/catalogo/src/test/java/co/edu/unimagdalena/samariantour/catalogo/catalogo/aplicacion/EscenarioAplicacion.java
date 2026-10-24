package co.edu.unimagdalena.samariantour.catalogo.catalogo.aplicacion;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.ContenidoComun;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.CriteriosBusqueda;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.DestinoFoto;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Foto;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.FotoSubida;
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

import co.edu.unimagdalena.samariantour.catalogo.compartido.concurrencia.EjecutorConReintento;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.AHORA;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.FOTO;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.FOTO_OTRO_PRESTADOR;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.OTRO_PRESTADOR;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.PRESTADOR;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.zonaActiva;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.zonaInactiva;

/**
 * Los servicios de aplicación conectados a dobles en memoria de los puertos de salida, con la hora fija.
 * Reúne las acciones que se repiten en las pruebas (habilitar, publicar, moderar).
 *
 * <p>Cada prestador de prueba ya subió una foto para sus ofertas: el PRESTADOR, {@code FOTO}, y el OTRO_PRESTADOR,
 * {@code FOTO_OTRO_PRESTADOR} (DC-47).
 */
class EscenarioAplicacion {

    /** Zona activa sin alta afluencia; la de DatosDePrueba (ZONA_ACTIVA) sí la tiene. */
    static final String ZONA_TAGANGA = "taganga";
    static final Solicitante AGENCIA = new Solicitante(PRESTADOR, TipoPrestador.AGENCIA_OPERADORA);
    static final Solicitante OTRA_AGENCIA = new Solicitante(OTRO_PRESTADOR, TipoPrestador.AGENCIA_OPERADORA);

    final OfertaRepositoryEnMemoria ofertas = new OfertaRepositoryEnMemoria();
    final ZonaRepositoryEnMemoria zonas = new ZonaRepositoryEnMemoria(zonaActiva(), zonaInactiva(),
            new Zona(ZONA_TAGANGA, "Taganga", null, null, false, false, true, List.of(), null));
    final PrestadorCatalogoRepositoryEnMemoria prestadores = new PrestadorCatalogoRepositoryEnMemoria();
    final PerfilRepositoryEnMemoria perfiles = new PerfilRepositoryEnMemoria();
    final AvisoProcesadoEnMemoria avisosProcesados = new AvisoProcesadoEnMemoria();
    final FotoSubidaRepositoryEnMemoria registroFotos = new FotoSubidaRepositoryEnMemoria();
    final FileStorageEnMemoria fotos = new FileStorageEnMemoria();
    /** Empieza en AHORA y solo avanza si la prueba lo adelanta. */
    final RelojAjustable reloj = new RelojAjustable(AHORA);
    /** Los mismos 3 intentos que en producción, sin esperar entre ellos. */
    private final EjecutorConReintento reintento = new EjecutorConReintento(3, Duration.ZERO);

    final GestionarOfertaService gestion = new GestionarOfertaService(ofertas, zonas, prestadores, registroFotos,
            reloj, reintento);
    final BuscarOfertasService busqueda = new BuscarOfertasService(ofertas);
    final ConsultarFichaOfertaService ficha = new ConsultarFichaOfertaService(ofertas, zonas, perfiles);
    final ConsultarOfertaInternaService consultaInterna = new ConsultarOfertaInternaService(ofertas);
    final ConsultarOfertaParaModulosService paraModulos = new ConsultarOfertaParaModulosService(ofertas);
    final ConsultarOfertasVisiblesService visibles = new ConsultarOfertasVisiblesService(ofertas, zonas);
    final ConsultarZonasService consultaZonas = new ConsultarZonasService(zonas);
    final GestionarZonasService gestionZonas = new GestionarZonasService(zonas);
    final ListarPendientesModeracionService moderacion = new ListarPendientesModeracionService(ofertas);
    final GestionarPerfilPublicoService perfilPublico = new GestionarPerfilPublicoService(perfiles, registroFotos,
            fotos, reloj, reintento);
    final SubirFotoService subirFoto = new SubirFotoService(registroFotos, fotos, reloj);
    final LimpiarFotosSinUsoService limpiezaFotos = new LimpiarFotosSinUsoService(registroFotos, fotos, ofertas,
            perfiles, reloj);
    final ConsultarFotoService consultaFoto = new ConsultarFotoService(fotos);
    final AplicarAvisoNucleoService avisos = new AplicarAvisoNucleoService(ofertas, prestadores, avisosProcesados,
            reloj, reintento);

    private int secuenciaAvisos;

    EscenarioAplicacion() {
        registroFotos.registrar(new FotoSubida(FOTO, PRESTADOR, DestinoFoto.OFERTAS, AHORA));
        registroFotos.registrar(new FotoSubida(FOTO_OTRO_PRESTADOR, OTRO_PRESTADOR, DestinoFoto.OFERTAS, AHORA));
    }

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
        return ofertaPublicada(actividad(nombre, precio, zonaId));
    }

    /** Publica el contenido con la agencia de prueba (habilitándola si hace falta) y aprueba su versión 1. */
    Oferta ofertaPublicada(VersionOferta contenido) {
        if (prestadores.buscarPorId(PRESTADOR).isEmpty()) {
            habilitar(PRESTADOR, true, 1);
        }
        Oferta oferta = publicar(AGENCIA, contenido);
        moderar(oferta.id(), 1, DecisionModeracion.APROBAR);
        return oferta;
    }

    Pagina<Oferta> buscarTodas() {
        return busqueda.buscar(new CriteriosBusqueda(null, null, null, null, null, null, null, false,
                Paginacion.de(null, null)));
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
        return actividad(nombre, precio, zonaId, FOTO);
    }

    /** Una actividad con esas fotos; para el OTRO_PRESTADOR, {@code FOTO_OTRO_PRESTADOR}. */
    static VersionActividad actividad(String nombre, String precio, String zonaId, Foto... fotos) {
        ContenidoComun contenido = new ContenidoComun(nombre, "Descripción de " + nombre, new BigDecimal(precio),
                null, zonaId, null, null, 10, List.of(fotos));
        return new VersionActividad(contenido, TipoActividad.SENDERISMO, NivelEsfuerzo.MODERADO, false,
                List.of(LocalTime.of(7, 0)), null, null, null, List.of());
    }
}
