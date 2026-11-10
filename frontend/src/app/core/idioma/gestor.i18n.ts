import { diccionario } from './diccionario';

/**
 * Textos del panel del gestor del destino (P09.1): el menú lateral, el filtro del periodo y la nota de privacidad. Los nombres del
 * panel y de sus tres vistas (Demanda, Embudo de conversión, Distribución por tipo de prestador) están en `app.i18n.ts`
 * (`app.titulo.*`), porque son también el título de la pestaña del navegador. Los textos de cada vista se suman aquí.
 */
export const GESTOR = diccionario(
  {
    // Panel (menú lateral)
    'gestor.panel.control': 'Panel del gestor del destino',

    // Encabezado
    'gestor.indicadores.subtitulo':
      'Una mirada al turismo en Santa Marta: la demanda, cómo avanzan las solicitudes y cómo se reparten entre los prestadores.',
    'gestor.pestanas.etiqueta': 'Vistas del panel de indicadores',
    'gestor.privacidad':
      'Los indicadores son datos agregados. Para proteger la privacidad (RET-03), una cifra de 1 o 2 solicitudes no se muestra y aparece como «menos de 3».',

    // Periodo
    'gestor.periodo.titulo': 'Periodo',
    'gestor.periodo.atajos': 'Atajos de periodo',
    'gestor.periodo.atajo.ultimos7': 'Últimos 7 días',
    'gestor.periodo.atajo.ultimos30': 'Últimos 30 días',
    'gestor.periodo.atajo.ultimos90': 'Últimos 90 días',
    'gestor.periodo.atajo.esteMes': 'Este mes',
    'gestor.periodo.atajo.mesAnterior': 'Mes anterior',
    'gestor.periodo.desde': 'Desde',
    'gestor.periodo.hasta': 'Hasta',
    'gestor.periodo.aplicar': 'Aplicar',
    'gestor.periodo.consultado': {
      uno: 'Periodo consultado: del {desde} al {hasta} ({n} día).',
      otros: 'Periodo consultado: del {desde} al {hasta} ({n} días).',
    },
    'gestor.periodo.error.desdeRequerida': 'Escribe la fecha de inicio.',
    'gestor.periodo.error.hastaRequerida': 'Escribe la fecha final.',
    'gestor.periodo.error.desdeMayor': 'La fecha de inicio no puede ser posterior a la final.',
    'gestor.periodo.error.hastaFutura': 'La fecha final no puede ser posterior a hoy.',
    'gestor.periodo.error.demasiadoLargo': 'El periodo puede tener como máximo 366 días.',

    // Vistas (comunes)
    'gestor.cargando': 'Cargando los indicadores…',
    'gestor.menosDeTres': 'menos de 3',
    'gestor.tabla.ver': 'Ver los datos en una tabla',

    // Demanda (RF-85, RF-86)
    'gestor.demanda.cifra.total': 'Solicitudes del periodo',
    'gestor.demanda.cifra.zonas': 'Zonas con solicitudes',
    'gestor.demanda.cifra.tipos': 'Tipos de oferta con solicitudes',
    'gestor.demanda.grafico.titulo': 'Solicitudes por zona y tipo de oferta',
    'gestor.demanda.grafico.ayuda':
      'De mayor a menor. Una zona aparece una vez por cada tipo de oferta que recibió solicitudes; las zonas sensibles llevan su marca.',
    'gestor.demanda.vacio': 'No hubo solicitudes en este periodo.',
    'gestor.demanda.zonaSinNombre': 'Zona sin nombre ({id})',
    'gestor.demanda.resumen':
      'Gráfico de barras horizontales de solicitudes por zona y tipo de oferta, de mayor a menor. La mayor es {zona}, {tipo}: {cantidad} solicitudes. Los datos completos están en la tabla que sigue.',
    'gestor.demanda.col.zona': 'Zona',
    'gestor.demanda.col.tipo': 'Tipo de oferta',
    'gestor.demanda.col.solicitudes': 'Solicitudes',
    'gestor.demanda.tabla.titulo': 'Solicitudes por zona y tipo de oferta, del {desde} al {hasta}',
  },
  {
    // Panel (menú lateral)
    'gestor.panel.control': 'Destination manager panel',

    // Encabezado
    'gestor.indicadores.subtitulo':
      'A look at tourism in Santa Marta: demand, how requests progress and how they are spread among providers.',
    'gestor.pestanas.etiqueta': 'Indicators dashboard views',
    'gestor.privacidad':
      'The indicators are aggregated data. To protect privacy (RET-03), a figure of 1 or 2 requests is not shown and appears as “fewer than 3”.',

    // Periodo
    'gestor.periodo.titulo': 'Period',
    'gestor.periodo.atajos': 'Period shortcuts',
    'gestor.periodo.atajo.ultimos7': 'Last 7 days',
    'gestor.periodo.atajo.ultimos30': 'Last 30 days',
    'gestor.periodo.atajo.ultimos90': 'Last 90 days',
    'gestor.periodo.atajo.esteMes': 'This month',
    'gestor.periodo.atajo.mesAnterior': 'Last month',
    'gestor.periodo.desde': 'From',
    'gestor.periodo.hasta': 'To',
    'gestor.periodo.aplicar': 'Apply',
    'gestor.periodo.consultado': {
      uno: 'Period shown: from {desde} to {hasta} ({n} day).',
      otros: 'Period shown: from {desde} to {hasta} ({n} days).',
    },
    'gestor.periodo.error.desdeRequerida': 'Enter the start date.',
    'gestor.periodo.error.hastaRequerida': 'Enter the end date.',
    'gestor.periodo.error.desdeMayor': 'The start date cannot be after the end date.',
    'gestor.periodo.error.hastaFutura': 'The end date cannot be after today.',
    'gestor.periodo.error.demasiadoLargo': 'The period can be at most 366 days long.',

    // Views (shared)
    'gestor.cargando': 'Loading the indicators…',
    'gestor.menosDeTres': 'fewer than 3',
    'gestor.tabla.ver': 'View the data in a table',

    // Demand (RF-85, RF-86)
    'gestor.demanda.cifra.total': 'Requests in the period',
    'gestor.demanda.cifra.zonas': 'Areas with requests',
    'gestor.demanda.cifra.tipos': 'Offer types with requests',
    'gestor.demanda.grafico.titulo': 'Requests by area and offer type',
    'gestor.demanda.grafico.ayuda':
      'From highest to lowest. An area appears once for each offer type that received requests; sensitive areas carry their label.',
    'gestor.demanda.vacio': 'There were no requests in this period.',
    'gestor.demanda.zonaSinNombre': 'Unnamed area ({id})',
    'gestor.demanda.resumen':
      'Horizontal bar chart of requests by area and offer type, from highest to lowest. The highest is {zona}, {tipo}: {cantidad} requests. The full data is in the table that follows.',
    'gestor.demanda.col.zona': 'Area',
    'gestor.demanda.col.tipo': 'Offer type',
    'gestor.demanda.col.solicitudes': 'Requests',
    'gestor.demanda.tabla.titulo': 'Requests by area and offer type, from {desde} to {hasta}',
  },
);
