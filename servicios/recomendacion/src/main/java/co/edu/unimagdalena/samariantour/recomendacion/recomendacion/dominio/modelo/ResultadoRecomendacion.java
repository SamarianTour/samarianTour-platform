package co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo;

import java.util.List;

/**
 * Lo que recibe el turista: el motor que eligió, un aviso opcional y de 3 a 5 recomendaciones (RF-27), o de 0 a 2
 * con el aviso de pocas coincidencias.
 *
 * @param aviso null si no hay nada que advertir
 */
public record ResultadoRecomendacion(MotorRecomendacion motor, AvisoRecomendacion aviso,
                                     List<Recomendacion> recomendaciones) {

    public static final int MINIMO = 3;
    public static final int MAXIMO = 5;

    public ResultadoRecomendacion {
        recomendaciones = List.copyOf(recomendaciones);
    }
}
