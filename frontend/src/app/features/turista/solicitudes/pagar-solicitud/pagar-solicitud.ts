import { Component, computed, inject, input, output, signal } from '@angular/core';
import { ClaveDeIdempotencia } from '../../../../core/api/idempotencia';
import { IntentoPago, SolicitudTurista } from '../../../../core/api/modelos-nucleo';
import { PagosService } from '../../../../core/api/pagos.service';
import { AvisosService } from '../../../../core/avisos/avisos.service';
import { aErrorApi } from '../../../../core/errores/errores.interceptor';
import { TPipe } from '../../../../core/idioma/t.pipe';
import { TextoVivo, textoVivo } from '../../../../core/idioma/texto-vivo';
import { Navegador } from '../../../../core/navegacion/navegador';
import { FechaPipe } from '../../../../shared/fecha.pipe';
import { Icono } from '../../../../shared/icono/icono';
import { PrecioPipe } from '../../../../shared/precio.pipe';
import { avisoDeIntentos, puedePagar, urlDeCheckoutSegura } from '../pagos';
import { normalizarHora } from '../peticion-solicitud';
import { Plazo } from '../plazo';

/** Los errores que dicen que la solicitud ya no se puede pagar como la vio el turista: hay que volver a consultarla. */
const CODIGOS_DE_ESTADO_CAMBIADO = new Set([
  'SOLICITUD_NO_COBRABLE',
  'INTENTOS_AGOTADOS',
  'PLAZO_VENCIDO',
  'CONFLICTO_CONCURRENCIA',
  'SOLICITUD_NO_ENCONTRADA',
]);

/**
 * P04.1 Pagar la solicitud (RF-46 a RF-49). Se muestra en el detalle de una solicitud confirmada: el resumen de lo que se paga
 * y el botón "Pagar", que pide la sesión de cobro y manda al turista a la pasarela externa (`checkout.url`, en esta misma
 * pestaña: la pasarela lo devuelve a `/turista/solicitudes/:id`). **Aquí nunca se piden datos de tarjeta ni de cuenta**
 * (RF-47): el cobro ocurre solo en la pasarela. El total es el precio base que fijó el operador, sin cargos adicionales
 * (RF-49). Si ya hay un intento en curso, abrir el pago devuelve ese mismo. El estado del pago lo decide solo el servidor
 * (RF-50, RF-51), no esta pantalla.
 */
@Component({
  selector: 'app-pagar-solicitud',
  imports: [TPipe, FechaPipe, PrecioPipe, Icono],
  templateUrl: './pagar-solicitud.html',
  styleUrl: './pagar-solicitud.css',
})
export class PagarSolicitud {
  readonly solicitud = input.required<SolicitudTurista>();
  /** Los intentos de pago que ya tiene la solicitud, para decir cuántos le quedan. */
  readonly intentos = input<readonly IntentoPago[]>([]);
  readonly plazo = input.required<Plazo>();
  /** La solicitud cambió por su lado (ya no se puede pagar): la pantalla debe volver a consultarla. */
  readonly reconsultar = output<void>();

  private readonly servicio = inject(PagosService);
  private readonly navegador = inject(Navegador);
  private readonly avisos = inject(AvisosService);
  private readonly clave = new ClaveDeIdempotencia();

  protected readonly enviando = signal(false);
  protected readonly error = signal<TextoVivo | null>(null);
  /** La dirección de la pasarela, por si el navegador no la abre solo. */
  protected readonly urlAbierta = signal<string | null>(null);

  protected readonly total = computed(() => this.solicitud().condiciones?.precioBase ?? null);
  protected readonly aviso = computed(() => avisoDeIntentos(this.intentos()));
  protected readonly puede = computed(() =>
    puedePagar(this.solicitud().estado, this.intentos(), this.plazo()),
  );
  protected readonly plazoVencido = computed(() => this.plazo().nivel === 'vencido');
  /** La hora de salida o de la reserva, sin segundos. */
  protected readonly hora = computed(() => {
    const hora = this.solicitud().horaSalida ?? this.solicitud().horaReserva;
    return hora ? normalizarHora(hora) : null;
  });

  protected pagar(): void {
    if (this.enviando() || !this.puede()) {
      return;
    }
    this.error.set(null);
    this.urlAbierta.set(null);
    this.enviando.set(true);
    this.servicio.abrir(this.solicitud().id, this.clave.para({})).subscribe({
      next: (sesion) => {
        const url = urlDeCheckoutSegura(sesion.checkout);
        this.clave.reiniciar();
        if (!url) {
          this.enviando.set(false);
          this.error.set(textoVivo('pagos.pagar.sinUrl'));
          return;
        }
        this.urlAbierta.set(url);
        this.navegador.irA(url);
      },
      error: (fallo: unknown) => {
        this.enviando.set(false);
        const problema = aErrorApi(fallo);
        this.error.set(() => problema.mensaje);
        if (problema.codigo && CODIGOS_DE_ESTADO_CAMBIADO.has(problema.codigo)) {
          // La clave de este intento ya no sirve: el estado cambió y lo que sigue es otra operación.
          this.clave.reiniciar();
          // Al reconsultar este bloque puede desaparecer (la solicitud ya no está por pagar): el aviso deja el motivo.
          this.avisos.mostrar(problema.mensaje);
          this.reconsultar.emit();
        }
      },
    });
  }
}
