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
    'gestor.sinDato': 'sin dato',
    'gestor.noAplica': 'no aplica',

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

    // Embudo de conversión (RF-87, RF-88)
    'gestor.embudo.etapa.ENVIADAS': 'Enviadas',
    'gestor.embudo.etapa.ATENDIDAS': 'Atendidas',
    'gestor.embudo.etapa.CONFIRMADAS': 'Confirmadas',
    'gestor.embudo.etapa.PAGADAS': 'Pagadas',
    'gestor.embudo.cifra.enviadas': 'Solicitudes enviadas',
    'gestor.embudo.cifra.pagadas': 'Solicitudes pagadas',
    'gestor.embudo.cifra.conversion': 'De las enviadas, se pagaron',
    'gestor.embudo.grafico.titulo': 'Embudo de conversión del periodo',
    'gestor.embudo.grafico.ayuda':
      'De las solicitudes enviadas en el periodo, cuántas llegaron a ser atendidas, confirmadas y pagadas. El porcentaje es sobre las enviadas; una solicitud que se atendió y después venció cuenta como atendida.',
    'gestor.embudo.deLaAnterior': '{porcentaje} de las {anterior}',
    'gestor.embudo.resumen':
      'Gráfico de barras horizontales del embudo de conversión, de las solicitudes enviadas a las pagadas. {etapas}. Los datos completos están en la tabla que sigue.',
    'gestor.embudo.resumenTipo':
      'Gráfico de barras horizontales del embudo de las ofertas de tipo {tipo}. {etapas}. Los datos completos están en la tabla que sigue.',
    'gestor.embudo.col.etapa': 'Etapa',
    'gestor.embudo.col.solicitudes': 'Solicitudes',
    'gestor.embudo.col.porcentaje': 'Porcentaje de las enviadas',
    'gestor.embudo.col.anterior': 'Porcentaje de la etapa anterior',
    'gestor.embudo.tabla.titulo': 'Embudo de conversión, del {desde} al {hasta}',
    'gestor.embudo.tabla.tituloTipo': 'Embudo de las ofertas de tipo {tipo}, del {desde} al {hasta}',
    'gestor.embudo.porTipo.titulo': 'Por tipo de oferta',
    'gestor.embudo.porTipo.ayuda': 'El mismo embudo, separado por el tipo de oferta de cada solicitud.',
    'gestor.embudo.vacio': 'No hubo solicitudes en este periodo.',
    'gestor.embudo.tipoVacio': 'No hubo solicitudes de este tipo en el periodo.',

    // Distribución por tipo de prestador (RF-89)
    'gestor.distribucion.categoria.SIN_CATEGORIA': 'Sin categoría',
    'gestor.distribucion.cifra.total': 'Solicitudes del periodo',
    'gestor.distribucion.cifra.mayor': 'Tipo de prestador con más solicitudes',
    'gestor.distribucion.cifra.categorias': 'Tipos de prestador con solicitudes',
    'gestor.distribucion.vacio': 'No hubo solicitudes en este periodo.',
    'gestor.distribucion.solicitudes.titulo': 'Solicitudes por tipo de prestador',
    'gestor.distribucion.solicitudes.ayuda':
      'Cómo se reparte la demanda entre los prestadores grandes, medianos y pequeños y los guías independientes, según la categoría que tenía el prestador al recibir la solicitud. El porcentaje es sobre todas las solicitudes del periodo.',
    'gestor.distribucion.solicitudes.resumen':
      'Gráfico de barras horizontales de las solicitudes por tipo de prestador. {categorias}. Los datos completos están en la tabla que sigue.',
    'gestor.distribucion.solicitudes.tabla': 'Solicitudes por tipo de prestador, del {desde} al {hasta}',
    'gestor.distribucion.pagadas.titulo': 'Solicitudes pagadas por tipo de prestador',
    'gestor.distribucion.pagadas.ayuda':
      'De las solicitudes de cada tipo de prestador, cuántas llegaron a pagarse. La barra es el porcentaje de pago de ese tipo.',
    'gestor.distribucion.pagadas.resumen':
      'Gráfico de barras horizontales del porcentaje de solicitudes pagadas por tipo de prestador. {categorias}. Los datos completos están en la tabla que sigue.',
    'gestor.distribucion.pagadas.tabla': 'Solicitudes pagadas por tipo de prestador, del {desde} al {hasta}',
    'gestor.distribucion.pagadasDe': '{pagadas} de {solicitudes}',
    'gestor.distribucion.col.tipo': 'Tipo de prestador',
    'gestor.distribucion.col.solicitudes': 'Solicitudes',
    'gestor.distribucion.col.porcentaje': 'Porcentaje del total',
    'gestor.distribucion.col.pagadas': 'Pagadas',
    'gestor.distribucion.col.tasa': 'Porcentaje de pago',
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
    'gestor.sinDato': 'no data',
    'gestor.noAplica': 'not applicable',

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

    // Conversion funnel (RF-87, RF-88)
    'gestor.embudo.etapa.ENVIADAS': 'Sent',
    'gestor.embudo.etapa.ATENDIDAS': 'Handled',
    'gestor.embudo.etapa.CONFIRMADAS': 'Confirmed',
    'gestor.embudo.etapa.PAGADAS': 'Paid',
    'gestor.embudo.cifra.enviadas': 'Requests sent',
    'gestor.embudo.cifra.pagadas': 'Requests paid',
    'gestor.embudo.cifra.conversion': 'Of those sent, were paid',
    'gestor.embudo.grafico.titulo': 'Conversion funnel for the period',
    'gestor.embudo.grafico.ayuda':
      'Of the requests sent in the period, how many were handled, confirmed and paid. The percentage is out of those sent; a request that was handled and later expired counts as handled.',
    'gestor.embudo.deLaAnterior': '{porcentaje} of those {anterior}',
    'gestor.embudo.resumen':
      'Horizontal bar chart of the conversion funnel, from requests sent to requests paid. {etapas}. The full data is in the table that follows.',
    'gestor.embudo.resumenTipo':
      'Horizontal bar chart of the funnel for offers of type {tipo}. {etapas}. The full data is in the table that follows.',
    'gestor.embudo.col.etapa': 'Stage',
    'gestor.embudo.col.solicitudes': 'Requests',
    'gestor.embudo.col.porcentaje': 'Percentage of those sent',
    'gestor.embudo.col.anterior': 'Percentage of the previous stage',
    'gestor.embudo.tabla.titulo': 'Conversion funnel, from {desde} to {hasta}',
    'gestor.embudo.tabla.tituloTipo': 'Funnel for offers of type {tipo}, from {desde} to {hasta}',
    'gestor.embudo.porTipo.titulo': 'By offer type',
    'gestor.embudo.porTipo.ayuda': 'The same funnel, split by the offer type of each request.',
    'gestor.embudo.vacio': 'There were no requests in this period.',
    'gestor.embudo.tipoVacio': 'There were no requests of this type in the period.',

    // Distribution by provider type (RF-89)
    'gestor.distribucion.categoria.SIN_CATEGORIA': 'No category',
    'gestor.distribucion.cifra.total': 'Requests in the period',
    'gestor.distribucion.cifra.mayor': 'Provider type with the most requests',
    'gestor.distribucion.cifra.categorias': 'Provider types with requests',
    'gestor.distribucion.vacio': 'There were no requests in this period.',
    'gestor.distribucion.solicitudes.titulo': 'Requests by provider type',
    'gestor.distribucion.solicitudes.ayuda':
      'How demand is spread among large, medium and small providers and independent guides, based on the category the provider had when it received the request. The percentage is out of all requests in the period.',
    'gestor.distribucion.solicitudes.resumen':
      'Horizontal bar chart of requests by provider type. {categorias}. The full data is in the table that follows.',
    'gestor.distribucion.solicitudes.tabla': 'Requests by provider type, from {desde} to {hasta}',
    'gestor.distribucion.pagadas.titulo': 'Paid requests by provider type',
    'gestor.distribucion.pagadas.ayuda':
      'Of the requests of each provider type, how many were paid. The bar is the payment rate of that type.',
    'gestor.distribucion.pagadas.resumen':
      'Horizontal bar chart of the percentage of paid requests by provider type. {categorias}. The full data is in the table that follows.',
    'gestor.distribucion.pagadas.tabla': 'Paid requests by provider type, from {desde} to {hasta}',
    'gestor.distribucion.pagadasDe': '{pagadas} of {solicitudes}',
    'gestor.distribucion.col.tipo': 'Provider type',
    'gestor.distribucion.col.solicitudes': 'Requests',
    'gestor.distribucion.col.porcentaje': 'Percentage of the total',
    'gestor.distribucion.col.pagadas': 'Paid',
    'gestor.distribucion.col.tasa': 'Payment rate',
  },
);
