package co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.persistencia.mapper;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.ContenidoComun;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Foto;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Oferta;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.RegistroModeracion;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.TipoOferta;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.VersionActividad;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.VersionAlojamiento;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.VersionOferta;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.VersionPaquete;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.VersionRestaurante;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.persistencia.document.OfertaDocument;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.persistencia.document.VersionDocument;

import java.util.List;

import static co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.persistencia.mapper.ConversionesMongo.aHoras;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.persistencia.mapper.ConversionesMongo.coordenada;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.persistencia.mapper.ConversionesMongo.horas;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.persistencia.mapper.ConversionesMongo.lista;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.persistencia.mapper.ConversionesMongo.punto;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.persistencia.mapper.ConversionesMongo.referencia;

/**
 * Convierte entre {@link Oferta} y {@link OfertaDocument}. Conserva la revisión y la habilitación copiada en los dos
 * sentidos (ADR-05).
 */
public final class OfertaMongoMapper {

    private OfertaMongoMapper() {
    }

    /** @param revision la revisión con la que se guarda el documento; la decide el adaptador */
    public static OfertaDocument aDocumento(Oferta oferta, long revision) {
        return new OfertaDocument(referencia(oferta.id()), oferta.tipo(), oferta.prestadorId(), oferta.retirada(),
                oferta.rntPrestadorActivo(), oferta.versionHabilitacion(), revision,
                oferta.publicada().map(OfertaMongoMapper::aDocumento).orElse(null),
                oferta.pendiente().map(OfertaMongoMapper::aDocumento).orElse(null));
    }

    public static Oferta aDominio(OfertaDocument documento) {
        return Oferta.reconstituir(documento.id().toHexString(), documento.tipo(), documento.prestadorId(),
                documento.retirada(), documento.rntPrestadorActivo(), documento.versionHabilitacion(),
                documento.revision(), aDominio(documento.tipo(), documento.publicada()),
                aDominio(documento.tipo(), documento.pendiente()));
    }

    private static VersionDocument aDocumento(VersionOferta version) {
        RegistroModeracion moderacion = version.moderacion();
        ContenidoComun contenido = version.contenido();
        return switch (version) {
            case VersionActividad actividad -> new VersionDocument(moderacion.numero(), moderacion.estado(),
                    moderacion.motivoRechazo(), moderacion.enviadaEn(), moderacion.revisadaEn(),
                    contenido.nombre(), contenido.descripcion(), contenido.precioReferencia(), contenido.horarios(),
                    referencia(contenido.zonaId()), punto(contenido.ubicacion()), contenido.comoLlegar(),
                    contenido.capacidadMaxima(), fotos(contenido),
                    actividad.tipoActividad(), actividad.nivelEsfuerzo(), actividad.puntoEncuentro(),
                    actividad.modalidad(), actividad.idiomasAtencion(),
                    horas(actividad.horariosSalida()), actividad.duracionMinutos(),
                    null, null, actividad.esAccesibleSillaRuedas());
            case VersionPaquete paquete -> new VersionDocument(moderacion.numero(), moderacion.estado(),
                    moderacion.motivoRechazo(), moderacion.enviadaEn(), moderacion.revisadaEn(),
                    contenido.nombre(), contenido.descripcion(), contenido.precioReferencia(), contenido.horarios(),
                    referencia(contenido.zonaId()), punto(contenido.ubicacion()), contenido.comoLlegar(),
                    contenido.capacidadMaxima(), fotos(contenido),
                    null, null, null, null, null,
                    horas(paquete.horariosSalida()), paquete.duracionMinutos(),
                    paquete.actividadIds().stream().map(ConversionesMongo::referencia).toList(), null, null);
            case VersionAlojamiento alojamiento -> new VersionDocument(moderacion.numero(), moderacion.estado(),
                    moderacion.motivoRechazo(), moderacion.enviadaEn(), moderacion.revisadaEn(),
                    contenido.nombre(), contenido.descripcion(), contenido.precioReferencia(), contenido.horarios(),
                    referencia(contenido.zonaId()), punto(contenido.ubicacion()), contenido.comoLlegar(),
                    contenido.capacidadMaxima(), fotos(contenido),
                    null, null, null, null, null, null, null, null,
                    alojamiento.tipoUnidad(), alojamiento.esAccesibleSillaRuedas());
            case VersionRestaurante restaurante -> new VersionDocument(moderacion.numero(), moderacion.estado(),
                    moderacion.motivoRechazo(), moderacion.enviadaEn(), moderacion.revisadaEn(),
                    contenido.nombre(), contenido.descripcion(), contenido.precioReferencia(), contenido.horarios(),
                    referencia(contenido.zonaId()), punto(contenido.ubicacion()), contenido.comoLlegar(),
                    contenido.capacidadMaxima(), fotos(contenido),
                    null, null, null, null, null, null, null, null, null,
                    restaurante.esAccesibleSillaRuedas());
        };
    }

    private static List<String> fotos(ContenidoComun contenido) {
        return contenido.fotos().stream().map(Foto::id).toList();
    }

    /** El tipo está en la raíz de la oferta y decide qué campos de la versión se leen (DC-16). */
    private static VersionOferta aDominio(TipoOferta tipo, VersionDocument documento) {
        if (documento == null) {
            return null;
        }
        RegistroModeracion moderacion = new RegistroModeracion(documento.numero(), documento.estadoModeracion(),
                documento.motivoRechazo(), documento.enviadaEn(), documento.revisadaEn());
        ContenidoComun contenido = new ContenidoComun(documento.nombre(), documento.descripcion(),
                documento.precioReferencia(), documento.horarios(), documento.zonaId().toHexString(),
                coordenada(documento.ubicacion()), documento.comoLlegar(), documento.capacidadMaxima(),
                lista(documento.fotos()).stream().map(Foto::new).toList());
        boolean accesible = Boolean.TRUE.equals(documento.accesibleSillaRuedas());
        return switch (tipo) {
            case ACTIVIDAD -> VersionActividad.reconstituir(contenido, documento.tipoActividad(),
                    documento.nivelEsfuerzo(), accesible, aHoras(documento.horariosSalida()),
                    documento.puntoEncuentro(), documento.duracionMinutos(), documento.modalidad(),
                    lista(documento.idiomasAtencion()), moderacion);
            case PAQUETE -> VersionPaquete.reconstituir(contenido,
                    lista(documento.actividadIds()).stream().map(id -> id.toHexString()).toList(),
                    documento.duracionMinutos(), aHoras(documento.horariosSalida()), moderacion);
            case ALOJAMIENTO -> VersionAlojamiento.reconstituir(contenido, documento.tipoUnidad(), accesible,
                    moderacion);
            case RESTAURANTE -> VersionRestaurante.reconstituir(contenido, accesible, moderacion);
        };
    }
}
