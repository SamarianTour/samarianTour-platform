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
  },
);
