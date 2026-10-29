package co.edu.unimagdalena.samariantour.nucleo.prestadores.infraestructura.config;

import co.edu.unimagdalena.samariantour.nucleo.prestadores.aplicacion.ConfigurarWhatsAppPrestadorService;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.aplicacion.ConsultarPrestadorService;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.aplicacion.GestionarPrestadorService;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.aplicacion.RegistrarCuentaCobroService;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.aplicacion.RegistrarPrestadorService;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.aplicacion.SincronizarRntService;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.puerto.entrada.ConfigurarWhatsAppPrestadorUseCase;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.puerto.entrada.ConsultarPrestadorUseCase;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.puerto.entrada.RegistrarCuentaCobroUseCase;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.puerto.entrada.RegistrarPrestadorUseCase;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.puerto.entrada.SincronizarRntUseCase;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.puerto.salida.AuditoriaPort;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.puerto.salida.CuentaCobroRepositoryPort;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.puerto.salida.CuentasPort;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.puerto.salida.EventoSalidaPort;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.puerto.salida.HasherPort;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.puerto.salida.NotificacionPrestadorPort;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.puerto.salida.PrestadorRepositoryPort;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.puerto.salida.RntVerificationPort;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.puerto.salida.TransaccionPort;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.infraestructura.salida.rnt.PropiedadesRntSimulado;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.SecureRandom;
import java.time.Clock;

/** Crea los casos de uso de los prestadores y los conecta con sus adaptadores. */
@Configuration
@EnableConfigurationProperties(PropiedadesRntSimulado.class)
public class PrestadoresConfig {

    @Bean
    RegistrarPrestadorUseCase registrarPrestador(PrestadorRepositoryPort prestadores, RntVerificationPort rnt,
                                                 CuentasPort cuentas, EventoSalidaPort eventos,
                                                 TransaccionPort transaccion, Clock reloj) {
        return new RegistrarPrestadorService(prestadores, rnt, cuentas, eventos, transaccion, reloj);
    }

    @Bean
    ConfigurarWhatsAppPrestadorUseCase configurarWhatsAppPrestador(PrestadorRepositoryPort prestadores,
                                                                   HasherPort hasher,
                                                                   NotificacionPrestadorPort notificacion,
                                                                   Clock reloj) {
        // Los códigos se adivinarían con un generador predecible.
        return new ConfigurarWhatsAppPrestadorService(prestadores, hasher, notificacion, new SecureRandom(), reloj);
    }

    @Bean
    RegistrarCuentaCobroUseCase registrarCuentaCobro(PrestadorRepositoryPort prestadores,
                                                     CuentaCobroRepositoryPort cuentas, AuditoriaPort auditoria,
                                                     Clock reloj) {
        return new RegistrarCuentaCobroService(prestadores, cuentas, auditoria, reloj);
    }

    @Bean
    ConsultarPrestadorUseCase consultarPrestador(PrestadorRepositoryPort prestadores,
                                                 CuentaCobroRepositoryPort cuentas) {
        return new ConsultarPrestadorService(prestadores, cuentas);
    }

    @Bean
    SincronizarRntUseCase sincronizarRnt(PrestadorRepositoryPort prestadores, RntVerificationPort rnt,
                                         EventoSalidaPort eventos, NotificacionPrestadorPort notificacion,
                                         TransaccionPort transaccion, Clock reloj) {
        return new SincronizarRntService(prestadores, rnt, eventos, notificacion, transaccion, reloj);
    }

    /** Implementa el cambio de tipo (RF-68, RF-69) y la consulta del estado RNT (RF-73). */
    @Bean
    GestionarPrestadorService gestionarPrestador(PrestadorRepositoryPort prestadores) {
        return new GestionarPrestadorService(prestadores);
    }
}
