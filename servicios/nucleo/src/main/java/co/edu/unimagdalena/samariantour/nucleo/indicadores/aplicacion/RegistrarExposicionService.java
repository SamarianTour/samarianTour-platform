package co.edu.unimagdalena.samariantour.nucleo.indicadores.aplicacion;

import co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.modelo.RegistroExposicion;
import co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.puerto.entrada.RegistrarExposicionUseCase;
import co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.puerto.salida.ExposicionRepositoryPort;
import org.springframework.transaction.annotation.Transactional;

/** Guarda lo que la recomendación mostró en una consulta, una fila por oferta (RET-02, DN-42). */
public class RegistrarExposicionService implements RegistrarExposicionUseCase {

    private final ExposicionRepositoryPort exposiciones;

    public RegistrarExposicionService(ExposicionRepositoryPort exposiciones) {
        this.exposiciones = exposiciones;
    }

    @Override
    @Transactional
    public void registrar(RegistroExposicion registro) {
        exposiciones.guardar(registro);
    }
}
