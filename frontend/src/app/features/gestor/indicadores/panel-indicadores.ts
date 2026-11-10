import { Component, ElementRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable, toSignal } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { map } from 'rxjs';
import { TPipe } from '../../../core/idioma/t.pipe';
import type { ClaveTexto } from '../../../core/idioma/textos';
import { traducir } from '../../../core/idioma/traducir';
import { FechaPipe } from '../../../shared/fecha.pipe';
import { Icono } from '../../../shared/icono/icono';
import {
  ATAJOS,
  Atajo,
  MotivoPeriodo,
  atajoActivo,
  diasDelPeriodo,
  hoyEnColombia,
  parametrosDe,
  periodoDeAtajo,
  periodoDeParametros,
  validarPeriodo,
} from '../periodo';

const CLAVE_DE_ATAJO: Readonly<Record<Atajo, ClaveTexto>> = {
  ultimos7: 'gestor.periodo.atajo.ultimos7',
  ultimos30: 'gestor.periodo.atajo.ultimos30',
  ultimos90: 'gestor.periodo.atajo.ultimos90',
  esteMes: 'gestor.periodo.atajo.esteMes',
  mesAnterior: 'gestor.periodo.atajo.mesAnterior',
};

const CLAVE_DE_ERROR: Readonly<Record<MotivoPeriodo, ClaveTexto>> = {
  desdeRequerida: 'gestor.periodo.error.desdeRequerida',
  hastaRequerida: 'gestor.periodo.error.hastaRequerida',
  desdeMayor: 'gestor.periodo.error.desdeMayor',
  hastaFutura: 'gestor.periodo.error.hastaFutura',
  demasiadoLargo: 'gestor.periodo.error.demasiadoLargo',
};

/** Qué campo recibe el foco según el motivo del error. */
const CAMPO_DEL_ERROR: Readonly<Record<MotivoPeriodo, 'desde' | 'hasta'>> = {
  desdeRequerida: 'desde',
  hastaRequerida: 'hasta',
  desdeMayor: 'desde',
  hastaFutura: 'hasta',
  demasiadoLargo: 'desde',
};

/** Una vista del panel: su dirección y el nombre que lleva su pestaña (es también el título de la página). */
interface Vista {
  readonly ruta: string;
  readonly clave: ClaveTexto;
}

const VISTAS: readonly Vista[] = [
  { ruta: '/gestor', clave: 'app.titulo.demanda' },
  { ruta: '/gestor/embudo', clave: 'app.titulo.embudo' },
  { ruta: '/gestor/distribucion', clave: 'app.titulo.distribucionPrestador' },
];

/**
 * P09.1 Panel de indicadores (HU-GES-01, RF-85 a RF-89). Reúne lo que comparten sus tres vistas: el encabezado, el filtro del
 * periodo, las pestañas y la nota de privacidad. El periodo viaja en la dirección (`?desde=…&hasta=…`), así se puede compartir y
 * sobrevive al cambiar de pestaña; sin él se consultan los últimos 30 días, en hora de Colombia, como hace el núcleo. El núcleo
 * no ofrece más filtros que el periodo.
 */
@Component({
  selector: 'app-panel-indicadores',
  imports: [ReactiveFormsModule, RouterLink, RouterLinkActive, RouterOutlet, Icono, TPipe, FechaPipe],
  templateUrl: './panel-indicadores.html',
  styleUrl: './panel-indicadores.css',
})
export class PanelIndicadores {
  private readonly ruta = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly anfitrion = inject<ElementRef<HTMLElement>>(ElementRef);

  /** Hoy en hora de Colombia, calculado al abrir la pantalla. */
  protected readonly hoy = signal(hoyEnColombia());
  protected readonly atajos = ATAJOS;
  protected readonly claveDeAtajo = CLAVE_DE_ATAJO;
  protected readonly vistas = VISTAS;

  /** El periodo que dice la dirección, o el de por defecto. */
  protected readonly periodo = toSignal(
    this.ruta.queryParamMap.pipe(
      map((parametros) => periodoDeParametros(parametros.get('desde'), parametros.get('hasta'), this.hoy())),
    ),
    { requireSync: true },
  );
  protected readonly dias = computed(() => diasDelPeriodo(this.periodo()));
  protected readonly atajo = computed(() => atajoActivo(this.periodo(), this.hoy()));

  protected readonly formulario = new FormGroup({
    desde: new FormControl('', { nonNullable: true }),
    hasta: new FormControl('', { nonNullable: true }),
  });
  private readonly motivo = signal<MotivoPeriodo | null>(null);
  /** El texto del error se arma al leerlo, así sigue el idioma. */
  protected readonly mensajeDeError = computed(() => {
    const motivo = this.motivo();
    return motivo ? traducir(CLAVE_DE_ERROR[motivo]) : null;
  });

  constructor() {
    // Los campos siempre muestran el periodo que se está consultando, también al elegir un atajo o al volver atrás.
    toObservable(this.periodo)
      .pipe(takeUntilDestroyed())
      .subscribe((periodo) => {
        this.formulario.reset({ desde: periodo.desde, hasta: periodo.hasta });
        this.motivo.set(null);
      });
  }

  protected elegirAtajo(atajo: Atajo): void {
    this.consultar(parametrosDe(periodoDeAtajo(atajo, this.hoy())));
  }

  protected aplicar(): void {
    const { desde, hasta } = this.formulario.getRawValue();
    const motivo = validarPeriodo(desde, hasta, this.hoy());
    this.motivo.set(motivo);
    if (motivo) {
      this.anfitrion.nativeElement.querySelector<HTMLElement>(`#periodo-${CAMPO_DEL_ERROR[motivo]}`)?.focus();
      return;
    }
    this.consultar({ desde, hasta });
  }

  /** Cambia el periodo de la dirección sin salir de la vista en que se está. */
  private consultar(parametros: { desde: string; hasta: string }): void {
    void this.router.navigate([], { queryParams: parametros, queryParamsHandling: 'merge' });
  }
}
