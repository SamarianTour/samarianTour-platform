import { Component, DestroyRef, ElementRef, Injector, afterNextRender, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { catchError, of } from 'rxjs';
import { CatalogoService } from '../../../core/api/catalogo.service';
import { Zona } from '../../../core/api/modelos';
import { RecomendacionService } from '../../../core/api/recomendacion.service';
import { TPipe } from '../../../core/idioma/t.pipe';
import type { ClaveTexto } from '../../../core/idioma/textos';
import { traducir } from '../../../core/idioma/traducir';
import { NIVEL_ESFUERZO, TIPO_ACTIVIDAD, TIPO_OFERTA } from '../../../shared/etiquetas';
import { Icono } from '../../../shared/icono/icono';
import { PrecioPipe } from '../../../shared/precio.pipe';
import { FiltrosConsulta } from '../componentes/filtros-consulta/filtros-consulta';
import { TarjetaRecomendada } from '../componentes/tarjeta-recomendada/tarjeta-recomendada';
import {
  FiltrosRecomendacion,
  MAXIMO_TEXTO,
  MotivoConsulta,
  SIN_FILTROS,
  construirConsulta,
  validarConsulta,
} from '../consulta';
import {
  BIENVENIDA,
  ConsultaEnviada,
  Mensaje,
  contenidoDeResultado,
  hayBusqueda,
  reemplazar,
  vistaDeRespuesta,
} from '../conversacion';

const CLAVE_DE_ERROR: Readonly<Record<MotivoConsulta, ClaveTexto>> = {
  vacia: 'recomendacion.error.vacia',
  textoLargo: 'recomendacion.error.textoLargo',
  precioInvalido: 'recomendacion.error.precioInvalido',
};

/** Las ideas para empezar: cada una se envía como si el turista la hubiera escrito. */
const IDEAS: readonly { readonly id: string; readonly clave: ClaveTexto }[] = [
  { id: 'mar', clave: 'recomendacion.idea.mar' },
  { id: 'minca', clave: 'recomendacion.idea.minca' },
  { id: 'ninos', clave: 'recomendacion.idea.ninos' },
  { id: 'buceo', clave: 'recomendacion.idea.buceo' },
];

/**
 * P08.1 Recomendaciones (HU-TUR-04, RF-26, RF-27). El turista escribe lo que busca, o toca una idea, y ve de 3 a 5 ofertas visibles
 * del catálogo con la explicación del criterio de cada una y quién las eligió (reglas o IA); si pocas ofertas cumplen sus filtros,
 * las que haya con el aviso. Es pública. Los filtros de accesibilidad, esfuerzo y precio nunca se relajan.
 *
 * El servicio de recomendación es opcional (ADR-04): si falla, no responde o tarda más de 10 s, la conversación lo dice con un
 * aviso, ofrece volver a intentar y lleva al catálogo; el resto de la aplicación no se entera. La conversación vive solo en memoria:
 * el texto del turista puede incluir datos personales, así que no se guarda ni viaja en la dirección.
 */
@Component({
  selector: 'app-chat-recomendaciones',
  imports: [ReactiveFormsModule, RouterLink, Icono, TPipe, TarjetaRecomendada, FiltrosConsulta],
  templateUrl: './chat-recomendaciones.html',
  styleUrl: './chat-recomendaciones.css',
})
export class ChatRecomendaciones {
  private readonly servicio = inject(RecomendacionService);
  private readonly catalogo = inject(CatalogoService);
  private readonly destruccion = inject(DestroyRef);
  private readonly injector = inject(Injector);
  private readonly anfitrion = inject<ElementRef<HTMLElement>>(ElementRef);
  private readonly precio = new PrecioPipe();
  private siguienteId = 1;

  protected readonly maximoTexto = MAXIMO_TEXTO;
  protected readonly ideas = IDEAS;

  protected readonly texto = new FormControl('', { nonNullable: true });
  protected readonly filtros = signal<FiltrosRecomendacion>(SIN_FILTROS);
  /** Las zonas del catálogo para el filtro; si no se pueden leer, el filtro de zona no se ofrece y lo demás sigue. */
  protected readonly zonas = signal<readonly Zona[]>([]);

  protected readonly mensajes = signal<Mensaje[]>([BIENVENIDA]);
  protected readonly buscando = computed(() => hayBusqueda(this.mensajes()));

  private readonly motivoDeError = signal<MotivoConsulta | null>(null);
  /** El texto del error se arma al leerlo, así sigue el idioma. */
  protected readonly mensajeDeError = computed(() => {
    const motivo = this.motivoDeError();
    return motivo ? traducir(CLAVE_DE_ERROR[motivo]) : null;
  });

  protected readonly vistaDeRespuesta = vistaDeRespuesta;

  constructor() {
    this.catalogo
      .zonas()
      .pipe(
        catchError(() => of<Zona[]>([])),
        takeUntilDestroyed(),
      )
      .subscribe((zonas) => this.zonas.set(zonas));

    // Al escribir desaparece el aviso de que faltaba algo.
    this.texto.valueChanges.pipe(takeUntilDestroyed()).subscribe(() => this.motivoDeError.set(null));
  }

  // --- Enviar ---

  protected usarIdea(clave: ClaveTexto): void {
    this.enviar(traducir(clave));
  }

  /** Envía lo escrito (o el texto de una idea) con los filtros elegidos. Mientras hay una búsqueda en curso no envía otra. */
  protected enviar(textoDirecto?: string): void {
    if (this.buscando()) {
      return;
    }
    const texto = textoDirecto ?? this.texto.value;
    const filtros = this.filtros();
    const motivo = validarConsulta(texto, filtros);
    this.motivoDeError.set(motivo);
    if (motivo) {
      this.anfitrion.nativeElement.querySelector<HTMLElement>('#chat-texto')?.focus();
      return;
    }

    const consulta = construirConsulta(texto, filtros);
    this.texto.setValue('', { emitEvent: false });
    this.mensajes.update((mensajes) => [...mensajes, { id: this.siguienteId++, autor: 'turista', consulta }]);
    const id = this.siguienteId++;
    this.mensajes.update((mensajes) => [
      ...mensajes,
      { id, autor: 'asistente', contenido: { tipo: 'buscando', consulta } },
    ]);
    this.buscar(id, consulta);
    this.irAlUltimo();
  }

  /** Vuelve a pedir lo mismo después de un aviso de que el servicio no estaba disponible. */
  protected reintentar(id: number, consulta: ConsultaEnviada): void {
    if (this.buscando()) {
      return;
    }
    this.mensajes.update((mensajes) => reemplazar(mensajes, id, { tipo: 'buscando', consulta }));
    this.buscar(id, consulta);
  }

  private buscar(id: number, consulta: ConsultaEnviada): void {
    this.servicio
      .recomendar(consulta)
      .pipe(takeUntilDestroyed(this.destruccion))
      .subscribe((resultado) => {
        this.mensajes.update((mensajes) => reemplazar(mensajes, id, contenidoDeResultado(consulta, resultado)));
        this.irAlUltimo();
      });
  }

  /** Lleva la vista al último mensaje: así se ve la respuesta sin buscarla. */
  private irAlUltimo(): void {
    afterNextRender(
      () => {
        const ultimo = this.anfitrion.nativeElement.querySelector<HTMLElement>('.mensaje:last-child');
        ultimo?.scrollIntoView?.({ block: 'nearest' });
      },
      { injector: this.injector },
    );
  }

  // --- Los filtros ---

  /** Un cambio de filtros: también quita el aviso de que faltaba algo, que ya no aplica. */
  protected cambiarFiltros(filtros: FiltrosRecomendacion): void {
    this.filtros.set(filtros);
    this.motivoDeError.set(null);
  }

  // --- Cómo se escribe lo enviado ---

  /** Los filtros de una consulta en palabras, para la burbuja del turista; siguen el idioma. */
  protected filtrosEnTexto(consulta: ConsultaEnviada): string[] {
    return [
      ...(consulta.tiposOferta ?? []).map((tipo) => TIPO_OFERTA[tipo]),
      ...(consulta.tiposActividad ?? []).map((tipo) => TIPO_ACTIVIDAD[tipo]),
      ...(consulta.zonaId ? [this.nombreDeZona(consulta.zonaId)] : []),
      ...(consulta.accesibleSillaRuedas ? [traducir('recomendacion.filtro.accesible')] : []),
      ...(consulta.nivelEsfuerzoMaximo
        ? [
            traducir('recomendacion.filtro.esfuerzoHasta', {
              nivel: NIVEL_ESFUERZO[consulta.nivelEsfuerzoMaximo].toLocaleLowerCase(),
            }),
          ]
        : []),
      ...(consulta.precioMaximo !== undefined
        ? [traducir('recomendacion.filtro.precioHasta', { precio: this.precio.transform(consulta.precioMaximo) })]
        : []),
    ];
  }

  private nombreDeZona(zonaId: string): string {
    return this.zonas().find((zona) => zona.id === zonaId)?.nombre ?? zonaId;
  }

  protected claveDeMotor(motor: 'IA' | 'REGLAS'): ClaveTexto {
    return motor === 'IA' ? 'recomendacion.respuesta.motor.IA' : 'recomendacion.respuesta.motor.REGLAS';
  }

  protected claveDeNoDisponible(motivo: 'tiempo' | 'servicio' | 'conexion'): ClaveTexto {
    switch (motivo) {
      case 'tiempo':
        return 'recomendacion.noDisponible.tiempo';
      case 'conexion':
        return 'recomendacion.noDisponible.conexion';
      case 'servicio':
        return 'recomendacion.noDisponible.servicio';
    }
  }
}
