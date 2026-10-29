package co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo;

import java.util.Objects;

/** El intento de pago en curso y lo que el frontend necesita para abrirlo en la pasarela (RF-46). */
public record SesionGenerada(IntentoPago intento, SesionCobro sesion) {

    public SesionGenerada {
        Objects.requireNonNull(intento, "intento");
        Objects.requireNonNull(sesion, "sesion");
    }
}
