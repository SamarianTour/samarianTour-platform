package co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.SubirFotoUseCase;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest.dto.FotoResponse;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest.mapper.FotoWebMapper;
import co.edu.unimagdalena.samariantour.catalogo.compartido.seguridad.Identidad;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * El prestador sube fotos para sus ofertas (DC-47), con el rol PRESTADOR (DC-13). La foto queda suelta hasta que la
 * pone en una oferta al publicarla o editarla; si no la usa en 24 horas, la limpieza la borra.
 */
@RestController
@RequestMapping("/prestador/fotos")
public class FotoPrestadorController {

    private final SubirFotoUseCase fotos;

    public FotoPrestadorController(SubirFotoUseCase fotos) {
        this.fotos = fotos;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public FotoResponse subir(@RequestPart("foto") MultipartFile foto, HttpServletRequest peticion) {
        return FotoWebMapper.aRespuesta(fotos.subir(Identidad.de(peticion).usuarioId(), ArchivoSubido.contenido(foto)));
    }
}
