package co.edu.unimagdalena.samariantour.catalogo.catalogo.aplicacion;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.OfertaNoEncontradaException;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.ZonaNoDisponibleException;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Oferta;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Pagina;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Paginacion;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.VersionOferta;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Zona;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.GestionarOfertaUseCase;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.comando.EditarOfertaCommand;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.comando.PublicarOfertaCommand;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.comando.RetirarOfertaCommand;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.salida.OfertaRepositoryPort;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.salida.PrestadorCatalogoRepositoryPort;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.salida.ZonaRepositoryPort;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * Publicar, editar y retirar ofertas del prestador (RF-65 a RF-67). Las reglas viven en {@link Oferta};
 * este servicio reúne lo que el dominio necesita (habilitación, zona, hora) y guarda el resultado.
 */
public class GestionarOfertaService implements GestionarOfertaUseCase {

    private final OfertaRepositoryPort ofertas;
    private final ZonaRepositoryPort zonas;
    private final PrestadorCatalogoRepositoryPort prestadores;
    private final Clock reloj;

    public GestionarOfertaService(OfertaRepositoryPort ofertas, ZonaRepositoryPort zonas,
                                  PrestadorCatalogoRepositoryPort prestadores, Clock reloj) {
        this.ofertas = ofertas;
        this.zonas = zonas;
        this.prestadores = prestadores;
        this.reloj = reloj;
    }

    @Override
    public Oferta publicar(PublicarOfertaCommand comando) {
        Oferta oferta = Oferta.publicar(UUID.randomUUID().toString(),
                prestadores.habilitacionDe(comando.solicitante().prestadorId()),
                comando.solicitante().tipoPrestador(), comando.contenido(), zonaDe(comando.contenido()), ahora());
        ofertas.guardar(oferta);
        return oferta;
    }

    @Override
    public Oferta editar(EditarOfertaCommand comando) {
        Oferta oferta = ofertaExistente(comando.ofertaId());
        oferta.editar(prestadores.habilitacionDe(comando.solicitante().prestadorId()),
                comando.solicitante().tipoPrestador(), comando.contenido(), zonaDe(comando.contenido()), ahora());
        ofertas.guardar(oferta);
        return oferta;
    }

    @Override
    public void retirar(RetirarOfertaCommand comando) {
        Oferta oferta = ofertaExistente(comando.ofertaId());
        oferta.retirar(comando.prestadorId());
        ofertas.guardar(oferta);
    }

    @Override
    public Pagina<Oferta> listarMias(UUID prestadorId, Paginacion paginacion) {
        return ofertas.buscarPorPrestador(prestadorId, paginacion);
    }

    private Zona zonaDe(VersionOferta contenido) {
        String zonaId = contenido.contenido().zonaId();
        return zonas.buscarPorId(zonaId).orElseThrow(() -> new ZonaNoDisponibleException(zonaId));
    }

    private Oferta ofertaExistente(String ofertaId) {
        return ofertas.buscarPorId(ofertaId).orElseThrow(() -> new OfertaNoEncontradaException(ofertaId));
    }

    private Instant ahora() {
        return Instant.now(reloj);
    }
}
