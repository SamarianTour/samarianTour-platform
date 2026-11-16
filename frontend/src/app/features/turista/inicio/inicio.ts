import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TipoOferta } from '../../../core/api/modelos';
import { TPipe } from '../../../core/idioma/t.pipe';
import type { ClaveTexto } from '../../../core/idioma/textos';
import { Icono, ICONO_DE_TIPO } from '../../../shared/icono/icono';
import { FiltrosInicio } from './filtros-inicio/filtros-inicio';
import { HeroInicio } from './hero-inicio/hero-inicio';
import { FOTO_DE_TIPO } from './medios';
import { ZonasInicio } from './zonas-inicio/zonas-inicio';

/** Una de las cuatro tarjetas de tipo. El paquete tiene su propia pantalla (`/paquetes`); los demás filtran el catálogo. */
interface TarjetaDeTipo {
  readonly tipo: TipoOferta;
  readonly nombre: ClaveTexto;
  readonly descripcion: ClaveTexto;
  readonly ruta: string;
  readonly consulta?: Readonly<Record<string, string>>;
}

const TARJETAS: readonly TarjetaDeTipo[] = [
  {
    tipo: 'ACTIVIDAD',
    nombre: 'turista.inicio.tipo.actividad',
    descripcion: 'turista.inicio.tipo.actividad.desc',
    ruta: '/catalogo',
    consulta: { tipo: 'ACTIVIDAD' },
  },
  {
    tipo: 'PAQUETE',
    nombre: 'turista.inicio.tipo.paquete',
    descripcion: 'turista.inicio.tipo.paquete.desc',
    ruta: '/paquetes',
  },
  {
    tipo: 'ALOJAMIENTO',
    nombre: 'turista.inicio.tipo.alojamiento',
    descripcion: 'turista.inicio.tipo.alojamiento.desc',
    ruta: '/catalogo',
    consulta: { tipo: 'ALOJAMIENTO' },
  },
  {
    tipo: 'RESTAURANTE',
    nombre: 'turista.inicio.tipo.restaurante',
    descripcion: 'turista.inicio.tipo.restaurante.desc',
    ruta: '/catalogo',
    consulta: { tipo: 'RESTAURANTE' },
  },
];

/**
 * P01.0 Inicio, público. Reúne la portada (`HeroInicio`: video, buscador y franja de confianza), las cuatro tarjetas de tipo con su foto, los filtros rápidos, las zonas reales
 * (`ZonasInicio`, que pide `GET /zonas`) y la invitación a pedir recomendaciones. No hay "destacados" ni patrocinados, y tampoco
 * consulta el servicio de recomendación: cada visita gastaría cupo de la IA y, sin la consulta del turista, la respuesta sería
 * genérica.
 */
@Component({
  selector: 'app-inicio',
  imports: [RouterLink, Icono, TPipe, FiltrosInicio, HeroInicio, ZonasInicio],
  templateUrl: './inicio.html',
  styleUrl: './inicio.css',
})
export class Inicio {
  protected readonly tarjetas = TARJETAS;
  protected readonly iconoDeTipo = ICONO_DE_TIPO;
  protected readonly fotoDeTipo = FOTO_DE_TIPO;
}
