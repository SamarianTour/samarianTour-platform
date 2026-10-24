package co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Zona;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.GestionarZonasUseCase;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest.dto.ZonaRequest;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest.dto.ZonaResponse;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest.mapper.ZonaWebMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;

/**
 * El administrador gestiona y marca las zonas (HU-ADM-06, RF-84, DC-49), con el rol ADMINISTRADOR (DC-13). Escribe
 * directo en el catálogo, que es el dueño de las zonas: a diferencia de la moderación, no pasa por el núcleo.
 */
@RestController
@RequestMapping("/admin/zonas")
public class ZonaAdminController {

    private final GestionarZonasUseCase zonas;

    public ZonaAdminController(GestionarZonasUseCase zonas) {
        this.zonas = zonas;
    }

    /** Activas e inactivas, ordenadas por nombre. */
    @GetMapping
    public List<ZonaResponse> todas() {
        return zonas.listarTodas().stream().map(ZonaWebMapper::aRespuesta).toList();
    }

    /** Responde 201 con la zona y su dirección pública en {@code Location}. */
    @PostMapping
    public ResponseEntity<ZonaResponse> registrar(@RequestBody ZonaRequest peticion) {
        Zona zona = zonas.registrar(ZonaWebMapper.aDatos(peticion));
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentContextPath()
                        .path("/zonas/{id}").buildAndExpand(zona.id()).toUri())
                .body(ZonaWebMapper.aRespuesta(zona));
    }

    /** Reemplaza todos los datos; con {@code activa} en false la inactiva y en true la reactiva. */
    @PutMapping("/{zonaId}")
    public ZonaResponse editar(@PathVariable String zonaId, @RequestBody ZonaRequest peticion) {
        boolean activa = ZonaWebMapper.activa(peticion);
        return ZonaWebMapper.aRespuesta(zonas.editar(zonaId, ZonaWebMapper.aDatos(peticion), activa));
    }
}
