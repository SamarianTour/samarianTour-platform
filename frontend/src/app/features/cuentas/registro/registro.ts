import {
  Component,
  ElementRef,
  OnInit,
  computed,
  inject,
  input,
  signal,
  viewChild,
} from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import {
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { startWith } from 'rxjs';
import { CuentasService } from '../../../core/api/cuentas.service';
import { PoliticaDatos, RegistroRequest } from '../../../core/api/modelos-nucleo';
import { AvisosService } from '../../../core/avisos/avisos.service';
import { MENSAJES } from '../../../core/errores/error-api';
import { aErrorApi } from '../../../core/errores/errores.interceptor';
import { INICIO_POR_ROL } from '../../../core/sesion/rutas-por-rol';
import { SesionService } from '../../../core/sesion/sesion.service';
import { Icono } from '../../../shared/icono/icono';
import { PaginaAcceso } from '../../../shared/pagina-acceso/pagina-acceso';
import { VisorPolitica } from '../politica-datos/visor-politica';
import {
  CONTRASENA_MAXIMA,
  CONTRASENA_MINIMA,
  contrasenaValida,
  igualA,
  normalizarWhatsapp,
  obligatorio,
  whatsappValido,
} from '../validaciones';

type RolRegistro = 'TURISTA' | 'PRESTADOR';
type Campo = 'nombre' | 'correo' | 'contrasena' | 'repetir' | 'whatsapp' | 'acepta';

/** Los campos que la API puede señalar en un DATO_INVALIDO, y el campo del formulario que les corresponde. */
const CAMPO_DE_API: Record<string, Campo> = {
  nombre: 'nombre',
  correo: 'correo',
  contrasena: 'contrasena',
  whatsapp: 'whatsapp',
  aceptaPolitica: 'acepta',
};

/**
 * P06.1 Registro (RF-01 a RF-03, RF-09 a RF-11). El turista o el prestador crea su cuenta aceptando la versión vigente de
 * la política de datos; el turista además da su WhatsApp. Al terminar entra solo, con los datos que acaba de escribir: el
 * turista va a verificar su WhatsApp y el prestador, a su panel.
 */
@Component({
  selector: 'app-registro',
  imports: [ReactiveFormsModule, RouterLink, PaginaAcceso, Icono, VisorPolitica],
  templateUrl: './registro.html',
  styleUrl: './registro.css',
})
export class Registro implements OnInit {
  /** `prestador` (del enlace "Soy prestador") deja elegido ese tipo de cuenta. */
  readonly tipo = input<string>();

  private readonly cuentas = inject(CuentasService);
  private readonly sesion = inject(SesionService);
  private readonly router = inject(Router);
  private readonly avisos = inject(AvisosService);
  private readonly anfitrion = inject<ElementRef<HTMLElement>>(ElementRef);
  private readonly visor = viewChild.required(VisorPolitica);

  protected readonly contrasenaMinima = CONTRASENA_MINIMA;
  protected readonly formulario = new FormGroup({
    rol: new FormControl<RolRegistro>('TURISTA', { nonNullable: true }),
    nombre: new FormControl('', {
      nonNullable: true,
      validators: [obligatorio, Validators.maxLength(100)],
    }),
    correo: new FormControl('', {
      nonNullable: true,
      validators: [obligatorio, Validators.email, Validators.maxLength(254)],
    }),
    contrasena: new FormControl('', {
      nonNullable: true,
      validators: [obligatorio, contrasenaValida],
    }),
    repetir: new FormControl('', {
      nonNullable: true,
      validators: [obligatorio, igualA('contrasena')],
    }),
    whatsapp: new FormControl('', { nonNullable: true, validators: [obligatorio, whatsappValido] }),
    acepta: new FormControl(false, { nonNullable: true, validators: Validators.requiredTrue }),
  });

  protected readonly rol = toSignal(
    this.formulario.controls.rol.valueChanges.pipe(startWith(this.formulario.controls.rol.value)),
    { initialValue: this.formulario.controls.rol.value },
  );
  protected readonly esTurista = computed(() => this.rol() === 'TURISTA');

  protected readonly politica = signal<PoliticaDatos | null>(null);
  protected readonly errorDePolitica = signal<string | null>(null);
  protected readonly enviando = signal(false);
  protected readonly errorGeneral = signal<string | null>(null);
  /** Errores de campo que devolvió la API, por campo del formulario. */
  private readonly erroresDeApi = signal<Partial<Record<Campo, string>>>({});
  /** Cambia con cada edición, para que los mensajes de error se vuelvan a calcular. */
  private readonly cambios = signal(0);

  constructor() {
    // El WhatsApp solo se pide al turista: al prestador el núcleo no se lo acepta (RF-03).
    this.formulario.controls.rol.valueChanges
      .pipe(takeUntilDestroyed())
      .subscribe((rol) => this.ajustarWhatsapp(rol));
    // "Repite la contraseña" se vuelve a comprobar al cambiar la contraseña.
    this.formulario.controls.contrasena.valueChanges
      .pipe(takeUntilDestroyed())
      .subscribe(() => this.formulario.controls.repetir.updateValueAndValidity());
    // Al corregir un dato desaparece el error que la API había marcado.
    this.formulario.valueChanges.pipe(takeUntilDestroyed()).subscribe(() => {
      this.erroresDeApi.set({});
      this.cambios.update((cambios) => cambios + 1);
    });
  }

  ngOnInit(): void {
    // Quien ya tiene sesión no necesita registrarse.
    const rol = this.sesion.rol();
    if (rol) {
      void this.router.navigateByUrl(INICIO_POR_ROL[rol], { replaceUrl: true });
      return;
    }
    if (this.tipo() === 'prestador') {
      this.formulario.controls.rol.setValue('PRESTADOR');
    }
    this.cargarPolitica();
  }

  protected cargarPolitica(): void {
    this.errorDePolitica.set(null);
    this.cuentas.politicaVigente().subscribe({
      next: (politica) => this.politica.set(politica),
      error: (fallo: unknown) => this.errorDePolitica.set(aErrorApi(fallo).mensaje),
    });
  }

  protected abrirPolitica(): void {
    this.visor().abrir();
  }

  /** Mensaje de error de un campo: primero el de la API; si no, el de la validación local una vez tocado. */
  protected errorDe(campo: Campo): string | null {
    this.cambios();
    const deApi = this.erroresDeApi()[campo];
    if (deApi) {
      return deApi;
    }
    const control = this.formulario.controls[campo];
    return control.invalid && control.touched
      ? mensajeDe(campo, control.errors, this.esTurista())
      : null;
  }

  /** El núcleo dice que el correo ya tiene cuenta: se ofrece iniciar sesión o recuperar la contraseña. */
  protected correoRepetido(): boolean {
    return this.erroresDeApi().correo === MENSAJES.CORREO_YA_REGISTRADO;
  }

  protected registrar(): void {
    if (this.enviando()) {
      return;
    }
    this.errorGeneral.set(null);
    this.erroresDeApi.set({});
    const politica = this.politica();
    if (this.formulario.invalid || !politica) {
      this.formulario.markAllAsTouched();
      this.cambios.update((cambios) => cambios + 1);
      if (this.formulario.valid && !politica) {
        this.errorGeneral.set(
          'No pudimos cargar la política de datos que debes aceptar. Vuelve a intentarlo antes de crear la cuenta.',
        );
        return;
      }
      this.enfocar(this.primerCampoInvalido());
      return;
    }

    const datos = this.formulario.getRawValue();
    const solicitud: RegistroRequest = {
      rol: datos.rol,
      nombre: datos.nombre.trim(),
      correo: datos.correo.trim(),
      contrasena: datos.contrasena,
      ...(datos.rol === 'TURISTA' && { whatsapp: normalizarWhatsapp(datos.whatsapp) }),
      aceptaPolitica: true,
      versionPolitica: politica.version,
    };
    this.enviando.set(true);
    this.cuentas.registrar(solicitud).subscribe({
      next: () => this.entrar(solicitud),
      error: (fallo: unknown) => {
        this.enviando.set(false);
        this.mostrarError(fallo);
      },
    });
  }

  /** La cuenta ya existe: se inicia la sesión con los mismos datos, sin pedirlos otra vez. */
  private entrar(solicitud: RegistroRequest): void {
    this.sesion.iniciarSesion(solicitud.correo, solicitud.contrasena, false).subscribe({
      next: (cuenta) => {
        if (cuenta.rol === 'TURISTA') {
          this.avisos.mostrar(
            'Cuenta creada. Te enviamos un código por WhatsApp para verificar tu número.',
          );
          void this.router.navigate(['/mis-datos'], {
            queryParams: { verificar: 'whatsapp' },
            replaceUrl: true,
          });
        } else {
          this.avisos.mostrar('Cuenta creada. Bienvenido a Samarian Tour.');
          void this.router.navigateByUrl(INICIO_POR_ROL[cuenta.rol], { replaceUrl: true });
        }
      },
      error: () => {
        // La cuenta se creó pero la sesión no abrió: basta con iniciar sesión a mano.
        this.avisos.mostrar('Cuenta creada. Inicia sesión para continuar.');
        void this.router.navigate(['/iniciar-sesion'], { replaceUrl: true });
      },
    });
  }

  private mostrarError(fallo: unknown): void {
    const error = aErrorApi(fallo);
    if (error.codigo === 'POLITICA_NO_VIGENTE') {
      // Cambió la versión mientras se llenaba el formulario: hay que leer y aceptar la nueva.
      this.formulario.controls.acepta.setValue(false);
      this.cargarPolitica();
      this.erroresDeApi.set({ acepta: MENSAJES.POLITICA_NO_VIGENTE });
      this.enfocar('acepta');
      return;
    }
    if (error.codigo === 'CORREO_YA_REGISTRADO') {
      this.erroresDeApi.set({ correo: MENSAJES.CORREO_YA_REGISTRADO });
      this.enfocar('correo');
      return;
    }
    const campo = error.campo ? CAMPO_DE_API[error.campo] : undefined;
    if (campo) {
      this.erroresDeApi.set({ [campo]: mayusculaInicial(error.detalleCampo ?? error.mensaje) });
      this.enfocar(campo);
      return;
    }
    this.errorGeneral.set(error.mensaje);
  }

  private ajustarWhatsapp(rol: RolRegistro): void {
    const whatsapp = this.formulario.controls.whatsapp;
    whatsapp.setValidators(rol === 'TURISTA' ? [obligatorio, whatsappValido] : null);
    whatsapp.updateValueAndValidity({ emitEvent: false });
  }

  private primerCampoInvalido(): Campo | undefined {
    const controles = this.formulario.controls;
    return (['nombre', 'correo', 'contrasena', 'repetir', 'whatsapp', 'acepta'] as const).find(
      (campo) => controles[campo].invalid,
    );
  }

  /** Lleva el foco al campo con error; los campos tienen como id el nombre de su control. */
  private enfocar(campo: Campo | undefined): void {
    if (campo) {
      this.anfitrion.nativeElement.querySelector<HTMLElement>(`#${campo}`)?.focus();
    }
  }
}

function mayusculaInicial(texto: string): string {
  return texto.charAt(0).toUpperCase() + texto.slice(1);
}

function mensajeDe(
  campo: Campo,
  errores: ValidationErrors | null,
  esTurista: boolean,
): string | null {
  if (!errores) {
    return null;
  }
  switch (campo) {
    case 'nombre':
      return errores['maxlength']
        ? 'El nombre puede tener hasta 100 caracteres.'
        : esTurista
          ? 'Escribe tu nombre completo.'
          : 'Escribe el nombre comercial de tu negocio.';
    case 'correo':
      if (errores['required']) {
        return 'Escribe tu correo.';
      }
      return errores['maxlength']
        ? 'El correo puede tener hasta 254 caracteres.'
        : 'Escribe un correo válido, por ejemplo nombre@correo.co.';
    case 'contrasena':
      if (errores['required']) {
        return 'Escribe una contraseña.';
      }
      return errores['largo']
        ? `La contraseña debe tener entre ${CONTRASENA_MINIMA} y ${CONTRASENA_MAXIMA} caracteres.`
        : 'La contraseña debe tener al menos una letra y un número.';
    case 'repetir':
      return errores['required'] ? 'Repite la contraseña.' : 'Las contraseñas no coinciden.';
    case 'whatsapp':
      return errores['required']
        ? 'Escribe tu número de WhatsApp.'
        : 'Escribe el número con el indicativo del país, por ejemplo +573001234567.';
    case 'acepta':
      return 'Debes aceptar la política de tratamiento de datos para crear tu cuenta.';
  }
}
