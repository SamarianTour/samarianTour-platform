package co.edu.unimagdalena.samariantour.nucleo.cuentas;

import co.edu.unimagdalena.samariantour.nucleo.IntegracionPostgresBase;
import co.edu.unimagdalena.samariantour.nucleo.compartido.concurrencia.ConflictoConcurrenciaException;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.excepcion.CorreoYaRegistradoException;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.AceptacionPolitica;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.CodigoUnUso;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.PropositoCodigo;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.Rol;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.Sesion;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.Turista;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.Usuario;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.salida.CodigoRepositoryPort;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.salida.PasswordHasherPort;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.salida.PoliticaDatosRepositoryPort;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.salida.SesionRepositoryPort;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.salida.UsuarioRepositoryPort;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.infraestructura.salida.demo.PrecargaCuentasDemo;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.infraestructura.salida.persistencia.V9__crear_administrador_inicial;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.UUID;

import static co.edu.unimagdalena.samariantour.nucleo.cuentas.CuentasDePrueba.AHORA;
import static co.edu.unimagdalena.samariantour.nucleo.cuentas.CuentasDePrueba.turista;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Las cuentas en PostgreSQL (V8, V9, DN-18): cifrado, correo único, bloqueo optimista y las cuentas de la demo. */
@SpringBootTest(properties = {"nucleo.bandeja.publicacion.cron=-", "nucleo.solicitudes.vencimiento.cron=-",
        "nucleo.solicitudes.notificaciones.cron=-"})
@ActiveProfiles("demo")
class CuentasPersistenciaIT extends IntegracionPostgresBase {

    private static final String DE_PRUEBA = "%@prueba.co";

    @Autowired
    private UsuarioRepositoryPort usuarios;

    @Autowired
    private CodigoRepositoryPort codigos;

    @Autowired
    private SesionRepositoryPort sesiones;

    @Autowired
    private PoliticaDatosRepositoryPort politicas;

    @Autowired
    private PasswordHasherPort hasher;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void vaciar() {
        String deEstasPruebas = "usuario_id IN (SELECT id FROM usuarios WHERE correo LIKE '" + DE_PRUEBA + "')";
        jdbc.update("DELETE FROM sesiones WHERE " + deEstasPruebas);
        jdbc.update("DELETE FROM codigos_un_uso WHERE " + deEstasPruebas);
        jdbc.update("DELETE FROM aceptaciones_politica WHERE " + deEstasPruebas);
        jdbc.update("DELETE FROM solicitudes_supresion WHERE " + deEstasPruebas);
        jdbc.update("DELETE FROM usuarios WHERE correo LIKE ?", DE_PRUEBA);
    }

    @Test
    @DisplayName("guarda el turista y lo lee igual; el nombre y el WhatsApp quedan cifrados en la tabla")
    void idaYVueltaCifrada() {
        Turista turista = turista("laura@prueba.co", "+573001234567", true);
        turista.aceptarPolitica(politicas.vigente(), AHORA);

        usuarios.insertar(turista);

        Turista leido = (Turista) usuarios.porCorreo("laura@prueba.co").orElseThrow();
        assertThat(leido.id()).isEqualTo(turista.id());
        assertThat(leido.nombre()).isEqualTo("Laura Gómez");
        assertThat(leido.whatsapp()).isEqualTo("+573001234567");
        assertThat(leido.whatsappVerificado()).isTrue();
        assertThat(leido.aceptaciones()).containsExactly(new AceptacionPolitica("v1", AHORA));
        assertThat(leido.revision()).isZero();
        String fila = jdbc.queryForObject("SELECT nombre_cifrado || ' ' || whatsapp_cifrado FROM usuarios WHERE id = ?",
                String.class, turista.id());
        assertThat(fila).startsWith("v1:").doesNotContain("Laura").doesNotContain("3001234567");
    }

    @Test
    @DisplayName("dos cuentas con el mismo correo responden CORREO_YA_REGISTRADO (RF-02)")
    void correoUnico() {
        usuarios.insertar(turista("repetido@prueba.co", "+573001234567", false));

        assertThatThrownBy(() -> usuarios.insertar(turista("Repetido@Prueba.co", "+573009999999", false)))
                .isInstanceOf(CorreoYaRegistradoException.class);
    }

    @Test
    @DisplayName("guardar una cuenta que cambió desde que se leyó es un conflicto (ADR-05)")
    void bloqueoOptimista() {
        usuarios.insertar(turista("conflicto@prueba.co", "+573001234567", false));
        Usuario primera = usuarios.porCorreo("conflicto@prueba.co").orElseThrow();
        Usuario segunda = usuarios.porCorreo("conflicto@prueba.co").orElseThrow();

        primera.registrarIntentoFallido(AHORA);
        usuarios.actualizar(primera);
        segunda.registrarIntentoFallido(AHORA);

        assertThatThrownBy(() -> usuarios.actualizar(segunda)).isInstanceOf(ConflictoConcurrenciaException.class);
        assertThat(usuarios.porId(primera.id()).orElseThrow().intentosFallidos()).isEqualTo(1);
    }

    @Test
    @DisplayName("el último código de un propósito y su uso; revocar todas las sesiones cierra las abiertas")
    void codigosYSesiones() {
        Turista turista = turista("codigos@prueba.co", "+573001234567", false);
        usuarios.insertar(turista);
        codigos.insertar(CodigoUnUso.emitir(UUID.randomUUID(), turista.id(), PropositoCodigo.VERIFICACION_WHATSAPP,
                "viejo", AHORA));
        CodigoUnUso nuevo = CodigoUnUso.emitir(UUID.randomUUID(), turista.id(), PropositoCodigo.VERIFICACION_WHATSAPP,
                "nuevo", AHORA.plusSeconds(60));
        codigos.insertar(nuevo);

        CodigoUnUso ultimo = codigos.ultimo(turista.id(), PropositoCodigo.VERIFICACION_WHATSAPP).orElseThrow();
        assertThat(ultimo.hashCodigo()).isEqualTo("nuevo");
        ultimo.usar(AHORA.plusSeconds(90));
        codigos.actualizar(ultimo);
        assertThat(codigos.porId(nuevo.id()).orElseThrow().esValido(AHORA.plusSeconds(90))).isFalse();
        assertThat(codigos.ultimo(turista.id(), PropositoCodigo.RESTABLECER_CONTRASENA)).isEmpty();

        Sesion sesion = Sesion.iniciar(UUID.randomUUID(), turista.id(), "renovacion", AHORA);
        sesiones.insertar(sesion);
        Sesion leidaAntes = sesiones.porId(sesion.id()).orElseThrow();
        sesiones.revocarTodas(turista.id(), AHORA.plus(Duration.ofHours(1)));

        assertThat(sesiones.porId(sesion.id()).orElseThrow().estaVigente(AHORA.plus(Duration.ofHours(1)))).isFalse();
        // Una renovación que la leyó antes del cierre no puede guardarla y deshacerlo.
        leidaAntes.renovar("otra", AHORA.plusSeconds(30));
        assertThatThrownBy(() -> sesiones.actualizar(leidaAntes)).isInstanceOf(ConflictoConcurrenciaException.class);
    }

    @Test
    @DisplayName("V9 crea al administrador con su contraseña inicial y la demo precarga sus cuentas con los ids "
            + "de siempre")
    void administradorYDemo() {
        Usuario administrador = usuarios.porId(V9__crear_administrador_inicial.ID).orElseThrow();
        assertThat(administrador.rol()).isEqualTo(Rol.ADMINISTRADOR);
        assertThat(administrador.correo()).isEqualTo(V9__crear_administrador_inicial.CORREO);
        assertThat(administrador.autenticar(V9__crear_administrador_inicial.CONTRASENA_INICIAL, hasher)).isTrue();

        Turista laura = (Turista) usuarios.porId(UUID.fromString("70000000-0000-0000-0000-000000000007"))
                .orElseThrow();
        assertThat(laura.correo()).isEqualTo("laura@demo.samariantour.co");
        assertThat(laura.autenticar(PrecargaCuentasDemo.CONTRASENA, hasher)).isTrue();
        assertThat(laura.puedeEnviarSolicitudes()).isTrue();
        assertThat(laura.debeAceptarPolitica(politicas.vigente())).isFalse();
        assertThat(usuarios.porId(UUID.fromString("60000000-0000-0000-0000-000000000001")).orElseThrow().rol())
                .isEqualTo(Rol.OPERADOR_DISPONIBILIDAD);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM usuarios WHERE correo LIKE '%@demo.samariantour.co'",
                Integer.class)).isEqualTo(13);
    }
}
