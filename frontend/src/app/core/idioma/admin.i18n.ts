import { diccionario } from './diccionario';

/**
 * Textos del panel del administrador (P05): su menú lateral y las pantallas de prestadores y RNT, moderación, cuentas internas,
 * comisión y pagos, zonas, alertas de sesgo y pendientes técnicos. Los nombres de las pantallas están en `app.i18n.ts`
 * (`app.titulo.*`), porque son también el título de la pestaña del navegador. Los textos de cada pantalla se suman aquí.
 */
export const ADMIN = diccionario(
  {
    // Panel (menú lateral)
    'admin.panel.control': 'Panel del administrador',

    // Alertas de sesgo (RET-02)
    'admin.sesgo.intro':
      'La recomendación no debe mostrar a un prestador mucho más de lo que le corresponde por su peso en el catálogo. Cada día se compara, en los últimos 30 días, el porcentaje de apariciones de cada prestador con su porcentaje de las ofertas visibles; si el índice pasa de 1,5, se abre una alerta. Una alerta se cierra sola cuando el índice baja.',
    'admin.sesgo.cargando': 'Cargando las alertas…',
    'admin.sesgo.vacioTitulo': 'Sin alertas abiertas',
    'admin.sesgo.vacioAyuda':
      'Ningún prestador aparece en las recomendaciones más de lo que le corresponde.',
    'admin.sesgo.prestadorSinNombre': 'Prestador {codigo}',
    'admin.sesgo.indice': 'Índice {indice}',
    'admin.sesgo.explicacion': 'Aparece {veces} veces lo que le corresponde.',
    'admin.sesgo.porcentajeApariciones': 'Parte de las apariciones',
    'admin.sesgo.porcentajeOfertas': 'Parte de las ofertas visibles',
    'admin.sesgo.apariciones': {
      uno: '{n} aparición en {recomendaciones} recomendaciones',
      otros: '{n} apariciones en {recomendaciones} recomendaciones',
    },
    'admin.sesgo.abierta': 'Abierta',
    'admin.sesgo.actualizada': 'Actualizada',
  },
  {
    'admin.panel.control': 'Administrator panel',

    'admin.sesgo.intro':
      "The recommendations should not show a provider much more than its weight in the catalog deserves. Every day, over the last 30 days, each provider's share of appearances is compared with its share of the visible offers; if the index goes above 1.5, an alert opens. An alert closes by itself when the index drops.",
    'admin.sesgo.cargando': 'Loading the alerts…',
    'admin.sesgo.vacioTitulo': 'No open alerts',
    'admin.sesgo.vacioAyuda': 'No provider appears in the recommendations more than it should.',
    'admin.sesgo.prestadorSinNombre': 'Provider {codigo}',
    'admin.sesgo.indice': 'Index {indice}',
    'admin.sesgo.explicacion': 'Appears {veces} times its fair share.',
    'admin.sesgo.porcentajeApariciones': 'Share of appearances',
    'admin.sesgo.porcentajeOfertas': 'Share of visible offers',
    'admin.sesgo.apariciones': {
      uno: '{n} appearance in {recomendaciones} recommendations',
      otros: '{n} appearances in {recomendaciones} recommendations',
    },
    'admin.sesgo.abierta': 'Opened',
    'admin.sesgo.actualizada': 'Updated',
  },
);
