import { Component } from '@angular/core';
import { EnlacePanel } from '../../../shared/panel-lateral/enlace-activo';
import { PanelLateral } from '../../../shared/panel-lateral/panel-lateral';

/**
 * Las secciones del panel del administrador, en el orden del menú. Los nombres son los mismos títulos de las pestañas
 * (`app.titulo.*`). El inicio (`/admin`) es la lista de prestadores y solo se marca en su dirección exacta.
 */
export const ENLACES_ADMIN: readonly EnlacePanel[] = [
  { clave: 'app.titulo.prestadoresRnt', ruta: '/admin', icono: 'escudo', exacto: true },
  { clave: 'app.titulo.moderacion', ruta: '/admin/moderacion', icono: 'estrella' },
  { clave: 'app.titulo.cuentasInternas', ruta: '/admin/cuentas', icono: 'personas' },
  { clave: 'app.titulo.comisionPagos', ruta: '/admin/comision', icono: 'paquete' },
  { clave: 'app.titulo.zonasAdmin', ruta: '/admin/zonas', icono: 'ubicacion' },
  { clave: 'app.titulo.alertasSesgo', ruta: '/admin/alertas-sesgo', icono: 'alerta' },
  { clave: 'app.titulo.pendientesTecnicos', ruta: '/admin/pendientes', icono: 'reloj' },
];

/**
 * Panel del administrador: barra lateral con su nombre y el menú de sus pantallas (P05), y a su lado la pantalla de la ruta. Solo
 * lo abre quien tiene la sesión de un administrador (la ruta lo exige).
 */
@Component({
  selector: 'app-panel-admin',
  imports: [PanelLateral],
  template: `<app-panel-lateral rol="etiquetas.rol.ADMINISTRADOR" titulo="admin.panel.control" [enlaces]="enlaces" />`,
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
export class PanelAdmin {
  protected readonly enlaces = ENLACES_ADMIN;
}
