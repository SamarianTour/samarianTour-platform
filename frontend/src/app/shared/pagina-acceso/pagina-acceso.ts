import { Component, input } from '@angular/core';
import { Icono } from '../icono/icono';

/**
 * Marco de las pantallas de acceso (iniciar sesión, registro, recuperar contraseña), con el diseño de `auth-card`:
 * una tarjeta centrada con el logo, el título y el subtítulo. El formulario va dentro y, en el espacio `pie`, el
 * enlace a la pantalla vecina ("¿No tienes cuenta? Crea una").
 */
@Component({
  selector: 'app-pagina-acceso',
  imports: [Icono],
  templateUrl: './pagina-acceso.html',
  styleUrl: './pagina-acceso.css',
})
export class PaginaAcceso {
  readonly titulo = input.required<string>();
  readonly subtitulo = input<string>();
  /** Una tarjeta más ancha, para formularios de dos columnas como el registro. */
  readonly ancha = input(false);
}
