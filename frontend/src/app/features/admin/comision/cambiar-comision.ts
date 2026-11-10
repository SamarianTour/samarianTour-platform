import { Component, ElementRef, computed, inject, signal, viewChild } from '@angular/core';
import { takeUntilDestroyed, toObservable, toSignal } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { BehaviorSubject, catchError, combineLatest, forkJoin, map, of, startWith, switchMap, tap } from 'rxjs';
import { AdminService } from '../../../core/api/admin.service';
import { Comision, CuentaInterna } from '../../../core/api/modelos-nucleo';
import { reconsultas$ } from '../../../core/api/reconsulta';
import { AvisosService } from '../../../core/avisos/avisos.service';
import { ErrorApi } from '../../../core/errores/error-api';
import { aErrorApi } from '../../../core/errores/errores.interceptor';
import { TPipe } from '../../../core/idioma/t.pipe';
import { TextoVivo } from '../../../core/idioma/texto-vivo';
import type { ClaveTexto } from '../../../core/idioma/textos';
import { traducir } from '../../../core/idioma/traducir';
import { SesionService } from '../../../core/sesion/sesion.service';
import { DialogoConfirmacion } from '../../../shared/dialogo-confirmacion/dialogo-confirmacion';
import { FechaPipe } from '../../../shared/fecha.pipe';
import { Icono } from '../../../shared/icono/icono';
import { NumeroPipe } from '../../../shared/numero.pipe';
import { PrecioPipe } from '../../../shared/precio.pipe';
import { codigoDelPrestador } from '../alertas-sesgo';
import {
  ErrorDePorcentaje,
  ejemploDeComision,
  nombreDelAdministrador,
  ordenarHistorial,
  porcentajeParaEnviar,
  validarPorcentaje,
} from '../comision';

const CLAVE_DE_ERROR: Readonly<Record<ErrorDePorcentaje, ClaveTexto>> = {
  requerido: 'admin.comision.error.requerido',
  rango: 'admin.comision.error.rango',
  decimales: 'admin.comision.error.decimales',
  igual: 'admin.comision.error.igual',
};

type Cargado<T> = { datos: T } | { error: ErrorApi };

/**
 * Comisión de la plataforma (RF-82, RF-83): el porcentaje del precio base que se descuenta al prestador al dispersarle el pago;
 * el turista no paga nada adicional (RF-49). Muestra la vigente, deja cambiarla con una confirmación y lista el historial. Un cambio
 * rige para los pagos que se confirmen desde ahora, sin efecto retroactivo. El historial trae solo el identificador de quien cambió
 * el porcentaje; su nombre sale de las cuentas internas [SUPUESTO].
 */
@Component({
  selector: 'app-cambiar-comision',
  imports: [ReactiveFormsModule, Icono, TPipe, FechaPipe, NumeroPipe, PrecioPipe, DialogoConfirmacion],
  templateUrl: './cambiar-comision.html',
  styleUrl: './cambiar-comision.css',
})
export class CambiarComision {
  private readonly admin = inject(AdminService);
  private readonly sesion = inject(SesionService);
  private readonly avisos = inject(AvisosService);
  private readonly anfitrion = inject<ElementRef<HTMLElement>>(ElementRef);
  private readonly dialogo = viewChild.required(DialogoConfirmacion);
  /** Solo cambia si entra otra cuenta: editar los datos personales no vuelve a pedir la comisión. */
  private readonly idDeLaCuenta = computed(() => this.sesion.cuenta()?.id);
  private readonly recarga = new BehaviorSubject<void>(undefined);
  private readonly numero = new NumeroPipe();

  protected readonly codigo = codigoDelPrestador;

  protected readonly cargando = signal(true);
  protected readonly comision = signal<Comision | null>(null);
  protected readonly error = signal<ErrorApi | null>(null);
  private readonly cuentas = signal<readonly CuentaInterna[]>([]);

  protected readonly historial = computed(() =>
    ordenarHistorial(this.comision()?.historial ?? []).map((cambio) => ({
      cambio,
      quien: nombreDelAdministrador(cambio.administradorId, this.cuentas()),
      vigente: cambio.vigenteDesde === this.comision()?.vigenteDesde,
    })),
  );

  protected readonly formulario = new FormGroup({
    porcentaje: new FormControl<number | null>(null),
  });
  private readonly valor = toSignal(
    this.formulario.controls.porcentaje.valueChanges.pipe(startWith(this.formulario.controls.porcentaje.value)),
    { initialValue: null },
  );
  private readonly errorLocal = computed(() => validarPorcentaje(this.valor(), this.comision()?.porcentaje));
  private readonly intentado = signal(false);
  private readonly errorDeApi = signal<TextoVivo | null>(null);
  protected readonly cambiando = signal(false);
  protected readonly errorGeneral = signal<TextoVivo | null>(null);

  /** El porcentaje que se está por confirmar, para el diálogo. */
  protected readonly pendiente = signal<number | null>(null);
  protected readonly tituloDelDialogo = computed(() =>
    traducir('admin.comision.dialogoTitulo', { nuevo: this.numero.transform(this.pendiente() ?? 0, 2) }),
  );
  protected readonly mensajeDelDialogo = computed(() =>
    traducir('admin.comision.dialogoMensaje', {
      actual: this.numero.transform(this.comision()?.porcentaje ?? 0, 2),
      nuevo: this.numero.transform(this.pendiente() ?? 0, 2),
    }),
  );

  /** El ejemplo de cómo quedaría un pago con el porcentaje escrito, mientras sea válido. */
  protected readonly ejemplo = computed(() =>
    this.errorLocal() === null || this.errorLocal() === 'igual' ? ejemploDeComision(this.valor()) : null,
  );

  constructor() {
    combineLatest([toObservable(this.idDeLaCuenta), this.recarga])
      .pipe(
        tap(() => this.cargando.set(true)),
        switchMap(() =>
          forkJoin({
            comision: this.admin.comision().pipe(
              map((datos): Cargado<Comision> => ({ datos })),
              catchError((fallo: unknown) => of<Cargado<Comision>>({ error: aErrorApi(fallo) })),
            ),
            // Las cuentas solo ponen nombre a quien cambió el porcentaje: si no se pueden leer, se muestra su código.
            cuentas: this.admin.cuentasInternas().pipe(catchError(() => of<CuentaInterna[]>([]))),
          }),
        ),
        takeUntilDestroyed(),
      )
      .subscribe(({ comision, cuentas }) => {
        this.cargando.set(false);
        // Si la consulta falla se conserva lo que ya se veía y se avisa del error.
        if ('datos' in comision) {
          this.comision.set(comision.datos);
        }
        this.error.set('error' in comision ? comision.error : null);
        this.cuentas.set(cuentas);
      });

    // Al corregir el valor desaparece el error que la API había marcado.
    this.formulario.controls.porcentaje.valueChanges.pipe(takeUntilDestroyed()).subscribe(() => this.errorDeApi.set(null));

    reconsultas$()
      .pipe(takeUntilDestroyed())
      .subscribe(() => this.recarga.next());
  }

  /** El mensaje de error del porcentaje: primero el de la API; si no, el de la validación local una vez tocado o al enviar. */
  protected errorDe(): string | null {
    const deApi = this.errorDeApi();
    if (deApi) {
      return deApi();
    }
    const motivo = this.errorLocal();
    if (!motivo || !(this.intentado() || this.formulario.controls.porcentaje.touched)) {
      return null;
    }
    return traducir(CLAVE_DE_ERROR[motivo]);
  }

  /** Valida y, si está bien, pide confirmar el cambio: la comisión afecta a todos los pagos que se confirmen desde ahora. */
  protected pedirCambio(): void {
    if (this.cambiando()) {
      return;
    }
    this.errorGeneral.set(null);
    this.intentado.set(true);
    if (this.errorLocal() !== null) {
      this.anfitrion.nativeElement.querySelector<HTMLElement>('#comision-porcentaje')?.focus();
      return;
    }
    this.pendiente.set(porcentajeParaEnviar(this.valor() ?? 0));
    this.dialogo().abrir();
  }

  protected cambiar(): void {
    const porcentaje = this.pendiente();
    if (porcentaje === null || this.cambiando()) {
      return;
    }
    this.cambiando.set(true);
    this.admin.cambiarComision(porcentaje).subscribe({
      next: (nueva) => {
        this.cambiando.set(false);
        this.comision.set(nueva);
        this.pendiente.set(null);
        this.formulario.reset({ porcentaje: null });
        this.intentado.set(false);
        this.avisos.mostrar(
          traducir('admin.comision.cambiada', { porcentaje: this.numero.transform(nueva.porcentaje, 2) }),
        );
        // El historial trae ahora un cambio hecho por esta cuenta: se vuelve a leer para tener el cambio completo.
        this.recarga.next();
      },
      error: (fallo: unknown) => {
        this.cambiando.set(false);
        const problema = aErrorApi(fallo);
        if (problema.codigo === 'DATO_INVALIDO' && problema.campo === 'porcentaje') {
          this.errorDeApi.set(() => problema.detalleCampo ?? problema.mensaje);
          this.anfitrion.nativeElement.querySelector<HTMLElement>('#comision-porcentaje')?.focus();
        } else {
          this.errorGeneral.set(() => problema.mensaje);
        }
      },
    });
  }
}
