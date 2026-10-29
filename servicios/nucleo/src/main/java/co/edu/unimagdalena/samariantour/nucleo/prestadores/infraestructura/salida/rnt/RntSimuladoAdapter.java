package co.edu.unimagdalena.samariantour.nucleo.prestadores.infraestructura.salida.rnt;

import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.modelo.ConsultaRnt;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.modelo.RegistroRnt;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.puerto.salida.RntVerificationPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * El RNT simulado (DN-33), la fuente por defecto: los prestadores de la demo son ficticios y no existen en el RNT real.
 * Se elige con {@code nucleo.rnt.fuente=simulada}. Los registros activos están en {@code application-demo.yml}; para
 * mostrar el retiro y la restitución (RF-77), {@link #retirar} y {@link #restituir} dan de baja y reactivan un número
 * en {@code rnt_simulado_bajas} (V12), que comparten las dos réplicas (DN-36). Los usa
 * {@link RntSimuladoDemoController}.
 */
@Component
@ConditionalOnProperty(name = "nucleo.rnt.fuente", havingValue = "simulada", matchIfMissing = true)
public class RntSimuladoAdapter implements RntVerificationPort {

    private final List<PropiedadesRntSimulado.Registro> registros;
    private final JdbcTemplate jdbc;

    public RntSimuladoAdapter(PropiedadesRntSimulado propiedades, JdbcTemplate jdbc) {
        this.registros = propiedades.registros();
        this.jdbc = jdbc;
    }

    @Override
    public ConsultaRnt consultar(String numeroRnt) {
        return registro(numeroRnt)
                .filter(registro -> !dadoDeBaja(numeroRnt))
                .<ConsultaRnt>map(registro -> new ConsultaRnt.Encontrado(new RegistroRnt(registro.numero(),
                        registro.razonSocial(), registro.municipio(), registro.categoria(), registro.subCategoria(),
                        registro.personaNatural(), registro.empleados())))
                .orElseGet(ConsultaRnt.NoEncontrado::new);
    }

    /** Da de baja un número de la tabla, como si el RNT lo hubiera cancelado. @return si estaba activo */
    public boolean retirar(String numeroRnt) {
        return registro(numeroRnt).isPresent() && jdbc.update(
                "INSERT INTO rnt_simulado_bajas (numero_rnt) VALUES (?) ON CONFLICT DO NOTHING", numeroRnt) == 1;
    }

    /** Reactiva un número que {@link #retirar} dio de baja. @return si estaba de baja */
    public boolean restituir(String numeroRnt) {
        return jdbc.update("DELETE FROM rnt_simulado_bajas WHERE numero_rnt = ?", numeroRnt) == 1;
    }

    private Optional<PropiedadesRntSimulado.Registro> registro(String numeroRnt) {
        return registros.stream().filter(registro -> Objects.equals(registro.numero(), numeroRnt)).findFirst();
    }

    private boolean dadoDeBaja(String numeroRnt) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM rnt_simulado_bajas WHERE numero_rnt = ?)", Boolean.class, numeroRnt));
    }
}
