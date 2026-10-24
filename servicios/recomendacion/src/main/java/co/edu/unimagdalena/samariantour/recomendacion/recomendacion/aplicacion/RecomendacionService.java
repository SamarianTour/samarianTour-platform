package co.edu.unimagdalena.samariantour.recomendacion.recomendacion.aplicacion;

import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.excepcion.MotorNoDisponibleException;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.AvisoRecomendacion;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.OfertaEvaluada;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.Preferencias;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.RegistroExposicion;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.ResultadoRecomendacion;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.politica.PoliticaRotacion;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.politica.ReglaCriteriosExplicitos;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.puerto.entrada.RecomendarOfertasUseCase;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.puerto.salida.CatalogoConsultaPort;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.puerto.salida.ExposicionPort;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.puerto.salida.RecommendationPort;

import java.time.Clock;
import java.util.List;

/**
 * Recomienda ofertas (RF-26, RF-27):
 * <ol>
 *   <li>lee las ofertas visibles del catálogo (o su copia en caché);</li>
 *   <li>aplica los filtros duros y calcula la afinidad ({@link ReglaCriteriosExplicitos});</li>
 *   <li>ordena por afinidad y rota entre empates ({@link PoliticaRotacion});</li>
 *   <li>el motor configurado elige y explica; si falla, responde el motor por reglas (QAS-03);</li>
 *   <li>registra la exposición, sin datos del turista (RET-02).</li>
 * </ol>
 * Si menos de 3 ofertas pasan los filtros duros, devuelve las que haya con el aviso POCAS_COINCIDENCIAS, sin relajar
 * los filtros.
 */
public class RecomendacionService implements RecomendarOfertasUseCase {

    private final CatalogoConsultaPort catalogo;
    private final RecommendationPort motor;
    private final RecommendationPort respaldo;
    private final ExposicionPort exposicion;
    private final ReglaCriteriosExplicitos regla;
    private final PoliticaRotacion rotacion;
    private final Clock reloj;

    /**
     * @param motor    el que se eligió por configuración: IA o reglas (DR-01)
     * @param respaldo el motor por reglas, que no depende de servicios externos
     */
    public RecomendacionService(CatalogoConsultaPort catalogo, RecommendationPort motor, RecommendationPort respaldo,
                                ExposicionPort exposicion, ReglaCriteriosExplicitos regla, PoliticaRotacion rotacion,
                                Clock reloj) {
        this.catalogo = catalogo;
        this.motor = motor;
        this.respaldo = respaldo;
        this.exposicion = exposicion;
        this.regla = regla;
        this.rotacion = rotacion;
        this.reloj = reloj;
    }

    @Override
    public ResultadoRecomendacion recomendar(Preferencias preferencias) {
        List<OfertaEvaluada> candidatas = rotacion.ordenar(regla.evaluar(catalogo.ofertasVisibles(), preferencias));
        AvisoRecomendacion aviso = candidatas.size() < ResultadoRecomendacion.MINIMO
                ? AvisoRecomendacion.POCAS_COINCIDENCIAS
                : null;
        ResultadoRecomendacion resultado = candidatas.isEmpty()
                ? new ResultadoRecomendacion(respaldo.motor(), aviso, List.of())
                : elegir(preferencias, candidatas, aviso);
        exposicion.registrar(RegistroExposicion.de(reloj.instant(), resultado));
        return resultado;
    }

    private ResultadoRecomendacion elegir(Preferencias preferencias, List<OfertaEvaluada> candidatas,
                                          AvisoRecomendacion aviso) {
        try {
            return new ResultadoRecomendacion(motor.motor(), aviso, motor.recomendar(preferencias, candidatas));
        } catch (MotorNoDisponibleException e) {
            // El adaptador ya registró la causa; el turista recibe la respuesta por reglas sin ver el error.
            return new ResultadoRecomendacion(respaldo.motor(), aviso, respaldo.recomendar(preferencias, candidatas));
        }
    }
}
