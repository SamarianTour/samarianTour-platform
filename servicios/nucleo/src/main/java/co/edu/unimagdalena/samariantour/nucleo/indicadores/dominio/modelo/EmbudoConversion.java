package co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.modelo;

import co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.politica.ReglaAnonimizacion;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * De las solicitudes enviadas en un periodo, cuántas llegaron a cada etapa, en total y por tipo de oferta (RF-87,
 * RF-88). Cuenta las etapas por las que pasaron: una que se atendió y después venció cuenta como atendida.
 */
public record EmbudoConversion(Periodo periodo, List<EtapaEmbudo> etapas, List<EmbudoTipo> porTipo) {

    /** Los tipos de oferta del catálogo, en el orden en que se muestran; siempre salen los cuatro. */
    public static final List<String> TIPOS_OFERTA = List.of("ACTIVIDAD", "PAQUETE", "ALOJAMIENTO", "RESTAURANTE");

    public static EmbudoConversion de(Periodo periodo, List<ConteoEstado> conteos) {
        List<EmbudoTipo> porTipo = TIPOS_OFERTA.stream()
                .map(tipo -> new EmbudoTipo(tipo, etapas(conteos.stream()
                        .filter(conteo -> conteo.tipoOferta().equals(tipo)).toList())))
                .toList();
        return new EmbudoConversion(periodo, etapas(conteos), porTipo);
    }

    private static List<EtapaEmbudo> etapas(List<ConteoEstado> conteos) {
        Map<String, Long> porEstado = conteos.stream().collect(Collectors.groupingBy(ConteoEstado::estado,
                Collectors.summingLong(ConteoEstado::solicitudes)));
        long enviadas = porEstado.getOrDefault(Etapa.ENVIADAS.estado(), 0L);
        return Arrays.stream(Etapa.values())
                .map(etapa -> {
                    long solicitudes = porEstado.getOrDefault(etapa.estado(), 0L);
                    return new EtapaEmbudo(etapa, ReglaAnonimizacion.publicable(solicitudes),
                            ReglaAnonimizacion.porcentaje(solicitudes, enviadas));
                })
                .toList();
    }
}
