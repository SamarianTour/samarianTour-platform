import {
  Component,
  DestroyRef,
  ElementRef,
  afterNextRender,
  effect,
  inject,
  input,
  output,
  viewChild,
} from '@angular/core';
import * as L from 'leaflet';
import { Coordenada } from '../../core/api/modelos';

/** Centro de Santa Marta, para cuando todavía no hay ubicación. */
const SANTA_MARTA: L.LatLngTuple = [11.2408, -74.199];
const ZOOM_CIUDAD = 11;
const ZOOM_PUNTO = 14;

/**
 * Mapa de Leaflet sobre OpenStreetMap, sin API key. Muestra un pin en la ubicación y, si es editable,
 * deja ponerlo con un clic. El pin es un divIcon con CSS: así no depende de las imágenes de Leaflet,
 * que el build de Angular no copia.
 */
@Component({
  selector: 'app-mapa',
  template: `<div #lienzo class="mapa" role="application" [attr.aria-label]="descripcion()"></div>`,
  styles: `
    :host {
      display: block;
    }
    .mapa {
      height: 320px;
      border: 1px solid var(--borde);
      border-radius: 12px;
      z-index: 0;
    }
  `,
})
export class Mapa {
  readonly ubicacion = input<Coordenada | null>(null);
  readonly editable = input(false);
  readonly descripcion = input('Mapa de la ubicación');
  readonly ubicacionElegida = output<Coordenada>();

  private readonly lienzo = viewChild.required<ElementRef<HTMLDivElement>>('lienzo');
  private mapa?: L.Map;
  private pin?: L.Marker;

  constructor() {
    afterNextRender(() => this.crear());
    // Vuelve a dibujar el pin cuando cambia la ubicación, también antes de que exista el mapa.
    effect(() => this.dibujarPin(this.ubicacion()));
    inject(DestroyRef).onDestroy(() => this.mapa?.remove());
  }

  private crear(): void {
    const ubicacion = this.ubicacion();
    this.mapa = L.map(this.lienzo().nativeElement, { scrollWheelZoom: false }).setView(
      ubicacion ? [ubicacion.latitud, ubicacion.longitud] : SANTA_MARTA,
      ubicacion ? ZOOM_PUNTO : ZOOM_CIUDAD,
    );
    L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors',
    }).addTo(this.mapa);
    this.mapa.on('click', (evento: L.LeafletMouseEvent) => {
      if (this.editable()) {
        this.ubicacionElegida.emit({
          latitud: redondear(evento.latlng.lat),
          longitud: redondear(evento.latlng.lng),
        });
      }
    });
    this.dibujarPin(ubicacion);
  }

  private dibujarPin(ubicacion: Coordenada | null): void {
    if (!this.mapa) {
      return;
    }
    this.pin?.remove();
    this.pin = undefined;
    if (ubicacion) {
      this.pin = L.marker([ubicacion.latitud, ubicacion.longitud], {
        // Leaflet ubica el icono con `transform`, así que la forma del pin va en un elemento interior.
        icon: L.divIcon({ className: 'pin-mapa', html: '<span></span>', iconSize: [28, 28], iconAnchor: [14, 28] }),
        keyboard: false,
      }).addTo(this.mapa);
      this.mapa.panTo([ubicacion.latitud, ubicacion.longitud]);
    }
  }
}

/** Cinco decimales son cerca de un metro: suficiente para ubicar una oferta. */
function redondear(grados: number): number {
  return Math.round(grados * 100000) / 100000;
}
