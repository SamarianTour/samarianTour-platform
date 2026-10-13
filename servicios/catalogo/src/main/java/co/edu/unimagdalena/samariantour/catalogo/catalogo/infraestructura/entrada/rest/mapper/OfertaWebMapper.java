package co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest.mapper;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.ContenidoComun;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.FichaOferta;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.ModalidadRecorrido;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.NivelEsfuerzo;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Oferta;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Pagina;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.PerfilPublico;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.TipoActividad;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.TipoOferta;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Validaciones;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.VersionActividad;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.VersionAlojamiento;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.VersionOferta;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.VersionPaquete;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.VersionRestaurante;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.politica.ReglasPaquete;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest.dto.ActividadIncluidaResponse;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest.dto.FichaOfertaResponse;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest.dto.FichaOfertaResponse.DetalleActividad;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest.dto.FichaOfertaResponse.DetalleAlojamiento;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest.dto.FichaOfertaResponse.DetallePaquete;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest.dto.OfertaPrestadorResponse;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest.dto.OfertaRequest;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest.dto.OfertaResumenResponse;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest.dto.PaginaResponse;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest.dto.PerfilPublicoResponse;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest.dto.VersionResponse;

import java.util.List;

/**
 * Convierte entre los DTO de ofertas y el dominio.
 */
public final class OfertaWebMapper {

    private OfertaWebMapper() {
    }

    /** Borrador de una versión a partir del cuerpo de publicar o editar; el tipo decide qué campos se usan. */
    public static VersionOferta aVersion(OfertaRequest peticion) {
        TipoOferta tipo = Conversiones.enumerado(peticion.tipo(), "tipo", TipoOferta.class);
        ContenidoComun contenido = new ContenidoComun(peticion.nombre(), peticion.descripcion(),
                peticion.precioReferencia(), peticion.horarios(), peticion.zonaId(),
                Conversiones.coordenada(peticion.ubicacion()), peticion.comoLlegar(),
                Validaciones.presente(peticion.capacidadMaxima(), "capacidadMaxima"));
        return switch (tipo) {
            case ACTIVIDAD -> new VersionActividad(contenido,
                    Conversiones.enumeradoOpcional(peticion.tipoActividad(), "tipoActividad", TipoActividad.class),
                    Conversiones.enumeradoOpcional(peticion.nivelEsfuerzo(), "nivelEsfuerzo", NivelEsfuerzo.class),
                    esAccesible(peticion), Conversiones.horas(peticion.horariosSalida(), "horariosSalida"),
                    peticion.puntoEncuentro(), peticion.duracionMinutos(),
                    Conversiones.enumeradoOpcional(peticion.modalidad(), "modalidad", ModalidadRecorrido.class),
                    peticion.idiomasAtencion());
            case PAQUETE -> new VersionPaquete(contenido, peticion.actividadIds(),
                    Validaciones.presente(peticion.duracionMinutos(), "duracionMinutos"),
                    Conversiones.horas(peticion.horariosSalida(), "horariosSalida"));
            case ALOJAMIENTO -> new VersionAlojamiento(contenido, peticion.tipoUnidad(), esAccesible(peticion));
            case RESTAURANTE -> new VersionRestaurante(contenido, esAccesible(peticion));
        };
    }

    public static PaginaResponse<OfertaResumenResponse> aPagina(Pagina<Oferta> pagina) {
        return aPaginaResponse(pagina.map(OfertaWebMapper::aResumen));
    }

    /** Ofertas propias del prestador, en cualquier estado. */
    public static PaginaResponse<OfertaPrestadorResponse> aPaginaPrestador(Pagina<Oferta> pagina) {
        return aPaginaResponse(pagina.map(OfertaWebMapper::aRespuestaPrestador));
    }

    public static FichaOfertaResponse aFicha(FichaOferta ficha) {
        Oferta oferta = ficha.oferta();
        VersionOferta version = oferta.publicada().orElseThrow();
        ContenidoComun contenido = version.contenido();
        return new FichaOfertaResponse(oferta.id(), oferta.tipo(), contenido.nombre(), contenido.descripcion(),
                contenido.precioReferencia(), version.unidadPrecio(), contenido.horarios(),
                Conversiones.coordenada(contenido.ubicacion()), contenido.comoLlegar(), contenido.capacidadMaxima(),
                esAccesible(version, ficha.actividades()),
                version instanceof VersionActividad actividad ? detalle(actividad) : null,
                version instanceof VersionPaquete paquete ? detalle(paquete, ficha.actividades()) : null,
                version instanceof VersionAlojamiento alojamiento
                        ? new DetalleAlojamiento(alojamiento.tipoUnidad()) : null,
                ficha.zona().map(ZonaWebMapper::aRespuesta).orElse(null),
                ficha.perfil().map(OfertaWebMapper::aPerfil).orElse(null));
    }

    public static OfertaPrestadorResponse aRespuestaPrestador(Oferta oferta) {
        return new OfertaPrestadorResponse(oferta.id(), oferta.tipo(), oferta.retirada(),
                oferta.publicada().map(OfertaWebMapper::aVersionResponse).orElse(null),
                oferta.pendiente().map(OfertaWebMapper::aVersionResponse).orElse(null));
    }

    private static <T> PaginaResponse<T> aPaginaResponse(Pagina<T> pagina) {
        return new PaginaResponse<>(pagina.contenido(), pagina.paginacion().pagina(), pagina.paginacion().tamano(),
                pagina.totalElementos(), pagina.totalPaginas());
    }

    private static OfertaResumenResponse aResumen(Oferta oferta) {
        VersionOferta version = oferta.publicada().orElseThrow();
        ContenidoComun contenido = version.contenido();
        return new OfertaResumenResponse(oferta.id(), oferta.tipo(), contenido.nombre(), contenido.descripcion(),
                contenido.precioReferencia(), version.unidadPrecio(), contenido.zonaId());
    }

    private static DetalleActividad detalle(VersionActividad actividad) {
        return new DetalleActividad(actividad.tipoActividad(), actividad.nivelEsfuerzo(),
                Conversiones.horas(actividad.horariosSalida()), actividad.puntoEncuentro(),
                actividad.duracionMinutos(), actividad.modalidad(),
                actividad.idiomasAtencion().isEmpty() ? null : actividad.idiomasAtencion());
    }

    /** El nivel de esfuerzo y los tipos de actividad del paquete se calculan con sus actividades (RF-17, DC-24). */
    private static DetallePaquete detalle(VersionPaquete paquete, List<Oferta> actividades) {
        List<ActividadIncluidaResponse> incluidas = actividades.stream()
                .map(actividad -> new ActividadIncluidaResponse(actividad.id(),
                        actividad.publicada().orElseThrow().contenido().nombre()))
                .toList();
        return new DetallePaquete(incluidas, ReglasPaquete.tiposActividad(actividades),
                ReglasPaquete.nivelEsfuerzo(actividades), paquete.duracionMinutos(),
                Conversiones.horas(paquete.horariosSalida()));
    }

    private static boolean esAccesible(VersionOferta version, List<Oferta> actividades) {
        return switch (version) {
            case VersionActividad actividad -> actividad.esAccesibleSillaRuedas();
            case VersionPaquete paquete -> ReglasPaquete.esAccesibleSillaRuedas(actividades);
            case VersionAlojamiento alojamiento -> alojamiento.esAccesibleSillaRuedas();
            case VersionRestaurante restaurante -> restaurante.esAccesibleSillaRuedas();
        };
    }

    /** Si el prestador no dice que la oferta es accesible, no se anuncia como accesible. */
    private static boolean esAccesible(OfertaRequest peticion) {
        return Boolean.TRUE.equals(peticion.accesibleSillaRuedas());
    }

    private static PerfilPublicoResponse aPerfil(PerfilPublico perfil) {
        return new PerfilPublicoResponse(perfil.nombreComercial(), perfil.descripcion(), perfil.fotos(),
                perfil.horariosAtencion(), perfil.sitioWeb(), perfil.redesSociales());
    }

    private static VersionResponse aVersionResponse(VersionOferta version) {
        return new VersionResponse(version.numero(), version.estadoModeracion(),
                version.moderacion().motivoRechazo(), version.moderacion().enviadaEn(),
                version.moderacion().revisadaEn(), version.contenido().nombre(),
                version.contenido().precioReferencia());
    }
}
