import { Component, computed, inject, signal, viewChild } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { BehaviorSubject, catchError, combineLatest, forkJoin, map, of, switchMap, tap } from 'rxjs';
import { AdminService } from '../../../core/api/admin.service';
import { CuentaInterna, PrestadorAdmin, SincronizacionRnt } from '../../../core/api/modelos-nucleo';
import { reconsultas$ } from '../../../core/api/reconsulta';
import { AvisosService } from '../../../core/avisos/avisos.service';
import { ErrorApi } from '../../../core/errores/error-api';
import { aErrorApi } from '../../../core/errores/errores.interceptor';
import { TPipe } from '../../../core/idioma/t.pipe';
import { TextoVivo } from '../../../core/idioma/texto-vivo';
import { traducir } from '../../../core/idioma/traducir';
import { SesionService } from '../../../core/sesion/sesion.service';
import { DialogoConfirmacion } from '../../../shared/dialogo-confirmacion/dialogo-confirmacion';
import { TIPO_PRESTADOR } from '../../../shared/etiquetas';
import { FechaPipe } from '../../../shared/fecha.pipe';
import { Icono } from '../../../shared/icono/icono';
import { codigoDelPrestador } from '../alertas-sesgo';
import {
  CLASE_DE_ESTADO_RNT,
  ETIQUETA_ESTADO_RNT,
  FiltroPrestadores,
  datosDelOperador,
  etiquetaDeOperador,
  filtroDePrestadores,
  operadoresAsignables,
  puedeAsignar,
  resumenDeSincronizacion,
} from '../prestadores';

type Cargado<T> = { datos: T } | { error: ErrorApi };

/** Los errores que dicen que lo que la pantalla muestra ya no es cierto: se vuelve a leer todo (ADR-05). */
const CODIGOS_DE_DATOS_CAMBIADOS = new Set([
  'OPERADOR_NO_VALIDO',
  'CONFLICTO_CONCURRENCIA',
  'PRESTADOR_NO_REGISTRADO',
]);

/**
 * P05.1 Prestadores y RNT (RF-76 a RF-78, RF-81). Lista a los prestadores con su estado frente al RNT y su operador, deja
 * asignarles uno (con el **nombre** del operador, nunca su identificador) y sincronizar con el RNT cuando el administrador lo pide.
 *
 * La sincronización puede tardar: el núcleo responde cuando termina. Mientras responde, el botón queda deshabilitado con
 * "Sincronizando…", y no hay ningún tiempo máximo que la corte. La petición no se cancela al salir de la pantalla: el resultado
 * llega igual y se avisa con un mensaje breve.
 */
@Component({
  selector: 'app-prestadores-rnt',
  imports: [RouterLink, Icono, TPipe, FechaPipe, DialogoConfirmacion],
  templateUrl: './prestadores-rnt.html',
  styleUrl: './prestadores-rnt.css',
})
export class PrestadoresRnt {
  private readonly ruta = inject(ActivatedRoute);
  private readonly admin = inject(AdminService);
  private readonly sesion = inject(SesionService);
  private readonly avisos = inject(AvisosService);
  /** Solo cambia si entra otra cuenta: editar los datos personales no vuelve a pedir la lista. */
  private readonly idDeLaCuenta = computed(() => this.sesion.cuenta()?.id);
  private readonly recarga = new BehaviorSubject<void>(undefined);
  private readonly dialogo = viewChild.required(DialogoConfirmacion);

  protected readonly etiquetaEstado = ETIQUETA_ESTADO_RNT;
  protected readonly claseDeEstado = CLASE_DE_ESTADO_RNT;
  protected readonly etiquetaTipo = TIPO_PRESTADOR;
  protected readonly codigo = codigoDelPrestador;

  protected readonly filtro = signal<FiltroPrestadores>('todos');
  protected readonly cargando = signal(true);
  protected readonly prestadores = signal<PrestadorAdmin[] | null>(null);
  private readonly cuentas = signal<readonly CuentaInterna[]>([]);
  protected readonly error = signal<ErrorApi | null>(null);
  /** No se pudieron leer las cuentas: se ve la lista, pero no se puede asignar. */
  protected readonly errorOperadores = signal<ErrorApi | null>(null);

  protected readonly asignables = computed(() => operadoresAsignables(this.cuentas()));

  /** El operador elegido en cada lista, por prestador, mientras no se envíe. */
  private readonly elegidos = signal<Readonly<Record<string, string>>>({});
  /** El prestador cuya asignación se está enviando. */
  protected readonly asignando = signal<string | null>(null);
  private readonly erroresDeFila = signal<Readonly<Record<string, TextoVivo>>>({});

  protected readonly filas = computed(() =>
    (this.prestadores() ?? []).map((prestador) => {
      const elegido = this.elegidos()[prestador.id] ?? prestador.operadorId ?? '';
      const actual = datosDelOperador(prestador.operadorId, this.cuentas());
      return {
        prestador,
        nombre: prestador.razonSocial?.trim() || null,
        actual,
        // El operador actual puede estar desactivado o no figurar entre los asignables: se muestra aparte, sin poder elegirlo.
        actualNoAsignable:
          !!prestador.operadorId &&
          !this.asignables().some((cuenta) => cuenta.id === prestador.operadorId),
        elegido,
        puede: puedeAsignar(elegido, prestador, this.asignables()),
        error: (this.erroresDeFila()[prestador.id] as TextoVivo | undefined) ?? null,
      };
    }),
  );

  protected readonly sincronizando = signal(false);
  protected readonly resultadoSync = signal<SincronizacionRnt | null>(null);
  protected readonly errorSync = signal<TextoVivo | null>(null);
  protected readonly resumenSync = computed(() => {
    const resultado = this.resultadoSync();
    return resultado ? resumenDeSincronizacion(resultado) : null;
  });

  constructor() {
    const filtro$ = this.ruta.queryParamMap.pipe(
      map((params) => filtroDePrestadores(params.get('sinOperador'))),
    );

    combineLatest([filtro$, toObservable(this.idDeLaCuenta), this.recarga])
      .pipe(
        tap(([filtro]) => {
          this.filtro.set(filtro);
          this.cargando.set(true);
        }),
        switchMap(([filtro]) =>
          forkJoin({
            prestadores: this.admin.prestadores(filtro === 'sin-operador').pipe(
              map((datos): Cargado<PrestadorAdmin[]> => ({ datos })),
              catchError((fallo: unknown) => of<Cargado<PrestadorAdmin[]>>({ error: aErrorApi(fallo) })),
            ),
            cuentas: this.admin.cuentasInternas().pipe(
              map((datos): Cargado<CuentaInterna[]> => ({ datos })),
              catchError((fallo: unknown) => of<Cargado<CuentaInterna[]>>({ error: aErrorApi(fallo) })),
            ),
          }),
        ),
        takeUntilDestroyed(),
      )
      .subscribe(({ prestadores, cuentas }) => {
        this.cargando.set(false);
        // Si la consulta falla se conserva lo que ya se veía y se avisa del error.
        if ('datos' in prestadores) {
          this.prestadores.set(prestadores.datos);
          this.elegidos.set({});
        }
        this.error.set('error' in prestadores ? prestadores.error : null);
        if ('datos' in cuentas) {
          this.cuentas.set(cuentas.datos);
        }
        this.errorOperadores.set('error' in cuentas ? cuentas.error : null);
      });

    reconsultas$()
      .pipe(takeUntilDestroyed())
      .subscribe(() => this.recarga.next());
  }

  // --- Asignar operador ---

  protected etiquetaOperador(cuenta: CuentaInterna): string {
    return etiquetaDeOperador(cuenta, this.asignables());
  }

  protected elegir(prestadorId: string, evento: Event): void {
    const valor = (evento.target as HTMLSelectElement).value;
    this.elegidos.update((elegidos) => ({ ...elegidos, [prestadorId]: valor }));
    this.erroresDeFila.update((errores) => sinClave(errores, prestadorId));
  }

  protected asignar(prestador: PrestadorAdmin): void {
    const operadorId = this.elegidos()[prestador.id];
    if (this.asignando() || !puedeAsignar(operadorId, prestador, this.asignables())) {
      return;
    }
    this.asignando.set(prestador.id);
    this.erroresDeFila.update((errores) => sinClave(errores, prestador.id));
    this.admin.asignarOperador(prestador.id, operadorId).subscribe({
      next: (actualizado) => {
        this.asignando.set(null);
        // Con el filtro "sin operador", el que ya tiene uno sale de la lista; si no, se actualiza su fila.
        this.prestadores.update((lista) =>
          lista === null
            ? lista
            : this.filtro() === 'sin-operador'
              ? lista.filter((otro) => otro.id !== prestador.id)
              : lista.map((otro) => (otro.id === prestador.id ? actualizado : otro)),
        );
        this.elegidos.update((elegidos) => sinClave(elegidos, prestador.id));
        this.avisos.mostrar(
          traducir('admin.prestadores.asignado', {
            operador: datosDelOperador(operadorId, this.cuentas())?.nombre ?? '',
            prestador:
              prestador.razonSocial?.trim() ||
              traducir('admin.prestadores.rnt', { numero: prestador.numeroRnt }),
          }),
        );
      },
      error: (fallo: unknown) => {
        this.asignando.set(null);
        const problema = aErrorApi(fallo);
        this.erroresDeFila.update((errores) => ({ ...errores, [prestador.id]: () => problema.mensaje }));
        if (problema.codigo && CODIGOS_DE_DATOS_CAMBIADOS.has(problema.codigo)) {
          // El operador o el prestador cambiaron por su lado: se vuelve a leer todo. El aviso de esta fila se conserva hasta
          // que el administrador cambie su elección.
          this.recarga.next();
        }
      },
    });
  }

  // --- Sincronizar con el RNT ---

  protected pedirSincronizacion(): void {
    if (!this.sincronizando()) {
      this.dialogo().abrir();
    }
  }

  protected sincronizar(): void {
    if (this.sincronizando()) {
      return;
    }
    this.sincronizando.set(true);
    this.errorSync.set(null);
    this.resultadoSync.set(null);
    // Sin `takeUntilDestroyed` ni tiempo máximo: la sincronización sigue en el servidor aunque se salga de la pantalla, y su
    // resultado se avisa igual.
    this.admin.sincronizarRnt().subscribe({
      next: (resultado) => {
        this.sincronizando.set(false);
        this.resultadoSync.set(resultado);
        this.avisos.mostrar(
          traducir('admin.rnt.sync.terminada', {
            revisados: resultado.revisados,
            retirados: resultado.retirados,
            restituidos: resultado.restituidos,
          }),
        );
        // Los estados del RNT y los operadores pueden haber cambiado.
        this.recarga.next();
      },
      error: (fallo: unknown) => {
        this.sincronizando.set(false);
        const problema = aErrorApi(fallo);
        this.errorSync.set(() => problema.mensaje);
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
