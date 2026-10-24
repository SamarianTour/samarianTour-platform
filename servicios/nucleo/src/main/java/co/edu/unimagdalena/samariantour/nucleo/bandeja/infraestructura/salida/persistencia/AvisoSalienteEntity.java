package co.edu.unimagdalena.samariantour.nucleo.bandeja.infraestructura.salida.persistencia;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

/** Una fila de {@code bandeja_salida} (V1). El esquema lo crea Flyway; Hibernate solo lo valida (DN-01). */
@Entity
@Table(name = "bandeja_salida")
public class AvisoSalienteEntity {

    @Id
    @Column(name = "id_aviso")
    private String idAviso;

    private String destino;

    @JdbcTypeCode(SqlTypes.JSON)
    private String cuerpo;

    private String estado;

    @Column(name = "creado_en")
    private Instant creadoEn;

    @Column(name = "proximo_intento")
    private Instant proximoIntento;

    private int intentos;

    @Column(name = "enviado_en")
    private Instant enviadoEn;

    @Column(name = "ultimo_error")
    private String ultimoError;

    protected AvisoSalienteEntity() {
    }

    AvisoSalienteEntity(String idAviso, String destino, String cuerpo, String estado, Instant creadoEn,
                        Instant proximoIntento, int intentos, Instant enviadoEn, String ultimoError) {
        this.idAviso = idAviso;
        this.destino = destino;
        this.cuerpo = cuerpo;
        this.estado = estado;
        this.creadoEn = creadoEn;
        this.proximoIntento = proximoIntento;
        this.intentos = intentos;
        this.enviadoEn = enviadoEn;
        this.ultimoError = ultimoError;
    }

    String idAviso() {
        return idAviso;
    }

    String destino() {
        return destino;
    }

    String cuerpo() {
        return cuerpo;
    }

    String estado() {
        return estado;
    }

    Instant creadoEn() {
        return creadoEn;
    }

    Instant proximoIntento() {
        return proximoIntento;
    }

    int intentos() {
        return intentos;
    }

    Instant enviadoEn() {
        return enviadoEn;
    }

    String ultimoError() {
        return ultimoError;
    }
}
