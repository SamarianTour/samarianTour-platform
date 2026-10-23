package co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.DestinoFoto;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Foto;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.FotoSubida;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.salida.FotoSubidaRepositoryPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Contrato de FotoSubidaRepositoryPort, común a los adaptadores en memoria y de MongoDB (DC-47). Cada prueba recibe un
 * registro vacío. Las horas tienen milisegundos, la precisión de MongoDB.
 */
public interface ContratoFotoSubidaRepository {

    Instant SUBIDA = Instant.parse("2026-10-20T10:00:00.123Z");
    UUID PRESTADOR = UUID.fromString("20000000-0000-0000-0000-000000000002");

    FotoSubidaRepositoryPort registro();

    @Test
    @DisplayName("devuelve cada foto como se registró, y omite las que no están registradas")
    default void registrarYBuscar() {
        FotoSubida subida = subida(DestinoFoto.OFERTAS, SUBIDA);
        FotoSubida delPerfil = subida(DestinoFoto.PERFIL, SUBIDA);
        registro().registrar(subida);
        registro().registrar(delPerfil);

        assertThat(registro().buscar(List.of(subida.foto(), delPerfil.foto(), Foto.nueva())))
                .containsExactlyInAnyOrder(subida, delPerfil);
        assertThat(registro().buscar(List.of())).isEmpty();
    }

    @Test
    @DisplayName("marcar el uso lo adelanta, nunca lo atrasa, e ignora las fotos que no están registradas")
    default void marcarEnUso() {
        FotoSubida subida = subida(DestinoFoto.OFERTAS, SUBIDA);
        registro().registrar(subida);
        Instant despues = SUBIDA.plus(Duration.ofHours(3));

        registro().marcarEnUso(List.of(subida.foto(), Foto.nueva()), despues);
        registro().marcarEnUso(List.of(subida.foto()), SUBIDA);

        assertThat(registro().buscar(List.of(subida.foto()))).singleElement()
                .extracting(FotoSubida::ultimoUso).isEqualTo(despues);
    }

    @Test
    @DisplayName("entrega las fotos sin uso desde antes del límite, de la más antigua a la más reciente, hasta el máximo")
    default void sinUsoDesde() {
        FotoSubida antigua = subida(DestinoFoto.OFERTAS, SUBIDA);
        FotoSubida intermedia = subida(DestinoFoto.PERFIL, SUBIDA.plus(Duration.ofHours(1)));
        FotoSubida reciente = subida(DestinoFoto.OFERTAS, SUBIDA.plus(Duration.ofHours(2)));
        List.of(reciente, antigua, intermedia).forEach(registro()::registrar);
        Instant limite = SUBIDA.plus(Duration.ofHours(2));

        assertThat(registro().sinUsoDesde(limite, 10)).containsExactly(antigua, intermedia);
        assertThat(registro().sinUsoDesde(limite, 1)).containsExactly(antigua);
    }

    @Test
    @DisplayName("borra el registro si su uso no cambió desde que se leyó; si alguien la usó después, no la borra")
    default void eliminarSiSigueSinUso() {
        FotoSubida sinUso = subida(DestinoFoto.OFERTAS, SUBIDA);
        FotoSubida usadaDespues = subida(DestinoFoto.OFERTAS, SUBIDA);
        registro().registrar(sinUso);
        registro().registrar(usadaDespues);
        registro().marcarEnUso(List.of(usadaDespues.foto()), SUBIDA.plusSeconds(1));

        assertThat(registro().eliminarSiSigueSinUso(sinUso)).isTrue();
        assertThat(registro().eliminarSiSigueSinUso(sinUso)).isFalse();
        assertThat(registro().eliminarSiSigueSinUso(usadaDespues)).isFalse();
        assertThat(registro().buscar(List.of(sinUso.foto(), usadaDespues.foto())))
                .extracting(FotoSubida::foto).containsExactly(usadaDespues.foto());
    }

    private static FotoSubida subida(DestinoFoto destino, Instant ultimoUso) {
        return new FotoSubida(Foto.nueva(), PRESTADOR, destino, ultimoUso);
    }
}
