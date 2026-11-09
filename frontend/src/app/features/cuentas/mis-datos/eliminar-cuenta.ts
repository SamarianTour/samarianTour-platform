import { Component, ElementRef, computed, inject, signal, viewChild } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { CuentasService } from '../../../core/api/cuentas.service';
import { AvisosService } from '../../../core/avisos/avisos.service';
import { aErrorApi } from '../../../core/errores/errores.interceptor';
import { TPipe } from '../../../core/idioma/t.pipe';
import { TextoVivo } from '../../../core/idioma/texto-vivo';
import { traducir } from '../../../core/idioma/traducir';
import { mensajeDeBloqueo } from '../../../core/sesion/bloqueo';
import { SesionService } from '../../../core/sesion/sesion.service';
import { Icono } from '../../../shared/icono/icono';
import { obligatorio } from '../validaciones';

/**
 * Eliminar la cuenta (RF-13, Ley 1581): el turista y el prestador borran sus datos personales, de inmediato y sin vuelta
 * atrás, confirmando con su contraseña. Operador, gestor y administrador no tienen esta opción: su cuenta la gestiona un
 * administrador. Al terminar, el núcleo ya cerró todas las sesiones y se vuelve al catálogo.
 */
@Component({
  selector: 'app-eliminar-cuenta',
  imports: [ReactiveFormsModule, Icono, TPipe],
  templateUrl: './eliminar-cuenta.html',
  styleUrl: './eliminar-cuenta.css',
})
export class EliminarCuenta {
  private readonly cuentas = inject(CuentasService);
  private readonly sesion = inject(SesionService);
  private readonly avisos = inject(AvisosService);
  private readonly router = inject(Router);
  /** Solo existe cuando la cuenta puede eliminarse: a las cuentas internas no se les dibuja el diálogo. */
  private readonly dialogo = viewChild<ElementRef<HTMLDialogElement>>('dialogo');

  protected readonly rol = this.sesion.rol;
  protected readonly esPrestador = computed(() => this.rol() === 'PRESTADOR');
  /** Solo el turista y el prestador tienen una ruta de supresión en el núcleo. */
  protected readonly puedeEliminar = computed(
    () => this.rol() === 'TURISTA' || this.rol() === 'PRESTADOR',
  );

  protected readonly formulario = new FormGroup({
    contrasena: new FormControl('', { nonNullable: true, validators: obligatorio }),
  });
  protected readonly eliminando = signal(false);
  protected readonly error = signal<TextoVivo | null>(null);
  /** Lo que el prestador debe cumplir antes de poder eliminar sus datos (409). */
  protected readonly compromisos = signal<readonly string[]>([]);

  protected abrir(): void {
    this.formulario.reset({ contrasena: '' });
    this.error.set(null);
    this.compromisos.set([]);
    this.dialogo()?.nativeElement.showModal();
  }

  protected cerrar(): void {
    this.dialogo()?.nativeElement.close();
  }

  protected errorDeContrasena(): string | null {
    const control = this.formulario.controls.contrasena;
    return control.invalid && control.touched ? traducir('cuentas.eliminar.errorContrasena') : null;
  }

  protected eliminar(): void {
    if (this.eliminando()) {
      return;
    }
    this.error.set(null);
    this.compromisos.set([]);
    if (this.formulario.invalid) {
      this.formulario.markAllAsTouched();
      this.enfocarContrasena();
      return;
    }

    const contrasena = this.formulario.controls.contrasena.value;
    this.eliminando.set(true);
    const peticion = this.esPrestador()
      ? this.cuentas.suprimirPrestador(contrasena)
      : this.cuentas.suprimirTurista(contrasena);
    peticion.subscribe({
      next: () => {
        this.eliminando.set(false);
        this.cerrar();
        // El núcleo cerró todas las sesiones de la cuenta: la de este navegador ya no sirve.
        this.sesion.descartarSesion();
        this.avisos.mostrar(traducir('cuentas.eliminar.aviso'));
        void this.router.navigateByUrl('/', { replaceUrl: true });
      },
      error: (fallo: unknown) => {
        this.eliminando.set(false);
        this.mostrarError(fallo);
      },
    });
  }

  private mostrarError(fallo: unknown): void {
    const problema = aErrorApi(fallo);
    if (problema.codigo === 'PRESTADOR_CON_COMPROMISOS') {
      this.compromisos.set(problema.extras?.compromisos ?? []);
      this.error.set(() => problema.mensaje);
      return;
    }
    const bloqueadaHasta = problema.extras?.bloqueadaHasta;
    const ahora = Date.now();
    this.error.set(
      problema.codigo === 'CUENTA_BLOQUEADA' && bloqueadaHasta
        ? () => mensajeDeBloqueo(bloqueadaHasta, ahora)
        : () => problema.mensaje,
    );
    // Una contraseña equivocada se escribe de nuevo.
    this.formulario.controls.contrasena.reset('');
    this.enfocarContrasena();
  }

  private enfocarContrasena(): void {
    this.dialogo()?.nativeElement.querySelector<HTMLElement>('#contrasena-eliminar')?.focus();
  }
}
