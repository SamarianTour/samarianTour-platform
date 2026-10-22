package co.edu.unimagdalena.samariantour.catalogo.catalogo.aplicacion;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.OfertaNoEncontradaException;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.FichaOferta;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Oferta;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.politica.ReglasPaquete;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.ConsultarFichaOfertaUseCase;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.salida.OfertaRepositoryPort;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.salida.PerfilRepositoryPort;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.salida.ZonaRepositoryPort;

import java.util.List;

/**
 * Ficha pública de una oferta (RF-20), con su zona (RF-25) y el perfil público del prestador (RF-24).
 * Una oferta que no es visible responde igual que una inexistente.
 */
public class ConsultarFichaOfertaService implements ConsultarFichaOfertaUseCase {

    private final OfertaRepositoryPort ofertas;
    private final ZonaRepositoryPort zonas;
    private final PerfilRepositoryPort perfiles;
    private final ActividadesDePaquetes actividades;

    public ConsultarFichaOfertaService(OfertaRepositoryPort ofertas, ZonaRepositoryPort zonas,
                                       PerfilRepositoryPort perfiles) {
        this.ofertas = ofertas;
        this.zonas = zonas;
        this.perfiles = perfiles;
        this.actividades = new ActividadesDePaquetes(ofertas);
    }

    @Override
    public FichaOferta consultar(String ofertaId) {
        Oferta oferta = ofertas.buscarPorId(ofertaId).orElseThrow(() -> new OfertaNoEncontradaException(ofertaId));
        List<Oferta> incluidas = actividades.de(oferta);
        // Un paquete además exige que todas sus actividades sean visibles (RF-22).
        if (!ReglasPaquete.esVisible(oferta, incluidas)) {
            throw new OfertaNoEncontradaException(ofertaId);
        }
        String zonaId = oferta.publicada().orElseThrow().contenido().zonaId();
        return new FichaOferta(oferta, zonas.buscarPorId(zonaId), perfiles.buscarPorPrestador(oferta.prestadorId()),
                incluidas);
    }
}
