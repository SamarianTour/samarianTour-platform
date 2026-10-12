package co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.memoria;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Oferta;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.VersionActividad;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.VersionAlojamiento;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.VersionOferta;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.VersionPaquete;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.VersionRestaurante;

/**
 * Copia una oferta con sus versiones. La oferta y sus versiones cambian de estado, así que el adaptador en memoria
 * nunca comparte el mismo objeto con quien lo consulta: hace lo que haría una base de datos al leer y al guardar.
 */
final class CopiaOferta {

    private CopiaOferta() {
    }

    static Oferta de(Oferta oferta) {
        return Oferta.reconstituir(oferta.id(), oferta.tipo(), oferta.prestadorId(), oferta.retirada(),
                oferta.rntPrestadorActivo(), oferta.publicada().map(CopiaOferta::de).orElse(null),
                oferta.pendiente().map(CopiaOferta::de).orElse(null));
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
