package co.edu.unimagdalena.samariantour.nucleo.bandeja;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Las entradas de avisos del catálogo, simuladas con el servidor HTTP del JDK: guarda cada petición y responde el
 * estado que la prueba elija, con una demora opcional.
 */
public final class CatalogoSimulado implements AutoCloseable {

    /** Una petición recibida: su ruta, sin el prefijo de la API, y su cuerpo. */
    public record Peticion(String ruta, String cuerpo) {
    }

    public static final String PREFIJO = "/api/catalogo";

    private final HttpServer servidor;
    private final List<Peticion> recibidas = new CopyOnWriteArrayList<>();
    private volatile int estado = 200;
    private volatile Duration demora = Duration.ZERO;

    public CatalogoSimulado() {
        try {
            servidor = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        servidor.createContext(PREFIJO, intercambio -> {
            String cuerpo = new String(intercambio.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            recibidas.add(new Peticion(intercambio.getRequestURI().getPath().substring(PREFIJO.length()), cuerpo));
            try {
                Thread.sleep(demora.toMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            byte[] respuesta = "{\"resultado\":\"APLICADO\"}".getBytes(StandardCharsets.UTF_8);
            intercambio.getResponseHeaders().add("Content-Type", "application/json");
            intercambio.sendResponseHeaders(estado, respuesta.length);
            intercambio.getResponseBody().write(respuesta);
            intercambio.close();
        });
        servidor.start();
    }

    /** La base de la API, como la configura el núcleo en {@code nucleo.catalogo.url}. */
    public String url() {
        return "http://localhost:" + servidor.getAddress().getPort() + PREFIJO;
    }

    public void responder(int estado) {
        this.estado = estado;
    }

    public void demorar(Duration demora) {
        this.demora = demora;
    }

    public List<Peticion> recibidas() {
        return List.copyOf(recibidas);
    }

    public void reiniciar() {
        recibidas.clear();
        estado = 200;
        demora = Duration.ZERO;
    }

    @Override
    public void close() {
        servidor.stop(0);
    }
}
