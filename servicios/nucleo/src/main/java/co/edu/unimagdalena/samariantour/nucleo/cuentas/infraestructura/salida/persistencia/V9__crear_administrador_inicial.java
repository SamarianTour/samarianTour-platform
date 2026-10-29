package co.edu.unimagdalena.samariantour.nucleo.cuentas.infraestructura.salida.persistencia;

import co.edu.unimagdalena.samariantour.nucleo.compartido.cifrado.CifradorDatos;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.salida.PasswordHasherPort;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.stereotype.Component;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

/**
 * La cuenta del administrador viene con la instalación (RF-01, DN-28). Es una migración de Flyway en Java, y no en SQL,
 * porque el nombre se guarda cifrado con la clave de cada entorno. Flyway la toma del contexto de Spring y la aplica
 * una sola vez, después de V8. El nombre de la clase es el que Flyway exige ({@code V<n>__<descripcion>}).
 * <p>
 * La contraseña inicial está en el repositorio: se cambia con el restablecimiento por correo (RF-07) al instalar.
 */
@Component
public class V9__crear_administrador_inicial extends BaseJavaMigration {

    /** El mismo identificador del administrador de la demo del catálogo. */
    public static final UUID ID = UUID.fromString("60000000-0000-0000-0000-000000000006");
    public static final String CORREO = "admin@samariantour.co";
    public static final String CONTRASENA_INICIAL = "SamarianAdmin2026";
    static final String NOMBRE = "Administración Samarian Tour";

    private final CifradorDatos cifrador;
    private final PasswordHasherPort hasher;

    V9__crear_administrador_inicial(CifradorDatos cifrador, PasswordHasherPort hasher) {
        this.cifrador = cifrador;
        this.hasher = hasher;
    }

    @Override
    public void migrate(Context contexto) throws Exception {
        try (PreparedStatement insercion = contexto.getConnection().prepareStatement("""
                INSERT INTO usuarios (id, rol, nombre_cifrado, correo, hash_contrasena, estado, intentos_fallidos,
                                      fecha_registro, whatsapp_verificado, revision)
                VALUES (?, 'ADMINISTRADOR', ?, ?, ?, 'ACTIVA', 0, ?, FALSE, 0)
                """)) {
            insercion.setObject(1, ID);
            insercion.setString(2, cifrador.cifrar(NOMBRE));
            insercion.setString(3, CORREO);
            insercion.setString(4, hasher.hash(CONTRASENA_INICIAL));
            insercion.setTimestamp(5, Timestamp.from(Instant.now()));
            insercion.executeUpdate();
        }
    }
}
