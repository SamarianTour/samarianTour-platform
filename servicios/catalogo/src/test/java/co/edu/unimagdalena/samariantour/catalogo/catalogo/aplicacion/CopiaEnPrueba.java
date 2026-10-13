package co.edu.unimagdalena.samariantour.catalogo.catalogo.aplicacion;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Oferta;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.VersionActividad;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.VersionAlojamiento;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.VersionOferta;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.VersionPaquete;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.VersionRestaurante;

/**
 * Copia profunda de una oferta para los dobles en memoria: igual que una base de datos, nunca entregan el mismo
 * objeto que guardan. Sin esto, un reintento (ADR-05) recargaría el objeto ya modificado por el intento fallido.
 */
final class CopiaEnPrueba {

    private CopiaEnPrueba() {
    }

    static Oferta de(Oferta oferta, boolean rntPrestadorActivo, long versionHabilitacion, long revision) {
        return Oferta.reconstituir(oferta.id(), oferta.tipo(), oferta.prestadorId(), oferta.retirada(),
                rntPrestadorActivo, versionHabilitacion, revision, oferta.publicada().map(CopiaEnPrueba::de).orElse(null),
                oferta.pendiente().map(CopiaEnPrueba::de).orElse(null));
    }

    static Oferta de(Oferta oferta) {
        return de(oferta, oferta.rntPrestadorActivo(), oferta.versionHabilitacion(), oferta.revision());
    }

    private static VersionOferta de(VersionOferta version) {
        return switch (version) {
            case VersionActividad actividad -> VersionActividad.reconstituir(actividad.contenido(),
                    actividad.tipoActividad(), actividad.nivelEsfuerzo(), actividad.esAccesibleSillaRuedas(),
                    actividad.horariosSalida(), actividad.puntoEncuentro(), actividad.duracionMinutos(),
                    actividad.modalidad(), actividad.idiomasAtencion(), actividad.moderacion());
            case VersionPaquete paquete -> VersionPaquete.reconstituir(paquete.contenido(), paquete.actividadIds(),
                    paquete.duracionMinutos(), paquete.horariosSalida(), paquete.moderacion());
            case VersionAlojamiento alojamiento -> VersionAlojamiento.reconstituir(alojamiento.contenido(),
                    alojamiento.tipoUnidad(), alojamiento.esAccesibleSillaRuedas(), alojamiento.moderacion());
            case VersionRestaurante restaurante -> VersionRestaurante.reconstituir(restaurante.contenido(),
                    restaurante.esAccesibleSillaRuedas(), restaurante.moderacion());
        };
    }
}
