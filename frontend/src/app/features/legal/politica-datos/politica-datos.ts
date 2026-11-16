import { Component, inject, signal } from '@angular/core';
import { map, switchMap } from 'rxjs';
import { CuentasService } from '../../../core/api/cuentas.service';
import { aErrorApi } from '../../../core/errores/errores.interceptor';
import { TPipe } from '../../../core/idioma/t.pipe';
import { TextoVivo } from '../../../core/idioma/texto-vivo';

/**
 * Política de tratamiento de datos (Ley 1581 de 2012), pública. Muestra el texto vigente que entrega el backend
 * (`politicaVigente()` y `textoPolitica()`), con el mismo render que el registro: el Markdown no se interpreta, se muestra
 * tal cual respetando los saltos de línea, sin riesgo de inyectar HTML. Si el backend falla, muestra el error de siempre.
 */
@Component({
  selector: 'app-politica-datos',
  imports: [TPipe],
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
