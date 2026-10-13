package co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Lo que el turista ve al abrir una oferta visible (RF-20): la oferta, su zona con las buenas prácticas y las
 * condiciones de visita (RF-25) y el perfil público del prestador (RF-24).
 *
 * @param zona        vacía si la zona de la oferta ya no existe
 * @param perfil      vacío si el prestador aún no ha completado su perfil público
 * @param actividades ofertas de las actividades de un paquete, en el orden en que el paquete las incluye (RF-22);
 *                    vacía para los demás tipos de oferta
 */
public record FichaOferta(Oferta oferta, Optional<Zona> zona, Optional<PerfilPublico> perfil, List<Oferta> actividades) {

    public FichaOferta {
        Objects.requireNonNull(oferta, "oferta");
        Objects.requireNonNull(zona, "zona");
        Objects.requireNonNull(perfil, "perfil");
        actividades = List.copyOf(actividades);
    }
}
