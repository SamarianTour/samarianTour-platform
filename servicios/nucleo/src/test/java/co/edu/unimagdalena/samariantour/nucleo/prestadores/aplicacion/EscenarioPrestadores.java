package co.edu.unimagdalena.samariantour.nucleo.prestadores.aplicacion;

import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.excepcion.RntYaRegistradoException;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.modelo.ConsultaRnt;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.modelo.Prestador;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.puerto.salida.EventoSalidaPort;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.puerto.salida.NotificacionPrestadorPort;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.puerto.salida.PrestadorRepositoryPort;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.puerto.salida.RntVerificationPort;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.puerto.salida.TransaccionPort;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

import static co.edu.unimagdalena.samariantour.nucleo.prestadores.PrestadoresDePrueba.AHORA;

/** Los casos de uso de los prestadores con dobles en memoria (DN-03). */
class EscenarioPrestadores {

    final Map<UUID, Prestador> prestadores = new LinkedHashMap<>();
    /** Lo que responde el RNT por número; un número que no está aquí no se encuentra. */
    final Map<String, ConsultaRnt> rnt = new HashMap<>();
    final Set<UUID> cuentasDePrestador = new HashSet<>();
    /** Los avisos al catálogo: prestador, habilitado y versión. */
    final List<String> avisos = new ArrayList<>();
    /** Los códigos enviados por WhatsApp: número y código. */
    final List<String> codigos = new ArrayList<>();
    /** Los avisos de retiro del catálogo por WhatsApp: número y RNT. */
    final List<String> retiros = new ArrayList<>();
    /** Cuándo tomó a cada prestador la última sincronización. */
    final Map<UUID, Instant> tomados = new HashMap<>();

    final PrestadorRepositoryPort repositorio = new PrestadorRepositoryPort() {
        @Override
        public void insertar(Prestador prestador) {
            verificarRntUnico(prestador);
            prestadores.put(prestador.id(), prestador);
        }

        @Override
        public void actualizar(Prestador prestador) {
            verificarRntUnico(prestador);
            prestadores.put(prestador.id(), prestador);
        }

        @Override
        public Optional<Prestador> porId(UUID id) {
            return Optional.ofNullable(prestadores.get(id));
        }

        @Override
        public List<UUID> tomarParaSincronizar(Instant inicio, int limite) {
            List<UUID> lote = prestadores.keySet().stream()
                    .filter(id -> !tomados.containsKey(id) || tomados.get(id).isBefore(inicio))
                    .limit(limite)
                    .toList();
            lote.forEach(id -> tomados.put(id, inicio));
            return lote;
        }

        private void verificarRntUnico(Prestador prestador) {
            if (prestadores.values().stream().anyMatch(otro -> !otro.id().equals(prestador.id())
                    && otro.numeroRnt().equals(prestador.numeroRnt()))) {
                throw new RntYaRegistradoException(prestador.numeroRnt());
            }
        }
    };

    final TransaccionPort transaccion = new TransaccionPort() {
        @Override
        public <T> T ejecutar(Supplier<T> operacion) {
            return operacion.get();
        }
    };

    final NotificacionPrestadorPort whatsapp = new NotificacionPrestadorPort() {
        @Override
        public void enviarCodigo(String numero, String codigo) {
            codigos.add(numero + ": " + codigo);
        }

        @Override
        public void avisarRetiroCatalogo(String numero, String numeroRnt) {
            retiros.add(numero + " " + numeroRnt);
        }
    };

    final RntVerificationPort fuenteRnt = numero -> rnt.getOrDefault(numero, new ConsultaRnt.NoEncontrado());

    final EventoSalidaPort eventos =
            (prestadorId, activo, version) -> avisos.add(prestadorId + " " + activo + " v" + version);

    final RegistrarPrestadorService registrar = new RegistrarPrestadorService(repositorio, fuenteRnt,
            cuentasDePrestador::contains, eventos, transaccion, Clock.fixed(AHORA, ZoneOffset.UTC));

    final GestionarPrestadorService gestionar = new GestionarPrestadorService(repositorio);

    UUID cuenta() {
        UUID id = UUID.randomUUID();
        cuentasDePrestador.add(id);
        return id;
    }
}
