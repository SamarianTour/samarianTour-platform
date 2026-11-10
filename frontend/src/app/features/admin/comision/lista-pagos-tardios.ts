import { DOCUMENT } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { BehaviorSubject, catchError, combineLatest, map, of, switchMap, tap } from 'rxjs';
import { AdminService } from '../../../core/api/admin.service';
import { PagoTardio } from '../../../core/api/modelos-nucleo';
import { reconsultas$ } from '../../../core/api/reconsulta';
import { AvisosService } from '../../../core/avisos/avisos.service';
import { ErrorApi } from '../../../core/errores/error-api';
import { aErrorApi } from '../../../core/errores/errores.interceptor';
import { TPipe } from '../../../core/idioma/t.pipe';
import { traducir } from '../../../core/idioma/traducir';
import { SesionService } from '../../../core/sesion/sesion.service';
import { FechaPipe } from '../../../shared/fecha.pipe';
import { Icono } from '../../../shared/icono/icono';
import { PrecioPipe } from '../../../shared/precio.pipe';
import {
  EXPLICACION_MOTIVO_PAGO,
  ETIQUETA_MOTIVO_PAGO,
  codigoDeSolicitud,
  ordenarPagosTardios,
  totalPorDevolver,
} from '../pagos-tardios';

type Resultado = { pagos: PagoTardio[] } | { error: ErrorApi };

/**
 * Pagos tardíos (RF-49, RF-50): pagos que la pasarela aprobó pero no pagaron una solicitud, porque llegaron con ella vencida,
 * cerrada o pagada, o por otro monto. La solicitud no cambia y el sistema no devuelve el dinero: el administrador lo reembolsa a mano,
 * desde el panel de la pasarela, con el id de la transacción. Por eso cada pago deja copiarlo. Solo se lee; no hay acciones sobre la
 * lista.
 */
@Component({
  selector: 'app-lista-pagos-tardios',
  imports: [TPipe, FechaPipe, PrecioPipe, Icono],
  templateUrl: './lista-pagos-tardios.html',
  styleUrl: './lista-pagos-tardios.css',
})
export class ListaPagosTardios {
  private readonly admin = inject(AdminService);
  private readonly sesion = inject(SesionService);
  private readonly avisos = inject(AvisosService);
  private readonly documento = inject(DOCUMENT);
  /** Solo cambia si entra otra cuenta: editar los datos personales no vuelve a pedir los pagos. */
  private readonly idDeLaCuenta = computed(() => this.sesion.cuenta()?.id);
  private readonly recarga = new BehaviorSubject<void>(undefined);

  protected readonly etiquetaMotivo = ETIQUETA_MOTIVO_PAGO;
  protected readonly explicacion = EXPLICACION_MOTIVO_PAGO;
  protected readonly codigo = codigoDeSolicitud;

  protected readonly cargando = signal(true);
  protected readonly pagos = signal<PagoTardio[] | null>(null);
  protected readonly error = signal<ErrorApi | null>(null);

  protected readonly ordenados = computed(() => ordenarPagosTardios(this.pagos() ?? []));
  protected readonly total = computed(() => totalPorDevolver(this.pagos() ?? []));

  constructor() {
    combineLatest([toObservable(this.idDeLaCuenta), this.recarga])
      .pipe(
        tap(() => this.cargando.set(true)),
        switchMap(() =>
          this.admin.pagosTardios().pipe(
            map((pagos): Resultado => ({ pagos })),
            catchError((fallo: unknown) => of<Resultado>({ error: aErrorApi(fallo) })),
          ),
        ),
        takeUntilDestroyed(),
      )
      .subscribe((resultado) => {
        this.cargando.set(false);
        // Si la consulta falla se conserva lo que ya se veía y se avisa del error.
        if ('pagos' in resultado) {
          this.pagos.set(resultado.pagos);
        }
        this.error.set('error' in resultado ? resultado.error : null);
      });

    reconsultas$()
      .pipe(takeUntilDestroyed())
      .subscribe(() => this.recarga.next());
  }

  /** Copia el id de la transacción para buscarla en el panel de la pasarela. Si el navegador no deja copiar, lo dice. */
  protected copiar(transaccionId: string): void {
    const portapapeles = this.documento.defaultView?.navigator?.clipboard;
    if (!portapapeles) {
      this.avisos.mostrar(traducir('admin.pagosTardios.noCopiado'));
      return;
    }
    portapapeles.writeText(transaccionId).then(
      () => this.avisos.mostrar(traducir('admin.pagosTardios.copiado')),
      () => this.avisos.mostrar(traducir('admin.pagosTardios.noCopiado')),
    );
  }
}
