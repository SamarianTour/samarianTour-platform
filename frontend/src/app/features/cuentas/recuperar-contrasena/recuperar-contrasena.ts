import { Component, ElementRef, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { CuentasService } from '../../../core/api/cuentas.service';
import { aErrorApi } from '../../../core/errores/errores.interceptor';
import { Icono } from '../../../shared/icono/icono';
import { PaginaAcceso } from '../../../shared/pagina-acceso/pagina-acceso';
import { obligatorio } from '../validaciones';

/**
 * P06.3 Recuperar el acceso (RF-07). Pide el correo y el núcleo envía un enlace para elegir una contraseña nueva. Responde
 * lo mismo exista o no la cuenta, así que la pantalla nunca dice si el correo está registrado.
 */
@Component({
  selector: 'app-recuperar-contrasena',
  imports: [ReactiveFormsModule, RouterLink, PaginaAcceso, Icono],
  templateUrl: './recuperar-contrasena.html',
  styleUrl: './recuperar-contrasena.css',
})
export class RecuperarContrasena {
  private readonly cuentas = inject(CuentasService);
  private readonly anfitrion = inject<ElementRef<HTMLElement>>(ElementRef);

  protected readonly formulario = new FormGroup({
    correo: new FormControl('', { nonNullable: true, validators: [obligatorio, Validators.email] }),
  });
  protected readonly enviando = signal(false);
  protected readonly error = signal<string | null>(null);
  /** El correo al que se pidió el enlace; mientras tenga valor se muestra la confirmación en lugar del formulario. */
  protected readonly enviadoA = signal<string | null>(null);

  protected errorDeCorreo(): string | null {
    const control = this.formulario.controls.correo;
    if (!control.invalid || !control.touched) {
      return null;
    }
    return control.errors?.['required']
      ? 'Escribe el correo de tu cuenta.'
      : 'Escribe un correo válido, por ejemplo nombre@correo.co.';
  }

  protected pedirEnlace(): void {
    if (this.enviando()) {
      return;
    }
    this.error.set(null);
    if (this.formulario.invalid) {
      this.formulario.markAllAsTouched();
      this.enfocarCorreo();
      return;
    }
    const correo = this.formulario.controls.correo.value.trim();
    this.enviando.set(true);
    this.cuentas.solicitarRestablecimiento(correo).subscribe({
      next: () => {
        this.enviando.set(false);
        this.enviadoA.set(correo);
      },
      error: (fallo: unknown) => {
        this.enviando.set(false);
        this.error.set(aErrorApi(fallo).mensaje);
      },
    });
  }

  /** Volver al formulario, con el correo escrito, para pedir otro enlace o corregir el correo. */
  protected pedirOtro(): void {
    this.enviadoA.set(null);
  }

  private enfocarCorreo(): void {
    this.anfitrion.nativeElement.querySelector<HTMLElement>('#correo')?.focus();
  }
}
