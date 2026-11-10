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
import { TPipe } from '../../../../core/idioma/t.pipe';
import { TextoVivo } from '../../../../core/idioma/texto-vivo';
import { traducir } from '../../../../core/idioma/traducir';
import { Icono } from '../../../../shared/icono/icono';
import {
  MAXIMO_FOTOS,
  TEXTOS_FOTOS_OFERTA,
  TIPOS_FOTO,
  TextosFotos,
  agregar,
  avisoDeMaximo,
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
  /** Por qué falló la subida; se arma al mostrarlo para que siga el idioma. */
  readonly mensaje?: TextoVivo;
}

type Accion = 'antes' | 'despues';

/**
 * Fotos de una oferta (DC-47): de 1 a 6, la primera es la portada. Cada foto se sube apenas se elige y entra a la
 * lista con la miniatura que devuelve la API. El orden se cambia con botones, no arrastrando, para que sirva con
 * teclado y en celular. Las fotos van en `fotos` (enlace de dos vías) y `subiendo` avisa si hay subidas en curso.
 *
 * También lo usa el perfil público (P03.2), con otras rutas y sin orden: `subirCon` y `quitarCon` dicen cómo se sube y se quita una
 * foto, `textos` cambia lo que dice de "la oferta", `ordenable` quita los botones de orden y la portada, y `obligatorio` quita el
 * asterisco. Sin ellos funciona como siempre para las ofertas.
 */
@Component({
  selector: 'app-selector-fotos',
  imports: [Icono, TPipe],
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
  /** Los textos que dependen de qué son las fotos; por defecto, los de una oferta. */
  readonly textos = input<TextosFotos>(TEXTOS_FOTOS_OFERTA);
  /** Si el orden importa (la primera es la portada). El perfil público no tiene orden ni portada. */
  readonly ordenable = input(true);
  /** Si hace falta al menos una foto. El perfil público puede quedarse sin fotos. */
  readonly obligatorio = input(true);
  /** Cómo se sube una foto. Sin esto se sube suelta, para una oferta (`POST /prestador/fotos`). */
  readonly subirCon = input<(archivo: File) => Observable<Foto>>();
  /** Cómo se quita una foto en el servidor. Sin esto se quita solo de la lista, como en una oferta que aún no se guarda. */
  readonly quitarCon = input<(foto: Foto) => Observable<unknown>>();

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
  /** Las fotos que se están quitando en el servidor: su botón queda deshabilitado mientras responde. */
  protected readonly quitando = signal<ReadonlySet<string>>(new Set());
  /**
   * Un aviso por cada archivo que no se subió al elegir. Se guarda la manera de armarlos y no los textos: así los avisos
   * que ya están en pantalla cambian de idioma con la interfaz.
   */
  protected readonly avisos = signal<() => readonly string[]>(() => []);
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
    const cupos = this.cupos();
    const motivoMaximo = this.textos().motivoMaximo;
    const { aceptados } = clasificarArchivos(archivos, cupos, motivoMaximo);
    this.avisos.set(() => clasificarArchivos(archivos, cupos, motivoMaximo).avisos);

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
      const motivoMaximo = this.textos().motivoMaximo;
      this.avisos.set(() => [avisoDeMaximo(pendiente.archivo.name, motivoMaximo)]);
      return;
    }
    this.avisos.set(() => []);
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
    const movida = traducir('prestador.fotos.movida', { posicion, total: nueva.length });
    this.anuncio.set(
      posicion === 1 ? `${movida} ${traducir('prestador.fotos.ahoraPortada')}` : movida,
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
    const quitarCon = this.quitarCon();
    if (!quitarCon) {
      this.quitarDeLaLista(foto);
      return;
    }
    if (this.quitando().has(foto.id)) {
      return;
    }
    this.avisos.set(() => []);
    this.quitando.update((ids) => new Set(ids).add(foto.id));
    quitarCon(foto)
      .pipe(takeUntilDestroyed(this.destruccion))
      .subscribe({
        next: () => {
          this.terminarDeQuitar(foto.id);
          this.quitarDeLaLista(foto);
        },
        error: (fallo: unknown) => {
          this.terminarDeQuitar(foto.id);
          const mensaje = aErrorApi(fallo).mensaje;
          this.avisos.set(() => [mensaje]);
        },
      });
  }

  private terminarDeQuitar(id: string): void {
    this.quitando.update((ids) => {
      const restantes = new Set(ids);
      restantes.delete(id);
      return restantes;
    });
  }

  private quitarDeLaLista(foto: Foto): void {
    const restantes = quitar(this.fotos(), foto.id);
    this.fotos.set(restantes);
    const textos = this.textos();
    this.anuncio.set(
      restantes.length === 0
        ? traducir(textos.quitadaSinFotos)
        : traducir(textos.quitada, { n: restantes.length, maximo: MAXIMO_FOTOS }),
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
    const textos = this.textos();
    return traducir(indice === 0 && this.ordenable() ? textos.altPortada : textos.alt, {
      indice: indice + 1,
      total: this.fotos().length,
    });
  }

  /** Sube un archivo; si sale bien entra a la lista de fotos y si falla queda como pendiente con su motivo. */
  private subir(pendiente: Pendiente): Observable<Foto> {
    const subirCon = this.subirCon();
    return (subirCon ? subirCon(pendiente.archivo) : this.servicio.subirFoto(pendiente.archivo)).pipe(
      tap((foto) => {
        this.fotos.update((actuales) => agregar(actuales, foto));
        this.cambiarPendientes((actuales) =>
          actuales.filter((actual) => actual.clave !== pendiente.clave),
        );
        this.anuncio.set(
          traducir(this.textos().agregada, { n: this.fotos().length, maximo: MAXIMO_FOTOS }),
        );
      }),
      catchError((fallo: unknown) => {
        const mensaje: TextoVivo = () => mensajeDeFallo(fallo);
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
