import { Component, ElementRef, OnInit, inject, input, signal } from '@angular/core';
import {
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  ValidatorFn,
  Validators,
} from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { aErrorApi } from '../../../core/errores/errores.interceptor';
import { TPipe } from '../../../core/idioma/t.pipe';
import { TextoVivo } from '../../../core/idioma/texto-vivo';
import { traducir } from '../../../core/idioma/traducir';
import { mensajeDeBloqueo } from '../../../core/sesion/bloqueo';
import { destinoTrasIniciarSesion } from '../../../core/sesion/rutas-por-rol';
import { SesionService } from '../../../core/sesion/sesion.service';
import { Icono } from '../../../shared/icono/icono';
import { PaginaAcceso } from '../../../shared/pagina-acceso/pagina-acceso';

/** Un texto con solo espacios no cuenta como escrito. */
const obligatorio: ValidatorFn = (control) =>
  typeof control.value === 'string' && control.value.trim() !== '' ? null : { required: true };

/**
 * P06.2 Iniciar sesión (RF-04, RF-05). Pide el correo y la contraseña y, si el núcleo los acepta, lleva al usuario a la
 * página que quería ver (`volver`) o, si no la hay, al inicio de su rol. Con "Recordarme" la sesión dura 7 días; sin
 * él, hasta cerrar la pestaña.
 */
@Component({
  selector: 'app-iniciar-sesion',
  imports: [ReactiveFormsModule, RouterLink, PaginaAcceso, Icono, TPipe],
  templateUrl: './iniciar-sesion.html',
  styleUrl: './iniciar-sesion.css',
})
export class IniciarSesion implements OnInit {
  /** La página a la que volver después de entrar, tomada del parámetro `volver` de la dirección. */
  readonly volver = input<string>();

  private readonly sesion = inject(SesionService);
  private readonly router = inject(Router);
  private readonly anfitrion = inject<ElementRef<HTMLElement>>(ElementRef);

  protected readonly formulario = new FormGroup({
    correo: new FormControl('', { nonNullable: true, validators: [obligatorio, Validators.email] }),
    contrasena: new FormControl('', { nonNullable: true, validators: obligatorio }),
    recordar: new FormControl(false, { nonNullable: true }),
  });
  protected readonly enviando = signal(false);
  protected readonly error = signal<TextoVivo | null>(null);

  ngOnInit(): void {
    // Quien ya tiene sesión no necesita esta pantalla.
    const rol = this.sesion.rol();
    if (rol) {
      void this.router.navigateByUrl(destinoTrasIniciarSesion(this.volver(), rol), {
        replaceUrl: true,
      });
    }
  }

  protected errorDe(campo: 'correo' | 'contrasena'): string | null {
    const control = this.formulario.controls[campo];
    if (!control.invalid || !control.touched) {
      return null;
    }
    if (campo === 'contrasena') {
      return traducir('cuentas.login.errorContrasena');
    }
    return control.errors?.['required']
      ? traducir('cuentas.login.errorCorreo')
      : traducir('cuentas.error.correoInvalido');
  }

  protected entrar(): void {
    if (this.enviando()) {
      return;
    }
    this.error.set(null);
    if (this.formulario.invalid) {
      this.formulario.markAllAsTouched();
      this.enfocar(this.formulario.controls.correo.invalid ? 'correo' : 'contrasena');
      return;
    }

    const { correo, contrasena, recordar } = this.formulario.getRawValue();
    this.enviando.set(true);
    this.sesion.iniciarSesion(correo.trim(), contrasena, recordar).subscribe({
      next: (cuenta) => {
        void this.router.navigateByUrl(destinoTrasIniciarSesion(this.volver(), cuenta.rol), {
          replaceUrl: true,
        });
      },
      error: (fallo: unknown) => {
        const problema = aErrorApi(fallo);
        const bloqueadaHasta = problema.extras?.bloqueadaHasta;
        const ahora = Date.now();
        // Se guarda una función: el mensaje se arma al mostrarlo y cambia de idioma con la interfaz.
        this.error.set(
          problema.codigo === 'CUENTA_BLOQUEADA' && bloqueadaHasta
            ? () => mensajeDeBloqueo(bloqueadaHasta, ahora)
            : () => problema.mensaje,
        );
        this.enviando.set(false);
        // La contraseña se escribe de nuevo; el correo se conserva.
        this.formulario.controls.contrasena.reset('');
        this.enfocar('contrasena');
      },
    });
  }

  private enfocar(campo: 'correo' | 'contrasena'): void {
    this.anfitrion.nativeElement.querySelector<HTMLElement>(`#${campo}`)?.focus();
  }
}
