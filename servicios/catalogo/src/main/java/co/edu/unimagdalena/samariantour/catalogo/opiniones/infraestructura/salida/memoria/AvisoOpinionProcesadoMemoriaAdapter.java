package co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.salida.memoria;

import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.puerto.salida.AvisoOpinionProcesadoPort;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Avisos de opiniones ya aplicados, en la memoria del proceso. Solo descarta los repetidos que llegan a la misma
 * réplica: entre réplicas hace falta la base de datos compartida.
 */
@Component
@Profile("memoria")
public class AvisoOpinionProcesadoMemoriaAdapter implements AvisoOpinionProcesadoPort {

    private final Set<String> procesados = ConcurrentHashMap.newKeySet();

    @Override
    public boolean fueProcesado(String idAviso) {
        return procesados.contains(idAviso);
    }

    @Override
    public void marcarProcesado(String idAviso) {
        procesados.add(idAviso);
    }
}
