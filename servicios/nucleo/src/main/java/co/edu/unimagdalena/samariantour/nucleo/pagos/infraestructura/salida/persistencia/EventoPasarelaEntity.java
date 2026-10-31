package co.edu.unimagdalena.samariantour.nucleo.pagos.infraestructura.salida.persistencia;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** Una fila de {@code eventos_pasarela} (V7). Se inserta con una consulta nativa; la entidad sirve para leer. */
@Entity
@Table(name = "eventos_pasarela")
public class EventoPasarelaEntity {

    @Id
    private UUID id;

    @Column(name = "clave_unica")
    private String claveUnica;

    @Column(name = "transaccion_id")
    private String transaccionId;

    private String referencia;

    private String estado;

    @Column(name = "monto_centavos")
    private long montoCentavos;

    private String cuerpo;

    @Column(name = "recibido_en")
    private Instant recibidoEn;

    @Column(name = "procesado_en")
    private Instant procesadoEn;

    private String resultado;

    private String procesamiento;

    @Column(name = "intentos_procesamiento")
    private int intentosProcesamiento;

    @Column(name = "ultimo_error")
    private String ultimoError;

    @Column(name = "siguiente_intento_en")
    private Instant siguienteIntentoEn;

    @Column(name = "aviso_enviado_en")
    private Instant avisoEnviadoEn;

    protected EventoPasarelaEntity() {
    }

    UUID id() {
        return id;
    }

    String claveUnica() {
        return claveUnica;
    }

    String transaccionId() {
        return transaccionId;
    }

    String referencia() {
        return referencia;
    }

    String estado() {
        return estado;
    }

    long montoCentavos() {
        return montoCentavos;
    }

    String cuerpo() {
        return cuerpo;
    }

    Instant recibidoEn() {
        return recibidoEn;
    }

    Instant procesadoEn() {
        return procesadoEn;
    }

    String resultado() {
        return resultado;
    }

    String procesamiento() {
        return procesamiento;
    }

    int intentosProcesamiento() {
        return intentosProcesamiento;
    }

    String ultimoError() {
        return ultimoError;
    }

    Instant siguienteIntentoEn() {
        return siguienteIntentoEn;
    }

    Instant avisoEnviadoEn() {
        return avisoEnviadoEn;
    }
}
