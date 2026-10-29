package co.edu.unimagdalena.samariantour.nucleo.cuentas.infraestructura.salida.persistencia;

import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.PoliticaDatos;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.salida.PoliticaDatosRepositoryPort;
import org.springframework.stereotype.Component;

/** Las versiones de la política en {@code politicas_datos} (V8, DN-02). */
@Component
public class PoliticaDatosJpaAdapter implements PoliticaDatosRepositoryPort {

    private final PoliticaDatosJpaRepository politicas;

    PoliticaDatosJpaAdapter(PoliticaDatosJpaRepository politicas) {
        this.politicas = politicas;
    }

    @Override
    public PoliticaDatos vigente() {
        return politicas.findFirstByOrderByFechaPublicacionDescVersionDesc()
                .map(fila -> new PoliticaDatos(fila.version(), fila.fechaPublicacion(), fila.urlTexto()))
                .orElseThrow(() -> new IllegalStateException("No hay ninguna política de datos publicada (V8)"));
    }
}
