package co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.politica;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.DatoInvalidoException;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.TipoOfertaNoPermitidoException;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.TipoOferta;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.TipoPrestador;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.VersionActividad;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.VersionOferta;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Qué puede publicar cada tipo de prestador (RF-65): las ofertas de su tipo y, si es agencia operadora o guía
 * independiente, paquetes de dos o más de sus actividades. Además, las actividades del guía independiente exigen
 * punto de encuentro, duración, modalidad e idiomas (RF-67).
 *
 * <p>Pendiente (RF-69): la modalidad de una actividad del guía debe estar entre las que declaró. Diferida en el prototipo.
 */
public final class ReglaTipoOferta {

    private static final Map<TipoPrestador, Set<TipoOferta>> PERMITIDOS = Map.of(
            TipoPrestador.ALOJAMIENTO, EnumSet.of(TipoOferta.ALOJAMIENTO),
            TipoPrestador.RESTAURANTE, EnumSet.of(TipoOferta.RESTAURANTE),
            TipoPrestador.AGENCIA_OPERADORA, EnumSet.of(TipoOferta.ACTIVIDAD, TipoOferta.PAQUETE),
            TipoPrestador.GUIA_INDEPENDIENTE, EnumSet.of(TipoOferta.ACTIVIDAD, TipoOferta.PAQUETE));

    private ReglaTipoOferta() {
    }

    public static void verificar(TipoPrestador tipoPrestador, VersionOferta version) {
        if (!PERMITIDOS.get(tipoPrestador).contains(version.tipo())) {
            throw new TipoOfertaNoPermitidoException(tipoPrestador, version.tipo());
        }
        if (tipoPrestador == TipoPrestador.GUIA_INDEPENDIENTE && version instanceof VersionActividad actividad) {
            verificarDatosDeGuia(actividad);
        }
    }

    private static void verificarDatosDeGuia(VersionActividad actividad) {
        exigir(actividad.puntoEncuentro() != null, "puntoEncuentro");
        exigir(actividad.duracionMinutos() != null, "duracionMinutos");
        exigir(actividad.modalidad() != null, "modalidad");
        exigir(!actividad.idiomasAtencion().isEmpty(), "idiomasAtencion");
    }

    private static void exigir(boolean presente, String campo) {
        if (!presente) {
            throw new DatoInvalidoException(campo, "es obligatorio para el guía independiente (RF-67)");
        }
    }
}
