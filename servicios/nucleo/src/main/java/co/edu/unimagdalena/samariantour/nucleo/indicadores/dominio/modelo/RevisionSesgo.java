package co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.modelo;

import java.time.LocalDate;

/** Lo que hizo una revisión del sesgo (DN-43), para el log de la tarea programada. */
public record RevisionSesgo(LocalDate dia, Resultado resultado, int abiertas, int actualizadas, int cerradas) {

    public enum Resultado {
        EVALUADA,
        /** Otra réplica, u otra hora del mismo día, ya la hizo. */
        YA_REVISADA,
        /** Menos de 200 recomendaciones en 30 días: no se mide y las alertas quedan como estaban. */
        POCAS_RECOMENDACIONES,
        /** El catálogo no dio las ofertas elegibles: se intenta a la hora siguiente. */
        CATALOGO_NO_DISPONIBLE
    }

    public static RevisionSesgo sinMedir(LocalDate dia, Resultado resultado) {
        return new RevisionSesgo(dia, resultado, 0, 0, 0);
    }
}
