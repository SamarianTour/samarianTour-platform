package co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.demo;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.ContenidoComun;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Foto;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.ModalidadRecorrido;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.NivelEsfuerzo;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Oferta;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.PrestadorCatalogo;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.TipoActividad;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.TipoPrestador;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.VersionActividad;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.VersionAlojamiento;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.VersionOferta;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.VersionPaquete;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.VersionRestaurante;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Zona;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.UUID;

/**
 * Catálogo sintético para medir la búsqueda bajo carga (QAS-07, DC-48): prestadores inventados con 25 ofertas cada
 * uno, repartidas entre las zonas de la demostración. No describe prestadores reales.
 *
 * <p>Las ofertas se construyen con las operaciones del dominio, como las de {@link DatosSantaMarta}, así que cumplen
 * sus reglas. La mezcla de estados imita un catálogo en uso para que los índices parciales de la búsqueda (DC-17)
 * dejen fuera lo que no es visible: la mayoría aprobadas, algunas con una edición en moderación, otras todavía sin
 * aprobar, algunas retiradas y las de los prestadores sin RNT activo. Todo sale de una semilla fija: cada réplica
 * genera exactamente los mismos datos, con los mismos identificadores.
 */
public final class DatosDeCarga {

    public static final int OFERTAS_POR_PRESTADOR = 25;
    /** Las agencias y los guías publican, de sus 25 ofertas, estos paquetes; el resto son actividades. */
    static final int PAQUETES_POR_PRESTADOR = 5;
    /** Uno de cada tantos prestadores perdió el RNT después de publicar: sus ofertas no son visibles (RF-77). */
    static final int CADA_CUANTOS_SIN_RNT = 20;

    private static final long SEMILLA = 2026_10_23L;
    private static final Instant CARGA = Instant.parse("2026-10-01T13:00:00Z");
    private static final List<TipoPrestador> TIPOS = List.of(TipoPrestador.AGENCIA_OPERADORA,
            TipoPrestador.GUIA_INDEPENDIENTE, TipoPrestador.ALOJAMIENTO, TipoPrestador.RESTAURANTE);

    private DatosDeCarga() {
    }

    /**
     * Identificador de la oferta en esa posición. Empieza por {@code d} para no cruzarse con las de la demostración
     * ({@code b…}) ni con otros documentos de identificador fijo (DC-30).
     */
    public static String idDeOferta(int posicion) {
        return "d%023x".formatted(posicion);
    }

    /** Prestador en esa posición; sus UUID empiezan por {@code f} y no se cruzan con los de la demostración. */
    public static UUID idDePrestador(int posicion) {
        return UUID.fromString("f0000000-0000-0000-0000-%012d".formatted(posicion));
    }

    public static int prestadoresPara(int ofertas) {
        return Math.ceilDiv(ofertas, OFERTAS_POR_PRESTADOR);
    }

    /** La réplica de la habilitación de cada prestador sintético (DC-02). */
    public static List<PrestadorCatalogo> prestadores(int ofertas) {
        List<PrestadorCatalogo> prestadores = new ArrayList<>();
        for (int posicion = 1; posicion <= prestadoresPara(ofertas); posicion++) {
            prestadores.add(sinRnt(posicion)
                    ? new PrestadorCatalogo(idDePrestador(posicion), false, 2)
                    : habilitado(idDePrestador(posicion)));
        }
        return prestadores;
    }

    /** Exactamente {@code cantidad} ofertas, con los identificadores {@code idDeOferta(1)} a {@code idDeOferta(cantidad)}. */
    public static List<Oferta> ofertas(int cantidad) {
        Random azar = new Random(SEMILLA);
        List<Zona> zonas = DatosSantaMarta.zonas();
        List<Foto> fotos = List.copyOf(DatosSantaMarta.archivosDeFotos().keySet());
        List<Oferta> ofertas = new ArrayList<>(cantidad);
        int siguiente = 1;
        for (int prestador = 1; siguiente <= cantidad; prestador++) {
            int hasta = Math.min(cantidad, siguiente + OFERTAS_POR_PRESTADOR - 1);
            new Generador(azar, zonas, fotos, prestador, siguiente, hasta).generar(ofertas);
            siguiente = hasta + 1;
        }
        return List.copyOf(ofertas);
    }

    private static boolean sinRnt(int prestador) {
        return prestador % CADA_CUANTOS_SIN_RNT == 0;
    }

    private static PrestadorCatalogo habilitado(UUID prestadorId) {
        return new PrestadorCatalogo(prestadorId, true, 1);
    }

    /** Las ofertas de un prestador, de la posición {@code desde} a la {@code hasta}. */
    private record Generador(Random azar, List<Zona> zonas, List<Foto> fotos, int prestador, int desde, int hasta) {

        void generar(List<Oferta> ofertas) {
            TipoPrestador tipo = TIPOS.get((prestador - 1) % TIPOS.size());
            PrestadorCatalogo habilitacion = habilitado(idDePrestador(prestador));
            boolean publicaPaquetes = tipo == TipoPrestador.AGENCIA_OPERADORA
                    || tipo == TipoPrestador.GUIA_INDEPENDIENTE;
            int ultimaSinPaquete = publicaPaquetes ? hasta - PAQUETES_POR_PRESTADOR : hasta;
            List<Oferta> propias = new ArrayList<>();
            List<Oferta> actividades = new ArrayList<>();
            for (int posicion = desde; posicion <= hasta; posicion++) {
                Oferta oferta;
                if (posicion > ultimaSinPaquete && actividades.size() >= 2) {
                    List<Oferta> incluidas = actividadesDelPaquete(actividades);
                    oferta = Oferta.publicar(idDeOferta(posicion), habilitacion, tipo, paquete(posicion, incluidas),
                            zona(incluidas.getFirst()), incluidas, CARGA);
                } else {
                    VersionOferta version = version(tipo, posicion);
                    oferta = Oferta.publicar(idDeOferta(posicion), habilitacion, tipo, version,
                            zonaPorId(version.contenido().zonaId()), CARGA);
                    if (version instanceof VersionActividad) {
                        actividades.add(oferta);
                    }
                }
                propias.add(oferta);
            }
            for (Oferta oferta : propias) {
                ofertas.add(conEstado(oferta, habilitacion, tipo));
            }
        }

        /**
         * Cerca del 7 % sigue sin aprobar y el 5 % está retirada; de las actividades aprobadas, cerca del 8 % tiene
         * además una edición en moderación (RF-66). Si el prestador perdió el RNT, sus ofertas quedan como la
         * expedición de {@link DatosSantaMarta}: con la copia del RNT inactiva y no visibles.
         */
        private Oferta conEstado(Oferta oferta, PrestadorCatalogo habilitacion, TipoPrestador tipo) {
            int dado = azar.nextInt(100);
            if (dado >= 7) {
                oferta.aprobar(1, habilitacion, CARGA);
                if (dado < 15 && oferta.publicada().orElseThrow() instanceof VersionActividad actividad) {
                    oferta.editar(habilitacion, tipo, conPrecio(actividad, actividad.contenido().precioReferencia()
                            .add(BigDecimal.valueOf(5000))), zonaPorId(actividad.contenido().zonaId()),
                            CARGA.plus(1, ChronoUnit.DAYS));
                } else if (dado >= 95) {
                    oferta.retirar(oferta.prestadorId());
                }
            }
            if (sinRnt(prestador)) {
                return Oferta.reconstituir(oferta.id(), oferta.tipo(), oferta.prestadorId(), oferta.retirada(), false,
                        2, Oferta.REVISION_NUEVA, oferta.publicada().orElse(null), oferta.pendiente().orElse(null));
            }
            return oferta;
        }

        private VersionOferta version(TipoPrestador tipo, int posicion) {
            Zona zona = zonas.get(azar.nextInt(zonas.size()));
            return switch (tipo) {
                case AGENCIA_OPERADORA -> actividad(posicion, zona, false);
                case GUIA_INDEPENDIENTE -> actividad(posicion, zona, true);
                case ALOJAMIENTO -> new VersionAlojamiento(
                        contenido("Alojamiento", posicion, zona, precio(120_000, 900_000), 1 + azar.nextInt(8)),
                        uno("habitación", "cabaña", "apartamento", "glamping"), azar.nextBoolean());
                case RESTAURANTE -> new VersionRestaurante(
                        contenido("Restaurante", posicion, zona, precio(30_000, 150_000), 4 + azar.nextInt(40)),
                        azar.nextBoolean());
            };
        }

        private VersionActividad actividad(int posicion, Zona zona, boolean deGuia) {
            TipoActividad tipoActividad = TipoActividad.values()[azar.nextInt(TipoActividad.values().length)];
            NivelEsfuerzo nivel = NivelEsfuerzo.values()[azar.nextInt(NivelEsfuerzo.values().length)];
            String nombre = tipoActividad.name().toLowerCase(Locale.ROOT).replace('_', ' ');
            ContenidoComun contenido = contenido("Actividad de " + nombre, posicion, zona, precio(40_000, 600_000),
                    2 + azar.nextInt(20));
            List<LocalTime> salidas = List.of(LocalTime.of(6 + azar.nextInt(10), 0));
            int duracion = 60 * (1 + azar.nextInt(8));
            if (deGuia) {
                return new VersionActividad(contenido, tipoActividad, nivel, false, salidas, "Plaza principal",
                        duracion, ModalidadRecorrido.values()[azar.nextInt(ModalidadRecorrido.values().length)],
                        List.of("es", "en"));
            }
            return new VersionActividad(contenido, tipoActividad, nivel, azar.nextInt(4) == 0, salidas, null, duracion,
                    null, List.of());
        }

        private VersionPaquete paquete(int posicion, List<Oferta> incluidas) {
            Zona zona = zona(incluidas.getFirst());
            List<String> ids = incluidas.stream().map(Oferta::id).toList();
            return new VersionPaquete(contenido("Paquete", posicion, zona, precio(150_000, 1_500_000),
                    2 + azar.nextInt(10)), ids, 60 * (4 + azar.nextInt(20)), List.of(LocalTime.of(7, 0)));
        }

        /** Dos o tres actividades distintas del prestador. */
        private List<Oferta> actividadesDelPaquete(List<Oferta> actividades) {
            List<Oferta> mezcladas = new ArrayList<>(actividades);
            Collections.shuffle(mezcladas, azar);
            return List.copyOf(mezcladas.subList(0, Math.min(mezcladas.size(), 2 + azar.nextInt(2))));
        }

        private ContenidoComun contenido(String clase, int posicion, Zona zona, BigDecimal precio, int capacidad) {
            String nombre = "%s %05d en %s".formatted(clase, posicion, zona.nombre());
            return new ContenidoComun(nombre, "Oferta sintética para la prueba de carga (DC-48)", precio, null,
                    zona.id(), zona.ubicacion(), null, capacidad, List.of(fotos.get(azar.nextInt(fotos.size()))));
        }

        private VersionActividad conPrecio(VersionActividad actividad, BigDecimal precio) {
            ContenidoComun c = actividad.contenido();
            return new VersionActividad(new ContenidoComun(c.nombre(), c.descripcion(), precio, c.horarios(),
                    c.zonaId(), c.ubicacion(), c.comoLlegar(), c.capacidadMaxima(), c.fotos()),
                    actividad.tipoActividad(), actividad.nivelEsfuerzo(), actividad.esAccesibleSillaRuedas(),
                    actividad.horariosSalida(), actividad.puntoEncuentro(), actividad.duracionMinutos(),
                    actividad.modalidad(), actividad.idiomasAtencion());
        }

        /** Precio en pesos, redondeado a miles de 5.000. */
        private BigDecimal precio(int minimo, int maximo) {
            int pasos = (maximo - minimo) / 5_000;
            return BigDecimal.valueOf(minimo + 5_000L * azar.nextInt(pasos + 1));
        }

        private String uno(String... opciones) {
            return opciones[azar.nextInt(opciones.length)];
        }

        private Zona zona(Oferta oferta) {
            return zonaPorId(oferta.pendiente().or(oferta::publicada).orElseThrow().contenido().zonaId());
        }

        private Zona zonaPorId(String id) {
            return zonas.stream().filter(zona -> zona.id().equals(id)).findFirst().orElseThrow();
        }
    }
}
