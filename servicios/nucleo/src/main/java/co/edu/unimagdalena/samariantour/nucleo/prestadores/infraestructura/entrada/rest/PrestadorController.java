package co.edu.unimagdalena.samariantour.nucleo.prestadores.infraestructura.entrada.rest;

import co.edu.unimagdalena.samariantour.nucleo.compartido.seguridad.Identidad;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.excepcion.DatoInvalidoException;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.modelo.CuentaCobro;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.modelo.DatosTipo;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.modelo.ModalidadGuianza;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.modelo.Prestador;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.modelo.TipoCuenta;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.modelo.TipoPrestador;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.puerto.entrada.ActualizarTipoYEmpleadosUseCase;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.puerto.entrada.ConfigurarWhatsAppPrestadorUseCase;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.puerto.entrada.ConsultarEstadoRntUseCase;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.puerto.entrada.RegistrarCuentaCobroUseCase;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.puerto.entrada.RegistrarPrestadorUseCase;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.puerto.entrada.comando.RegistrarPrestadorCommand;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.infraestructura.entrada.rest.dto.CambioRntRequest;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.infraestructura.entrada.rest.dto.CodigoWhatsappRequest;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.infraestructura.entrada.rest.dto.CuentaCobroRequest;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.infraestructura.entrada.rest.dto.CuentaCobroResponse;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.infraestructura.entrada.rest.dto.DatosTipoRequest;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.infraestructura.entrada.rest.dto.PrestadorResponse;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.infraestructura.entrada.rest.dto.RegistroPrestadorRequest;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.infraestructura.entrada.rest.dto.WhatsappPrestadorRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * El prestador registra su RNT, su tipo y sus datos de contacto y cobro (RF-62, RF-63, RF-68 a RF-71, RF-73 a RF-75),
 * con el rol PRESTADOR (DN-05). Su perfil público y sus ofertas están en el catálogo.
 */
@RestController
public class PrestadorController {

    private final RegistrarPrestadorUseCase registrar;
    private final ActualizarTipoYEmpleadosUseCase tipo;
    private final ConsultarEstadoRntUseCase consultar;
    private final ConfigurarWhatsAppPrestadorUseCase whatsapp;
    private final RegistrarCuentaCobroUseCase cuentaCobro;

    public PrestadorController(RegistrarPrestadorUseCase registrar, ActualizarTipoYEmpleadosUseCase tipo,
                               ConsultarEstadoRntUseCase consultar, ConfigurarWhatsAppPrestadorUseCase whatsapp,
                               RegistrarCuentaCobroUseCase cuentaCobro) {
        this.registrar = registrar;
        this.tipo = tipo;
        this.consultar = consultar;
        this.whatsapp = whatsapp;
        this.cuentaCobro = cuentaCobro;
    }

    @PostMapping("/prestador/registro")
    @ResponseStatus(HttpStatus.CREATED)
    public PrestadorResponse registrar(@RequestBody RegistroPrestadorRequest peticion, HttpServletRequest http) {
        return aRespuesta(registrar.registrar(new RegistrarPrestadorCommand(prestador(http), peticion.numeroRnt(),
                datosTipo(peticion.tipo(), peticion.numeroEmpleados(), peticion.modalidades(), peticion.idiomas()))));
    }

    @GetMapping("/prestador/registro")
    public PrestadorResponse consultar(HttpServletRequest http) {
        return aRespuesta(consultar.consultar(prestador(http)));
    }

    @PutMapping("/prestador/rnt")
    public PrestadorResponse cambiarRnt(@RequestBody CambioRntRequest peticion, HttpServletRequest http) {
        return aRespuesta(registrar.cambiarRnt(prestador(http), peticion.numeroRnt()));
    }

    @PutMapping("/prestador/tipo")
    public PrestadorResponse cambiarTipo(@RequestBody DatosTipoRequest peticion, HttpServletRequest http) {
        return aRespuesta(tipo.actualizar(prestador(http), datosTipo(peticion.tipo(), peticion.numeroEmpleados(),
                peticion.modalidades(), peticion.idiomas())));
    }

    /** Un número nuevo queda sin verificar y recibe un código (RF-71). */
    @PutMapping("/prestador/whatsapp")
    public PrestadorResponse configurarWhatsapp(@RequestBody WhatsappPrestadorRequest peticion,
                                                HttpServletRequest http) {
        return aRespuesta(whatsapp.configurar(prestador(http), peticion.whatsapp()));
    }

    @PostMapping("/prestador/whatsapp/codigo")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void enviarCodigo(HttpServletRequest http) {
        whatsapp.enviarCodigo(prestador(http));
    }

    @PostMapping("/prestador/whatsapp/verificacion")
    public PrestadorResponse verificarWhatsapp(@RequestBody CodigoWhatsappRequest peticion, HttpServletRequest http) {
        return aRespuesta(whatsapp.verificar(prestador(http), peticion.codigo()));
    }

    /** Si ya había una, queda en el historial (RF-74). */
    @PutMapping("/prestador/cuenta-cobro")
    public CuentaCobroResponse registrarCuentaCobro(@RequestBody CuentaCobroRequest peticion,
                                                    HttpServletRequest http) {
        return aRespuesta(cuentaCobro.registrar(prestador(http), valor(TipoCuenta.class, peticion.tipo(), "tipo"),
                peticion.entidad(), peticion.numero(), peticion.titular()));
    }

    @GetMapping("/prestador/cuenta-cobro")
    public CuentaCobroResponse cuentaCobro(HttpServletRequest http) {
        return aRespuesta(cuentaCobro.vigente(prestador(http)));
    }

    private static CuentaCobroResponse aRespuesta(CuentaCobro cuenta) {
        return new CuentaCobroResponse(cuenta.tipo().name(), cuenta.entidad(), cuenta.numeroEnmascarado(),
                cuenta.titular(), cuenta.vigenteDesde());
    }

    private static UUID prestador(HttpServletRequest http) {
        return Identidad.de(http).usuarioId();
    }

    private static DatosTipo datosTipo(String tipo, Integer empleados, List<String> modalidades, List<String> idiomas) {
        return new DatosTipo(valor(TipoPrestador.class, tipo, "tipo"), empleados,
                modalidades == null ? List.of()
                        : modalidades.stream().map(m -> valor(ModalidadGuianza.class, m, "modalidades")).toList(),
                idiomas);
    }

    private static <E extends Enum<E>> E valor(Class<E> tipo, String texto, String campo) {
        return Arrays.stream(tipo.getEnumConstants())
                .filter(constante -> constante.name().equals(texto))
                .findFirst()
                .orElseThrow(() -> new DatoInvalidoException(campo, "debe ser uno de "
                        + Arrays.toString(tipo.getEnumConstants())));
    }

    static PrestadorResponse aRespuesta(Prestador prestador) {
        DatosTipo datos = prestador.datosTipo();
        return new PrestadorResponse(prestador.id(), prestador.numeroRnt(), datos.tipo().name(),
                datos.numeroEmpleados(), datos.modalidades().isEmpty() ? null
                        : datos.modalidades().stream().map(Enum::name).toList(),
                datos.idiomas().isEmpty() ? null : datos.idiomas(), prestador.categoria().name(),
                prestador.razonSocial(), prestador.municipio(), prestador.estadoRnt().name(),
                prestador.ultimaSincronizacion(), prestador.estaHabilitado(), prestador.whatsapp(),
                prestador.whatsapp() == null ? null : prestador.whatsappVerificado());
    }
}
