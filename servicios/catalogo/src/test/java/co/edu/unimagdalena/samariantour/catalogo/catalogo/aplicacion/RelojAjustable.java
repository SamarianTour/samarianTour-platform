package co.edu.unimagdalena.samariantour.catalogo.catalogo.aplicacion;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/** Reloj detenido que la prueba adelanta a mano, para probar plazos como el de las fotos sin uso. */
class RelojAjustable extends Clock {

    private Instant ahora;

    RelojAjustable(Instant inicio) {
        this.ahora = inicio;
    }

    void adelantar(Duration tiempo) {
        ahora = ahora.plus(tiempo);
    }

    @Override
    public Instant instant() {
        return ahora;
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zona) {
        return this;
    }
}
