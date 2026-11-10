import { Component } from '@angular/core';
import { EnlacePanel } from '../../../shared/panel-lateral/enlace-activo';
import { PanelLateral } from '../../../shared/panel-lateral/panel-lateral';

/**
 * Las secciones del panel del gestor del destino. Por ahora es una sola: el panel de indicadores (P09.1), que se marca en todas sus
 * vistas (Demanda, Embudo de conversión y Distribución por tipo de prestador).
 */
export const ENLACES_GESTOR: readonly EnlacePanel[] = [
  { clave: 'app.titulo.indicadores', ruta: '/gestor', icono: 'esfuerzo' },
];

/**
 * Panel del gestor del destino: barra lateral con su nombre y su menú, y a su lado la pantalla de la ruta. Usa el panel lateral
 * compartido con el del administrador. Solo lo abre quien tiene la sesión de un gestor (la ruta lo exige).
 */
@Component({
  selector: 'app-panel-gestor',
  imports: [PanelLateral],
  template: `<app-panel-lateral rol="etiquetas.rol.GESTOR_DESTINO" titulo="gestor.panel.control" [enlaces]="enlaces" />`,
  styles: `
    :host {
      display: flex;
      flex: 1;
    }
    app-panel-lateral {
      flex: 1;
    }
  `,
})
export class PanelGestor {
  protected readonly enlaces = ENLACES_GESTOR;
}
