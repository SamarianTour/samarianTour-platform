package co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.puerto.entrada;

import co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.modelo.RegistroExposicion;

/** La recomendación avisa qué ofertas mostró en una consulta (RET-02, DN-42). */
public interface RegistrarExposicionUseCase {

    void registrar(RegistroExposicion registro);
}
