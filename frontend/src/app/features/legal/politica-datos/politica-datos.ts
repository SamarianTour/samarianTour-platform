import { Component, inject, signal } from '@angular/core';
import { map, switchMap } from 'rxjs';
import { CuentasService } from '../../../core/api/cuentas.service';
import { aErrorApi } from '../../../core/errores/errores.interceptor';
import { TPipe } from '../../../core/idioma/t.pipe';
import { TextoVivo } from '../../../core/idioma/texto-vivo';
import { TextoFormateado } from '../../../shared/texto-formateado/texto-formateado';

/**
 * Política de tratamiento de datos (Ley 1581 de 2012), pública. Muestra el texto vigente que entrega el backend
 * (`politicaVigente()` y `textoPolitica()`), con el mismo render que el registro (`TextoFormateado`): títulos,
 * negrita, citas y listas sin librerías ni `innerHTML`, así que el HTML que traiga se ve como texto. Omite el primer título del
 * texto si repite el de la página. Si el backend falla, muestra el error de siempre.
 */
@Component({
  selector: 'app-politica-datos',
  imports: [TextoFormateado, TPipe],
  templateUrl: './politica-datos.html',
  styleUrl: '../legal.css',
})
export class PoliticaDatosPagina {
  private readonly cuentas = inject(CuentasService);

  protected readonly cargando = signal(true);
  protected readonly version = signal<string | null>(null);
  protected readonly texto = signal<string | null>(null);
  protected readonly error = signal<TextoVivo | null>(null);

  constructor() {
    this.cargar();
  }

  protected cargar(): void {
    this.cargando.set(true);
    this.error.set(null);
    this.cuentas
      .politicaVigente()
      .pipe(
        switchMap((politica) =>
          this.cuentas.textoPolitica(politica.version).pipe(map((texto) => ({ version: politica.version, texto }))),
        ),
      )
      .subscribe({
        next: ({ version, texto }) => {
          this.version.set(version);
          this.texto.set(texto);
          this.cargando.set(false);
        },
        error: (fallo: unknown) => {
          const problema = aErrorApi(fallo);
          this.error.set(() => problema.mensaje);
          this.cargando.set(false);
        },
      });
  }
}
