package co.edu.unimagdalena.samariantour.recomendacion.recomendacion.infraestructura.config;

import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.CatalogoDePrueba;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.puerto.salida.CatalogoConsultaPort;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.infraestructura.entrada.programada.AlertasScheduler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Las lecturas programadas de alertas siguen encendidas por defecto y solo se apagan con la propiedad (DR-16). El
 * catálogo es un doble sin Mockito: si la lectura del arranque corre, no hay nada que configurar a la vez.
 */
class AlertasConfigTest {

    private final ApplicationContextRunner contexto = new ApplicationContextRunner()
            .withUserConfiguration(AlertasConfig.class)
            .withBean(CatalogoConsultaPort.class, () -> CatalogoDePrueba.de(List.of()))
            .withBean(ObjectMapper.class, ObjectMapper::new);

    @Test
    @DisplayName("sin la propiedad, como en producción y en Docker Compose, las tareas programadas existen")
    void porDefecto() {
        contexto.run(c -> assertThat(c).hasSingleBean(AlertasScheduler.class));
    }

    @Test
    @DisplayName("con recomendacion.alertas.programadas=false no se registran")
    void apagadas() {
        contexto.withPropertyValues("recomendacion.alertas.programadas=false")
                .run(c -> assertThat(c).doesNotHaveBean(AlertasScheduler.class));
    }
}
