package co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.ConsultarFotoUseCase;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest.mapper.PerfilWebMapper;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

/**
 * Entrega las fotos al navegador (DC-39). MinIO no se expone hacia afuera: el catálogo es la única puerta.
 *
 * <p>Una foto nunca cambia (cambiarla es subir otra, con otro id), así que el navegador puede guardarla un año sin
 * volver a preguntar. Eso ahorra datos en red móvil (QAS-07, RT-03).
 */
@RestController
public class FotoController {

    private static final CacheControl UN_ANO_SIN_CAMBIOS = CacheControl.maxAge(Duration.ofDays(365))
            .cachePublic()
            .immutable();

    private final ConsultarFotoUseCase fotos;

    public FotoController(ConsultarFotoUseCase fotos) {
        this.fotos = fotos;
    }

    @GetMapping("/fotos/{fotoId}/{tamano}")
    public ResponseEntity<byte[]> foto(@PathVariable String fotoId, @PathVariable String tamano) {
        byte[] jpg = fotos.consultar(fotoId, PerfilWebMapper.aTamano(fotoId, tamano));
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .cacheControl(UN_ANO_SIN_CAMBIOS)
                // El navegador no debe adivinar otro tipo de contenido que el declarado.
                .header("X-Content-Type-Options", "nosniff")
                .body(jpg);
    }
}
