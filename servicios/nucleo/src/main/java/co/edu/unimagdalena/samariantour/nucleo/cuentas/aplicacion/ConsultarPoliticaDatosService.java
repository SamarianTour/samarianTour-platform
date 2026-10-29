package co.edu.unimagdalena.samariantour.nucleo.cuentas.aplicacion;

import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.PoliticaDatos;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.entrada.ConsultarPoliticaDatosUseCase;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.salida.PoliticaDatosRepositoryPort;

/** La versión vigente de la política de datos (RF-09, RF-10). */
public class ConsultarPoliticaDatosService implements ConsultarPoliticaDatosUseCase {

    private final PoliticaDatosRepositoryPort politicas;

    public ConsultarPoliticaDatosService(PoliticaDatosRepositoryPort politicas) {
        this.politicas = politicas;
    }

    @Override
    public PoliticaDatos vigente() {
        return politicas.vigente();
    }
}
