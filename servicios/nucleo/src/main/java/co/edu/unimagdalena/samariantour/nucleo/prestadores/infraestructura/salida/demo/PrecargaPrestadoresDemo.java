package co.edu.unimagdalena.samariantour.nucleo.prestadores.infraestructura.salida.demo;

import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.excepcion.RntYaRegistradoException;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.modelo.CuentaCobro;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.modelo.DatosTipo;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.modelo.EstadoRnt;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.modelo.ModalidadGuianza;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.modelo.Prestador;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.modelo.TipoCuenta;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.modelo.TipoPrestador;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.puerto.salida.CuentaCobroRepositoryPort;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.puerto.salida.PrestadorRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Los prestadores de la demostración (DN-35), con los identificadores del catálogo y de su selector de usuario: su RNT
 * de la tabla simulada, su WhatsApp verificado, su operador y su cuenta de cobro. Solo con el perfil demo; corre
 * después de {@code PrecargaCuentasDemo}, porque cada prestador apunta a su cuenta. La habilitación arranca con la
 * versión que el catálogo ya tiene de ellos (DC-02): 1 para los activos y 2 para la agencia sin RNT, así su primer
 * cambio llega con una versión mayor. Si las dos réplicas arrancan a la vez, la segunda encuentra al prestador y lo
 * salta.
 */
@Component
@Profile("demo")
@Order(2)
public class PrecargaPrestadoresDemo implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(PrecargaPrestadoresDemo.class);
    private static final UUID OPERADOR_1 = UUID.fromString("60000000-0000-0000-0000-000000000001");
    private static final UUID OPERADOR_2 = UUID.fromString("60000000-0000-0000-0000-000000000002");

    /**
     * Un prestador de la demo; los números son ficticios.
     *
     * @param operadorId vacío: la agencia sin RNT no tiene operador (muestra RF-32)
     */
    record PrestadorDemo(String id, String numeroRnt, DatosTipo datos, String razonSocial, String categoria,
                         boolean personaNatural, boolean activo, String whatsapp, UUID operadorId,
                         TipoCuenta tipoCuenta, String entidad, String numeroCuenta) {
    }

    static final List<PrestadorDemo> PRESTADORES = List.of(
            new PrestadorDemo("10000000-0000-0000-0000-000000000001", "910001",
                    new DatosTipo(TipoPrestador.AGENCIA_OPERADORA, 12, List.of(), List.of()),
                    "MAR Y SELVA EXPEDICIONES S.A.S.", "AGENCIAS DE VIAJES", false, true, "+573001110101", OPERADOR_1,
                    TipoCuenta.AHORROS, "Bancolombia", "40112233440101"),
            new PrestadorDemo("20000000-0000-0000-0000-000000000002", "910002",
                    new DatosTipo(TipoPrestador.GUIA_INDEPENDIENTE, null,
                            List.of(ModalidadGuianza.CAMINATA), List.of("es", "en")),
                    "JUAN CARLOS PEREZ ARIAS", "GUIAS DE TURISMO", true, true, "+573001110202", OPERADOR_1,
                    TipoCuenta.NEQUI, "Nequi", "3001110202"),
            new PrestadorDemo("30000000-0000-0000-0000-000000000003", "910003",
                    new DatosTipo(TipoPrestador.ALOJAMIENTO, 25, List.of(), List.of()),
                    "POSADAS SIERRA Y MAR S.A.S.", "ESTABLECIMIENTOS DE ALOJAMIENTO TURÍSTICO", false, true,
                    "+573001110303", OPERADOR_2, TipoCuenta.CORRIENTE, "Banco de Bogotá", "00987654320303"),
            // Su cuenta está configurada para fallar en la dispersión simulada (nucleo.pagos.simulada, DN-24).
            new PrestadorDemo("40000000-0000-0000-0000-000000000004", "910004",
                    new DatosTipo(TipoPrestador.RESTAURANTE, 8, List.of(), List.of()),
                    "FOGON DEL CARIBE S.A.S.", "ESTABLECIMIENTOS DE GASTRONOMÍA", false, true, "+573001110404",
                    OPERADOR_2, TipoCuenta.DAVIPLATA, "Daviplata", "3001110404"),
            // Su RNT se cayó: no está en la tabla simulada y el catálogo ya la tiene inhabilitada.
            new PrestadorDemo("50000000-0000-0000-0000-000000000005", "910005",
                    new DatosTipo(TipoPrestador.AGENCIA_OPERADORA, 5, List.of(), List.of()),
                    "TURISMO SIERRA NEVADA LTDA.", "AGENCIAS DE VIAJES", false, false, "+573001110505", null,
                    null, null, null));

    private final PrestadorRepositoryPort prestadores;
    private final CuentaCobroRepositoryPort cuentas;
    private final TransactionTemplate transaccion;
    private final Clock reloj;

    PrecargaPrestadoresDemo(PrestadorRepositoryPort prestadores, CuentaCobroRepositoryPort cuentas,
                            TransactionTemplate transaccion, Clock reloj) {
        this.prestadores = prestadores;
        this.cuentas = cuentas;
        this.transaccion = transaccion;
        this.reloj = reloj;
    }

    @Override
    public void run(ApplicationArguments argumentos) {
        Instant ahora = reloj.instant();
        int creados = 0;
        for (PrestadorDemo demo : PRESTADORES) {
            UUID id = UUID.fromString(demo.id());
            try {
                Boolean creado = transaccion.execute(estado -> {
                    if (prestadores.porId(id).isPresent()) {
                        return false;
                    }
                    prestadores.insertar(prestador(id, demo, ahora));
                    if (demo.tipoCuenta() != null) {
                        cuentas.reemplazar(null, CuentaCobro.nueva(UUID.randomUUID(), id, demo.tipoCuenta(),
                                demo.entidad(), demo.numeroCuenta(), demo.razonSocial(), ahora));
                    }
                    return true;
                });
                creados += Boolean.TRUE.equals(creado) ? 1 : 0;
            } catch (RntYaRegistradoException | DataIntegrityViolationException e) {
                // La otra réplica lo creó entre la consulta y la inserción.
            }
        }
        LOG.info("Prestadores de la demo: {} creados, {} ya estaban", creados, PRESTADORES.size() - creados);
    }

    private static Prestador prestador(UUID id, PrestadorDemo demo, Instant ahora) {
        Prestador prestador = new Prestador(id, demo.numeroRnt(), demo.datos(), demo.razonSocial(), "SANTA MARTA",
                demo.categoria(), demo.personaNatural(), demo.activo() ? EstadoRnt.ACTIVO : EstadoRnt.INACTIVO, true,
                ahora, null, false, null, demo.operadorId(), demo.activo(), demo.activo() ? 1 : 2, ahora, null);
        prestador.cambiarWhatsapp(demo.whatsapp());
        prestador.verificarWhatsapp();
        return prestador;
    }
}
