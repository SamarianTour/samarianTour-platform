import { Component, inject, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { TipoOferta } from '../../../core/api/modelos';
import { TPipe } from '../../../core/idioma/t.pipe';
import type { ClaveTexto } from '../../../core/idioma/textos';
import { Icono, ICONO_DE_TIPO } from '../../../shared/icono/icono';
import { TEXTO_MAXIMO, normalizarTexto, textoInvalido } from '../catalogo/filtros';
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
 * P01.0 Inicio, público. Reúne el buscador (lleva al catálogo con el texto), las cuatro tarjetas de tipo, las zonas reales
 * (`ZonasInicio`, que pide `GET /zonas`) y la invitación a pedir recomendaciones. No hay "destacados" ni patrocinados, y tampoco
 * consulta el servicio de recomendación: cada visita gastaría cupo de la IA y, sin la consulta del turista, la respuesta sería
 * genérica.
 */
@Component({
  selector: 'app-inicio',
  imports: [ReactiveFormsModule, RouterLink, Icono, TPipe, ZonasInicio],
  templateUrl: './inicio.html',
  styleUrl: './inicio.css',
})
export class Inicio {
  private readonly router = inject(Router);

  protected readonly tarjetas = TARJETAS;
  protected readonly iconoDeTipo = ICONO_DE_TIPO;
  protected readonly textoMaximo = TEXTO_MAXIMO;

  protected readonly texto = new FormControl('', { nonNullable: true });
  /** Se avisa al enviar, no mientras se escribe: la persona aún puede estar a medias. */
  protected readonly textoCorto = signal(false);

  protected buscar(evento: Event): void {
    evento.preventDefault();
    const escrito = this.texto.value;
    if (textoInvalido(escrito)) {
      this.textoCorto.set(true);
      return;
    }
    this.textoCorto.set(false);
    const texto = normalizarTexto(escrito);
    void this.router.navigate(['/catalogo'], { queryParams: texto ? { texto } : {} });
  }
}
