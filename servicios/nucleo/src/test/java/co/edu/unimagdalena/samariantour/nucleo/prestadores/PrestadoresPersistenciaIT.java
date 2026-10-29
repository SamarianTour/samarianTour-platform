package co.edu.unimagdalena.samariantour.nucleo.prestadores;

import co.edu.unimagdalena.samariantour.nucleo.IntegracionPostgresBase;
import co.edu.unimagdalena.samariantour.nucleo.compartido.concurrencia.ConflictoConcurrenciaException;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.excepcion.RntYaRegistradoException;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.modelo.ConsultaRnt;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.modelo.EstadoRnt;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.modelo.ModalidadGuianza;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.modelo.Prestador;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.puerto.salida.PrestadorRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

import static co.edu.unimagdalena.samariantour.nucleo.prestadores.PrestadoresDePrueba.AGENCIA;
import static co.edu.unimagdalena.samariantour.nucleo.prestadores.PrestadoresDePrueba.AHORA;
import static co.edu.unimagdalena.samariantour.nucleo.prestadores.PrestadoresDePrueba.GUIA;
import static co.edu.unimagdalena.samariantour.nucleo.prestadores.PrestadoresDePrueba.agenciaActiva;
import static co.edu.unimagdalena.samariantour.nucleo.prestadores.PrestadoresDePrueba.guiaActivo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Los prestadores en PostgreSQL (V10, DN-33): ida y vuelta, WhatsApp cifrado, RNT único y bloqueo optimista. */
@SpringBootTest(properties = "nucleo.bandeja.publicacion.cron=-")
class PrestadoresPersistenciaIT extends IntegracionPostgresBase {

    @Autowired
    private PrestadorRepositoryPort prestadores;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void vaciar() {
        jdbc.update("DELETE FROM prestadores WHERE numero_rnt LIKE '99%'");
        jdbc.update("DELETE FROM usuarios WHERE correo LIKE '%@persistencia-prestadores.co'");
    }

    @Test
    @DisplayName("guarda el guía y lo lee igual, con sus modalidades e idiomas; el WhatsApp queda cifrado")
    void idaYVuelta() {
        Prestador guia = Prestador.registrar(cuenta("guia"), "990002", GUIA, guiaActivo("990002"), AHORA);
        guia.cambiarWhatsapp("+573001110202");
        guia.verificarWhatsapp();

        prestadores.insertar(guia);

        Prestador leido = prestadores.porId(guia.id()).orElseThrow();
        assertThat(leido.datosTipo().modalidades()).containsExactly(ModalidadGuianza.CAMINATA);
        assertThat(leido.datosTipo().idiomas()).containsExactly("es", "en");
        assertThat(leido.estadoRnt()).isEqualTo(EstadoRnt.ACTIVO);
        assertThat(leido.razonSocial()).isEqualTo("JUAN CARLOS PEREZ ARIAS");
        assertThat(leido.whatsappContactable()).isEqualTo("+573001110202");
        assertThat(leido.versionHabilitacion()).isEqualTo(1);
        assertThat(leido.revision()).isZero();
        assertThat(jdbc.queryForObject("SELECT whatsapp_cifrado FROM prestadores WHERE id = ?", String.class,
                guia.id())).startsWith("v1:").doesNotContain("3001110202");
    }

    @Test
    @DisplayName("un RNT ya registrado por otro prestador responde RNT_YA_REGISTRADO; una lectura vieja, conflicto")
    void rntUnicoYConflicto() {
        prestadores.insertar(Prestador.registrar(cuenta("agencia"), "990001", AGENCIA, agenciaActiva("990001"),
                AHORA));
        assertThatThrownBy(() -> prestadores.insertar(Prestador.registrar(cuenta("otra"), "990001", AGENCIA,
                agenciaActiva("990001"), AHORA))).isInstanceOf(RntYaRegistradoException.class);

        UUID id = cuenta("tercera");
        prestadores.insertar(Prestador.registrar(id, "990003", AGENCIA, agenciaActiva("990003"), AHORA));
        Prestador primera = prestadores.porId(id).orElseThrow();
        Prestador segunda = prestadores.porId(id).orElseThrow();
        primera.sincronizar(new ConsultaRnt.NoEncontrado(), AHORA);
        prestadores.actualizar(primera);
        segunda.cambiarWhatsapp("+573009999999");

        assertThatThrownBy(() -> prestadores.actualizar(segunda)).isInstanceOf(ConflictoConcurrenciaException.class);
    }

    /** Una cuenta de prestador mínima: la tabla de prestadores apunta a usuarios (V10). */
    private UUID cuenta(String nombre) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO usuarios (id, rol, nombre_cifrado, correo, estado, fecha_registro, revision)
                VALUES (?, 'PRESTADOR', 'v1:x', ?, 'ACTIVA', now(), 0)""", id,
                nombre + "-" + id + "@persistencia-prestadores.co");
        return id;
    }
}
