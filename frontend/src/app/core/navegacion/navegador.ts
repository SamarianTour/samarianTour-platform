import { DOCUMENT } from '@angular/common';
import { Injectable, inject } from '@angular/core';

/**
 * Sale de la aplicación hacia otra dirección, en la misma pestaña (por ejemplo, a la pasarela de pago, que devuelve al
 * turista a su solicitud). Es un servicio aparte para que las pruebas puedan comprobar a dónde se va sin abrir la dirección.
 */
@Injectable({ providedIn: 'root' })
export class Navegador {
  private readonly documento = inject(DOCUMENT);

  /** Abre la dirección en esta pestaña. La aplicación se descarga: al volver, se carga de nuevo. */
  irA(url: string): void {
    this.documento.defaultView?.location.assign(url);
  }
}
