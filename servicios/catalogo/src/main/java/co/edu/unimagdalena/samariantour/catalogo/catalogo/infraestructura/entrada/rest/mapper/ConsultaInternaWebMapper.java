package co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest.mapper;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.ContenidoComun;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Oferta;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.OfertaVisible;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Pagina;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.TipoOferta;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.VersionActividad;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.VersionOferta;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.VersionPaquete;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest.dto.OfertaInternaResponse;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest.dto.OfertaVisibleResponse;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest.dto.PaginaResponse;

import java.util.List;

/**
 * Convierte las consultas internas del núcleo (RF-30) y de la recomendación (RF-26, RF-29) a sus DTO.
 */
public final class ConsultaInternaWebMapper {

    private ConsultaInternaWebMapper() {
    }

    public static TipoOferta aTipo(String tipo) {
        return Conversiones.enumeradoOpcional(tipo, "tipo", TipoOferta.class);
    }

    public static OfertaInternaResponse aOfertaInterna(Oferta oferta) {
        VersionOferta version = oferta.publicada().orElseThrow();
        ContenidoComun contenido = version.contenido();
        return new OfertaInternaResponse(oferta.id(), oferta.tipo(), contenido.nombre(), oferta.prestadorId(),
                contenido.zonaId(), horariosSalida(version), contenido.capacidadMaxima());
    }

    public static PaginaResponse<OfertaVisibleResponse> aPaginaVisibles(Pagina<OfertaVisible> pagina) {
        return OfertaWebMapper.aPaginaResponse(pagina.map(ConsultaInternaWebMapper::aOfertaVisible));
    }

    private static OfertaVisibleResponse aOfertaVisible(OfertaVisible visible) {
        Oferta oferta = visible.oferta();
        VersionOferta version = visible.version();
        ContenidoComun contenido = version.contenido();
        return new OfertaVisibleResponse(oferta.id(), oferta.tipo(), contenido.nombre(), contenido.descripcion(),
                oferta.prestadorId(), contenido.zonaId(), visible.zonaAltaAfluencia(), contenido.precioReferencia(),
                version.unidadPrecio(), visible.tiposActividad(), visible.nivelEsfuerzo().orElse(null),
                visible.accesibleSillaRuedas());
    }

    /** Solo la actividad y el paquete tienen horarios de salida; el turista elige uno al pedir (RF-30). */
    private static List<String> horariosSalida(VersionOferta version) {
        return switch (version) {
            case VersionActividad actividad -> Conversiones.horas(actividad.horariosSalida());
            case VersionPaquete paquete -> Conversiones.horas(paquete.horariosSalida());
            default -> null;
        };
    }
}
