package co.edu.unimagdalena.samariantour.nucleo.cuentas.infraestructura.config;

import co.edu.unimagdalena.samariantour.nucleo.cuentas.aplicacion.CerrarSesionService;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.aplicacion.ConsultarContactoService;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.aplicacion.ConsultarPoliticaDatosService;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.aplicacion.EmisionCodigoWhatsapp;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.aplicacion.EmisionSesion;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.aplicacion.GestionarDatosPersonalesService;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.aplicacion.IniciarSesionService;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.aplicacion.RegistrarUsuarioService;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.aplicacion.RenovarSesionService;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.aplicacion.RestablecerContrasenaService;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.aplicacion.SolicitarRestablecimientoService;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.aplicacion.SolicitarSupresionService;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.aplicacion.VerificarWhatsAppService;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.entrada.CerrarSesionUseCase;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.entrada.ConsultarContactoUseCase;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.entrada.ConsultarPoliticaDatosUseCase;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.entrada.GestionarDatosPersonalesUseCase;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.entrada.IniciarSesionUseCase;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.entrada.RegistrarUsuarioUseCase;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.entrada.RenovarSesionUseCase;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.entrada.RestablecerContrasenaUseCase;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.entrada.SolicitarRestablecimientoUseCase;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.entrada.SolicitarSupresionUseCase;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.entrada.VerificarWhatsAppUseCase;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.salida.AvisosCatalogoPort;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.salida.CodigoRepositoryPort;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.salida.EmailPort;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.salida.NotificationPort;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.salida.PasswordHasherPort;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.salida.PoliticaDatosRepositoryPort;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.salida.SesionRepositoryPort;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.salida.TipoPrestadorPort;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.salida.TokenPort;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.salida.UsuarioRepositoryPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.SecureRandom;
import java.time.Clock;

/** Crea los casos de uso de las cuentas y los conecta con sus adaptadores. */
@Configuration
public class CuentasConfig {

    @Bean
    EmisionCodigoWhatsapp emisionCodigoWhatsapp(CodigoRepositoryPort codigos, PasswordHasherPort hasher,
                                                NotificationPort notificacion) {
        // Los códigos se adivinarían con un generador predecible.
        return new EmisionCodigoWhatsapp(codigos, hasher, notificacion, new SecureRandom());
    }

    @Bean
    RegistrarUsuarioUseCase registrarUsuario(UsuarioRepositoryPort usuarios, PoliticaDatosRepositoryPort politicas,
                                             PasswordHasherPort hasher, EmisionCodigoWhatsapp emision, Clock reloj) {
        return new RegistrarUsuarioService(usuarios, politicas, hasher, emision, reloj);
    }

    @Bean
    VerificarWhatsAppUseCase verificarWhatsApp(UsuarioRepositoryPort usuarios, CodigoRepositoryPort codigos,
                                               PasswordHasherPort hasher, EmisionCodigoWhatsapp emision,
                                               Clock reloj) {
        return new VerificarWhatsAppService(usuarios, codigos, hasher, emision, reloj);
    }

    @Bean
    EmisionSesion emisionSesion(SesionRepositoryPort sesiones, PasswordHasherPort hasher, TokenPort tokens,
                                TipoPrestadorPort tiposPrestador) {
        // El token de renovación se adivinaría con un generador predecible.
        return new EmisionSesion(sesiones, hasher, tokens, tiposPrestador, new SecureRandom());
    }

    @Bean
    IniciarSesionUseCase iniciarSesion(UsuarioRepositoryPort usuarios, PasswordHasherPort hasher,
                                       EmisionSesion emision, Clock reloj) {
        return new IniciarSesionService(usuarios, hasher, emision, reloj);
    }

    @Bean
    RenovarSesionUseCase renovarSesion(SesionRepositoryPort sesiones, UsuarioRepositoryPort usuarios,
                                       PasswordHasherPort hasher, EmisionSesion emision, Clock reloj) {
        return new RenovarSesionService(sesiones, usuarios, hasher, emision, reloj);
    }

    @Bean
    CerrarSesionUseCase cerrarSesion(SesionRepositoryPort sesiones, PasswordHasherPort hasher, Clock reloj) {
        return new CerrarSesionService(sesiones, hasher, reloj);
    }

    @Bean
    SolicitarRestablecimientoUseCase solicitarRestablecimiento(UsuarioRepositoryPort usuarios,
                                                               CodigoRepositoryPort codigos, PasswordHasherPort hasher,
                                                               EmailPort correo, Clock reloj) {
        // El secreto del enlace se adivinaría con un generador predecible.
        return new SolicitarRestablecimientoService(usuarios, codigos, hasher, correo, new SecureRandom(), reloj);
    }

    @Bean
    RestablecerContrasenaUseCase restablecerContrasena(UsuarioRepositoryPort usuarios, CodigoRepositoryPort codigos,
                                                       SesionRepositoryPort sesiones, PasswordHasherPort hasher,
                                                       Clock reloj) {
        return new RestablecerContrasenaService(usuarios, codigos, sesiones, hasher, reloj);
    }

    @Bean
    GestionarDatosPersonalesUseCase gestionarDatosPersonales(UsuarioRepositoryPort usuarios,
                                                             EmisionCodigoWhatsapp emision, Clock reloj) {
        return new GestionarDatosPersonalesService(usuarios, emision, reloj);
    }

    @Bean
    SolicitarSupresionUseCase solicitarSupresion(UsuarioRepositoryPort usuarios, SesionRepositoryPort sesiones,
                                                 AvisosCatalogoPort avisos, PasswordHasherPort hasher, Clock reloj) {
        return new SolicitarSupresionService(usuarios, sesiones, avisos, hasher, reloj);
    }

    @Bean
    ConsultarContactoUseCase consultarContacto(UsuarioRepositoryPort usuarios) {
        return new ConsultarContactoService(usuarios);
    }

    @Bean
    ConsultarPoliticaDatosUseCase consultarPoliticaDatos(PoliticaDatosRepositoryPort politicas) {
        return new ConsultarPoliticaDatosService(politicas);
    }
}
