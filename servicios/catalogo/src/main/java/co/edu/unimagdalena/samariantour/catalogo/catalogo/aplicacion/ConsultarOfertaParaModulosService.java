package co.edu.unimagdalena.samariantour.catalogo.catalogo.aplicacion;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Oferta;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.politica.ReglasPaquete;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.ConsultarOfertaParaModulosUseCase;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.salida.OfertaRepositoryPort;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Consultas de los otros módulos sobre las ofertas (DC-40). La visibilidad es la misma de la ficha y de la consulta
 * interna del núcleo (DC-35).
 */
public class ConsultarOfertaParaModulosService implements ConsultarOfertaParaModulosUseCase {

    private final OfertaRepositoryPort ofertas;
    private final ActividadesDePaquetes actividades;

    public ConsultarOfertaParaModulosService(OfertaRepositoryPort ofertas) {
        this.ofertas = ofertas;
        this.actividades = new ActividadesDePaquetes(ofertas);
    }

    @Override
    public boolean existe(String ofertaId) {
        return ofertas.buscarPorId(ofertaId).isPresent();
    }

    @Override
    public boolean esVisible(String ofertaId) {
        return ofertas.buscarPorId(ofertaId)
                .map(oferta -> ReglasPaquete.esVisible(oferta, actividades.de(oferta)))
                .orElse(false);
    }

    /** Una sola consulta para todas las ofertas. */
    @Override
    public Map<String, String> nombres(Collection<String> ofertaIds) {
        if (ofertaIds.isEmpty()) {
            return Map.of();
        }
        List<Oferta> encontradas = ofertas.buscarPorIds(ofertaIds.stream().distinct().toList());
        return encontradas.stream().collect(Collectors.toMap(Oferta::id,
                ConsultarOfertaParaModulosService::nombre, (primero, repetido) -> primero));
    }

    private static String nombre(Oferta oferta) {
        return oferta.publicada().or(oferta::pendiente)
                .map(version -> version.contenido().nombre())
                .orElseThrow();
    }
}
