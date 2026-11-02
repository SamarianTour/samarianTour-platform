package co.edu.unimagdalena.samariantour.nucleo.prestadores.infraestructura.entrada.modulos;

import co.edu.unimagdalena.samariantour.nucleo.prestadores.api.CuentaCobroVigente;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.api.PrestadorAdministrado;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.api.PrestadorConsultado;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.api.PrestadoresApiInterna;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.api.SincronizacionRnt;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.modelo.Prestador;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.modelo.ResultadoSincronizacion;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.puerto.entrada.AsignarOperadorUseCase;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.puerto.entrada.ConsultarPrestadorUseCase;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.puerto.entrada.SincronizarRntUseCase;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * La entrada de los demás módulos a los prestadores (DN-35), como un controlador: traduce a los casos de uso y entrega
 * los tipos del {@code api}, nunca los del dominio (regla 11 de {@code ArquitecturaTest}).
 */
@Component
public class PrestadoresApiInternaAdapter implements PrestadoresApiInterna {

    private final ConsultarPrestadorUseCase consultas;
    private final SincronizarRntUseCase sincronizacion;
    private final AsignarOperadorUseCase asignacion;

    public PrestadoresApiInternaAdapter(ConsultarPrestadorUseCase consultas, SincronizarRntUseCase sincronizacion,
                                        AsignarOperadorUseCase asignacion) {
        this.consultas = consultas;
        this.sincronizacion = sincronizacion;
        this.asignacion = asignacion;
    }

    @Override
    public Optional<PrestadorConsultado> consultar(UUID prestadorId) {
        return consultas.consultar(prestadorId).map(prestador -> new PrestadorConsultado(prestador.id(),
                prestador.tipo().name(), prestador.categoria().name(), prestador.estaHabilitado(),
                prestador.operadorId(), prestador.whatsappContactable()));
    }

    @Override
    public Optional<CuentaCobroVigente> cuentaCobroVigente(UUID prestadorId) {
        return consultas.cuentaCobroVigente(prestadorId)
                .map(cuenta -> new CuentaCobroVigente(cuenta.tipo().name(), cuenta.entidad(), cuenta.numero()));
    }

    @Override
    public SincronizacionRnt sincronizarRnt() {
        ResultadoSincronizacion resultado = sincronizacion.sincronizar();
        return new SincronizacionRnt(resultado.revisados(), resultado.retirados(), resultado.restituidos(),
                resultado.sinRespuesta(), resultado.interrumpida());
    }

    @Override
    public List<PrestadorAdministrado> prestadores(boolean soloSinOperador) {
        return consultas.todos(soloSinOperador).stream().map(PrestadoresApiInternaAdapter::administrado).toList();
    }

    @Override
    public Optional<PrestadorAdministrado> asignarOperador(UUID prestadorId, UUID operadorId) {
        return asignacion.asignar(prestadorId, operadorId).map(PrestadoresApiInternaAdapter::administrado);
    }

    @Override
    public long prestadoresAsignados(UUID operadorId) {
        return consultas.asignadosA(operadorId);
    }

    private static PrestadorAdministrado administrado(Prestador prestador) {
        return new PrestadorAdministrado(prestador.id(), prestador.numeroRnt(), prestador.razonSocial(),
                prestador.tipo().name(), prestador.estadoRnt().name(), prestador.operadorId(),
                prestador.fechaRegistro());
    }
}
