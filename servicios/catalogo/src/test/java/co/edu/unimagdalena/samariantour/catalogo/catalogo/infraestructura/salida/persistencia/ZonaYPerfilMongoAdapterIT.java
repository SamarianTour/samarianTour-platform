package co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.persistencia;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Coordenada;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.PerfilPublico;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Zona;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.demo.DatosSantaMarta;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.persistencia.document.PerfilDocument;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.persistencia.document.ZonaDocument;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.persistencia.mapper.PerfilMongoMapper;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.persistencia.mapper.ZonaMongoMapper;
import org.bson.BsonDocument;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.query.Query;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.demo.DatosSantaMarta.GUIA;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.demo.DatosSantaMarta.TAYRONA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Los adaptadores de solo lectura, zonas y perfiles, contra un MongoDB real con los datos de demostración.
 */
class ZonaYPerfilMongoAdapterIT extends BasePersistenciaMongo {

    @Autowired
    private ZonaMongoAdapter zonas;

    @Autowired
    private PerfilMongoAdapter perfiles;

    @Autowired
    private IndicesMongo indices;

    @BeforeEach
    void cargarDatosDeDemostracion() {
        mongo.remove(new Query(), ZonaDocument.class);
        mongo.remove(new Query(), PerfilDocument.class);
        indices.crear();
        mongo.insertAll(DatosSantaMarta.zonas().stream().map(ZonaMongoMapper::aDocumento).toList());
        mongo.insertAll(DatosSantaMarta.perfiles().stream()
                .map(perfil -> PerfilMongoMapper.aDocumento(new ObjectId(), perfil))
                .toList());
    }

    @Test
    @DisplayName("devuelve cada zona como se guardó, y un id inexistente o sin formato ObjectId no encuentra nada")
    void zonaPorId() {
        for (Zona original : DatosSantaMarta.zonas()) {
            assertThat(zonas.buscarPorId(original.id())).contains(original);
        }
        assertThat(zonas.buscarPorId(new ObjectId().toHexString())).isEmpty();
        assertThat(zonas.buscarPorId("parque-tayrona")).isEmpty();
    }

    @Test
    @DisplayName("la ubicación de la zona es un punto GeoJSON [longitud, latitud], sin _class")
    void ubicacionGeoJson() {
        BsonDocument tayrona = documentoGuardado(ZonaDocument.COLECCION, new ObjectId(TAYRONA));
        BsonDocument ubicacion = tayrona.getDocument("ubicacion");
        Coordenada esperada = zonas.buscarPorId(TAYRONA).orElseThrow().ubicacion();

        assertThat(tayrona.containsKey("_class")).isFalse();
        assertThat(ubicacion.getString("type").getValue()).isEqualTo("Point");
        assertThat(ubicacion.getArray("coordinates")).extracting(valor -> valor.asDouble().getValue())
                .containsExactly(esperada.longitud(), esperada.latitud());
    }

    @Test
    @DisplayName("lista solo las zonas activas, ordenadas por nombre")
    void zonasActivas() {
        Zona tayrona = zonas.buscarPorId(TAYRONA).orElseThrow();
        mongo.save(ZonaMongoMapper.aDocumento(new Zona(tayrona.id(), tayrona.nombre(), tayrona.descripcion(),
                tayrona.ubicacion(), tayrona.areaProtegida(), tayrona.altaAfluencia(), false,
                tayrona.buenasPracticas(), tayrona.condicionesVisita())));

        List<Zona> activas = zonas.buscarActivas();

        assertThat(activas).hasSize(DatosSantaMarta.zonas().size() - 1)
                .extracting(Zona::id).doesNotContain(TAYRONA);
        assertThat(activas).isSortedAccordingTo(Comparator.comparing(Zona::nombre));
    }

    @Test
    @DisplayName("devuelve el perfil del prestador, con los campos opcionales vacíos si no los completó (DC-26)")
    void perfilPorPrestador() {
        assertThat(perfiles.buscarPorPrestador(GUIA)).hasValueSatisfying(perfil -> {
            assertThat(perfil.descripcion()).contains("Minca");
            assertThat(perfil.fotos()).isEmpty();
            assertThat(perfil.sitioWeb()).isNull();
        });
        for (PerfilPublico original : DatosSantaMarta.perfiles()) {
            assertThat(perfiles.buscarPorPrestador(original.prestadorId())).contains(original);
        }
        assertThat(perfiles.buscarPorPrestador(UUID.randomUUID())).isEmpty();
    }

    @Test
    @DisplayName("un prestador tiene a lo sumo un perfil: el índice único lo garantiza")
    void unPerfilPorPrestador() {
        PerfilPublico otro = new PerfilPublico(GUIA, "Otro perfil", List.of(), null, null, List.of());

        assertThatThrownBy(() -> mongo.insert(PerfilMongoMapper.aDocumento(new ObjectId(), otro)))
                .isInstanceOf(DuplicateKeyException.class);
    }
}
