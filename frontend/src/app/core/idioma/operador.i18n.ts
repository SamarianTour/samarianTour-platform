import { diccionario } from './diccionario';

/**
 * Textos del panel del operador de disponibilidad (P02.4, P02.5): su menú lateral, la cola, el historial, atender una
 * solicitud y sus avisos. Los textos de cada pantalla se suman aquí.
 */
export const OPERADOR = diccionario(
  {
    // Panel (menú lateral)
    'operador.panel.control': 'Panel del operador',
    'operador.panel.cola': 'Cola',
    'operador.panel.historial': 'Historial',
    'operador.panel.avisos': 'Avisos',
  },
  {
    'operador.panel.control': 'Operator panel',
    'operador.panel.cola': 'Queue',
    'operador.panel.historial': 'History',
    'operador.panel.avisos': 'Notices',
  },
);
