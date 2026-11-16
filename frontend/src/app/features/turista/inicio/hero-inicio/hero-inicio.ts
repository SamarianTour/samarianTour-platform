import { Component, ElementRef, inject, signal, viewChild } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { TPipe } from '../../../../core/idioma/t.pipe';
import { Icono } from '../../../../shared/icono/icono';
import { TEXTO_MAXIMO, normalizarTexto, textoInvalido } from '../../catalogo/filtros';
import { HERO_POSTER, HERO_VIDEO } from '../medios';

/** Si la persona pidió menos movimiento o ahorrar datos, el video no se carga: se ve solo el poster. */
export function sinVideo(): boolean {
  const reducido = typeof matchMedia === 'function' && matchMedia('(prefers-reduced-motion: reduce)').matches;
  const conexion = (navigator as Navigator & { connection?: { saveData?: boolean } }).connection;
  return reducido || conexion?.saveData === true;
}

/**
 * Portada del Inicio: video de fondo (o solo el poster), título, buscador y la franja de confianza que se monta sobre su
 * borde inferior. El buscador lleva al catálogo con el texto (de 2 a 100 caracteres).
 */
@Component({
  selector: 'app-hero-inicio',
  imports: [ReactiveFormsModule, RouterLink, Icono, TPipe],
  templateUrl: './hero-inicio.html',
  styleUrl: './hero-inicio.css',
})
export class HeroInicio {
  private readonly router = inject(Router);
  private readonly video = viewChild<ElementRef<HTMLVideoElement>>('video');

  protected readonly poster = HERO_POSTER;
  protected readonly fuente = HERO_VIDEO;
  protected readonly textoMaximo = TEXTO_MAXIMO;
  protected readonly conVideo = !sinVideo();
  protected readonly pausado = signal(false);

  protected readonly texto = new FormControl('', { nonNullable: true });
  /** Se avisa al enviar, no mientras se escribe: la persona aún puede estar a medias. */
  protected readonly textoCorto = signal(false);

  protected alternarVideo(): void {
    const video = this.video()?.nativeElement;
    const pausar = !this.pausado();
    this.pausado.set(pausar);
    if (!video) {
      return;
    }
    if (pausar) {
      video.pause?.();
    } else {
      // El navegador puede rechazar la reproducción; la persona sigue viendo el cuadro actual.
      void video.play?.()?.catch(() => undefined);
    }
  }

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
