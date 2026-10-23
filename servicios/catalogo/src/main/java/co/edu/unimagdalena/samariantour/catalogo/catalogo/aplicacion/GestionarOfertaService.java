package co.edu.unimagdalena.samariantour.catalogo.catalogo.aplicacion;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.DatoInvalidoException;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.OfertaNoEncontradaException;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.ZonaNoDisponibleException;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Foto;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.FotoSubida;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Oferta;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Pagina;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Paginacion;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.VersionOferta;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.VersionPaquete;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Zona;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.GestionarOfertaUseCase;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.comando.EditarOfertaCommand;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.comando.PublicarOfertaCommand;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.comando.RetirarOfertaCommand;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.salida.FotoSubidaRepositoryPort;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.salida.OfertaRepositoryPort;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.salida.PrestadorCatalogoRepositoryPort;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.salida.ZonaRepositoryPort;

import co.edu.unimagdalena.samariantour.catalogo.compartido.concurrencia.EjecutorConReintento;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Publicar, editar y retirar ofertas del prestador (RF-65 a RF-67). Las reglas viven en {@link Oferta};
 * este servicio reúne lo que el dominio necesita (habilitación, zona, hora) y guarda el resultado.
 *
 * <p>Las fotos de la versión deben ser fotos que el prestador subió para sus ofertas (DC-47).
 *
 * <p>Editar y retirar se reintentan completos si otra escritura se cruza con ellos (ADR-05). Publicar no: crea un
 * documento nuevo, así que no puede chocar con otra escritura.
 */
public class GestionarOfertaService implements GestionarOfertaUseCase {

    private final OfertaRepositoryPort ofertas;
    private final ZonaRepositoryPort zonas;
    private final PrestadorCatalogoRepositoryPort prestadores;
    private final FotoSubidaRepositoryPort fotos;
    private final Clock reloj;
    private final EjecutorConReintento reintento;

    public GestionarOfertaService(OfertaRepositoryPort ofertas, ZonaRepositoryPort zonas,
                                  PrestadorCatalogoRepositoryPort prestadores, FotoSubidaRepositoryPort fotos,
                                  Clock reloj, EjecutorConReintento reintento) {
        this.ofertas = ofertas;
        this.zonas = zonas;
        this.prestadores = prestadores;
        this.fotos = fotos;
        this.reloj = reloj;
        this.reintento = reintento;
    }

    @Override
    public Oferta publicar(PublicarOfertaCommand comando) {
        Oferta oferta = Oferta.publicar(ofertas.nuevoId(),
                prestadores.habilitacionDe(comando.solicitante().prestadorId()),
                comando.solicitante().tipoPrestador(), comando.contenido(), zonaDe(comando.contenido()),
                actividadesDe(comando.contenido()), ahora());
        asignarFotos(comando.solicitante().prestadorId(), comando.contenido());
        ofertas.guardar(oferta);
        return oferta;
    }

    @Override
    public Oferta editar(EditarOfertaCommand comando) {
        return reintento.ejecutar(() -> {
            Oferta oferta = ofertaExistente(comando.ofertaId());
            oferta.editar(prestadores.habilitacionDe(comando.solicitante().prestadorId()),
                    comando.solicitante().tipoPrestador(), comando.contenido(), zonaDe(comando.contenido()),
                    actividadesDe(comando.contenido()), ahora());
            asignarFotos(comando.solicitante().prestadorId(), comando.contenido());
            ofertas.guardar(oferta);
            return oferta;
        });
    }

    @Override
    public void retirar(RetirarOfertaCommand comando) {
        reintento.ejecutar(() -> {
            Oferta oferta = ofertaExistente(comando.ofertaId());
            oferta.retirar(comando.prestadorId());
            ofertas.guardar(oferta);
        });
    }

    @Override
    public Pagina<Oferta> listarMias(UUID prestadorId, Paginacion paginacion) {
        return ofertas.buscarPorPrestador(prestadorId, paginacion);
    }

    private Zona zonaDe(VersionOferta contenido) {
        String zonaId = contenido.contenido().zonaId();
        return zonas.buscarPorId(zonaId).orElseThrow(() -> new ZonaNoDisponibleException(zonaId));
    }

    /** Ofertas que referencia el contenido cuando es un paquete; el dominio verifica que sean del prestador (RF-22). */
    private List<Oferta> actividadesDe(VersionOferta contenido) {
        return contenido instanceof VersionPaquete paquete ? ofertas.buscarPorIds(paquete.actividadIds()) : List.of();
    }

    /**
     * Marca las fotos como usadas y comprueba que el prestador las subió para sus ofertas. Se marca antes de comprobar:
     * si la limpieza borra una foto, o ya no la encuentra sin uso o la borró antes y aquí no aparece
     * (FotoSubidaRepositoryPort).
     */
    private void asignarFotos(UUID prestadorId, VersionOferta contenido) {
        List<Foto> asignadas = contenido.contenido().fotos();
        fotos.marcarEnUso(asignadas, ahora());
        Set<Foto> propias = fotos.buscar(asignadas).stream()
                .filter(subida -> subida.sirveParaOfertasDe(prestadorId))
                .map(FotoSubida::foto)
                .collect(Collectors.toSet());
        if (!propias.containsAll(asignadas)) {
            throw new DatoInvalidoException("fotos", "incluye una foto que no existe o no es tuya; vuelve a subirla");
        }
    }

    private Oferta ofertaExistente(String ofertaId) {
        return ofertas.buscarPorId(ofertaId).orElseThrow(() -> new OfertaNoEncontradaException(ofertaId));
    }

    private Instant ahora() {
        return Instant.now(reloj);
    }
}
