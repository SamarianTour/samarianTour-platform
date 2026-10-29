package co.edu.unimagdalena.samariantour.nucleo.pagos.infraestructura.salida.demo;

import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo.CuentaCobro;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.salida.ContactosPagosPort;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.salida.CuentasCobroPort;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * PROVISIONAL [SUPUESTO] (DN-26): la cuenta de cobro y el WhatsApp de cada prestador, y el del administrador, de la
 * tabla de la demo. Se reemplaza por adaptadores que llamen a la API de los módulos de prestadores y de cuentas cuando
 * existan (RF-74), sin tocar el dominio.
 */
@Component
public class CuentasCobroDemoAdapter implements CuentasCobroPort, ContactosPagosPort {

    private final PropiedadesDemoPagos demo;

    public CuentasCobroDemoAdapter(PropiedadesDemoPagos demo) {
        this.demo = demo;
    }

    @Override
    public Optional<CuentaCobro> cuentaVigente(UUID prestadorId) {
        return prestador(prestadorId).map(PropiedadesDemoPagos.Prestador::cuenta)
                .map(cuenta -> new CuentaCobro(cuenta.tipo(), cuenta.entidad(), cuenta.numero()));
    }

    @Override
    public Optional<String> whatsappAdministrador() {
        return Optional.ofNullable(demo.administrador()).map(PropiedadesDemoPagos.Administrador::whatsapp);
    }

    @Override
    public Optional<String> whatsappPrestador(UUID prestadorId) {
        return prestador(prestadorId).map(PropiedadesDemoPagos.Prestador::whatsapp);
    }

    private Optional<PropiedadesDemoPagos.Prestador> prestador(UUID prestadorId) {
        return demo.prestadores().stream().filter(prestador -> Objects.equals(prestador.id(), prestadorId))
                .findFirst();
    }
}
