package co.edu.unimagdalena.samariantour.catalogo.catalogo.aplicacion;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.DatoInvalidoException;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.PerfilNoEncontradoException;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.DestinoFoto;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Foto;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.FotoSubida;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.PerfilPublico;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.GestionarPerfilPublicoUseCase;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.comando.GuardarPerfilCommand;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.salida.FileStoragePort;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.salida.FotoSubidaRepositoryPort;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.salida.PerfilRepositoryPort;
import co.edu.unimagdalena.samariantour.catalogo.compartido.concurrencia.EjecutorConReintento;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

/**
 * El prestador gestiona su perfil público (RF-64, RF-72, DC-39). Cada cambio relee el perfil y lo guarda con bloqueo
 * optimista, reintentando si otra escritura se cruzó (ADR-05): por ejemplo, dos fotos subidas a la vez.
 *
 * <p>La foto se guarda en el almacenamiento antes que en el perfil, fuera del reintento, porque procesarla es lo más
 * costoso. Si el perfil no se puede guardar, la foto se borra para no dejarla huérfana. Las fotos del perfil solo las
 * usa el perfil ({@link DestinoFoto#PERFIL}), así que quitar una la borra de inmediato (DC-47).
 */
public class GestionarPerfilPublicoService implements GestionarPerfilPublicoUseCase {

    private final PerfilRepositoryPort perfiles;
    private final FotoSubidaRepositoryPort registro;
    private final RegistroDeFotos fotos;
    private final EjecutorConReintento reintento;

    public GestionarPerfilPublicoService(PerfilRepositoryPort perfiles, FotoSubidaRepositoryPort registro,
                                         FileStoragePort almacenamiento, Clock reloj,
                                         EjecutorConReintento reintento) {
        this.perfiles = perfiles;
        this.registro = registro;
        this.fotos = new RegistroDeFotos(registro, almacenamiento, reloj);
        this.reintento = reintento;
    }

    @Override
    public PerfilPublico consultar(UUID prestadorId) {
        return perfiles.buscarPorPrestador(prestadorId).orElseThrow(() -> new PerfilNoEncontradoException(prestadorId));
    }

    @Override
    public PerfilPublico guardar(GuardarPerfilCommand comando) {
        return reintento.ejecutar(() -> {
            PerfilPublico perfil = perfiles.buscarPorPrestador(comando.prestadorId())
                    .map(actual -> actual.conDatos(comando.nombreComercial(), comando.descripcion(),
                            comando.horariosAtencion(), comando.sitioWeb(), comando.redesSociales()))
                    .orElseGet(() -> new PerfilPublico(comando.prestadorId(), comando.nombreComercial(),
                            comando.descripcion(), List.of(), comando.horariosAtencion(), comando.sitioWeb(),
                            comando.redesSociales()));
            perfiles.guardar(perfil);
            return perfil;
        });
    }

    @Override
    public PerfilPublico agregarFoto(UUID prestadorId, byte[] imagen) {
        RegistroDeFotos.validar(imagen);
        // Falla antes de procesar la imagen si no hay perfil o ya tiene todas sus fotos.
        consultar(prestadorId).verificarCupoDeFotos();
        FotoSubida subida = fotos.subir(prestadorId, DestinoFoto.PERFIL, imagen);
        try {
            return reintento.ejecutar(() -> {
                PerfilPublico perfil = consultar(prestadorId).agregarFoto(subida.foto());
                perfiles.guardar(perfil);
                return perfil;
            });
        } catch (RuntimeException e) {
            fotos.descartar(subida);
            throw e;
        }
    }

    @Override
    public void quitarFoto(UUID prestadorId, String fotoId) {
        Foto foto;
        try {
            foto = new Foto(fotoId);
        } catch (DatoInvalidoException e) {
            // Un identificador que no es de foto no puede estar en el perfil: no hay nada que quitar.
            consultar(prestadorId);
            return;
        }
        boolean quitada = reintento.ejecutar(() -> {
            PerfilPublico perfil = consultar(prestadorId);
            if (!perfil.tieneFoto(foto)) {
                return false;
            }
            perfiles.guardar(perfil.quitarFoto(foto));
            return true;
        });
        if (quitada) {
            borrar(foto);
        }
    }

    /**
     * El perfil ya no la tiene, y nadie más la usa. Una foto subida antes de DC-47 no tiene registro: se borra el
     * archivo directamente.
     */
    private void borrar(Foto foto) {
        registro.buscar(List.of(foto)).stream().findFirst()
                .ifPresentOrElse(fotos::descartar, () -> fotos.borrarArchivo(foto));
    }
}
