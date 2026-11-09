import { Component, inject } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { TPipe } from '../../../core/idioma/t.pipe';
import { INICIO_POR_ROL } from '../../../core/sesion/rutas-por-rol';
import { SesionService } from '../../../core/sesion/sesion.service';
import { ROL } from '../../../shared/etiquetas';
import { Icono } from '../../../shared/icono/icono';

/**
 * "No tienes permiso": la página a la que lleva una sección que es de otro rol. Ofrece volver al inicio del rol propio
 * o cambiar de cuenta, que es lo que suele hacer falta cuando se abre el enlace equivocado.
 */
@Component({
  selector: 'app-sin-permiso',
  imports: [RouterLink, Icono, TPipe],
  templateUrl: './sin-permiso.html',
  styleUrl: './sin-permiso.css',
})
export class SinPermiso {
  private readonly sesion = inject(SesionService);
  private readonly router = inject(Router);

  protected readonly rol = this.sesion.rol;
  protected readonly etiquetaRol = ROL;
  protected readonly inicio = INICIO_POR_ROL;

  protected cambiarDeCuenta(): void {
    this.sesion.cerrarSesion().subscribe(() => void this.router.navigate(['/iniciar-sesion']));
  }
}
