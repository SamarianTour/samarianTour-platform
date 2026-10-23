package co.edu.unimagdalena.samariantour.catalogo.catalogo.aplicacion;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.DatoInvalidoException;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.DestinoFoto;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Foto;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.FotoSubida;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.salida.FileStoragePort;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.salida.FotoSubidaRepositoryPort;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * Sube y descarta fotos manteniendo juntos el registro y el archivo (DC-47). El registro se crea antes que el archivo y
 * se borra antes que él: si algo falla en medio, lo que queda es un registro sin uso, que la limpieza borra.
 */
final class RegistroDeFotos {

    private static final System.Logger LOG = System.getLogger(RegistroDeFotos.class.getName());

    private final FotoSubidaRepositoryPort registro;
    private final FileStoragePort almacenamiento;
    private final Clock reloj;

    RegistroDeFotos(FotoSubidaRepositoryPort registro, FileStoragePort almacenamiento, Clock reloj) {
        this.registro = registro;
        this.almacenamiento = almacenamiento;
        this.reloj = reloj;
    }

    /** @throws DatoInvalidoException si no llegó nada o pesa más de 5 MB; el formato lo revisa el almacenamiento */
    static void validar(byte[] imagen) {
        if (imagen == null || imagen.length == 0) {
            throw new DatoInvalidoException("foto", "no llegó ninguna imagen");
        }
        if (imagen.length > Foto.TAMANO_MAXIMO_BYTES) {
            throw new DatoInvalidoException("foto", "pesa más de 5 MB");
        }
    }

    /**
     * Registra la foto como del prestador y la guarda en el almacenamiento.
     *
     * @throws DatoInvalidoException si no llegó nada, pesa más de 5 MB o no es una imagen aceptada
     */
    FotoSubida subir(UUID prestadorId, DestinoFoto destino, byte[] imagen) {
        validar(imagen);
        FotoSubida subida = new FotoSubida(Foto.nueva(), prestadorId, destino, ahora());
        registro.registrar(subida);
        try {
            almacenamiento.guardar(subida.foto(), imagen);
        } catch (RuntimeException e) {
            descartar(subida);
            throw e;
        }
        return subida;
    }

    /**
     * Borra la foto si su uso no cambió desde que se leyó: primero el registro y después el archivo. Si alguien la usó
     * entre medio, no borra nada.
     *
     * @return true si la borró
     */
    boolean descartar(FotoSubida leida) {
        if (!registro.eliminarSiSigueSinUso(leida)) {
            return false;
        }
        borrarArchivo(leida.foto());
        return true;
    }

    /**
     * Ningún perfil ni oferta la tiene. Si el almacenamiento falla, queda un archivo huérfano que no afecta a nadie; se
     * registra en el log para poder limpiarlo.
     */
    void borrarArchivo(Foto foto) {
        try {
            almacenamiento.eliminar(foto);
        } catch (RuntimeException e) {
            LOG.log(System.Logger.Level.WARNING, "No se pudo borrar la foto " + foto.id() + " del almacenamiento", e);
        }
    }

    Instant ahora() {
        return Instant.now(reloj);
    }
}
