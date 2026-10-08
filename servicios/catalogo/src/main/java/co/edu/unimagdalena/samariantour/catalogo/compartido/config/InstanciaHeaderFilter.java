package co.edu.unimagdalena.samariantour.catalogo.compartido.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Agrega a cada respuesta el header {@code X-Instancia} con el hostname del contenedor que la atendió.
 * En la demostración muestra que Nginx reparte las peticiones entre las réplicas del catálogo (QAS-02).
 */
@Component
public class InstanciaHeaderFilter extends OncePerRequestFilter {

    static final String HEADER = "X-Instancia";

    private final String instancia;

    public InstanciaHeaderFilter() {
        this(resolverHostname());
    }

    InstanciaHeaderFilter(String instancia) {
        this.instancia = instancia;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        response.setHeader(HEADER, instancia);
        chain.doFilter(request, response);
    }

    private static String resolverHostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            return System.getenv().getOrDefault("HOSTNAME", "desconocida");
        }
    }
}
