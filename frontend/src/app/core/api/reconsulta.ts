import { DOCUMENT } from '@angular/common';
import { inject } from '@angular/core';
import { EMPTY, Observable, filter, fromEvent, map, merge } from 'rxjs';

/**
 * Cada vez que la persona vuelve a la pestaña o la conexión regresa, la aplicación debe preguntar el estado real en lugar de
 * suponerlo (RF-33, ADR-04). Emite en esos dos momentos; cada pantalla decide qué volver a consultar.
 */
export function reconsultas(documento: Document): Observable<void> {
  const ventana = documento.defaultView;
  const alVolverALaPestana = fromEvent(documento, 'visibilitychange').pipe(
    filter(() => documento.visibilityState === 'visible'),
  );
  const alReconectar = ventana ? fromEvent(ventana, 'online') : EMPTY;
  return merge(alVolverALaPestana, alReconectar).pipe(map(() => undefined));
}

/** `reconsultas` para el documento de la aplicación. Se llama dentro de un contexto de inyección. */
export function reconsultas$(): Observable<void> {
  return reconsultas(inject(DOCUMENT));
}
