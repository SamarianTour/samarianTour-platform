package co.edu.unimagdalena.samariantour.catalogo.catalogo.aplicacion;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Foto;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.FotoSubida;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.LimpiarFotosSinUsoUseCase;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.salida.FileStoragePort;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.salida.FotoSubidaRepositoryPort;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.salida.OfertaRepositoryPort;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.salida.PerfilRepositoryPort;

import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Borra las fotos que pasaron {@link FotoSubida#PLAZO_SIN_USO} sin uso y que ninguna oferta no retirada ni ningún perfil
 * tiene (DC-47). Recorre el registro por lotes, de la foto con el uso más antiguo a la más reciente.
 *
 * <p>Una foto que todavía está en uso se marca como vista en uso, para no revisarla otra vez hasta que pase el plazo.
 * Una foto que alguien asignó mientras corría la limpieza no se borra: el registro solo se borra si su uso no cambió.
 */
public class LimpiarFotosSinUsoService implements LimpiarFotosSinUsoUseCase {

    /** Fotos que se revisan por consulta. */
    static final int LOTE = 100;

    /** Lotes por ejecución, para que una ejecución termine pronto; lo que quede lo borra la siguiente. */
    static final int MAXIMO_LOTES = 50;

    private final FotoSubidaRepositoryPort registro;
    private final OfertaRepositoryPort ofertas;
    private final PerfilRepositoryPort perfiles;
    private final RegistroDeFotos fotos;

    public LimpiarFotosSinUsoService(FotoSubidaRepositoryPort registro, FileStoragePort almacenamiento,
                                     OfertaRepositoryPort ofertas, PerfilRepositoryPort perfiles, Clock reloj) {
        this.registro = registro;
        this.ofertas = ofertas;
        this.perfiles = perfiles;
        this.fotos = new RegistroDeFotos(registro, almacenamiento, reloj);
    }

    @Override
    public int limpiar() {
        int borradas = 0;
        for (int lote = 0; lote < MAXIMO_LOTES; lote++) {
            Instant ahora = fotos.ahora();
            List<FotoSubida> candidatas = registro.sinUsoDesde(ahora.minus(FotoSubida.PLAZO_SIN_USO), LOTE);
            Set<Foto> enUso = enUso(candidatas.stream().map(FotoSubida::foto).toList());
            if (!enUso.isEmpty()) {
                registro.marcarEnUso(enUso, ahora);
            }
            for (FotoSubida candidata : candidatas) {
                if (!enUso.contains(candidata.foto()) && fotos.descartar(candidata)) {
                    borradas++;
                }
            }
            if (candidatas.size() < LOTE) {
                break;
            }
        }
        return borradas;
    }

    private Set<Foto> enUso(List<Foto> candidatas) {
        if (candidatas.isEmpty()) {
            return Set.of();
        }
        Set<Foto> enUso = new HashSet<>(ofertas.fotosEnUso(candidatas));
        enUso.addAll(perfiles.fotosEnUso(candidatas));
        return enUso;
    }
}
