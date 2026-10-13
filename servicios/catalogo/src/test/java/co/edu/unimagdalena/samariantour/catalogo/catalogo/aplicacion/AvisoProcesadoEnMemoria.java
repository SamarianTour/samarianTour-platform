package co.edu.unimagdalena.samariantour.catalogo.catalogo.aplicacion;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.salida.AvisoProcesadoPort;

import java.util.HashSet;
import java.util.Set;

class AvisoProcesadoEnMemoria implements AvisoProcesadoPort {

    private final Set<String> procesados = new HashSet<>();

    @Override
    public boolean fueProcesado(String idAviso) {
        return procesados.contains(idAviso);
    }

    @Override
    public void marcarProcesado(String idAviso) {
        procesados.add(idAviso);
    }
}
