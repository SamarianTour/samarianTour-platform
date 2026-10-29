package co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo;

import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.excepcion.DatoInvalidoException;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.politica.PoliticaBloqueo;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.salida.PasswordHasherPort;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Los datos comunes de cualquier persona que usa el sistema (RF-01 a RF-13). Es abstracta: siempre es uno de los cinco
 * roles, como en el diccionario de clases. Solo el turista tiene datos propios en este módulo; los del prestador son
 * del módulo de prestadores (RF-62 a RF-75).
 */
public abstract sealed class Usuario permits Turista, Prestador, OperadorDisponibilidad, Administrador, GestorDestino {

    private final UUID id;
    private String nombre;
    private String correo;
    private String hashContrasena;
    private EstadoCuenta estado;
    private int intentosFallidos;
    private Instant bloqueadaHasta;
    private final Instant fechaRegistro;
    private final Long revision;
    private final List<AceptacionPolitica> aceptaciones;
    private SolicitudSupresion supresion;

    protected Usuario(DatosCuenta datos) {
        this.id = datos.id();
        this.nombre = datos.nombre();
        this.correo = datos.correo();
        this.hashContrasena = datos.hashContrasena();
        this.estado = datos.estado();
        this.intentosFallidos = datos.intentosFallidos();
        this.bloqueadaHasta = micros(datos.bloqueadaHasta());
        this.fechaRegistro = micros(datos.fechaRegistro());
        this.revision = datos.revision();
        this.aceptaciones = new ArrayList<>(datos.aceptaciones());
        this.supresion = datos.supresion();
    }

    /**
     * Reconstruye la cuenta guardada con la subclase de su rol.
     *
     * @param whatsapp           solo para el turista
     * @param whatsappVerificado solo para el turista
     */
    public static Usuario reconstruir(Rol rol, DatosCuenta datos, String whatsapp, boolean whatsappVerificado) {
        return switch (Objects.requireNonNull(rol, "rol")) {
            case TURISTA -> new Turista(datos, whatsapp, whatsappVerificado);
            case PRESTADOR -> new Prestador(datos);
            case ADMINISTRADOR -> new Administrador(datos);
            case OPERADOR_DISPONIBILIDAD -> new OperadorDisponibilidad(datos);
            case GESTOR_DESTINO -> new GestorDestino(datos);
        };
    }

    /**
     * Una cuenta que se registra sola: turista o prestador (RF-01, RF-02, RF-03). Nace activa y con la política vigente
     * aceptada (RF-09, RF-10); el turista, con su WhatsApp todavía sin verificar.
     *
     * @param hashContrasena la contraseña ya validada con {@link Validaciones#contrasena} y hasheada
     * @param whatsapp       obligatorio para el turista; el prestador no lo registra aquí, sino en sus datos de
     *                       prestador (RF-71)
     */
    public static Usuario registrar(UUID id, Rol rol, String nombre, String correo, String hashContrasena,
                                    String whatsapp, PoliticaDatos vigente, Instant ahora) {
        if (rol == null || !rol.seRegistraSolo()) {
            throw new DatoInvalidoException("rol", "debe ser TURISTA o PRESTADOR: las demás cuentas las crea el "
                    + "administrador (RF-01)");
        }
        String numero = null;
        if (rol == Rol.TURISTA) {
            numero = Validaciones.whatsapp(whatsapp);
        } else if (whatsapp != null && !whatsapp.isBlank()) {
            throw new DatoInvalidoException("whatsapp", "solo lo registra el turista; el del prestador va en sus "
                    + "datos de prestador");
        }
        Usuario usuario = reconstruir(rol, DatosCuenta.nueva(id, nombre, correo, hashContrasena, ahora), numero,
                false);
        usuario.aceptarPolitica(vigente, ahora);
        return usuario;
    }

    public abstract Rol rol();

    // ---- Inicio de sesión (RF-04, RF-05)

    /** Verifica la contraseña (RF-04). Una cuenta sin contraseña, suprimida o interna sin estrenar, nunca coincide. */
    public boolean autenticar(String contrasena, PasswordHasherPort hasher) {
        return hashContrasena != null && contrasena != null && hasher.coincide(contrasena, hashContrasena);
    }

    /** Si el bloqueo de RF-05 sigue vigente. */
    public boolean estaBloqueada(Instant ahora) {
        return bloqueadaHasta != null && ahora.isBefore(bloqueadaHasta);
    }

    /** Suma un fallo; al quinto consecutivo bloquea la cuenta 15 minutos y vuelve a contar desde cero (RF-05). */
    public void registrarIntentoFallido(Instant ahora) {
        intentosFallidos++;
        if (PoliticaBloqueo.debeBloquear(intentosFallidos)) {
            bloqueadaHasta = micros(PoliticaBloqueo.bloqueadaHasta(ahora));
            intentosFallidos = 0;
        }
    }

    /** Un inicio de sesión correcto corta la racha de fallos. */
    public void registrarIngreso() {
        intentosFallidos = 0;
        bloqueadaHasta = null;
    }

    /**
     * Pone una contraseña nueva, ya validada y hasheada (RF-08, RF-81). Corta también el bloqueo: quien demostró que
     * es el dueño del correo vuelve a entrar.
     */
    public void cambiarContrasena(String nuevoHash) {
        hashContrasena = Objects.requireNonNull(nuevoHash, "nuevoHash");
        registrarIngreso();
    }

    /** RF-12. */
    public void cambiarNombre(String nuevo) {
        nombre = Validaciones.nombre(nuevo);
    }

    public boolean estaActiva() {
        return estado == EstadoCuenta.ACTIVA;
    }

    // ---- Política de datos (RF-09, RF-10)

    /** Si le falta aceptar la versión vigente de la política. */
    public boolean debeAceptarPolitica(PoliticaDatos vigente) {
        return aceptaciones.stream().noneMatch(aceptacion -> aceptacion.version().equals(vigente.version()));
    }

    /** Deja constancia de la versión y el momento (RF-10). Aceptar dos veces la misma versión no la repite. */
    public void aceptarPolitica(PoliticaDatos vigente, Instant ahora) {
        if (debeAceptarPolitica(vigente)) {
            aceptaciones.add(new AceptacionPolitica(vigente.version(), ahora));
        }
    }

    // ---- Para las subclases

    /** El nombre que queda en una cuenta suprimida (RF-13). */
    public static final String NOMBRE_SUPRIMIDO = "Titular suprimido";

    /**
     * Borra los datos personales comunes y deja la constancia de la supresión ya ejecutada (RF-13, DN-21). El correo
     * se reemplaza por uno que no existe, para que el titular pueda volver a registrarse con el suyo. Las aceptaciones
     * de la política se conservan: no tienen datos personales y son la evidencia que pide RNF-09.
     */
    protected void anonimizar(UUID supresionId, Instant ahora) {
        if (estado == EstadoCuenta.SUPRIMIDA) {
            throw new IllegalStateException("La cuenta %s ya está suprimida".formatted(id));
        }
        nombre = NOMBRE_SUPRIMIDO;
        correo = "suprimido-" + id + "@cuentas.invalid";
        hashContrasena = null;
        estado = EstadoCuenta.SUPRIMIDA;
        intentosFallidos = 0;
        bloqueadaHasta = null;
        supresion = new SolicitudSupresion(supresionId, ahora, EstadoSupresion.EJECUTADA, ahora);
    }

    protected void cambiarEstado(EstadoCuenta nuevo) {
        this.estado = nuevo;
    }

    protected static Instant micros(Instant instante) {
        return instante == null ? null : instante.truncatedTo(ChronoUnit.MICROS);
    }

    // ---- Consultas

    public UUID id() {
        return id;
    }

    public String nombre() {
        return nombre;
    }

    public String correo() {
        return correo;
    }

    public String hashContrasena() {
        return hashContrasena;
    }

    public EstadoCuenta estado() {
        return estado;
    }

    public int intentosFallidos() {
        return intentosFallidos;
    }

    public Instant bloqueadaHasta() {
        return bloqueadaHasta;
    }

    public Instant fechaRegistro() {
        return fechaRegistro;
    }

    public Long revision() {
        return revision;
    }

    public List<AceptacionPolitica> aceptaciones() {
        return List.copyOf(aceptaciones);
    }

    public Optional<SolicitudSupresion> supresion() {
        return Optional.ofNullable(supresion);
    }
}
