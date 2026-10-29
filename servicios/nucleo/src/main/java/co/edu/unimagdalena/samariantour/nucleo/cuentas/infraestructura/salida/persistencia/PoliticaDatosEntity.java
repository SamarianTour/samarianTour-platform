package co.edu.unimagdalena.samariantour.nucleo.cuentas.infraestructura.salida.persistencia;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

/** Una fila de {@code politicas_datos} (V8). Las versiones se publican con una migración. */
@Entity
@Table(name = "politicas_datos")
public class PoliticaDatosEntity {

    @Id
    private String version;

    @Column(name = "fecha_publicacion")
    private LocalDate fechaPublicacion;

    @Column(name = "url_texto")
    private String urlTexto;

    protected PoliticaDatosEntity() {
    }

    String version() {
        return version;
    }

    LocalDate fechaPublicacion() {
        return fechaPublicacion;
    }

    String urlTexto() {
        return urlTexto;
    }
}
