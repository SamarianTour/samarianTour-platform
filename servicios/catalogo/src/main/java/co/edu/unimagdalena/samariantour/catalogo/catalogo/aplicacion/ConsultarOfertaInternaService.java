package co.edu.unimagdalena.samariantour.catalogo.catalogo.aplicacion;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.OfertaNoEncontradaException;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Oferta;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.politica.ReglasPaquete;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.ConsultarOfertaInternaUseCase;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.salida.OfertaRepositoryPort;

/**
 * Consulta del núcleo al crear una solicitud (RF-30). Aplica la misma visibilidad que la ficha pública: si el
 * turista no puede ver la oferta, tampoco puede pedirla.
 */
public class ConsultarOfertaInternaService implements ConsultarOfertaInternaUseCase {

    private final OfertaRepositoryPort ofertas;
    private final ActividadesDePaquetes actividades;

    public ConsultarOfertaInternaService(OfertaRepositoryPort ofertas) {
        this.ofertas = ofertas;
        this.actividades = new ActividadesDePaquetes(ofertas);
    }

    @Override
    public Oferta consultar(String ofertaId) {
        Oferta oferta = ofertas.buscarPorId(ofertaId).orElseThrow(() -> new OfertaNoEncontradaException(ofertaId));
        // Un paquete además exige que todas sus actividades sean visibles (RF-22).
        if (!ReglasPaquete.esVisible(oferta, actividades.de(oferta))) {
            throw new OfertaNoEncontradaException(ofertaId);
        }
        return oferta;
    }
}
