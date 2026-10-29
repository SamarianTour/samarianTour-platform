package co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;

import static co.edu.unimagdalena.samariantour.nucleo.cuentas.CuentasDePrueba.AHORA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Los códigos de un solo uso (RF-08) y las sesiones (RF-04, RF-06). */
class CodigoYSesionTest {

    private static final UUID USUARIO = UUID.randomUUID();

    @Test
    @DisplayName("un código vale 30 minutos y una sola vez (RF-08)")
    void codigoUnUso() {
        CodigoUnUso codigo = CodigoUnUso.emitir(UUID.randomUUID(), USUARIO, PropositoCodigo.RESTABLECER_CONTRASENA,
                "hash", AHORA);

        assertThat(codigo.expiraEn()).isEqualTo(AHORA.plus(Duration.ofMinutes(30)));
        assertThat(codigo.esValido(AHORA.plus(Duration.ofMinutes(30)).minusMillis(1))).isTrue();
        assertThat(codigo.esValido(AHORA.plus(Duration.ofMinutes(30)))).isFalse();

        codigo.usar(AHORA);

        assertThat(codigo.esValido(AHORA)).isFalse();
        assertThatThrownBy(() -> codigo.usar(AHORA)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("tres códigos equivocados invalidan el código (DN-18)")
    void tresIntentos() {
        CodigoUnUso codigo = CodigoUnUso.emitir(UUID.randomUUID(), USUARIO, PropositoCodigo.VERIFICACION_WHATSAPP,
                "hash", AHORA);
        codigo.registrarIntentoFallido();
        codigo.registrarIntentoFallido();
        assertThat(codigo.esValido(AHORA)).isTrue();

        codigo.registrarIntentoFallido();

        assertThat(codigo.esValido(AHORA)).isFalse();
    }

    @Test
    @DisplayName("la sesión dura 7 días desde el último uso del token de renovación; cerrarla la revoca (RF-06)")
    void sesion() {
        Sesion sesion = Sesion.iniciar(UUID.randomUUID(), USUARIO, "hash-1", AHORA);
        assertThat(sesion.estaVigente(AHORA.plus(Duration.ofDays(7)).minusMillis(1))).isTrue();
        assertThat(sesion.estaVigente(AHORA.plus(Duration.ofDays(7)))).isFalse();

        sesion.renovar("hash-2", AHORA.plus(Duration.ofDays(6)));

        assertThat(sesion.hashRenovacion()).isEqualTo("hash-2");
        assertThat(sesion.estaVigente(AHORA.plus(Duration.ofDays(12)))).isTrue();

        sesion.revocar(AHORA.plus(Duration.ofDays(8)));
        sesion.revocar(AHORA.plus(Duration.ofDays(9)));

        assertThat(sesion.estaVigente(AHORA.plus(Duration.ofDays(8)))).isFalse();
        assertThat(sesion.revocadaEn()).isEqualTo(AHORA.plus(Duration.ofDays(8)));
        assertThatThrownBy(() -> sesion.renovar("hash-3", AHORA.plus(Duration.ofDays(8))))
                .isInstanceOf(IllegalStateException.class);
    }
}
