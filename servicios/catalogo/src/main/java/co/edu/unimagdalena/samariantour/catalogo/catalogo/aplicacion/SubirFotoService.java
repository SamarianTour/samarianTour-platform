package co.edu.unimagdalena.samariantour.catalogo.catalogo.aplicacion;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.DestinoFoto;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Foto;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.SubirFotoUseCase;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.salida.FileStoragePort;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.salida.FotoSubidaRepositoryPort;

import java.time.Clock;
import java.util.UUID;

/**
 * El prestador sube una foto para sus ofertas (DC-47). No exige el RNT activo: subir una foto no publica nada, y
 * publicar o editar la oferta sí lo exige.
 */
public class SubirFotoService implements SubirFotoUseCase {

    private final RegistroDeFotos fotos;

    public SubirFotoService(FotoSubidaRepositoryPort registro, FileStoragePort almacenamiento, Clock reloj) {
        this.fotos = new RegistroDeFotos(registro, almacenamiento, reloj);
    }

    @Override
    public Foto subir(UUID prestadorId, byte[] imagen) {
        return fotos.subir(prestadorId, DestinoFoto.OFERTAS, imagen).foto();
    }
}
