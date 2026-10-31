package co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo;

import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.politica.PoliticaReintentos;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Un evento de la pasarela tal como llegó al webhook, con su clave de unicidad (DN-21), y en qué va su procesamiento.
 * Si procesarlo falla por un error inesperado, se reintenta con espera creciente y, al quinto fallo, queda fallido para
 * revisarlo a mano: así nunca bloquea a los que llegan después.
 *
 * @param intentosProcesamiento los fallos al procesarlo hasta ahora
 * @param ultimoError           el tipo y el mensaje del último fallo; nunca datos del turista
 * @param siguienteIntentoEn    desde cuándo se puede tomar otra vez; al recibirlo, de inmediato
 * @param avisoEnviadoEn        cuándo se avisó al administrador de que quedó fallido
 */
public record EventoPasarela(UUID id, String claveUnica, String transaccionId, String referencia,
                             EstadoTransaccion estado, long montoCentavos, String cuerpo, Instant recibidoEn,
                             Instant procesadoEn, String resultado, EstadoProcesamiento procesamiento,
                             int intentosProcesamiento, String ultimoError, Instant siguienteIntentoEn,
                             Instant avisoEnviadoEn) {

    /** Lo que se guarda del error, para que quepa en la columna. */
    static final int LARGO_ERROR = 500;

    public EventoPasarela {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(claveUnica, "claveUnica");
        Objects.requireNonNull(transaccionId, "transaccionId");
        Objects.requireNonNull(referencia, "referencia");
        Objects.requireNonNull(estado, "estado");
        Objects.requireNonNull(cuerpo, "cuerpo");
        Objects.requireNonNull(recibidoEn, "recibidoEn");
        Objects.requireNonNull(procesamiento, "procesamiento");
        Objects.requireNonNull(siguienteIntentoEn, "siguienteIntentoEn");
    }

    public static EventoPasarela recibido(UUID id, EventoVerificado verificado, Instant ahora) {
        return new EventoPasarela(id, verificado.claveUnica(), verificado.transaccionId(), verificado.referencia(),
                verificado.estado(), verificado.montoCentavos(), verificado.cuerpo(), ahora, null, null,
                EstadoProcesamiento.PENDIENTE, 0, null, ahora, null);
    }

    public EventoPasarela procesado(String resultado, Instant ahora) {
        return new EventoPasarela(id, claveUnica, transaccionId, referencia, estado, montoCentavos, cuerpo, recibidoEn,
                ahora, resultado, EstadoProcesamiento.PROCESADO, intentosProcesamiento, ultimoError,
                siguienteIntentoEn, avisoEnviadoEn);
    }

    /**
     * Un fallo más al procesarlo: se aplaza con espera creciente o, al quinto, queda fallido (DN-21).
     *
     * @param error el tipo y el mensaje de la excepción
     */
    public EventoPasarela conFallo(String error, Instant ahora) {
        int fallos = intentosProcesamiento + 1;
        boolean fallido = PoliticaReintentos.eventoFallido(fallos);
        return new EventoPasarela(id, claveUnica, transaccionId, referencia, estado, montoCentavos, cuerpo, recibidoEn,
                procesadoEn, resultado, fallido ? EstadoProcesamiento.FALLIDO : EstadoProcesamiento.PENDIENTE, fallos,
                recortar(error), fallido ? siguienteIntentoEn : ahora.plus(PoliticaReintentos.esperaEvento(fallos)),
                avisoEnviadoEn);
    }

    public EventoPasarela avisado(Instant ahora) {
        return new EventoPasarela(id, claveUnica, transaccionId, referencia, estado, montoCentavos, cuerpo, recibidoEn,
                procesadoEn, resultado, procesamiento, intentosProcesamiento, ultimoError, siguienteIntentoEn, ahora);
    }

    private static String recortar(String error) {
        if (error == null) {
            return null;
        }
        return error.length() <= LARGO_ERROR ? error : error.substring(0, LARGO_ERROR);
    }
}
