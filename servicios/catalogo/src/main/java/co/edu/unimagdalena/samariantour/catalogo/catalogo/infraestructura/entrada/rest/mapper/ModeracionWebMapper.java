package co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest.mapper;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.ContenidoComun;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Oferta;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Pagina;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.RegistroModeracion;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.VersionActividad;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.VersionAlojamiento;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.VersionOferta;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.VersionPaquete;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.VersionRestaurante;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest.dto.ContenidoVersionResponse;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest.dto.OfertaEnModeracionResponse;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest.dto.OfertaPendienteResponse;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest.dto.PaginaResponse;

/**
 * Convierte las ofertas en moderación (RF-79) a lo que ven el administrador y el núcleo.
 */
public final class ModeracionWebMapper {

    private ModeracionWebMapper() {
    }

    public static PaginaResponse<OfertaPendienteResponse> aPaginaPendientes(Pagina<Oferta> pagina) {
        return OfertaWebMapper.aPaginaResponse(pagina.map(ModeracionWebMapper::aPendiente));
    }

    public static OfertaEnModeracionResponse aEnModeracion(Oferta oferta) {
        VersionOferta pendiente = oferta.pendiente().orElseThrow();
        RegistroModeracion moderacion = pendiente.moderacion();
        return new OfertaEnModeracionResponse(oferta.id(), oferta.tipo(), pendiente.contenido().nombre(),
                oferta.prestadorId(), moderacion.numero(), moderacion.enviadaEn());
    }

    private static OfertaPendienteResponse aPendiente(Oferta oferta) {
        VersionOferta pendiente = oferta.pendiente().orElseThrow();
        return new OfertaPendienteResponse(oferta.id(), oferta.tipo(), oferta.prestadorId(),
                oferta.rntPrestadorActivo(), pendiente.numero(), pendiente.moderacion().enviadaEn(),
                aContenido(pendiente), oferta.publicada().map(ModeracionWebMapper::aContenido).orElse(null));
    }

    private static ContenidoVersionResponse aContenido(VersionOferta version) {
        ContenidoComun contenido = version.contenido();
        VersionActividad actividad = version instanceof VersionActividad v ? v : null;
        VersionPaquete paquete = version instanceof VersionPaquete v ? v : null;
        return new ContenidoVersionResponse(contenido.nombre(), contenido.descripcion(),
                contenido.precioReferencia(), version.unidadPrecio(), contenido.horarios(), contenido.zonaId(),
                Conversiones.coordenada(contenido.ubicacion()), contenido.comoLlegar(), contenido.capacidadMaxima(),
                FotoWebMapper.aRespuestas(contenido.fotos()),
                actividad == null ? null : actividad.tipoActividad(),
                actividad == null ? null : actividad.nivelEsfuerzo(),
                accesible(version),
                actividad != null ? Conversiones.horas(actividad.horariosSalida())
                        : paquete != null ? Conversiones.horas(paquete.horariosSalida()) : null,
                actividad == null ? null : actividad.puntoEncuentro(),
                actividad != null ? actividad.duracionMinutos() : paquete != null ? paquete.duracionMinutos() : null,
                actividad == null ? null : actividad.modalidad(),
                actividad == null || actividad.idiomasAtencion().isEmpty() ? null : actividad.idiomasAtencion(),
                paquete == null ? null : paquete.actividadIds(),
                version instanceof VersionAlojamiento alojamiento ? alojamiento.tipoUnidad() : null);
    }

    /** El paquete no guarda su accesibilidad: se calcula con sus actividades (RF-17). */
    private static Boolean accesible(VersionOferta version) {
        return switch (version) {
            case VersionActividad actividad -> actividad.esAccesibleSillaRuedas();
            case VersionPaquete paquete -> null;
            case VersionAlojamiento alojamiento -> alojamiento.esAccesibleSillaRuedas();
            case VersionRestaurante restaurante -> restaurante.esAccesibleSillaRuedas();
        };
    }
}
