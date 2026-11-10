import { Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { BehaviorSubject, catchError, combineLatest, map, of, switchMap, tap } from 'rxjs';
import { AdminService } from '../../../core/api/admin.service';
import { AvisoBandeja } from '../../../core/api/modelos-nucleo';
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
import {
  ETIQUETA_DESTINO_AVISO,
  etiquetaDeTipoDeAviso,
  ordenarAvisos,
  primeraLinea,
  puedeReenviar,
  tieneMasQueLaPrimeraLinea,
} from '../pendientes';

type Resultado = { avisos: AvisoBandeja[] } | { error: ErrorApi };

/** Los errores que dicen que el aviso ya no está como lo vio el administrador: se vuelve a leer la lista. */
const CODIGOS_DE_AVISO_CAMBIADO = new Set(['AVISO_NO_ENCONTRADO', 'AVISO_NO_RECHAZADO']);

/**
 * Avisos al catálogo rechazados (DN-06, DN-44). El núcleo le avisa al catálogo por una bandeja de salida; cuando el catálogo responde con
 * un error 4xx, el aviso queda RECHAZADO y no se reintenta solo, porque fallaría igual. Aquí se ve lo que respondió el catálogo y, una
 * vez corregida la causa, se reenvía: queda pendiente y la bandeja lo envía en los siguientes 15 segundos.
 */
@Component({
  selector: 'app-lista-avisos-rechazados',
  imports: [TPipe, FechaPipe, Icono],
  templateUrl: './lista-avisos-rechazados.html',
  styleUrl: './pendientes-lista.css',
})
export class ListaAvisosRechazados {
  private readonly admin = inject(AdminService);
  private readonly sesion = inject(SesionService);
  private readonly avisos = inject(AvisosService);
  /** Solo cambia si entra otra cuenta: editar los datos personales no vuelve a pedir la lista. */
  private readonly idDeLaCuenta = computed(() => this.sesion.cuenta()?.id);
  private readonly recarga = new BehaviorSubject<void>(undefined);

  protected readonly etiquetaDestino = ETIQUETA_DESTINO_AVISO;
  protected readonly etiquetaTipo = etiquetaDeTipoDeAviso;
  protected readonly primeraLinea = primeraLinea;
  protected readonly hayMas = tieneMasQueLaPrimeraLinea;

  protected readonly cargando = signal(true);
  protected readonly lista = signal<AvisoBandeja[] | null>(null);
  protected readonly error = signal<ErrorApi | null>(null);
  /** El aviso que se está reenviando. */
  protected readonly reenviando = signal<string | null>(null);
  private readonly erroresDeFila = signal<Readonly<Record<string, TextoVivo>>>({});

  protected readonly filas = computed(() =>
    ordenarAvisos(this.lista() ?? []).map((aviso) => ({
      aviso,
      puede: puedeReenviar(aviso),
      error: (this.erroresDeFila()[aviso.idAviso] as TextoVivo | undefined) ?? null,
    })),
  );

  constructor() {
    combineLatest([toObservable(this.idDeLaCuenta), this.recarga])
      .pipe(
        tap(() => this.cargando.set(true)),
        switchMap(() =>
          this.admin.avisosRechazados().pipe(
            map((avisos): Resultado => ({ avisos })),
            catchError((fallo: unknown) => of<Resultado>({ error: aErrorApi(fallo) })),
          ),
        ),
        takeUntilDestroyed(),
      )
      .subscribe((resultado) => {
        this.cargando.set(false);
        // Si la consulta falla se conserva lo que ya se veía y se avisa del error.
        if ('avisos' in resultado) {
          this.lista.set(resultado.avisos);
        }
        this.error.set('error' in resultado ? resultado.error : null);
      });

    reconsultas$()
      .pipe(takeUntilDestroyed())
      .subscribe(() => this.recarga.next());
  }

  protected reenviar(aviso: AvisoBandeja): void {
    if (this.reenviando() !== null || !puedeReenviar(aviso)) {
      return;
    }
    this.reenviando.set(aviso.idAviso);
    this.erroresDeFila.update((errores) => sinClave(errores, aviso.idAviso));
    this.admin.reenviarAviso(aviso.idAviso).subscribe({
      next: () => {
        this.reenviando.set(null);
        // Ya no está rechazado: sale de la lista. Si el catálogo lo vuelve a rechazar, reaparecerá al volver a leerla.
        this.lista.update((lista) => lista?.filter((otro) => otro.idAviso !== aviso.idAviso) ?? lista);
        this.avisos.mostrar(traducir('admin.pendientes.avisos.reenviado'));
      },
      error: (fallo: unknown) => {
        this.reenviando.set(null);
        const problema = aErrorApi(fallo);
        this.erroresDeFila.update((errores) => ({ ...errores, [aviso.idAviso]: () => problema.mensaje }));
        if (problema.codigo && CODIGOS_DE_AVISO_CAMBIADO.has(problema.codigo)) {
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
