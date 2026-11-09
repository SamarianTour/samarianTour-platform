import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { Rol } from '../api/modelos-nucleo';
import { SesionService } from './sesion.service';

/**
 * Solo pasa quien tiene sesión. Sin sesión lleva a iniciar sesión y recuerda la página que se quería ver (`volver`),
 * para regresar a ella después de entrar.
 */
export const autenticadoGuard: CanActivateFn = (_ruta, estado) => {
  const router = inject(Router);
  if (inject(SesionService).autenticada()) {
    return true;
  }
  return router.createUrlTree(['/iniciar-sesion'], { queryParams: { volver: estado.url } });
};

/**
 * Solo pasa quien tiene sesión con uno de los roles dados. Sin sesión, lleva a iniciar sesión; con otro rol, a la página
 * "no tienes permiso". El servidor vuelve a comprobar el rol en cada petición: esto solo evita mostrar pantallas
 * que no le sirven a quien las abre.
 */
export function rolGuard(...roles: Rol[]): CanActivateFn {
  return (ruta, estado) => {
    const rol = inject(SesionService).rol();
    if (rol && roles.includes(rol)) {
      return true;
    }
    return rol ? inject(Router).createUrlTree(['/sin-permiso']) : autenticadoGuard(ruta, estado);
  };
}
