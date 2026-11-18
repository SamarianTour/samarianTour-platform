// =====================================================================================================================
// Samarian Tour · Catálogo · Base de datos MongoDB 7 (catalogo_db)
// Colecciones e índices del catálogo y de las opiniones, tal como los crea el servicio al arrancar
// (IndicesMongo, IndicesOpinionesMongo y ShedLock). Extraídos el 17 de noviembre de 2026 del servicio en ejecución.
//
// Uso normal: no hace falta correr este script. El catálogo crea sus índices al arrancar con el perfil "mongo"
// (crear un índice que ya existe con la misma definición no hace nada). Este archivo sirve para revisar el modelo o
// preparar la base a mano:
//   mongosh "mongodb://localhost:27017/catalogo_db" 02-catalogo-mongodb.js
//
// MongoDB no tiene esquema fijo: los campos de cada documento los define el código (paquete
// infraestructura/salida/persistencia de cada módulo). Los datos de la demostración (zonas, prestadores, ofertas,
// fotos, perfiles y opiniones) los carga el perfil "demo" al arrancar; las fotos en sí viven en MinIO.
// =====================================================================================================================

const catalogo = db.getSiblingDB("catalogo_db");

// Las ofertas visibles: prestador con RNT activo, no retiradas y con una versión publicada (ReglaVisibilidad).
const soloVisibles = { rntPrestadorActivo: true, retirada: false, publicada: { $exists: true } };

const colecciones = {
  // Ofertas con su versión publicada y su versión pendiente embebidas (DC-05 a DC-07), de 1 a 6 fotos (DC-47).
  ofertas: [
    // La clave de QAS-07: la búsqueda que filtra por tipo y zona, ordenada por precio (DC-17).
    [{ tipo: 1, "publicada.zonaId": 1, "publicada.precioReferencia": 1, _id: 1 },
      { name: "busqueda_tipo_zona_precio", partialFilterExpression: soloVisibles }],
    // La búsqueda que no filtra por tipo.
    [{ "publicada.precioReferencia": 1, _id: 1 }, { name: "busqueda_precio", partialFilterExpression: soloVisibles }],
    // Las ofertas de un prestador: el retiro por RNT y "mis ofertas".
    [{ prestadorId: 1, _id: 1 }, { name: "ofertas_del_prestador" }],
    // La cola de moderación del administrador, de la más antigua a la más reciente (RF-79).
    [{ "pendiente.enviadaEn": 1, _id: 1 },
      { name: "en_moderacion", partialFilterExpression: { retirada: false, "pendiente.estadoModeracion": "EN_MODERACION" } }],
    // Si una foto está en uso antes de borrarla (DC-47).
    [{ "publicada.fotos": 1 }, { name: "fotos_publicadas" }],
    [{ "pendiente.fotos": 1 }, { name: "fotos_pendientes" }],
  ],
  // Perfil público del prestador: uno por prestador (DC-26, DC-39).
  perfiles: [
    [{ prestadorId: 1 }, { name: "perfil_por_prestador", unique: true }],
    [{ fotos: 1 }, { name: "fotos_del_perfil" }],
  ],
  // Fotos subidas (los archivos están en MinIO) y su último uso, para la limpieza (DC-47, DC-52).
  fotos: [
    [{ ultimoUso: 1, _id: 1 }, { name: "fotos_por_ultimo_uso" }],
    [{ prestadorId: 1 }, { name: "fotos_por_prestador" }],
  ],
  // Zonas del destino, con área protegida, alta afluencia y buenas prácticas (RF-84, DC-49).
  zonas: [
    [{ nombreClave: 1 }, { name: "zona_por_nombre", unique: true }],
  ],
  // Copia de la habilitación RNT de cada prestador, que llega del núcleo por aviso (DC-02).
  prestadores: [],
  // Avisos del núcleo ya procesados, para descartar los repetidos (DC-04).
  avisos_procesados: [],
  // Opiniones con su estado de moderación (RF-59 a RF-61, DC-42).
  opiniones: [
    [{ ofertaId: 1, turistaId: 1 }, { name: "opinion_unica", unique: true }],
    [{ ofertaId: 1, fecha: -1, _id: 1 }, { name: "opiniones_aprobadas", partialFilterExpression: { estado: "APROBADA" } }],
    [{ fecha: 1, _id: 1 }, { name: "opiniones_en_moderacion", partialFilterExpression: { estado: "EN_MODERACION" } }],
    [{ turistaId: 1, fecha: -1, _id: 1 }, { name: "opiniones_del_turista" }],
  ],
  // Quién puede opinar de qué oferta: llega del núcleo cuando una solicitud queda Pagada (DC-41).
  habilitaciones_opinion: [
    [{ turistaId: 1, ofertaId: 1 }, { name: "habilitacion_unica", unique: true }],
  ],
  // Avisos de opiniones ya procesados (DC-46).
  avisos_opiniones_procesados: [],
  // Candado de las tareas programadas entre réplicas (ShedLock).
  shedLock: [],
};

const existentes = catalogo.getCollectionNames();
for (const [nombre, indices] of Object.entries(colecciones)) {
  if (!existentes.includes(nombre)) catalogo.createCollection(nombre);
  for (const [clave, opciones] of indices) catalogo.getCollection(nombre).createIndex(clave, opciones);
  print(`${nombre}: ${catalogo.getCollection(nombre).getIndexes().length - 1} índices`);
}
