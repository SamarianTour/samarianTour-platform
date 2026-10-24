package co.edu.unimagdalena.samariantour.nucleo.bandeja.dominio.modelo;

import co.edu.unimagdalena.samariantour.nucleo.bandeja.dominio.politica.EsperaCreciente;

import java.time.Instant;
import java.util.Objects;

/**
 * Un aviso para el catálogo guardado en la bandeja de salida (DN-06). Nace pendiente y deja de estarlo cuando el
 * catálogo lo recibe o lo rechaza; mientras falle, se reprograma con una espera creciente.
 */
public final class AvisoSaliente {

    public static final int LONGITUD_MAXIMA_ID = 200;
    public static final int LONGITUD_MAXIMA_ERROR = 1000;

    private final String idAviso;
    private final Destino destino;
    private final String cuerpo;
    private final Instant creadoEn;
    private EstadoAviso estado;
    private Instant proximoIntento;
    private int intentos;
    private Instant enviadoEn;
    private String ultimoError;

    private AvisoSaliente(String idAviso, Destino destino, String cuerpo, Instant creadoEn, EstadoAviso estado,
                          Instant proximoIntento, int intentos, Instant enviadoEn, String ultimoError) {
        this.idAviso = idAviso;
        this.destino = destino;
        this.cuerpo = cuerpo;
        this.creadoEn = creadoEn;
        this.estado = estado;
        this.proximoIntento = proximoIntento;
        this.intentos = intentos;
        this.enviadoEn = enviadoEn;
        this.ultimoError = ultimoError;
    }

    /**
     * Un aviso pendiente que se puede enviar de inmediato.
     *
     * @param idAviso identificador con el que el catálogo descarta los repetidos
     * @param cuerpo  el aviso en JSON, como lo espera el catálogo
     */
    public static AvisoSaliente nuevo(String idAviso, Destino destino, String cuerpo, Instant ahora) {
        if (idAviso == null || idAviso.isBlank() || idAviso.length() > LONGITUD_MAXIMA_ID) {
            throw new IllegalArgumentException("El idAviso es obligatorio y tiene como máximo "
                    + LONGITUD_MAXIMA_ID + " caracteres");
        }
        if (cuerpo == null || cuerpo.isBlank()) {
            throw new IllegalArgumentException("El cuerpo del aviso es obligatorio");
        }
        Objects.requireNonNull(destino, "destino");
        Objects.requireNonNull(ahora, "ahora");
        return new AvisoSaliente(idAviso, destino, cuerpo, ahora, EstadoAviso.PENDIENTE, ahora, 0, null, null);
    }

    public static AvisoSaliente reconstituir(String idAviso, Destino destino, String cuerpo, Instant creadoEn,
                                             EstadoAviso estado, Instant proximoIntento, int intentos,
                                             Instant enviadoEn, String ultimoError) {
        return new AvisoSaliente(idAviso, destino, cuerpo, creadoEn, estado, proximoIntento, intentos, enviadoEn,
                ultimoError);
    }

    /** Aplica lo que respondió el catálogo al intentar enviarlo. */
    public void registrar(ResultadoEnvio resultado, Instant ahora) {
        if (estado != EstadoAviso.PENDIENTE) {
            throw new IllegalStateException("El aviso " + idAviso + " ya no está pendiente: " + estado);
        }
        intentos++;
        switch (resultado) {
            case ResultadoEnvio.Recibido recibido -> {
                estado = EstadoAviso.ENVIADO;
                enviadoEn = ahora;
                ultimoError = null;
            }
            case ResultadoEnvio.Rechazado rechazado -> {
                estado = EstadoAviso.RECHAZADO;
                ultimoError = recortar(rechazado.motivo());
            }
            case ResultadoEnvio.Fallido fallido -> {
                proximoIntento = ahora.plus(EsperaCreciente.despuesDe(intentos));
                ultimoError = recortar(fallido.motivo());
            }
        }
    }

    private static String recortar(String motivo) {
        return motivo == null || motivo.length() <= LONGITUD_MAXIMA_ERROR
                ? motivo : motivo.substring(0, LONGITUD_MAXIMA_ERROR);
    }

    public String idAviso() {
        return idAviso;
    }

    public Destino destino() {
        return destino;
    }

    public String cuerpo() {
        return cuerpo;
    }

    public Instant creadoEn() {
        return creadoEn;
    }

    public EstadoAviso estado() {
        return estado;
    }

    public Instant proximoIntento() {
        return proximoIntento;
    }

    public int intentos() {
        return intentos;
    }

    public Instant enviadoEn() {
        return enviadoEn;
    }

    public String ultimoError() {
        return ultimoError;
    }
}
