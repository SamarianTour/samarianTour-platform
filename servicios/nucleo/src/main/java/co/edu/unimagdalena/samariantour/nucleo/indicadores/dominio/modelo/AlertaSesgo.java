package co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.modelo;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * El aviso en el panel del administrador de que la recomendación muestra a un prestador más de lo que le corresponde
 * (RET-02, DN-43). Hay a lo sumo una abierta por prestador: se actualiza con cada revisión que lo sigue midiendo por
 * encima del umbral y se cierra cuando baja.
 *
 * @param recomendaciones las de los 30 días con las que se midió
 * @param cerradaEn       vacía mientras está abierta
 */
public record AlertaSesgo(UUID id, UUID prestadorId, BigDecimal indice, BigDecimal porcentajeApariciones,
                          BigDecimal porcentajeOfertas, long apariciones, long recomendaciones, Instant abiertaEn,
                          Instant actualizadaEn, Instant cerradaEn) {

    public static AlertaSesgo abrir(UUID id, MedicionExposicion medicion, long recomendaciones, Instant ahora) {
        Instant momento = ahora.truncatedTo(ChronoUnit.MICROS);
        return new AlertaSesgo(id, medicion.prestadorId(), medicion.indice(), medicion.porcentajeApariciones(),
                medicion.porcentajeOfertas(), medicion.apariciones(), recomendaciones, momento, momento, null);
    }

    public AlertaSesgo actualizada(MedicionExposicion medicion, long recomendaciones, Instant ahora) {
        return new AlertaSesgo(id, prestadorId, medicion.indice(), medicion.porcentajeApariciones(),
                medicion.porcentajeOfertas(), medicion.apariciones(), recomendaciones, abiertaEn,
                ahora.truncatedTo(ChronoUnit.MICROS), null);
    }

    public AlertaSesgo cerrada(Instant ahora) {
        return new AlertaSesgo(id, prestadorId, indice, porcentajeApariciones, porcentajeOfertas, apariciones,
                recomendaciones, abiertaEn, actualizadaEn, ahora.truncatedTo(ChronoUnit.MICROS));
    }
}
