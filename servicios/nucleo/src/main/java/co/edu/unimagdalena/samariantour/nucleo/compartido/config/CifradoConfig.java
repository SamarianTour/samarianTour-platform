package co.edu.unimagdalena.samariantour.nucleo.compartido.config;

import co.edu.unimagdalena.samariantour.nucleo.compartido.cifrado.CifradorDatos;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * El cifrador de los datos personales (RNF-08, DN-28). La clave llega por {@code NUCLEO_CIFRADO_CLAVE} y no tiene
 * valor por defecto: sin ella el núcleo no arranca, en lugar de guardar datos sin cifrar.
 */
@Configuration
public class CifradoConfig {

    @Bean
    CifradorDatos cifradorDatos(@Value("${nucleo.cifrado.clave:}") String clave) {
        return CifradorDatos.conClave(clave);
    }
}
