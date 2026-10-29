package co.edu.unimagdalena.samariantour.nucleo.prestadores.infraestructura.salida.persistencia;

import co.edu.unimagdalena.samariantour.nucleo.compartido.cifrado.CifradorDatos;
import co.edu.unimagdalena.samariantour.nucleo.compartido.concurrencia.ConflictoConcurrenciaException;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.excepcion.RntYaRegistradoException;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.modelo.CodigoWhatsapp;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.modelo.DatosTipo;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.modelo.EstadoRnt;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.modelo.ModalidadGuianza;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.modelo.Prestador;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.modelo.TipoPrestador;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.puerto.salida.PrestadorRepositoryPort;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

/**
 * Los prestadores en {@code prestadores} (V10, DN-02). Cifra el WhatsApp al guardar y lo descifra al leer (RNF-08):
 * el dominio nunca ve un valor cifrado.
 */
@Component
public class PrestadorJpaAdapter implements PrestadorRepositoryPort {

    /** La restricción única del número de RNT (V10). */
    static final String RNT_UNICO = "prestadores_rnt_unico";

    private final PrestadorJpaRepository prestadores;
    private final CifradorDatos cifrador;

    PrestadorJpaAdapter(PrestadorJpaRepository prestadores, CifradorDatos cifrador) {
        this.prestadores = prestadores;
        this.cifrador = cifrador;
    }

    @Override
    public void insertar(Prestador prestador) {
        PrestadorEntity fila = new PrestadorEntity(prestador.id(), prestador.fechaRegistro());
        copiar(prestador, fila);
        guardar(prestador, fila);
    }

    @Override
    public void actualizar(Prestador prestador) {
        PrestadorEntity fila = prestadores.findById(prestador.id())
                .orElseThrow(() -> new ConflictoConcurrenciaException(
                        "El prestador %s ya no existe".formatted(prestador.id())));
        if (!Objects.equals(fila.revision(), prestador.revision())) {
            throw new ConflictoConcurrenciaException(
                    "El prestador %s cambió desde que se leyó".formatted(prestador.id()));
        }
        copiar(prestador, fila);
        try {
            guardar(prestador, fila);
        } catch (OptimisticLockingFailureException e) {
            throw new ConflictoConcurrenciaException(
                    "El prestador %s cambió desde que se leyó".formatted(prestador.id()));
        }
    }

    @Override
    public Optional<Prestador> porId(UUID id) {
        return prestadores.findById(id).map(this::aDominio);
    }

    @Override
    public List<UUID> tomarParaSincronizar(Instant inicio, int limite) {
        // PostgreSQL guarda microsegundos: con nanosegundos, la marca quedaría antes del inicio y se volvería a tomar.
        Instant marca = inicio.truncatedTo(ChronoUnit.MICROS);
        List<UUID> ids = prestadores.porSincronizar(marca, limite);
        if (!ids.isEmpty()) {
            prestadores.marcarTomados(ids, marca);
        }
        return ids;
    }

    private void guardar(Prestador prestador, PrestadorEntity fila) {
        try {
            prestadores.saveAndFlush(fila);
        } catch (DataIntegrityViolationException e) {
            if (String.valueOf(e.getMostSpecificCause().getMessage()).contains(RNT_UNICO)) {
                throw new RntYaRegistradoException(prestador.numeroRnt());
            }
            throw e;
        }
    }

    private void copiar(Prestador prestador, PrestadorEntity fila) {
        DatosTipo datos = prestador.datosTipo();
        CodigoWhatsapp codigo = prestador.codigo();
        fila.copiar(prestador.numeroRnt(), datos.tipo().name(), datos.numeroEmpleados(),
                unir(datos.modalidades().stream().map(Enum::name).toList()), unir(datos.idiomas()),
                prestador.razonSocial(), prestador.municipio(), prestador.categoriaRnt(),
                prestador.personaNaturalRnt(), prestador.estadoRnt().name(), prestador.vistoEnRnt(),
                prestador.ultimaSincronizacion(), cifrador.cifrar(prestador.whatsapp()),
                prestador.whatsappVerificado(), codigo == null ? null : codigo.hash(),
                codigo == null ? null : codigo.emitidoEn(), codigo == null ? 0 : codigo.intentosFallidos(),
                prestador.operadorId(), prestador.habilitadoEnCatalogo(),
                prestador.versionHabilitacion());
    }

    private Prestador aDominio(PrestadorEntity fila) {
        DatosTipo datos = new DatosTipo(TipoPrestador.valueOf(fila.tipo()), fila.numeroEmpleados(),
                separar(fila.modalidades(), ModalidadGuianza::valueOf), separar(fila.idiomas(), Function.identity()));
        return new Prestador(fila.id(), fila.numeroRnt(), datos, fila.razonSocial(), fila.municipio(),
                fila.categoriaRnt(), fila.personaNaturalRnt(), EstadoRnt.valueOf(fila.estadoRnt()), fila.vistoEnRnt(),
                fila.ultimaSincronizacion(), cifrador.descifrar(fila.whatsappCifrado()), fila.whatsappVerificado(),
                fila.codigoHash() == null ? null
                        : new CodigoWhatsapp(fila.codigoHash(), fila.codigoEmitido(), fila.codigoIntentos()),
                fila.operadorId(), fila.habilitadoEnCatalogo(), fila.versionHabilitacion(), fila.fechaRegistro(),
                fila.revision());
    }

    private static String unir(List<String> valores) {
        return valores.isEmpty() ? null : String.join(",", valores);
    }

    private static <T> List<T> separar(String valores, Function<String, T> convertir) {
        return valores == null || valores.isBlank() ? List.of()
                : Arrays.stream(valores.split(",")).map(convertir).toList();
    }
}
