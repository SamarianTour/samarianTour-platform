package co.edu.unimagdalena.samariantour.nucleo.prestadores.infraestructura.salida.rnt;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * La tabla del RNT simulado (DN-33), en {@code application-demo.yml}: los registros activos con los números de los
 * prestadores de la demo, que son ficticios y no están en el RNT real. Un número que no está aquí no se encuentra.
 */
@ConfigurationProperties("nucleo.rnt.simulada")
public record PropiedadesRntSimulado(List<Registro> registros) {

    public PropiedadesRntSimulado {
        registros = registros == null ? List.of() : List.copyOf(registros);
    }

    /**
     * Los mismos campos que publica el conjunto del RNT en datos.gov.co.
     *
     * @param personaNatural "No Comerciante" en el RNT
     */
    public record Registro(String numero, String razonSocial, String municipio, String categoria, String subCategoria,
                           boolean personaNatural, Integer empleados) {
    }
}
