package co.edu.unimagdalena.samariantour.nucleo.prestadores.aplicacion;

import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.modelo.CuentaCobro;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.modelo.Prestador;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.puerto.entrada.ConsultarPrestadorUseCase;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.puerto.salida.CuentaCobroRepositoryPort;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.puerto.salida.PrestadorRepositoryPort;

import java.util.Optional;
import java.util.UUID;

/** Las consultas de los demás módulos sobre los prestadores (DN-35). */
public class ConsultarPrestadorService implements ConsultarPrestadorUseCase {

    private final PrestadorRepositoryPort prestadores;
    private final CuentaCobroRepositoryPort cuentas;

    public ConsultarPrestadorService(PrestadorRepositoryPort prestadores, CuentaCobroRepositoryPort cuentas) {
        this.prestadores = prestadores;
        this.cuentas = cuentas;
    }

    @Override
    public Optional<Prestador> consultar(UUID prestadorId) {
        return prestadores.porId(prestadorId);
    }

    @Override
    public Optional<CuentaCobro> cuentaCobroVigente(UUID prestadorId) {
        return cuentas.vigente(prestadorId);
    }
}
