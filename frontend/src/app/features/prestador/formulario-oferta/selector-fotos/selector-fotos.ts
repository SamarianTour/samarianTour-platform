import {
  Component,
  DestroyRef,
  ElementRef,
  Injector,
  afterNextRender,
  computed,
  inject,
  input,
  model,
  signal,
  viewChild,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { EMPTY, Observable, catchError, concatMap, from, tap } from 'rxjs';
import { Foto } from '../../../../core/api/modelos';
import { OfertasPrestadorService } from '../../../../core/api/ofertas-prestador.service';
import { aErrorApi } from '../../../../core/errores/errores.interceptor';
import { Icono } from '../../../../shared/icono/icono';
import {
  MAXIMO_FOTOS,
  TIPOS_FOTO,
  agregar,
  clasificarArchivos,
  cuposLibres,
  mover,
  quitar,
} from '../fotos-oferta';

/** Un archivo que se está subiendo o cuya subida falló; todavía no es una foto de la oferta. */
interface Pendiente {
  readonly clave: number;
  readonly archivo: File;
  readonly estado: 'subiendo' | 'fallida';
  readonly mensaje?: string;
}

type Accion = 'antes' | 'despues';

/**
 * Fotos de una oferta (DC-47): de 1 a 6, la primera es la portada. Cada foto se sube apenas se elige y entra a la
 * lista con la miniatura que devuelve la API. El orden se cambia con botones, no arrastrando, para que sirva con
 * teclado y en celular. Las fotos van en `fotos` (enlace de dos vías) y `subiendo` avisa si hay subidas en curso.
 */
@Component({
  selector: 'app-selector-fotos',
  imports: [Icono],
  templateUrl: './selector-fotos.html',
  styleUrl: './selector-fotos.css',
})
export class SelectorFotos {
  /** Las fotos de la oferta, en orden. */
  readonly fotos = model<Foto[]>([]);
  /** Verdadero mientras alguna foto se está subiendo. */
  readonly subiendo = model(false);
  /** Mensaje de error del formulario para este campo, por ejemplo que falta al menos una foto. */
  readonly error = input<string | null>(null);

  private readonly servicio = inject(OfertasPrestadorService);
  private readonly destruccion = inject(DestroyRef);
  private readonly injector = inject(Injector);
  private readonly anfitrion = inject<ElementRef<HTMLElement>>(ElementRef);
  private readonly entrada = viewChild.required<ElementRef<HTMLInputElement>>('entrada');
  private readonly botonAgregar = viewChild.required<ElementRef<HTMLButtonElement>>('agregar');
  private contador = 0;

  protected readonly maximo = MAXIMO_FOTOS;
  protected readonly formatos = TIPOS_FOTO.join(',');
  protected readonly pendientes = signal<readonly Pendiente[]>([]);
  /** Un aviso por cada archivo que no se subió al elegir. */
  protected readonly avisos = signal<readonly string[]>([]);
  /** Texto que leen los lectores de pantalla cuando una foto se mueve, se quita o termina de subir. */
  protected readonly anuncio = signal('');
  protected readonly cupos = computed(() =>
    cuposLibres(
      this.fotos().length + this.pendientes().filter((p) => p.estado === 'subiendo').length,
    ),
  );

  protected abrirSelector(): void {
    this.entrada().nativeElement.click();
  }

  protected elegidos(evento: Event): void {
    const entrada = evento.target as HTMLInputElement;
    const archivos = Array.from(entrada.files ?? []);
    // Se vacía para que elegir otra vez el mismo archivo vuelva a disparar el evento.
    entrada.value = '';
    const { aceptados, avisos } = clasificarArchivos(archivos, this.cupos());
    this.avisos.set(avisos);

    const nuevos = aceptados.map((archivo): Pendiente => ({
      clave: ++this.contador,
      archivo,
      estado: 'subiendo',
    }));
    this.cambiarPendientes((actuales) => [...actuales, ...nuevos]);
    // Una por una, para que las fotos entren en el orden en que se eligieron.
    from(nuevos)
      .pipe(
        concatMap((pendiente) => this.subir(pendiente)),
        takeUntilDestroyed(this.destruccion),
      )
      .subscribe();
  }

  protected reintentar(pendiente: Pendiente): void {
    if (this.cupos() === 0) {
      this.avisos.set([
        `No se subió «${pendiente.archivo.name}»: una oferta lleva máximo ${MAXIMO_FOTOS} fotos.`,
      ]);
      return;
    }
    this.avisos.set([]);
    this.cambiarPendientes((actuales) =>
      actuales.map((actual) =>
        actual.clave === pendiente.clave
          ? { ...actual, estado: 'subiendo', mensaje: undefined }
          : actual,
      ),
    );
    this.subir(pendiente).pipe(takeUntilDestroyed(this.destruccion)).subscribe();
  }

  protected descartar(clave: number): void {
    this.cambiarPendientes((actuales) => actuales.filter((actual) => actual.clave !== clave));
  }

  protected moverFoto(foto: Foto, desplazamiento: -1 | 1): void {
    const nueva = mover(this.fotos(), foto.id, desplazamiento);
    this.fotos.set(nueva);
    const posicion = nueva.findIndex((una) => una.id === foto.id) + 1;
    this.anuncio.set(
      `Foto movida a la posición ${posicion} de ${nueva.length}.${posicion === 1 ? ' Ahora es la portada.' : ''}`,
    );

    // Al cambiar de lugar, el navegador puede soltar el foco: se devuelve al mismo botón, o al contrario si ya no se puede usar.
    const preferido: Accion = desplazamiento === -1 ? 'antes' : 'despues';
    const contrario: Accion = preferido === 'antes' ? 'despues' : 'antes';
    afterNextRender(
      () => {
        const boton = this.boton(foto.id, preferido);
        (boton && !boton.disabled ? boton : this.boton(foto.id, contrario))?.focus();
      },
      { injector: this.injector },
    );
  }

  protected quitarFoto(foto: Foto): void {
    const restantes = quitar(this.fotos(), foto.id);
    this.fotos.set(restantes);
    this.anuncio.set(
      restantes.length === 0
        ? 'Foto quitada. La oferta no tiene fotos.'
        : `Foto quitada. La oferta tiene ${restantes.length} de ${MAXIMO_FOTOS} fotos.`,
    );
    afterNextRender(
      () => {
        // La foto quitada ya no existe: el foco vuelve al botón de agregar.
        this.botonAgregar().nativeElement.focus();
      },
      { injector: this.injector },
    );
  }

  /**
   * [SUPUESTO] La API no guarda una descripción de cada foto (DC-47), así que el texto alternativo es genérico:
   * la posición de la foto y si es la portada.
   */
  protected textoAlternativo(indice: number): string {
    const total = this.fotos().length;
    return `Foto ${indice + 1} de ${total} de la oferta${indice === 0 ? ' (portada)' : ''}`;
  }

  /** Sube un archivo; si sale bien entra a la lista de fotos y si falla queda como pendiente con su motivo. */
  private subir(pendiente: Pendiente): Observable<Foto> {
    return this.servicio.subirFoto(pendiente.archivo).pipe(
      tap((foto) => {
        this.fotos.update((actuales) => agregar(actuales, foto));
        this.cambiarPendientes((actuales) =>
          actuales.filter((actual) => actual.clave !== pendiente.clave),
        );
        this.anuncio.set(
          `Foto agregada. La oferta tiene ${this.fotos().length} de ${MAXIMO_FOTOS} fotos.`,
        );
      }),
      catchError((fallo: unknown) => {
        const mensaje = mensajeDeFallo(fallo);
        this.cambiarPendientes((actuales) =>
          actuales.map((actual) =>
            actual.clave === pendiente.clave ? { ...actual, estado: 'fallida', mensaje } : actual,
          ),
        );
        return EMPTY;
      }),
    );
  }

  private cambiarPendientes(
    cambio: (actuales: readonly Pendiente[]) => readonly Pendiente[],
  ): void {
    const nuevos = cambio(this.pendientes());
    this.pendientes.set(nuevos);
    this.subiendo.set(nuevos.some((pendiente) => pendiente.estado === 'subiendo'));
  }

  private boton(id: string, accion: Accion): HTMLButtonElement | null {
    return this.anfitrion.nativeElement.querySelector<HTMLButtonElement>(
      `[data-foto="${id}"][data-accion="${accion}"]`,
    );
  }
}

/** Si la API dice qué pasa con la foto ("pesa más de 5 MB") se muestra eso; si no, el mensaje general del error. */
function mensajeDeFallo(fallo: unknown): string {
  const error = aErrorApi(fallo);
  const detalle = error.campo === 'foto' ? error.detalleCampo : undefined;
  if (!detalle) {
    return error.mensaje;
  }
  const texto = detalle.charAt(0).toUpperCase() + detalle.slice(1);
  return texto.endsWith('.') ? texto : `${texto}.`;
}
