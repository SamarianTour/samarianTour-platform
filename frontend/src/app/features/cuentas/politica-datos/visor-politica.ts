import { Component, ElementRef, inject, input, signal, viewChild } from '@angular/core';
import { CuentasService } from '../../../core/api/cuentas.service';
import { aErrorApi } from '../../../core/errores/errores.interceptor';
import { TPipe } from '../../../core/idioma/t.pipe';
import { TextoVivo } from '../../../core/idioma/texto-vivo';
import { TextoFormateado } from '../../../shared/texto-formateado/texto-formateado';

/**
 * Muestra el texto de una versión de la política de datos en un `<dialog>` (RF-09, RF-11). El texto llega en Markdown
 * y se pinta con `TextoFormateado` (títulos, negrita, citas y listas, sin librerías ni `innerHTML`: el HTML que traiga se ve como texto). El diálogo atrapa el foco, se cierra con Escape y devuelve el foco al
 * enlace que lo abrió.
 */
@Component({
  selector: 'app-visor-politica',
  imports: [TextoFormateado, TPipe],
  templateUrl: './visor-politica.html',
  styleUrl: './visor-politica.css',
})
export class VisorPolitica {
  /** La versión de la política que se lee, por ejemplo "v1". */
  readonly version = input.required<string>();

  private readonly cuentas = inject(CuentasService);
  private readonly dialogo = viewChild.required<ElementRef<HTMLDialogElement>>('dialogo');

  protected readonly cargando = signal(false);
  protected readonly texto = signal<string | null>(null);
  protected readonly error = signal<TextoVivo | null>(null);
  /** La versión cuyo texto ya se cargó, para no volver a pedirlo al reabrir. */
  private versionCargada: string | null = null;

  abrir(): void {
    this.dialogo().nativeElement.showModal();
    if (this.versionCargada !== this.version()) {
      this.cargar();
    }
  }

  protected cerrar(): void {
    this.dialogo().nativeElement.close();
  }

  protected cargar(): void {
    const version = this.version();
    this.cargando.set(true);
    this.error.set(null);
    this.cuentas.textoPolitica(version).subscribe({
      next: (texto) => {
        this.versionCargada = version;
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
