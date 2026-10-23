package co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.GestionarPerfilPublicoUseCase;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest.dto.PerfilPublicoResponse;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest.dto.PerfilRequest;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest.mapper.PerfilWebMapper;
import co.edu.unimagdalena.samariantour.catalogo.compartido.seguridad.Identidad;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * El prestador gestiona su perfil público (RF-64, RF-72, DC-39), con el rol PRESTADOR (DC-13). Siempre es el perfil
 * del prestador de la identidad: la ruta no lleva su id, así que nadie puede tocar el perfil de otro.
 */
@RestController
@RequestMapping("/prestador/perfil")
public class PerfilPrestadorController {

    private final GestionarPerfilPublicoUseCase perfiles;

    public PerfilPrestadorController(GestionarPerfilPublicoUseCase perfiles) {
        this.perfiles = perfiles;
    }

    @GetMapping
    public PerfilPublicoResponse consultar(HttpServletRequest peticion) {
        return PerfilWebMapper.aRespuesta(perfiles.consultar(prestador(peticion)));
    }

    /** Crea el perfil o reemplaza sus datos; se publica de inmediato, sin moderación. */
    @PutMapping
    public PerfilPublicoResponse guardar(@RequestBody PerfilRequest perfil, HttpServletRequest peticion) {
        return PerfilWebMapper.aRespuesta(perfiles.guardar(PerfilWebMapper.aComando(prestador(peticion), perfil)));
    }

    @PostMapping(path = "/fotos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public PerfilPublicoResponse agregarFoto(@RequestPart("foto") MultipartFile foto, HttpServletRequest peticion) {
        return PerfilWebMapper.aRespuesta(perfiles.agregarFoto(prestador(peticion), ArchivoSubido.contenido(foto)));
    }

    @DeleteMapping("/fotos/{fotoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void quitarFoto(@PathVariable String fotoId, HttpServletRequest peticion) {
        perfiles.quitarFoto(prestador(peticion), fotoId);
    }

    private static UUID prestador(HttpServletRequest peticion) {
        return Identidad.de(peticion).usuarioId();
    }
}
