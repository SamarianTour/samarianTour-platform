package co.edu.unimagdalena.samariantour.nucleo.pagos.infraestructura.salida.modulos;

import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo.CuentaCobro;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo.TipoCuenta;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.salida.ContactosPagosPort;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.salida.CuentasCobroPort;
import co.edu.unimagdalena.samariantour.nucleo.pagos.infraestructura.salida.demo.PropiedadesDemoPagos;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.api.PrestadorConsultado;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.api.PrestadoresApiInterna;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * La cuenta de cobro vigente y el WhatsApp del prestador, de su módulo (RF-54, RF-71, RF-75, DN-35). El WhatsApp del
 * administrador sigue siendo el provisional de la demo (DN-26).
 */
@Component
public class PrestadoresPagosAdapter implements CuentasCobroPort, ContactosPagosPort {

    private final PrestadoresApiInterna prestadores;
    private final PropiedadesDemoPagos demo;

    public PrestadoresPagosAdapter(PrestadoresApiInterna prestadores, PropiedadesDemoPagos demo) {
        this.prestadores = prestadores;
        this.demo = demo;
    }

    @Override
    public Optional<CuentaCobro> cuentaVigente(UUID prestadorId) {
        return prestadores.cuentaCobroVigente(prestadorId)
                .map(cuenta -> new CuentaCobro(TipoCuenta.valueOf(cuenta.tipo()), cuenta.entidad(), cuenta.numero()));
    }

    @Override
    public Optional<String> whatsappAdministrador() {
        return Optional.ofNullable(demo.administrador()).map(PropiedadesDemoPagos.Administrador::whatsapp);
    }

    @Override
    public Optional<String> whatsappPrestador(UUID prestadorId) {
        return prestadores.consultar(prestadorId).map(PrestadorConsultado::whatsapp);
    }
}
