import { Component, ElementRef, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { CuentasService } from '../../../core/api/cuentas.service';
import { AvisosService } from '../../../core/avisos/avisos.service';
import { MENSAJES } from '../../../core/errores/error-api';
import { aErrorApi } from '../../../core/errores/errores.interceptor';
import { TPipe } from '../../../core/idioma/t.pipe';
import { TextoVivo } from '../../../core/idioma/texto-vivo';
import { traducir } from '../../../core/idioma/traducir';
import { SesionService } from '../../../core/sesion/sesion.service';
import { Icono } from '../../../shared/icono/icono';

const CODIGO = /^\d{6}$/;

/**
 * Verificar el WhatsApp del turista con el código de 6 dígitos que le llegó (RF-03). Mientras no lo verifique no puede
 * enviar solicitudes (RF-30). El código vale 30 minutos y admite 3 intentos [SUPUESTO] (DN-28). Al verificar, la cuenta
 * de la sesión se actualiza y el panel desaparece solo.
 */
@Component({
  selector: 'app-verificar-whatsapp',
  imports: [ReactiveFormsModule, Icono, TPipe],
  templateUrl: './verificar-whatsapp.html',
  styleUrl: './verificar-whatsapp.css',
})
export class VerificarWhatsapp {
  private readonly cuentas = inject(CuentasService);
  private readonly sesion = inject(SesionService);
  private readonly avisos = inject(AvisosService);
  private readonly anfitrion = inject<ElementRef<HTMLElement>>(ElementRef);

  protected readonly whatsapp = () => this.sesion.cuenta()?.whatsapp;
  protected readonly codigo = new FormControl('', {
    nonNullable: true,
    validators: [Validators.required, Validators.pattern(CODIGO)],
  });
  /** El formulario necesita su grupo para que el envío lo maneje Angular y no recargue la página. */
  protected readonly formulario = new FormGroup({ codigo: this.codigo });
  protected readonly enviando = signal(false);
  protected readonly reenviando = signal(false);
  protected readonly error = signal<TextoVivo | null>(null);
  protected readonly reenviado = signal(false);

  /** Lleva el foco al campo del código, por ejemplo cuando el turista llega desde el registro. */
  enfocar(): void {
    this.anfitrion.nativeElement.querySelector<HTMLElement>('#codigo')?.focus();
  }

  protected errorDelCampo(): string | null {
    if (!this.codigo.invalid || !this.codigo.touched) {
      return null;
    }
    return this.codigo.errors?.['required']
      ? traducir('cuentas.verificar.errorVacio')
      : traducir('cuentas.verificar.errorFormato');
  }

  protected verificar(): void {
    if (this.enviando()) {
      return;
    }
    this.error.set(null);
    this.reenviado.set(false);
    if (this.codigo.invalid) {
      this.codigo.markAsTouched();
      this.enfocar();
      return;
    }
    this.enviando.set(true);
    this.cuentas.verificarWhatsapp(this.codigo.value).subscribe({
      next: (cuenta) => {
        this.enviando.set(false);
        this.sesion.actualizarCuenta(cuenta);
        this.avisos.mostrar(traducir('cuentas.verificar.verificado'));
      },
      error: (fallo: unknown) => {
        this.enviando.set(false);
        const problema = aErrorApi(fallo);
        const intentos = problema.extras?.intentosRestantes;
        this.error.set(
          problema.codigo === 'CODIGO_INCORRECTO'
            ? () => mensajeDeCodigoIncorrecto(intentos)
            : () => problema.mensaje,
        );
        this.codigo.reset('');
        this.enfocar();
      },
    });
  }

  protected reenviar(): void {
    if (this.reenviando()) {
      return;
    }
    this.error.set(null);
    this.reenviando.set(true);
    this.cuentas.enviarCodigoWhatsapp().subscribe({
      next: () => {
        this.reenviando.set(false);
        this.reenviado.set(true);
        this.codigo.reset('');
        this.enfocar();
      },
      error: (fallo: unknown) => {
        this.reenviando.set(false);
        this.reenviado.set(false);
        const problema = aErrorApi(fallo);
        this.error.set(() => problema.mensaje);
      },
    });
  }
}

/** Al tercer error el código queda invalidado y hay que pedir otro. */
function mensajeDeCodigoIncorrecto(intentosRestantes: number | undefined): string {
  if (intentosRestantes === undefined) {
    return MENSAJES.CODIGO_INCORRECTO;
  }
  if (intentosRestantes <= 0) {
    return traducir('cuentas.verificar.agotado');
  }
  return traducir('cuentas.verificar.intentos', { n: intentosRestantes });
}
