package co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.politica.ReglasPaquete;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Una oferta visible con los datos que la recomendación usa para elegir y explicar (RF-26, RF-27) y para proponer
 * alternativas fuera de las zonas de alta afluencia (RF-29). No lleva calificaciones, comisión ni tamaño del
 * prestador: la recomendación no puede favorecer a un prestador por razones ajenas a sus criterios (RET-02).
 *
 * @param zonaAltaAfluencia marca de alta afluencia de la zona de la oferta (RF-29); false si la zona ya no existe
 * @param actividades       ofertas de las actividades de un paquete (RF-22); vacía para los demás tipos de oferta
 */
public record OfertaVisible(Oferta oferta, boolean zonaAltaAfluencia, List<Oferta> actividades) {

    public OfertaVisible {
        Objects.requireNonNull(oferta, "oferta");
        if (oferta.publicada().isEmpty()) {
            throw new IllegalArgumentException("Una oferta visible tiene versión publicada");
        }
        actividades = List.copyOf(actividades);
    }

    public VersionOferta version() {
        return oferta.publicada().orElseThrow();
    }

    /**
     * Tipos de actividad, en el orden de {@link TipoActividad}: el de una actividad, los de las actividades de un
     * paquete (DC-24) y ninguno para alojamiento y restaurante.
     */
    public List<TipoActividad> tiposActividad() {
        return switch (version()) {
            case VersionActividad actividad -> List.of(actividad.tipoActividad());
            case VersionPaquete paquete -> ReglasPaquete.tiposActividad(actividades).stream()
                    .sorted(Comparator.naturalOrder())
                    .toList();
            case VersionAlojamiento alojamiento -> List.of();
            case VersionRestaurante restaurante -> List.of();
        };
    }

    /** Nivel de esfuerzo de una actividad, o el mayor de las actividades de un paquete (RF-17); vacío para los demás. */
    public Optional<NivelEsfuerzo> nivelEsfuerzo() {
        return switch (version()) {
            case VersionActividad actividad -> Optional.of(actividad.nivelEsfuerzo());
            case VersionPaquete paquete -> Optional.of(ReglasPaquete.nivelEsfuerzo(actividades));
            case VersionAlojamiento alojamiento -> Optional.empty();
            case VersionRestaurante restaurante -> Optional.empty();
        };
    }

    /** Un paquete es accesible en silla de ruedas solo si todas sus actividades lo son (RF-17). */
    public boolean accesibleSillaRuedas() {
        return switch (version()) {
            case VersionActividad actividad -> actividad.esAccesibleSillaRuedas();
            case VersionPaquete paquete -> ReglasPaquete.esAccesibleSillaRuedas(actividades);
            case VersionAlojamiento alojamiento -> alojamiento.esAccesibleSillaRuedas();
            case VersionRestaurante restaurante -> restaurante.esAccesibleSillaRuedas();
        };
    }
}
