package co.edu.unimagdalena.samariantour.nucleo.prestadores.infraestructura.salida.rnt;

import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.modelo.ResultadoSincronizacion;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.puerto.entrada.SincronizarRntUseCase;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Las rutas de la demo que dan de baja y restituyen un número en el RNT simulado (DN-36), con el rol ADMINISTRADOR.
 * Cada una sincroniza enseguida, para ver el retiro del catálogo (RF-77) y el aviso al prestador (RF-78) sin esperar
 * la tarea semanal. No son parte del contrato, como la pasarela simulada: solo existen con el perfil demo y el RNT
 * simulado.
 */
@RestController
@Profile("demo")
@ConditionalOnProperty(name = "nucleo.rnt.fuente", havingValue = "simulada", matchIfMissing = true)
@RequestMapping("/admin/rnt-simulado/{numeroRnt}")
public class RntSimuladoDemoController {

    private final RntSimuladoAdapter rnt;
    private final SincronizarRntUseCase sincronizacion;

    public RntSimuladoDemoController(RntSimuladoAdapter rnt, SincronizarRntUseCase sincronizacion) {
        this.rnt = rnt;
        this.sincronizacion = sincronizacion;
    }

    /** 404 si el número no está activo en la tabla. */
    @DeleteMapping
    public ResponseEntity<ResultadoSincronizacion> darDeBaja(@PathVariable String numeroRnt) {
        return rnt.retirar(numeroRnt) ? ResponseEntity.ok(sincronizacion.sincronizar())
                : ResponseEntity.notFound().build();
    }

    /** 404 si el número no se dio de baja con la ruta anterior. */
    @PutMapping
    public ResponseEntity<ResultadoSincronizacion> restituir(@PathVariable String numeroRnt) {
        return rnt.restituir(numeroRnt) ? ResponseEntity.ok(sincronizacion.sincronizar())
                : ResponseEntity.notFound().build();
    }
}
