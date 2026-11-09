import {
  Component,
  ElementRef,
  Injector,
  OnInit,
  afterNextRender,
  computed,
  inject,
  input,
  signal,
  viewChild,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { CuentasService } from '../../../core/api/cuentas.service';
import { DatosPersonalesRequest, PoliticaDatos } from '../../../core/api/modelos-nucleo';
import { AvisosService } from '../../../core/avisos/avisos.service';
import { aErrorApi } from '../../../core/errores/errores.interceptor';
import { SesionService } from '../../../core/sesion/sesion.service';
import { ROL } from '../../../shared/etiquetas';
import { FechaPipe } from '../../../shared/fecha.pipe';
import { Icono } from '../../../shared/icono/icono';
import { VisorPolitica } from '../politica-datos/visor-politica';
import { mayusculaInicial, normalizarWhatsapp, obligatorio, whatsappValido } from '../validaciones';
import { EliminarCuenta } from './eliminar-cuenta';
import { VerificarWhatsapp } from './verificar-whatsapp';

type Campo = 'nombre' | 'whatsapp';

/**
 * P06.4 Mis datos y derechos del titular (RF-12, Ley 1581 de 2012). Cualquier rol consulta sus datos y puede corregirlos;
 * el turista además verifica su WhatsApp. El correo no se cambia (DN-31). De los seis derechos del titular, el contrato del
 * núcleo solo da una acción para acceso, rectificación y supresión: los otros tres se explican pero no tienen botón. La
 * supresión (eliminar la cuenta) está en su propio componente.
 */
@Component({
  selector: 'app-mis-datos',
  imports: [ReactiveFormsModule, FechaPipe, Icono, VisorPolitica, VerificarWhatsapp, EliminarCuenta],
  templateUrl: './mis-datos.html',
  styleUrl: './mis-datos.css',
})
export class MisDatos implements OnInit {
  /** `whatsapp` (del enlace al terminar el registro) lleva el foco directo al código de verificación. */
  readonly verificar = input<string>();

  private readonly cuentas = inject(CuentasService);
  private readonly sesion = inject(SesionService);
  private readonly avisos = inject(AvisosService);
  private readonly anfitrion = inject<ElementRef<HTMLElement>>(ElementRef);
  private readonly injector = inject(Injector);
  private readonly visor = viewChild.required(VisorPolitica);
  private readonly panelWhatsapp = viewChild(VerificarWhatsapp);

  protected readonly cuenta = this.sesion.cuenta;
  protected readonly etiquetaRol = ROL;
  protected readonly esTurista = computed(() => this.cuenta()?.rol === 'TURISTA');
  /** Solo el turista y el prestador pueden eliminar su cuenta desde aquí. */
  protected readonly puedeEliminar = computed(
    () => this.cuenta()?.rol === 'TURISTA' || this.cuenta()?.rol === 'PRESTADOR',
  );
  protected readonly faltaVerificar = computed(
    () => this.esTurista() && this.cuenta()?.whatsappVerificado === false,
  );

  protected readonly cargando = signal(true);
  protected readonly errorDeCarga = signal<string | null>(null);
  protected readonly politica = signal<PoliticaDatos | null>(null);

  protected readonly editando = signal(false);
  protected readonly guardando = signal(false);
  protected readonly errorGeneral = signal<string | null>(null);
  /** Errores de campo que devolvió la API, por campo. */
  private readonly erroresDeApi = signal<Partial<Record<Campo, string>>>({});
  private readonly cambios = signal(0);
  protected readonly formulario = new FormGroup({
    nombre: new FormControl('', {
      nonNullable: true,
      validators: [obligatorio, Validators.maxLength(100)],
    }),
    whatsapp: new FormControl('', { nonNullable: true, validators: [obligatorio, whatsappValido] }),
  });

  constructor() {
    this.formulario.valueChanges.pipe(takeUntilDestroyed()).subscribe(() => {
      this.erroresDeApi.set({});
      this.cambios.update((cambios) => cambios + 1);
    });
  }

  ngOnInit(): void {
    this.cargar();
    // La política es un dato secundario: si no se puede leer, solo falta el botón para abrirla.
    this.cuentas
      .politicaVigente()
      .subscribe({ next: (politica) => this.politica.set(politica), error: () => undefined });
    this.enfocarVerificacionSiCorresponde();
  }

  /** Trae los datos del servidor: lo que guarda la sesión puede estar desactualizado, por ejemplo el WhatsApp verificado. */
  protected cargar(): void {
    this.cargando.set(true);
    this.errorDeCarga.set(null);
    this.cuentas.consultarCuenta().subscribe({
      next: (cuenta) => {
        this.sesion.actualizarCuenta(cuenta);
        this.cargando.set(false);
        // Con la cuenta del servidor puede aparecer el panel de verificación que la sesión todavía no mostraba.
        this.enfocarVerificacionSiCorresponde();
      },
      error: (fallo: unknown) => {
        this.errorDeCarga.set(aErrorApi(fallo).mensaje);
        this.cargando.set(false);
      },
    });
  }

  protected abrirPolitica(): void {
    this.visor().abrir();
  }

  protected irAEliminar(): void {
    this.anfitrion.nativeElement.querySelector<HTMLElement>('#eliminar-titulo')?.focus();
  }

  protected irADatos(): void {
    this.anfitrion.nativeElement.querySelector<HTMLElement>('#datos-titulo')?.focus();
  }

  protected empezarEdicion(): void {
    const cuenta = this.cuenta();
    if (!cuenta) {
      return;
    }
    this.formulario.reset({ nombre: cuenta.nombre, whatsapp: cuenta.whatsapp ?? '' });
    this.ajustarWhatsapp();
    this.errorGeneral.set(null);
    this.editando.set(true);
    this.despuesDePintar(() => this.enfocar('nombre'));
  }

  protected cancelarEdicion(): void {
    this.editando.set(false);
    this.despuesDePintar(() =>
      this.anfitrion.nativeElement.querySelector<HTMLElement>('#editar')?.focus(),
    );
  }

  /** Mensaje de error de un campo: primero el de la API; si no, el de la validación local una vez tocado. */
  protected errorDe(campo: Campo): string | null {
    this.cambios();
    const deApi = this.erroresDeApi()[campo];
    if (deApi) {
      return deApi;
    }
    const control = this.formulario.controls[campo];
    if (!control.invalid || !control.touched) {
      return null;
    }
    if (campo === 'nombre') {
      return control.errors?.['maxlength']
        ? 'El nombre puede tener hasta 100 caracteres.'
        : 'Escribe tu nombre.';
    }
    return control.errors?.['required']
      ? 'Escribe tu número de WhatsApp.'
      : 'Escribe el número con el indicativo del país, por ejemplo +573001234567.';
  }

  protected guardar(): void {
    const cuenta = this.cuenta();
    if (this.guardando() || !cuenta) {
      return;
    }
    this.errorGeneral.set(null);
    if (this.formulario.invalid) {
      this.formulario.markAllAsTouched();
      this.cambios.update((cambios) => cambios + 1);
      this.enfocar(this.formulario.controls.nombre.invalid ? 'nombre' : 'whatsapp');
      return;
    }

    const nombre = this.formulario.controls.nombre.value.trim();
    const whatsapp = normalizarWhatsapp(this.formulario.controls.whatsapp.value);
    const cambioWhatsapp = this.esTurista() && whatsapp !== (cuenta.whatsapp ?? '');
    if (nombre === cuenta.nombre && !cambioWhatsapp) {
      // No cambió nada: no hace falta molestar al servidor.
      this.cancelarEdicion();
      return;
    }

    const datos: DatosPersonalesRequest = { nombre, ...(this.esTurista() && { whatsapp }) };
    this.guardando.set(true);
    this.cuentas.guardarDatos(datos).subscribe({
      next: (actualizada) => {
        this.guardando.set(false);
        this.sesion.actualizarCuenta(actualizada);
        this.avisos.mostrar(
          cambioWhatsapp
            ? 'Guardamos tus datos. Te enviamos un código por WhatsApp para verificar tu número nuevo.'
            : 'Guardamos tus datos.',
        );
        this.cancelarEdicion();
      },
      error: (fallo: unknown) => {
        this.guardando.set(false);
        this.mostrarError(fallo);
      },
    });
  }

  private mostrarError(fallo: unknown): void {
    const error = aErrorApi(fallo);
    if (error.campo === 'nombre' || error.campo === 'whatsapp') {
      this.erroresDeApi.set({
        [error.campo]: mayusculaInicial(error.detalleCampo ?? error.mensaje),
      });
      this.enfocar(error.campo);
      return;
    }
    // El texto general de este código habla de ofertas: aquí lo que cambió fueron los datos de la cuenta.
    this.errorGeneral.set(
      error.codigo === 'CONFLICTO_CONCURRENCIA'
        ? 'Tus datos cambiaron mientras los editabas. Vuelve a intentarlo.'
        : error.mensaje,
    );
  }

  /** Solo el turista tiene WhatsApp en su cuenta: a las demás cuentas no se les pide ni se les valida. */
  private ajustarWhatsapp(): void {
    const whatsapp = this.formulario.controls.whatsapp;
    whatsapp.setValidators(this.esTurista() ? [obligatorio, whatsappValido] : null);
    whatsapp.updateValueAndValidity({ emitEvent: false });
  }

  /** El foco se mueve cuando el elemento ya está en pantalla, no antes. */
  private despuesDePintar(accion: () => void): void {
    afterNextRender(accion, { injector: this.injector });
  }

  private enfocarVerificacionSiCorresponde(): void {
    if (this.verificar() === 'whatsapp') {
      this.despuesDePintar(() => this.panelWhatsapp()?.enfocar());
    }
  }

  private enfocar(campo: Campo): void {
    this.anfitrion.nativeElement.querySelector<HTMLElement>(`#${campo}`)?.focus();
  }
}
