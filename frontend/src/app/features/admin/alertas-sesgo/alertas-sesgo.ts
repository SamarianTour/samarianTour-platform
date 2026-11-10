import { Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { BehaviorSubject, catchError, combineLatest, map, of, switchMap, tap } from 'rxjs';
import { AdminService } from '../../../core/api/admin.service';
import { AlertaSesgo, PrestadorAdmin } from '../../../core/api/modelos-nucleo';
import { reconsultas$ } from '../../../core/api/reconsulta';
import { ErrorApi } from '../../../core/errores/error-api';
import { aErrorApi } from '../../../core/errores/errores.interceptor';
import { TPipe } from '../../../core/idioma/t.pipe';
import { SesionService } from '../../../core/sesion/sesion.service';
import { TIPO_PRESTADOR } from '../../../shared/etiquetas';
import { FechaPipe } from '../../../shared/fecha.pipe';
import { Icono } from '../../../shared/icono/icono';
import { NumeroPipe } from '../../../shared/numero.pipe';
import { codigoDelPrestador, nombreDelPrestador, ordenarPorIndice } from '../alertas-sesgo';

type Resultado = { alertas: AlertaSesgo[] } | { error: ErrorApi };

/**
 * Alertas de sesgo (RET-02): los prestadores que la recomendación muestra más de lo que les corresponde, de mayor a menor
 * índice. El núcleo las abre cada día y las cierra solas cuando el índice baja, así que aquí solo se leen. La alerta trae el
 * identificador del prestador y no su nombre, que sale de la lista de prestadores [SUPUESTO]; si esa lista no se puede leer,
 * se muestra el código. Vuelve a consultar al regresar a la pestaña y al reconectarse.
 */
@Component({
  selector: 'app-alertas-sesgo',
  imports: [TPipe, FechaPipe, NumeroPipe, Icono],
  templateUrl: './alertas-sesgo.html',
  styleUrl: './alertas-sesgo.css',
})
export class AlertasSesgo {
  private readonly servicio = inject(AdminService);
  private readonly sesion = inject(SesionService);
  /** Solo cambia si entra otra cuenta: editar los datos personales no vuelve a pedir las alertas. */
  private readonly idDeLaCuenta = computed(() => this.sesion.cuenta()?.id);
  private readonly recarga = new BehaviorSubject<void>(undefined);

  protected readonly etiquetaTipo = TIPO_PRESTADOR;
  protected readonly codigo = codigoDelPrestador;

  protected readonly cargando = signal(true);
  protected readonly alertas = signal<AlertaSesgo[] | null>(null);
  protected readonly error = signal<ErrorApi | null>(null);
  private readonly prestadores = signal<readonly PrestadorAdmin[]>([]);

  protected readonly filas = computed(() =>
    ordenarPorIndice(this.alertas() ?? []).map((alerta) => ({
      alerta,
      nombre: nombreDelPrestador(alerta.prestadorId, this.prestadores()),
      tipo: this.prestadores().find((prestador) => prestador.id === alerta.prestadorId)?.tipo ?? null,
    })),
  );

  constructor() {
    combineLatest([toObservable(this.idDeLaCuenta), this.recarga])
      .pipe(
        tap(() => this.cargando.set(true)),
        switchMap(() =>
          this.servicio.alertasSesgo().pipe(
            map((alertas): Resultado => ({ alertas })),
            catchError((fallo: unknown) => of<Resultado>({ error: aErrorApi(fallo) })),
          ),
        ),
        takeUntilDestroyed(),
      )
      .subscribe((resultado) => {
        this.cargando.set(false);
        // Si la consulta falla se conserva lo que ya se veía y se avisa del error.
        if ('alertas' in resultado) {
          this.alertas.set(resultado.alertas);
        }
        this.error.set('error' in resultado ? resultado.error : null);
      });

    // Los nombres son un apoyo: si la lista de prestadores falla, las alertas se siguen viendo con el código.
    combineLatest([toObservable(this.idDeLaCuenta), this.recarga])
      .pipe(
        switchMap(() =>
          this.servicio.prestadores().pipe(catchError(() => of<PrestadorAdmin[]>([]))),
        ),
        takeUntilDestroyed(),
      )
      .subscribe((prestadores) => this.prestadores.set(prestadores));

    reconsultas$()
      .pipe(takeUntilDestroyed())
      .subscribe(() => this.recarga.next());
  }
}
