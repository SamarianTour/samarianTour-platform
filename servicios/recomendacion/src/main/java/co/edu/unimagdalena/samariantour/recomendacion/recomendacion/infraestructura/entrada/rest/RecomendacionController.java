package co.edu.unimagdalena.samariantour.recomendacion.recomendacion.infraestructura.entrada.rest;

import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.puerto.entrada.RecomendarOfertasUseCase;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.infraestructura.entrada.rest.dto.RecomendacionRequest;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.infraestructura.entrada.rest.dto.RecomendacionesResponse;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.infraestructura.entrada.rest.mapper.RecomendacionWebMapper;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code POST /api/recomendacion/recomendaciones} (RF-26, RF-27). Pública: la sesión solo se exige al enviar una
 * solicitud. POST y no GET para que el texto libre no quede en la URL ni en los logs de Nginx (DR-09).
 */
@RestController
@RequestMapping("/recomendaciones")
public class RecomendacionController {

    private final RecomendarOfertasUseCase recomendarOfertas;

    public RecomendacionController(RecomendarOfertasUseCase recomendarOfertas) {
        this.recomendarOfertas = recomendarOfertas;
    }

    @PostMapping
    public RecomendacionesResponse recomendar(@RequestBody RecomendacionRequest consulta) {
        return RecomendacionWebMapper.aRespuesta(
                recomendarOfertas.recomendar(RecomendacionWebMapper.aPreferencias(consulta)));
    }
}
