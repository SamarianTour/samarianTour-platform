package co.edu.unimagdalena.samariantour.catalogo.opiniones.aplicacion;

import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.excepcion.OfertaNoEncontradaException;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.excepcion.OpinionDuplicadaException;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.excepcion.OpinionNoHabilitadaException;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.modelo.Opinion;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.modelo.OpinionConOferta;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.puerto.entrada.PublicarOpinionUseCase;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.puerto.entrada.comando.PublicarOpinionCommand;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.puerto.salida.HabilitacionOpinionRepositoryPort;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.puerto.salida.OfertaCatalogoPort;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.puerto.salida.OpinionRepositoryPort;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * El turista deja su opinión sobre una oferta que pagó (RF-60). Queda en moderación (RF-61).
 *
 * <p>Orden de las reglas: primero los datos (400), luego que la oferta exista (404), que el turista la haya pagado
 * (403, DC-40) y que no haya opinado ya (409, RF-61). La última la respalda además el índice único de la base, por si
 * dos peticiones del mismo turista se cruzan.
 */
public class PublicarOpinionService implements PublicarOpinionUseCase {

    private final OpinionRepositoryPort opiniones;
    private final HabilitacionOpinionRepositoryPort habilitaciones;
    private final OfertaCatalogoPort ofertas;
    private final Clock reloj;

    public PublicarOpinionService(OpinionRepositoryPort opiniones, HabilitacionOpinionRepositoryPort habilitaciones,
                                  OfertaCatalogoPort ofertas, Clock reloj) {
        this.opiniones = opiniones;
        this.habilitaciones = habilitaciones;
        this.ofertas = ofertas;
        this.reloj = reloj;
    }

    @Override
    public OpinionConOferta publicar(PublicarOpinionCommand comando) {
        // MongoDB guarda las fechas en milisegundos: así la respuesta muestra la misma fecha que se lee después.
        Instant ahora = Instant.now(reloj).truncatedTo(ChronoUnit.MILLIS);
        Opinion opinion = Opinion.enviar(opiniones.nuevoId(), comando.ofertaId(), comando.turistaId(),
                comando.calificacion(), comando.comentario(), ahora);
        if (!ofertas.existe(comando.ofertaId())) {
            throw new OfertaNoEncontradaException(comando.ofertaId());
        }
        if (!habilitaciones.existe(comando.turistaId(), comando.ofertaId())) {
            throw new OpinionNoHabilitadaException(comando.turistaId(), comando.ofertaId());
        }
        if (opiniones.existeDe(comando.turistaId(), comando.ofertaId())) {
            throw new OpinionDuplicadaException(comando.turistaId(), comando.ofertaId());
        }
        opiniones.guardar(opinion);
        return new OpinionConOferta(opinion, ofertas.nombres(List.of(comando.ofertaId())).get(comando.ofertaId()));
    }
}
