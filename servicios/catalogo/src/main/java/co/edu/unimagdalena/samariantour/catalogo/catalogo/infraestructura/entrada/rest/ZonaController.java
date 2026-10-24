package co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.ConsultarZonasUseCase;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest.dto.ZonaResponse;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest.mapper.ZonaWebMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Zonas activas, para filtrar la búsqueda y elegir la zona de una oferta (RF-14), y la ficha de una zona (RF-25).
 */
@RestController
@RequestMapping("/zonas")
public class ZonaController {

    private final ConsultarZonasUseCase consultarZonas;

    public ZonaController(ConsultarZonasUseCase consultarZonas) {
        this.consultarZonas = consultarZonas;
    }

    @GetMapping
    public List<ZonaResponse> activas() {
        return consultarZonas.consultarActivas().stream().map(ZonaWebMapper::aRespuesta).toList();
    }

    /** Ficha de la zona con sus buenas prácticas (RF-25); también de una inactiva, cuyas ofertas siguen visibles. */
    @GetMapping("/{zonaId}")
    public ZonaResponse ficha(@PathVariable String zonaId) {
        return ZonaWebMapper.aRespuesta(consultarZonas.consultar(zonaId));
    }
}
