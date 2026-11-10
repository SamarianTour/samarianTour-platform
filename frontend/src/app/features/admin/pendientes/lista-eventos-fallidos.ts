import { Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { BehaviorSubject, catchError, combineLatest, map, of, switchMap, tap } from 'rxjs';
import { AdminService } from '../../../core/api/admin.service';
import { EventoPagoFallido } from '../../../core/api/modelos-nucleo';
import { reconsultas$ } from '../../../core/api/reconsulta';
import { AvisosService } from '../../../core/avisos/avisos.service';
import { ErrorApi } from '../../../core/errores/error-api';
import { aErrorApi } from '../../../core/errores/errores.interceptor';
import { TPipe } from '../../../core/idioma/t.pipe';
import { TextoVivo } from '../../../core/idioma/texto-vivo';
import { traducir } from '../../../core/idioma/traducir';
import { SesionService } from '../../../core/sesion/sesion.service';
import { FechaPipe } from '../../../shared/fecha.pipe';
import { Icono } from '../../../shared/icono/icono';
import { PrecioPipe } from '../../../shared/precio.pipe';
import {
  etiquetaDeEstadoDePago,
  ordenarEventos,
  primeraLinea,
  tieneMasQueLaPrimeraLinea,
} from '../pendientes';

type Resultado = { eventos: EventoPagoFallido[] } | { error: ErrorApi };

/** Los errores que dicen que el evento ya no está como lo vio el administrador: se vuelve a leer la lista. */
const CODIGOS_DE_EVENTO_CAMBIADO = new Set(['EVENTO_NO_ENCONTRADO', 'EVENTO_NO_FALLIDO']);

/**
 * Eventos de pago fallidos (DN-21, DN-44). Un evento de la pasarela que falla por un error inesperado se reintenta con espera creciente;
 * al quinto fallo queda FALLIDO, sale de la cola y se avisa al administrador por WhatsApp. Mientras no se procese, el pago no se confirma.
 * Aquí se ve el último error y, una vez corregida la causa, se devuelve el evento a la cola: el núcleo lo procesa en los siguientes
 * 5 segundos. El evento nunca trae datos del turista.
 */
@Component({
  selector: 'app-lista-eventos-fallidos',
  imports: [TPipe, FechaPipe, PrecioPipe, Icono],
  templateUrl: './lista-eventos-fallidos.html',
  styleUrl: './pendientes-lista.css',
})
export class ListaEventosFallidos {
  private readonly admin = inject(AdminService);
  private readonly sesion = inject(SesionService);
  private readonly avisos = inject(AvisosService);
  /** Solo cambia si entra otra cuenta: editar los datos personales no vuelve a pedir la lista. */
  private readonly idDeLaCuenta = computed(() => this.sesion.cuenta()?.id);
  private readonly recarga = new BehaviorSubject<void>(undefined);

  protected readonly etiquetaEstado = etiquetaDeEstadoDePago;
  protected readonly primeraLinea = primeraLinea;
  protected readonly hayMas = tieneMasQueLaPrimeraLinea;

  protected readonly cargando = signal(true);
  protected readonly lista = signal<EventoPagoFallido[] | null>(null);
  protected readonly error = signal<ErrorApi | null>(null);
  /** El evento que se está devolviendo a la cola. */
  protected readonly reintentando = signal<string | null>(null);
  private readonly erroresDeFila = signal<Readonly<Record<string, TextoVivo>>>({});

  protected readonly filas = computed(() =>
    ordenarEventos(this.lista() ?? []).map((evento) => ({
      evento,
      error: (this.erroresDeFila()[evento.id] as TextoVivo | undefined) ?? null,
    })),
  );

  constructor() {
    combineLatest([toObservable(this.idDeLaCuenta), this.recarga])
      .pipe(
        tap(() => this.cargando.set(true)),
        switchMap(() =>
          this.admin.eventosPagoFallidos().pipe(
            map((eventos): Resultado => ({ eventos })),
            catchError((fallo: unknown) => of<Resultado>({ error: aErrorApi(fallo) })),
          ),
        ),
        takeUntilDestroyed(),
      )
      .subscribe((resultado) => {
        this.cargando.set(false);
        // Si la consulta falla se conserva lo que ya se veía y se avisa del error.
        if ('eventos' in resultado) {
          this.lista.set(resultado.eventos);
        }
        this.error.set('error' in resultado ? resultado.error : null);
      });

    reconsultas$()
      .pipe(takeUntilDestroyed())
      .subscribe(() => this.recarga.next());
  }

  protected reintentar(evento: EventoPagoFallido): void {
    if (this.reintentando() !== null) {
      return;
    }
    this.reintentando.set(evento.id);
    this.erroresDeFila.update((errores) => sinClave(errores, evento.id));
    this.admin.reintentarEventoFallido(evento.id).subscribe({
      next: () => {
        this.reintentando.set(null);
        // Vuelve a la cola: ya no está fallido y sale de la lista. Si vuelve a fallar cinco veces, reaparecerá al volver a leerla.
        this.lista.update((lista) => lista?.filter((otro) => otro.id !== evento.id) ?? lista);
        this.avisos.mostrar(traducir('admin.pendientes.eventos.reintentado'));
      },
      error: (fallo: unknown) => {
        this.reintentando.set(null);
        const problema = aErrorApi(fallo);
        this.erroresDeFila.update((errores) => ({ ...errores, [evento.id]: () => problema.mensaje }));
        if (problema.codigo && CODIGOS_DE_EVENTO_CAMBIADO.has(problema.codigo)) {
          this.recarga.next();
        }
      },
    });
  }
}

/** Una copia del registro sin esa clave. */
function sinClave<T>(registro: Readonly<Record<string, T>>, clave: string): Record<string, T> {
  const copia = { ...registro };
  delete copia[clave];
  return copia;
}
