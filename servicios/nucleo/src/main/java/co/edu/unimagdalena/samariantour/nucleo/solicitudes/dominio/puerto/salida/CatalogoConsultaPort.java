package co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.salida;

import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.OfertaSolicitable;

import java.util.Optional;

/**
 * La oferta que pide el turista, desde la consulta interna del catálogo (RF-30, DC-35). Es el único punto de red del
 * flujo de solicitud: tiene corte de circuito y una espera máxima de 3 s (ADR-04, QAS-03, RNF-05).
 */
public interface CatalogoConsultaPort {

    /**
     * @return vacío si el catálogo no la tiene visible
     * @throws co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.excepcion.CatalogoNoDisponibleException si
     *         no responde a tiempo, responde un error o su circuito está abierto
     */
    Optional<OfertaSolicitable> oferta(String ofertaId);
}
