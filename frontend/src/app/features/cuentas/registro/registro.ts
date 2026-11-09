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
import { TPipe } from '../../../core/idioma/t.pipe';
import { TextoVivo, textoVivo } from '../../../core/idioma/texto-vivo';
import { traducir } from '../../../core/idioma/traducir';
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
  mayusculaInicial,
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

/** El error "ya hay una cuenta con ese correo". Es siempre el mismo objeto: así la pantalla lo reconoce para ofrecer entrar. */
const CORREO_REPETIDO: TextoVivo = () => MENSAJES.CORREO_YA_REGISTRADO;

/**
 * P06.1 Registro (RF-01 a RF-03, RF-09 a RF-11). El turista o el prestador crea su cuenta aceptando la versión vigente de
 * la política de datos; el turista además da su WhatsApp. Al terminar entra solo, con los datos que acaba de escribir: el
 * turista va a verificar su WhatsApp y el prestador, a su panel.
 */
@Component({
  selector: 'app-registro',
  imports: [ReactiveFormsModule, RouterLink, PaginaAcceso, Icono, VisorPolitica, TPipe],
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
  protected readonly errorDePolitica = signal<TextoVivo | null>(null);
  protected readonly enviando = signal(false);
  protected readonly errorGeneral = signal<TextoVivo | null>(null);
  /** Errores de campo que devolvió la API, por campo del formulario. */
  private readonly erroresDeApi = signal<Partial<Record<Campo, TextoVivo>>>({});
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
      error: (fallo: unknown) => {
        const problema = aErrorApi(fallo);
        this.errorDePolitica.set(() => problema.mensaje);
      },
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
      return deApi();
    }
    const control = this.formulario.controls[campo];
    return control.invalid && control.touched
      ? mensajeDe(campo, control.errors, this.esTurista())
      : null;
  }

  /** El núcleo dice que el correo ya tiene cuenta: se ofrece iniciar sesión o recuperar la contraseña. */
  protected correoRepetido(): boolean {
    return this.erroresDeApi().correo === CORREO_REPETIDO;
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
        this.errorGeneral.set(textoVivo('cuentas.registro.sinPolitica'));
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
          this.avisos.mostrar(traducir('cuentas.registro.avisoTurista'));
          void this.router.navigate(['/mis-datos'], {
            queryParams: { verificar: 'whatsapp' },
            replaceUrl: true,
          });
        } else {
          this.avisos.mostrar(traducir('cuentas.registro.avisoPrestador'));
          void this.router.navigateByUrl(INICIO_POR_ROL[cuenta.rol], { replaceUrl: true });
        }
      },
      error: () => {
        // La cuenta se creó pero la sesión no abrió: basta con iniciar sesión a mano.
        this.avisos.mostrar(traducir('cuentas.registro.avisoSinSesion'));
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
      this.erroresDeApi.set({ acepta: () => MENSAJES.POLITICA_NO_VIGENTE });
      this.enfocar('acepta');
      return;
    }
    if (error.codigo === 'CORREO_YA_REGISTRADO') {
      this.erroresDeApi.set({ correo: CORREO_REPETIDO });
      this.enfocar('correo');
      return;
    }
    const campo = error.campo ? CAMPO_DE_API[error.campo] : undefined;
    if (campo) {
      this.erroresDeApi.set({
        [campo]: () => mayusculaInicial(error.detalleCampo ?? error.mensaje),
      });
      this.enfocar(campo);
      return;
    }
    this.errorGeneral.set(() => error.mensaje);
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
        ? traducir('cuentas.error.nombreLargo')
        : esTurista
          ? traducir('cuentas.registro.errorNombreTurista')
          : traducir('cuentas.registro.errorNombrePrestador');
    case 'correo':
      if (errores['required']) {
        return traducir('cuentas.registro.errorCorreo');
      }
      return errores['maxlength']
        ? traducir('cuentas.registro.errorCorreoLargo')
        : traducir('cuentas.error.correoInvalido');
    case 'contrasena':
      if (errores['required']) {
        return traducir('cuentas.error.escribeContrasena');
      }
      return errores['largo']
        ? traducir('cuentas.error.contrasenaLargo', {
            minimo: CONTRASENA_MINIMA,
            maximo: CONTRASENA_MAXIMA,
          })
        : traducir('cuentas.error.contrasenaLetraNumero');
    case 'repetir':
      return errores['required']
        ? traducir('cuentas.error.repiteContrasena')
        : traducir('cuentas.error.noCoinciden');
    case 'whatsapp':
      return errores['required']
        ? traducir('cuentas.error.escribeWhatsapp')
        : traducir('cuentas.error.whatsappInvalido');
    case 'acepta':
      return traducir('cuentas.registro.errorAcepta');
  }
}
