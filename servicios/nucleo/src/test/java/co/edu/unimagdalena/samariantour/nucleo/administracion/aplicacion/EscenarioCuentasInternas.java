package co.edu.unimagdalena.samariantour.nucleo.administracion.aplicacion;

import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.excepcion.CuentaInternaNoEncontradaException;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.modelo.CuentaInterna;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.modelo.PrestadorRegistrado;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.puerto.salida.CuentasInternasPort;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.puerto.salida.OperadoresPrestadoresPort;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Los servicios de las cuentas internas y de la asignación del operador con dobles en memoria de las cuentas y los
 * prestadores. Las reglas propias de cada módulo están en sus pruebas; aquí solo cuenta lo que cruza los dos.
 */
class EscenarioCuentasInternas {

    static final UUID ADMINISTRADOR = UUID.fromString("60000000-0000-0000-0000-000000000006");
    static final UUID OPERADOR = UUID.fromString("60000000-0000-0000-0000-000000000001");
    static final UUID GESTOR = UUID.fromString("60000000-0000-0000-0000-000000000003");
    static final UUID AGENCIA = UUID.fromString("10000000-0000-0000-0000-000000000001");
    static final UUID SIN_OPERADOR = UUID.fromString("50000000-0000-0000-0000-000000000005");
    static final Instant ENERO = Instant.parse("2026-01-01T00:00:00Z");

    final Map<UUID, CuentaInterna> cuentas = new LinkedHashMap<>();
    final Map<UUID, PrestadorRegistrado> prestadores = new LinkedHashMap<>();

    EscenarioCuentasInternas() {
        cuenta(ADMINISTRADOR, "ADMINISTRADOR");
        cuenta(OPERADOR, CuentaInterna.OPERADOR);
        cuenta(GESTOR, "GESTOR_DESTINO");
        prestador(AGENCIA, OPERADOR);
        prestador(SIN_OPERADOR, null);
    }

    final CuentasInternasPort puertoCuentas = new CuentasInternasPort() {
        @Override
        public List<CuentaInterna> internas() {
            return List.copyOf(cuentas.values());
        }

        @Override
        public Optional<CuentaInterna> porId(UUID usuarioId) {
            return Optional.ofNullable(cuentas.get(usuarioId));
        }

        @Override
        public CuentaInterna crear(String rol, String nombre, String correo) {
            return cuenta(UUID.randomUUID(), rol);
        }

        @Override
        public CuentaInterna desactivar(UUID usuarioId) {
            return cambiarEstado(usuarioId, "DESACTIVADA");
        }

        @Override
        public CuentaInterna reactivar(UUID usuarioId) {
            return cambiarEstado(usuarioId, "ACTIVA");
        }

        @Override
        public boolean esOperadorActivo(UUID usuarioId) {
            return porId(usuarioId).filter(CuentaInterna::esOperador)
                    .filter(cuenta -> cuenta.estado().equals("ACTIVA")).isPresent();
        }
    };

    final OperadoresPrestadoresPort puertoPrestadores = new OperadoresPrestadoresPort() {
        @Override
        public List<PrestadorRegistrado> prestadores(boolean soloSinOperador) {
            return prestadores.values().stream()
                    .filter(prestador -> !soloSinOperador || prestador.operadorId() == null).toList();
        }

        @Override
        public Optional<PrestadorRegistrado> asignarOperador(UUID prestadorId, UUID operadorId) {
            return Optional.ofNullable(prestadores.computeIfPresent(prestadorId, (id, prestador) ->
                    new PrestadorRegistrado(id, prestador.numeroRnt(), prestador.razonSocial(), prestador.tipo(),
                            prestador.estadoRnt(), operadorId, prestador.registradoEn())));
        }

        @Override
        public long asignadosA(UUID operadorId) {
            return prestadores.values().stream()
                    .filter(prestador -> Objects.equals(prestador.operadorId(), operadorId)).count();
        }
    };

    final GestionarCuentasInternasService gestionar = new GestionarCuentasInternasService(puertoCuentas,
            puertoPrestadores);

    final AsignarOperadorPrestadorService asignar = new AsignarOperadorPrestadorService(puertoCuentas,
            puertoPrestadores);

    private CuentaInterna cuenta(UUID id, String rol) {
        CuentaInterna cuenta = new CuentaInterna(id, rol, "Cuenta " + rol, rol.toLowerCase() + "@correo.co",
                "ACTIVA", true, ENERO);
        cuentas.put(id, cuenta);
        return cuenta;
    }

    private void prestador(UUID id, UUID operadorId) {
        prestadores.put(id, new PrestadorRegistrado(id, "91000" + prestadores.size(), "Prestador", "AGENCIA_OPERADORA",
                "ACTIVO", operadorId, ENERO));
    }

    private CuentaInterna cambiarEstado(UUID usuarioId, String estado) {
        CuentaInterna actual = Optional.ofNullable(cuentas.get(usuarioId))
                .orElseThrow(() -> new CuentaInternaNoEncontradaException(usuarioId));
        CuentaInterna nueva = new CuentaInterna(usuarioId, actual.rol(), actual.nombre(), actual.correo(), estado,
                actual.contrasenaEstablecida(), actual.registradaEn());
        cuentas.put(usuarioId, nueva);
        return nueva;
    }
}
