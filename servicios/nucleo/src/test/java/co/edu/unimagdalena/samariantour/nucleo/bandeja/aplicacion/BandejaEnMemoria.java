package co.edu.unimagdalena.samariantour.nucleo.bandeja.aplicacion;

import co.edu.unimagdalena.samariantour.nucleo.bandeja.dominio.modelo.AvisoSaliente;
import co.edu.unimagdalena.samariantour.nucleo.bandeja.dominio.modelo.EstadoAviso;
import co.edu.unimagdalena.samariantour.nucleo.bandeja.dominio.puerto.salida.BandejaSalidaRepositoryPort;

import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Doble en memoria de la bandeja, sin bloqueos: las pruebas de la aplicación corren en un solo hilo. */
class BandejaEnMemoria implements BandejaSalidaRepositoryPort {

    private final Map<String, AvisoSaliente> avisos = new LinkedHashMap<>();

    @Override
    public boolean agregar(AvisoSaliente aviso) {
        return avisos.putIfAbsent(aviso.idAviso(), aviso) == null;
    }

    @Override
    public List<AvisoSaliente> tomarPendientes(Instant ahora, int limite) {
        return avisos.values().stream()
                .filter(aviso -> aviso.estado() == EstadoAviso.PENDIENTE && !aviso.proximoIntento().isAfter(ahora))
                .sorted(Comparator.comparing(AvisoSaliente::proximoIntento).thenComparing(AvisoSaliente::creadoEn))
                .limit(limite)
                .toList();
    }

    @Override
    public void guardar(AvisoSaliente aviso) {
        avisos.put(aviso.idAviso(), aviso);
    }

    @Override
    public Optional<AvisoSaliente> buscar(String idAviso) {
        return Optional.ofNullable(avisos.get(idAviso));
    }
}
