import { Component, ElementRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable, toSignal } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { BehaviorSubject, catchError, combineLatest, map, of, startWith, switchMap, tap } from 'rxjs';
import { PrestadorPropio } from '../../../core/api/modelos-nucleo';
import { PrestadorService } from '../../../core/api/prestador.service';
import { AvisosService } from '../../../core/avisos/avisos.service';
import { ErrorApi } from '../../../core/errores/error-api';
import { aErrorApi } from '../../../core/errores/errores.interceptor';
import { TPipe } from '../../../core/idioma/t.pipe';
import { TextoVivo } from '../../../core/idioma/texto-vivo';
import { traducir } from '../../../core/idioma/traducir';
import { SesionService } from '../../../core/sesion/sesion.service';
import { VerificarWhatsapp } from '../../cuentas/mis-datos/verificar-whatsapp';
import { Icono } from '../../../shared/icono/icono';
import {
  CLASE_SITUACION_WHATSAPP,
  CLAVE_SITUACION_WHATSAPP,
  situacionWhatsapp,
} from '../inicio/inicio';
import { numeroParaEnviar, validarWhatsapp } from './whatsapp';

type Cargado = { prestador: PrestadorPropio | null } | { error: ErrorApi };

/**
 * P03.4 WhatsApp de contacto (HU-PRE-04, RF-71). El prestador configura el número por el que le avisamos de una solicitud pagada;
 * nunca es público. Un número nuevo queda sin verificar y el núcleo le envía un código por WhatsApp (vale 30 minutos y admite 3
 * intentos): mientras no lo verifique no recibe esos avisos (RF-57). La verificación es el mismo componente que usa el turista.
 */
@Component({
  selector: 'app-whatsapp-prestador',
  imports: [ReactiveFormsModule, RouterLink, Icono, TPipe, VerificarWhatsapp],
  templateUrl: './whatsapp-prestador.html',
  styleUrl: './whatsapp-prestador.css',
})
export class WhatsappPrestador {
  private readonly servicio = inject(PrestadorService);
  private readonly sesion = inject(SesionService);
  private readonly avisos = inject(AvisosService);
  private readonly anfitrion = inject<ElementRef<HTMLElement>>(ElementRef);
  private readonly recarga = new BehaviorSubject<void>(undefined);
  /** Solo cambia si entra otra cuenta: editar los datos personales no vuelve a pedir el registro. */
  private readonly idDeLaCuenta = computed(() => this.sesion.cuenta()?.id);

  protected readonly claveSituacion = CLAVE_SITUACION_WHATSAPP;
  protected readonly claseSituacion = CLASE_SITUACION_WHATSAPP;

  // --- El registro ---

  protected readonly cargando = signal(true);
  /** `undefined` mientras no se ha leído; `null` si todavía no registró el RNT (sin él no existe el prestador en el núcleo). */
  protected readonly prestador = signal<PrestadorPropio | null | undefined>(undefined);
  protected readonly errorCarga = signal<ErrorApi | null>(null);
  protected readonly situacion = computed(() => {
    const prestador = this.prestador();
    return prestador === undefined ? null : situacionWhatsapp(prestador);
  });

  // --- El número ---

  protected readonly formulario = new FormGroup({ whatsapp: new FormControl('', { nonNullable: true }) });
  private readonly escrito = toSignal(
    this.formulario.controls.whatsapp.valueChanges.pipe(startWith('')),
    { initialValue: '' },
  );
  private readonly intentado = signal(false);
  protected readonly guardando = signal(false);
  protected readonly errorGeneral = signal<TextoVivo | null>(null);
  private readonly errorDeApi = signal<TextoVivo | null>(null);

  /** Cómo se verifica y se pide otro código: las rutas del prestador, no las del turista. */
  protected readonly verificarCon = (codigo: string) => this.servicio.verificarWhatsapp(codigo);
  protected readonly reenviarCon = () => this.servicio.enviarCodigoWhatsapp();

  constructor() {
    combineLatest([toObservable(this.idDeLaCuenta), this.recarga])
      .pipe(
        tap(() => this.cargando.set(true)),
        switchMap(() =>
          this.servicio.registro().pipe(
            map((prestador): Cargado => ({ prestador })),
            catchError((fallo: unknown) => of<Cargado>({ error: aErrorApi(fallo) })),
          ),
        ),
        takeUntilDestroyed(),
      )
      .subscribe((resultado) => {
        this.cargando.set(false);
        if ('error' in resultado) {
          this.errorCarga.set(resultado.error);
          return;
        }
        this.errorCarga.set(null);
        this.prestador.set(resultado.prestador);
        if (!this.formulario.dirty) {
          this.formulario.reset({ whatsapp: resultado.prestador?.whatsapp ?? '' });
        }
      });

    // Al corregir el número desaparece el error que el núcleo había marcado.
    this.formulario.controls.whatsapp.valueChanges
      .pipe(takeUntilDestroyed())
      .subscribe(() => this.errorDeApi.set(null));
  }

  /** El error del campo: primero el del núcleo; si no, el de la validación local una vez tocado o al enviar. */
  protected errorDelCampo(): string | null {
    const deApi = this.errorDeApi();
    if (deApi) {
      return deApi();
    }
    const motivo = validarWhatsapp(this.escrito());
    if (!motivo || !(this.intentado() || this.formulario.controls.whatsapp.touched)) {
      return null;
    }
    return traducir(motivo === 'requerido' ? 'cuentas.error.escribeWhatsapp' : 'cuentas.error.whatsappInvalido');
  }

  protected guardar(): void {
    if (this.guardando()) {
      return;
    }
    this.errorGeneral.set(null);
    this.errorDeApi.set(null);
    this.intentado.set(true);
    const escrito = this.formulario.controls.whatsapp.value;
    if (validarWhatsapp(escrito)) {
      this.enfocar();
      return;
    }
    this.guardando.set(true);
    this.servicio.configurarWhatsapp(numeroParaEnviar(escrito)).subscribe({
      next: (guardado) => {
        this.guardando.set(false);
        this.intentado.set(false);
        this.prestador.set(guardado);
        this.formulario.reset({ whatsapp: guardado.whatsapp ?? '' });
        this.avisos.mostrar(
          traducir(guardado.whatsappVerificado ? 'prestador.whatsapp.guardado' : 'prestador.whatsapp.guardadoSinVerificar'),
        );
      },
      error: (fallo: unknown) => {
        this.guardando.set(false);
        const error = aErrorApi(fallo);
        if (error.campo === 'whatsapp') {
          this.errorDeApi.set(() => error.detalleCampo ?? error.mensaje);
          this.enfocar();
          return;
        }
        this.errorGeneral.set(() => error.mensaje);
        if (error.codigo === 'PRESTADOR_NO_REGISTRADO') {
          this.recarga.next();
        }
      },
    });
  }

  /** El código fue correcto: el núcleo devuelve al prestador con el número verificado. */
  protected alVerificar(resultado: unknown): void {
    this.prestador.set(resultado as PrestadorPropio);
  }

  private enfocar(): void {
    this.anfitrion.nativeElement.querySelector<HTMLElement>('#whatsapp-numero')?.focus();
  }
}
