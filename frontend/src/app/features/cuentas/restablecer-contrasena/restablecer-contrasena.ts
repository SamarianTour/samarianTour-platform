import { Component, ElementRef, inject, input, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { CuentasService } from '../../../core/api/cuentas.service';
import { AvisosService } from '../../../core/avisos/avisos.service';
import { aErrorApi } from '../../../core/errores/errores.interceptor';
import { SesionService } from '../../../core/sesion/sesion.service';
import { Icono } from '../../../shared/icono/icono';
import { PaginaAcceso } from '../../../shared/pagina-acceso/pagina-acceso';
import {
  CONTRASENA_MAXIMA,
  CONTRASENA_MINIMA,
  contrasenaValida,
  igualA,
  mayusculaInicial,
  obligatorio,
} from '../validaciones';

/**
 * Restablecer la contraseña (RF-08). Es la pantalla a la que lleva el enlace del correo, con el `token` en la dirección:
 * `/restablecer-contrasena?token=…`. El núcleo enlaza a ese nombre exacto. Al terminar, todas las sesiones de la cuenta
 * quedan cerradas y hay que iniciar sesión con la contraseña nueva.
 */
@Component({
  selector: 'app-restablecer-contrasena',
  imports: [ReactiveFormsModule, RouterLink, PaginaAcceso, Icono],
  templateUrl: './restablecer-contrasena.html',
  styleUrl: './restablecer-contrasena.css',
})
export class RestablecerContrasena {
  /** El `token` del enlace del correo, tomado de la dirección. */
  readonly token = input<string>();

  private readonly cuentas = inject(CuentasService);
  private readonly sesion = inject(SesionService);
  private readonly router = inject(Router);
  private readonly avisos = inject(AvisosService);
  private readonly anfitrion = inject<ElementRef<HTMLElement>>(ElementRef);

  protected readonly contrasenaMinima = CONTRASENA_MINIMA;
  protected readonly formulario = new FormGroup({
    contrasena: new FormControl('', {
      nonNullable: true,
      validators: [obligatorio, contrasenaValida],
    }),
    repetir: new FormControl('', {
      nonNullable: true,
      validators: [obligatorio, igualA('contrasena')],
    }),
  });
  protected readonly enviando = signal(false);
  protected readonly error = signal<string | null>(null);
  /** El enlace venció, ya se usó o se pidió uno más reciente: no se puede seguir con este. */
  protected readonly enlaceVencido = signal(false);
  /** Error de la contraseña que devolvió la API (DATO_INVALIDO). */
  private readonly errorDeApi = signal<string | null>(null);

  constructor() {
    // "Repite la contraseña" se vuelve a comprobar al cambiar la contraseña.
    this.formulario.controls.contrasena.valueChanges
      .pipe(takeUntilDestroyed())
      .subscribe(() => this.formulario.controls.repetir.updateValueAndValidity());
    this.formulario.valueChanges
      .pipe(takeUntilDestroyed())
      .subscribe(() => this.errorDeApi.set(null));
  }

  protected errorDe(campo: 'contrasena' | 'repetir'): string | null {
    if (campo === 'contrasena' && this.errorDeApi()) {
      return this.errorDeApi();
    }
    const control = this.formulario.controls[campo];
    if (!control.invalid || !control.touched) {
      return null;
    }
    if (campo === 'repetir') {
      return control.errors?.['required']
        ? 'Repite la contraseña.'
        : 'Las contraseñas no coinciden.';
    }
    if (control.errors?.['required']) {
      return 'Escribe una contraseña.';
    }
    return control.errors?.['largo']
      ? `La contraseña debe tener entre ${CONTRASENA_MINIMA} y ${CONTRASENA_MAXIMA} caracteres.`
      : 'La contraseña debe tener al menos una letra y un número.';
  }

  protected cambiar(): void {
    const token = this.token();
    if (this.enviando() || !token) {
      return;
    }
    this.error.set(null);
    if (this.formulario.invalid) {
      this.formulario.markAllAsTouched();
      this.enfocar(this.formulario.controls.contrasena.invalid ? 'contrasena' : 'repetir');
      return;
    }

    this.enviando.set(true);
    this.cuentas.restablecerContrasena(token, this.formulario.controls.contrasena.value).subscribe({
      next: () => {
        // El núcleo cerró todas las sesiones de la cuenta: la de este navegador, si la había, ya no sirve.
        this.sesion.descartarSesion();
        this.avisos.mostrar('Cambiaste tu contraseña. Inicia sesión con la nueva.');
        void this.router.navigate(['/iniciar-sesion'], { replaceUrl: true });
      },
      error: (fallo: unknown) => {
        this.enviando.set(false);
        const problema = aErrorApi(fallo);
        if (problema.codigo === 'CODIGO_NO_VIGENTE') {
          this.enlaceVencido.set(true);
        } else if (problema.campo === 'contrasena') {
          this.errorDeApi.set(mayusculaInicial(problema.detalleCampo ?? problema.mensaje));
          this.enfocar('contrasena');
        } else {
          this.error.set(problema.mensaje);
        }
      },
    });
  }

  private enfocar(campo: 'contrasena' | 'repetir'): void {
    this.anfitrion.nativeElement.querySelector<HTMLElement>(`#${campo}`)?.focus();
  }
}
