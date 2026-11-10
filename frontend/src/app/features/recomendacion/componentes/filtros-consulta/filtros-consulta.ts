import { Component, computed, input, output } from '@angular/core';
import { nivelEsfuerzoValues, tipoActividadValues, tipoOfertaValues } from '../../../../core/api/catalogo-api';
import { NivelEsfuerzo, TipoActividad, TipoOferta, Zona } from '../../../../core/api/modelos';
import { TPipe } from '../../../../core/idioma/t.pipe';
import { NIVEL_ESFUERZO, TIPO_ACTIVIDAD, TIPO_OFERTA } from '../../../../shared/etiquetas';
import { FiltrosRecomendacion, SIN_FILTROS, hayFiltros } from '../../consulta';

/**
 * Los filtros de una consulta de recomendación, en un desplegable ("Afinar la búsqueda"): tipo de oferta, tipo de actividad, zona,
 * esfuerzo máximo, precio máximo y accesibilidad. Escribir lo que se busca sigue siendo lo primero; esto afina. Los de accesibilidad,
 * esfuerzo y precio son estrictos: el servicio nunca los relaja. No guarda nada: cada cambio se avisa con `filtrosChange` y quien lo
 * usa decide qué hacer con él.
 */
@Component({
  selector: 'app-filtros-consulta',
  imports: [TPipe],
  templateUrl: './filtros-consulta.html',
  styleUrl: './filtros-consulta.css',
})
export class FiltrosConsulta {
  readonly filtros = input.required<FiltrosRecomendacion>();
  /** Las zonas del catálogo para el filtro; sin ellas el filtro de zona no se ofrece y lo demás sigue. */
  readonly zonas = input<readonly Zona[]>([]);
  readonly filtrosChange = output<FiltrosRecomendacion>();

  protected readonly tiposOferta = tipoOfertaValues;
  protected readonly tiposActividad = tipoActividadValues;
  protected readonly niveles = nivelEsfuerzoValues;
  protected readonly etiquetaTipoOferta = TIPO_OFERTA;
  protected readonly etiquetaActividad = TIPO_ACTIVIDAD;
  protected readonly etiquetaNivel = NIVEL_ESFUERZO;
  protected readonly hayFiltros = computed(() => hayFiltros(this.filtros()));

  private cambiar(cambio: Partial<FiltrosRecomendacion>): void {
    this.filtrosChange.emit({ ...this.filtros(), ...cambio });
  }

  protected alternarTipoOferta(tipo: TipoOferta, marcado: boolean): void {
    this.cambiar({ tiposOferta: alternar(this.filtros().tiposOferta, tipo, marcado) });
  }

  protected alternarActividad(tipo: TipoActividad, marcado: boolean): void {
    this.cambiar({ tiposActividad: alternar(this.filtros().tiposActividad, tipo, marcado) });
  }

  protected cambiarZona(zonaId: string): void {
    this.cambiar({ zonaId });
  }

  protected cambiarAccesible(marcado: boolean): void {
    this.cambiar({ accesibleSillaRuedas: marcado });
  }

  protected cambiarNivel(nivel: string): void {
    this.cambiar({ nivelEsfuerzoMaximo: nivel as NivelEsfuerzo | '' });
  }

  /** El campo vacío es "sin precio máximo"; un 0 o un negativo se deja pasar para que la consulta avise. */
  protected cambiarPrecio(valor: number): void {
    this.cambiar({ precioMaximo: Number.isNaN(valor) ? null : valor });
  }

  protected quitar(): void {
    this.filtrosChange.emit(SIN_FILTROS);
  }
}

/** Agrega o quita un valor de una lista de opciones marcadas, sin repetirlo. */
function alternar<T>(lista: readonly T[], valor: T, marcado: boolean): T[] {
  const resto = lista.filter((otro) => otro !== valor);
  return marcado ? [...resto, valor] : resto;
}
