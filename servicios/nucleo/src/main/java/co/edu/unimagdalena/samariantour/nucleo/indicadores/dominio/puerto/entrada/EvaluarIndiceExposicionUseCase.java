package co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.puerto.entrada;

import co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.modelo.RevisionSesgo;

/**
 * La revisión diaria del sesgo de la recomendación (RET-02, DN-43): mide el índice de exposición de cada prestador y
 * abre, actualiza o cierra sus alertas. Se hace una vez al día aunque la pidan varias réplicas.
 */
public interface EvaluarIndiceExposicionUseCase {

    RevisionSesgo evaluar();
}
