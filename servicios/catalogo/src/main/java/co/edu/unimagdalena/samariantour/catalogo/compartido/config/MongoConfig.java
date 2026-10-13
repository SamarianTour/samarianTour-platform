package co.edu.unimagdalena.samariantour.catalogo.compartido.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.convert.DefaultDbRefResolver;
import org.springframework.data.mongodb.core.convert.DefaultMongoTypeMapper;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;

/**
 * Conversión de documentos de MongoDB para todos los módulos del servicio, con el perfil {@code mongo}.
 *
 * <p>Reemplaza el conversor que crea Spring Boot solo para no guardar el campo {@code _class} (DC-16): el modelo
 * documental no lo tiene y cada colección guarda un solo tipo de documento. Los UUID se guardan con la
 * representación estándar ({@code spring.data.mongodb.uuid-representation} en application.yml).
 */
@Configuration
@Profile("mongo")
public class MongoConfig {

    @Bean
    MappingMongoConverter mappingMongoConverter(MongoDatabaseFactory baseDeDatos, MongoMappingContext contexto,
                                                MongoCustomConversions conversiones) {
        MappingMongoConverter conversor = new MappingMongoConverter(new DefaultDbRefResolver(baseDeDatos), contexto);
        conversor.setCustomConversions(conversiones);
        conversor.setTypeMapper(new DefaultMongoTypeMapper(null));
        return conversor;
    }
}
