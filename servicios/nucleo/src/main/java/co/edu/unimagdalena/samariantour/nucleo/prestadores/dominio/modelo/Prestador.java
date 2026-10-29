package co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.modelo;

import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.excepcion.CodigoIncorrectoException;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.excepcion.CodigoNoVigenteException;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.excepcion.DatoInvalidoException;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.excepcion.WhatsappNoConfiguradoException;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.politica.PoliticaCodigoWhatsapp;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.politica.ReglaCategoria;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.politica.ReglaCoherenciaTipoRnt;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Los datos maestros de un prestador (RF-62, RF-63, RF-68 a RF-71, RF-73, DN-33): su RNT y lo que dice de él, su tipo
 * y sus campos condicionales, su WhatsApp de contacto y el operador que lo atiende. Su identificador es el de su
 * cuenta (módulo de cuentas). El perfil público y las ofertas viven en el catálogo.
 * <p>
 * El catálogo tiene una réplica de la habilitación (DC-02): solo un prestador con el RNT activo publica. Cada vez que
 * la habilitación cambia, sube {@link #versionHabilitacion()} y la aplicación avisa al catálogo; un aviso con una
 * versión igual o menor a la que el catálogo tiene se ignora.
 */
public final class Prestador {

    /** El número de RNT, solo dígitos [SUPUESTO] (DN-33): en el conjunto del RNT tienen de 1 a 10. */
    private static final Pattern NUMERO_RNT = Pattern.compile("^\\d{1,10}$");
    /** Formato internacional E.164, como el WhatsApp del turista. */
    private static final Pattern WHATSAPP = Pattern.compile("^\\+[1-9]\\d{7,14}$");

    private final UUID id;
    private String numeroRnt;
    private DatosTipo datosTipo;
    private String razonSocial;
    private String municipio;
    private String categoriaRnt;
    private boolean personaNaturalRnt;
    private EstadoRnt estadoRnt;
    private boolean vistoEnRnt;
    private Instant ultimaSincronizacion;
    private String whatsapp;
    private boolean whatsappVerificado;
    private String hashCodigo;
    private Instant codigoEmitidoEn;
    private int intentosCodigo;
    private final UUID operadorId;
    private boolean habilitadoEnCatalogo;
    private long versionHabilitacion;
    private final Instant fechaRegistro;
    private final Long revision;

    public Prestador(UUID id, String numeroRnt, DatosTipo datosTipo, String razonSocial, String municipio,
                     String categoriaRnt, boolean personaNaturalRnt, EstadoRnt estadoRnt, boolean vistoEnRnt,
                     Instant ultimaSincronizacion, String whatsapp, boolean whatsappVerificado, CodigoWhatsapp codigo,
                     UUID operadorId, boolean habilitadoEnCatalogo, long versionHabilitacion, Instant fechaRegistro,
                     Long revision) {
        this.id = Objects.requireNonNull(id, "id");
        this.numeroRnt = Objects.requireNonNull(numeroRnt, "numeroRnt");
        this.datosTipo = Objects.requireNonNull(datosTipo, "datosTipo");
        this.razonSocial = razonSocial;
        this.municipio = municipio;
        this.categoriaRnt = categoriaRnt;
        this.personaNaturalRnt = personaNaturalRnt;
        this.estadoRnt = Objects.requireNonNull(estadoRnt, "estadoRnt");
        this.vistoEnRnt = vistoEnRnt;
        this.ultimaSincronizacion = micros(ultimaSincronizacion);
        this.whatsapp = whatsapp;
        this.whatsappVerificado = whatsapp != null && whatsappVerificado;
        if (codigo != null) {
            this.hashCodigo = codigo.hash();
            this.codigoEmitidoEn = micros(codigo.emitidoEn());
            this.intentosCodigo = codigo.intentosFallidos();
        }
        this.operadorId = operadorId;
        this.habilitadoEnCatalogo = habilitadoEnCatalogo;
        this.versionHabilitacion = versionHabilitacion;
        this.fechaRegistro = micros(Objects.requireNonNull(fechaRegistro, "fechaRegistro"));
        this.revision = revision;
    }

    /**
     * Un prestador con cuenta registra su RNT y su tipo (RF-62, RF-63, RF-68, RF-70). Con el registro encontrado, el
     * tipo tiene que coincidir con la categoría del RNT y se autocompletan la razón social y el municipio; si no se
     * encuentra queda No encontrado, y si la fuente no responde, en Verificación pendiente. Si queda activo,
     * {@link #habilitadoEnCatalogo()} es verdadero y hay que avisar al catálogo con la versión 1.
     *
     * @throws co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.excepcion.TipoNoCoincideConRntException si
     *         el RNT encontrado es de otra categoría
     */
    public static Prestador registrar(UUID id, String numeroRnt, DatosTipo datosTipo, ConsultaRnt consulta,
                                      Instant ahora) {
        Prestador prestador = new Prestador(id, validarNumero(numeroRnt), datosTipo, null, null, null, false,
                EstadoRnt.VERIFICACION_PENDIENTE, false, null, null, false, null, null, false, 0, ahora, null);
        prestador.aplicarRegistro(consulta, ahora);
        prestador.actualizarHabilitacion();
        return prestador;
    }

    /**
     * Cambia el número de RNT y vuelve a validarlo, como al registrarse (RF-62, RF-63).
     *
     * @return si cambió la habilitación y hay que avisar al catálogo
     */
    public boolean cambiarRnt(String nuevoNumero, ConsultaRnt consulta, Instant ahora) {
        numeroRnt = validarNumero(nuevoNumero);
        razonSocial = null;
        municipio = null;
        categoriaRnt = null;
        personaNaturalRnt = false;
        vistoEnRnt = false;
        estadoRnt = EstadoRnt.VERIFICACION_PENDIENTE;
        aplicarRegistro(consulta, ahora);
        return actualizarHabilitacion();
    }

    /**
     * Cambia el tipo y sus campos condicionales (RF-68, RF-69). Si ya se conoce la categoría del RNT, el tipo nuevo
     * tiene que coincidir con ella.
     */
    public void cambiarTipo(DatosTipo nuevos) {
        Objects.requireNonNull(nuevos, "nuevos");
        if (categoriaRnt != null) {
            ReglaCoherenciaTipoRnt.verificar(nuevos.tipo(), categoriaRnt, personaNaturalRnt);
        }
        datosTipo = nuevos;
    }

    /**
     * Aplica lo que dice el RNT en una sincronización (RF-76, DN-33). Encontrado, queda Activo con los datos al día;
     * no encontrado, Inactivo si alguna vez estuvo, o No encontrado si nunca; si la fuente no responde, conserva el
     * último estado conocido (QAS-03).
     *
     * @return si cambió la habilitación y hay que avisar al catálogo (RF-77)
     */
    public boolean sincronizar(ConsultaRnt consulta, Instant ahora) {
        switch (consulta) {
            case ConsultaRnt.Encontrado encontrado -> {
                copiar(encontrado.registro());
                estadoRnt = EstadoRnt.ACTIVO;
                vistoEnRnt = true;
                ultimaSincronizacion = micros(ahora);
            }
            case ConsultaRnt.NoEncontrado noEncontrado -> {
                estadoRnt = vistoEnRnt ? EstadoRnt.INACTIVO : EstadoRnt.NO_ENCONTRADO;
                ultimaSincronizacion = micros(ahora);
            }
            case ConsultaRnt.NoDisponible noDisponible -> {
                // Se conserva el último estado conocido.
            }
        }
        return actualizarHabilitacion();
    }

    private void aplicarRegistro(ConsultaRnt consulta, Instant ahora) {
        switch (consulta) {
            case ConsultaRnt.Encontrado encontrado -> {
                RegistroRnt registro = encontrado.registro();
                ReglaCoherenciaTipoRnt.verificar(datosTipo.tipo(), registro.categoria(), registro.personaNatural());
                copiar(registro);
                estadoRnt = EstadoRnt.ACTIVO;
                vistoEnRnt = true;
                ultimaSincronizacion = micros(ahora);
            }
            case ConsultaRnt.NoEncontrado noEncontrado -> {
                estadoRnt = EstadoRnt.NO_ENCONTRADO;
                ultimaSincronizacion = micros(ahora);
            }
            case ConsultaRnt.NoDisponible noDisponible -> estadoRnt = EstadoRnt.VERIFICACION_PENDIENTE;
        }
    }

    private void copiar(RegistroRnt registro) {
        razonSocial = registro.razonSocial();
        municipio = registro.municipio();
        categoriaRnt = registro.categoria();
        personaNaturalRnt = registro.personaNatural();
    }

    /**
     * Compara la habilitación con la que el catálogo conoce. Un prestador nuevo empieza deshabilitado en el catálogo
     * (DC-02), así que solo se avisa cuando el RNT queda activo.
     */
    private boolean actualizarHabilitacion() {
        boolean habilitado = estaHabilitado();
        if (habilitado == habilitadoEnCatalogo) {
            return false;
        }
        habilitadoEnCatalogo = habilitado;
        versionHabilitacion++;
        return true;
    }

    // ---- WhatsApp de contacto (RF-71)

    /**
     * El canal por el que recibe la notificación de una solicitud Pagada; nunca es público (RF-71). Un número nuevo
     * queda sin verificar hasta que confirme el código (DN-33); el mismo número no cambia nada.
     *
     * @return si cambió
     */
    public boolean cambiarWhatsapp(String numero) {
        String limpio = numero == null ? "" : numero.replaceAll("[\\s-]", "");
        if (!WHATSAPP.matcher(limpio).matches()) {
            throw new DatoInvalidoException("whatsapp", "debe tener el indicativo del país, por ejemplo +573001234567");
        }
        if (limpio.equals(whatsapp)) {
            return false;
        }
        whatsapp = limpio;
        whatsappVerificado = false;
        hashCodigo = null;
        return true;
    }

    /**
     * Guarda el hash de un código nuevo; el anterior deja de servir (DN-33).
     *
     * @return si hay que enviarlo: con el número ya verificado no se envía nada
     * @throws WhatsappNoConfiguradoException si todavía no configuró su WhatsApp
     */
    public boolean emitirCodigo(String hash, Instant ahora) {
        if (whatsapp == null) {
            throw new WhatsappNoConfiguradoException();
        }
        if (whatsappVerificado) {
            return false;
        }
        hashCodigo = hash;
        codigoEmitidoEn = micros(ahora);
        intentosCodigo = 0;
        return true;
    }

    /** Si hay un código que todavía sirve: emitido hace menos de 30 minutos y con intentos (RF-08, DN-33). */
    public boolean codigoVigente(Instant ahora) {
        return hashCodigo != null && ahora.isBefore(codigoEmitidoEn.plus(PoliticaCodigoWhatsapp.VIGENCIA))
                && intentosCodigo < PoliticaCodigoWhatsapp.INTENTOS_MAXIMOS;
    }

    /**
     * Verifica el número con el resultado de comparar el código contra el hash. Uno ya verificado no cambia.
     *
     * @param coincide si el código escrito corresponde a {@link #hashCodigo()}
     * @throws CodigoNoVigenteException  si el código venció, se usó o agotó sus intentos
     * @throws CodigoIncorrectoException si no coincide; el intento queda contado
     */
    public void verificarCodigo(boolean coincide, Instant ahora) {
        if (whatsappVerificado) {
            return;
        }
        if (!codigoVigente(ahora)) {
            throw new CodigoNoVigenteException();
        }
        if (!coincide) {
            intentosCodigo++;
            throw new CodigoIncorrectoException(PoliticaCodigoWhatsapp.INTENTOS_MAXIMOS - intentosCodigo);
        }
        verificarWhatsapp();
    }

    /** Lo marca verificado y gasta el código. La precarga de la demo lo usa directamente. */
    public void verificarWhatsapp() {
        if (whatsapp == null) {
            throw new IllegalStateException("El prestador %s no tiene WhatsApp que verificar".formatted(id));
        }
        whatsappVerificado = true;
        hashCodigo = null;
    }

    // ---- Consultas

    /** Solo un prestador con el RNT activo publica y aparece en el catálogo (RF-77, RF-79). */
    public boolean estaHabilitado() {
        return estadoRnt == EstadoRnt.ACTIVO;
    }

    public CategoriaPrestador categoria() {
        return ReglaCategoria.de(datosTipo.tipo(), datosTipo.numeroEmpleados());
    }

    /** El WhatsApp al que se le notifica, solo si está verificado (RF-57, RF-71). */
    public String whatsappContactable() {
        return whatsappVerificado ? whatsapp : null;
    }

    private static String validarNumero(String numero) {
        String limpio = numero == null ? "" : numero.strip();
        if (!NUMERO_RNT.matcher(limpio).matches()) {
            throw new DatoInvalidoException("numeroRnt", "son solo dígitos, de 1 a 10");
        }
        return limpio;
    }

    private static Instant micros(Instant instante) {
        return instante == null ? null : instante.truncatedTo(ChronoUnit.MICROS);
    }

    public UUID id() {
        return id;
    }

    public String numeroRnt() {
        return numeroRnt;
    }

    public DatosTipo datosTipo() {
        return datosTipo;
    }

    public TipoPrestador tipo() {
        return datosTipo.tipo();
    }

    public String razonSocial() {
        return razonSocial;
    }

    public String municipio() {
        return municipio;
    }

    public String categoriaRnt() {
        return categoriaRnt;
    }

    public boolean personaNaturalRnt() {
        return personaNaturalRnt;
    }

    public EstadoRnt estadoRnt() {
        return estadoRnt;
    }

    public boolean vistoEnRnt() {
        return vistoEnRnt;
    }

    public Instant ultimaSincronizacion() {
        return ultimaSincronizacion;
    }

    public String whatsapp() {
        return whatsapp;
    }

    public boolean whatsappVerificado() {
        return whatsappVerificado;
    }

    /** El código pendiente, para guardarlo; vacío si no hay uno. */
    public CodigoWhatsapp codigo() {
        return hashCodigo == null ? null : new CodigoWhatsapp(hashCodigo, codigoEmitidoEn, intentosCodigo);
    }

    public String hashCodigo() {
        return hashCodigo;
    }

    public UUID operadorId() {
        return operadorId;
    }

    public boolean habilitadoEnCatalogo() {
        return habilitadoEnCatalogo;
    }

    public long versionHabilitacion() {
        return versionHabilitacion;
    }

    public Instant fechaRegistro() {
        return fechaRegistro;
    }

    public Long revision() {
        return revision;
    }
}
