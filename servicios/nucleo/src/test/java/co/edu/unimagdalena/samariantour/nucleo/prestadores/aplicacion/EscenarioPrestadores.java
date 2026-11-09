package co.edu.unimagdalena.samariantour.nucleo.prestadores.aplicacion;

import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.excepcion.ContrasenaRechazadaException;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.excepcion.RntYaRegistradoException;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.modelo.ConsultaRnt;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.modelo.CuentaCobro;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.modelo.Prestador;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.puerto.salida.AuditoriaPort;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.puerto.salida.CuentaCobroRepositoryPort;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.puerto.salida.CuentasPort;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.puerto.salida.EventoSalidaPort;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.puerto.salida.NotificacionPrestadorPort;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.puerto.salida.PrestadorRepositoryPort;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.puerto.salida.RntVerificationPort;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.puerto.salida.TransaccionPort;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
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
    static final String CONTRASENA = "Minca2026";

    final Set<UUID> cuentasDePrestador = new HashSet<>();
    final List<UUID> cuentasSuprimidas = new ArrayList<>();
    /** Lo que cada prestador todavía debe, según solicitudes y pagos; sin entrada, nada. */
    final Map<UUID, List<String>> compromisos = new HashMap<>();
    /** Los avisos al catálogo: prestador, habilitado y versión. */
    final List<String> avisos = new ArrayList<>();
    /** Los códigos enviados por WhatsApp: número y código. */
    final List<String> codigos = new ArrayList<>();
    /** Los avisos de retiro del catálogo por WhatsApp: número y RNT. */
    final List<String> retiros = new ArrayList<>();
    /** Cuándo tomó a cada prestador la última sincronización. */
    final Map<UUID, Instant> tomados = new HashMap<>();
    /** La auditoría: prestador, acción y detalle. */
    final List<String> auditoria = new ArrayList<>();

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
            return Optional.ofNullable(prestadores.get(id)).filter(prestador -> prestador.suprimidoEn() == null);
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

        @Override
        public List<Prestador> todos(boolean soloSinOperador) {
            return prestadores.values().stream().filter(prestador -> prestador.suprimidoEn() == null)
                    .filter(prestador -> !soloSinOperador || prestador.operadorId() == null)
                    .sorted(Comparator.comparing(Prestador::fechaRegistro))
                    .toList();
        }

        @Override
        public long asignadosA(UUID operadorId) {
            return prestadores.values().stream().filter(prestador -> prestador.suprimidoEn() == null)
                    .filter(prestador -> operadorId.equals(prestador.operadorId())).count();
        }

        /** Como el índice único parcial de V17: el RNT de un prestador suprimido queda libre (DN-45). */
        private void verificarRntUnico(Prestador prestador) {
            if (prestador.suprimidoEn() == null && prestadores.values().stream()
                    .filter(otro -> otro.suprimidoEn() == null && !otro.id().equals(prestador.id()))
                    .anyMatch(otro -> otro.numeroRnt().equals(prestador.numeroRnt()))) {
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

    final EventoSalidaPort eventos = new EventoSalidaPort() {
        @Override
        public void avisarHabilitacion(UUID prestadorId, boolean activo, long version) {
            avisos.add(prestadorId + " " + activo + " v" + version);
        }

        @Override
        public void avisarSupresion(UUID prestadorId) {
            avisos.add(prestadorId + " suprimido");
        }
    };

    /** La cuenta de cada prestador se confirma con {@link #CONTRASENA}; al suprimirse, deja de estar activa. */
    final CuentasPort cuentas = new CuentasPort() {
        @Override
        public boolean esPrestadorActivo(UUID usuarioId) {
            return cuentasDePrestador.contains(usuarioId);
        }

        @Override
        public void verificarContrasena(UUID prestadorId, String contrasena) {
            if (!cuentasDePrestador.contains(prestadorId)) {
                throw new ContrasenaRechazadaException("CUENTA_NO_ENCONTRADA", "No existe la cuenta.", null);
            }
            if (!CONTRASENA.equals(contrasena)) {
                throw new ContrasenaRechazadaException("CONTRASENA_INCORRECTA", "La contraseña no es correcta.", null);
            }
        }

        @Override
        public void suprimirCuenta(UUID prestadorId) {
            cuentasDePrestador.remove(prestadorId);
            cuentasSuprimidas.add(prestadorId);
        }
    };

    final RegistrarPrestadorService registrar = new RegistrarPrestadorService(repositorio, fuenteRnt,
            cuentas, eventos, transaccion, Clock.fixed(AHORA, ZoneOffset.UTC));

    final GestionarPrestadorService gestionar = new GestionarPrestadorService(repositorio);

    private final AuditoriaPort registroAuditoria = (prestadorId, accion, detalle, ocurridoEn) ->
            auditoria.add(prestadorId + " " + accion + " " + detalle);

    final AsignarOperadorService asignarOperador = new AsignarOperadorService(repositorio, registroAuditoria,
            Clock.fixed(AHORA, ZoneOffset.UTC));

    /** Sin cuentas de cobro: estas pruebas no las usan. */
    final ConsultarPrestadorService consultar = new ConsultarPrestadorService(repositorio,
            new CuentaCobroRepositoryPort() {
                @Override
                public Optional<CuentaCobro> vigente(UUID prestadorId) {
                    return Optional.empty();
                }

                @Override
                public void reemplazar(CuentaCobro anterior, CuentaCobro nueva) {
                    throw new UnsupportedOperationException();
                }
            });

    final SuprimirPrestadorService suprimir = new SuprimirPrestadorService(repositorio, cuentas,
            prestadorId -> compromisos.getOrDefault(prestadorId, List.of()), eventos, registroAuditoria, transaccion,
            Clock.fixed(AHORA, ZoneOffset.UTC));

    UUID cuenta() {
        UUID id = UUID.randomUUID();
        cuentasDePrestador.add(id);
        return id;
    }
}
