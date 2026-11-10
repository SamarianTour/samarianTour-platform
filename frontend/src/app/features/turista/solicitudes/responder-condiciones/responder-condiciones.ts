import { Component, inject, input, output, signal, viewChild } from '@angular/core';
import { ClaveDeIdempotencia } from '../../../../core/api/idempotencia';
import { DecisionCondiciones, SolicitudTurista } from '../../../../core/api/modelos-nucleo';
import { SolicitudesService } from '../../../../core/api/solicitudes.service';
import { AvisosService } from '../../../../core/avisos/avisos.service';
import { aErrorApi } from '../../../../core/errores/errores.interceptor';
import { TPipe } from '../../../../core/idioma/t.pipe';
import { TextoVivo } from '../../../../core/idioma/texto-vivo';
import { traducir } from '../../../../core/idioma/traducir';
import { DialogoConfirmacion } from '../../../../shared/dialogo-confirmacion/dialogo-confirmacion';
import { FechaPipe } from '../../../../shared/fecha.pipe';

/** Los errores que dicen que la solicitud ya no está como la vio el turista: hay que volver a consultarla (RF-33). */
const CODIGOS_DE_ESTADO_CAMBIADO = new Set([
  'PLAZO_VENCIDO',
  'TRANSICION_INVALIDA',
  'SIN_TIEMPO_PARA_CONFIRMAR',
  'SOLICITUD_NO_ENCONTRADA',
  'CONFLICTO_CONCURRENCIA',
]);

/**
 * P02.3 Confirmar o declinar las condiciones (RF-43). Se muestra en el detalle de una solicitud atendida, junto a lo que
 * propuso el operador y el plazo. Confirmar la deja confirmada para pagar dentro de la misma ventana; declinar la cierra y
 * libera el cupo, y pide confirmación porque no se deshace. Ambas llevan una `Idempotency-Key`: reintentar la misma decisión
 * repite la clave. Si el núcleo dice que la solicitud cambió (el plazo venció, ya estaba cerrada), avisa y pide reconsultar.
 */
@Component({
  selector: 'app-responder-condiciones',
  imports: [TPipe, FechaPipe, DialogoConfirmacion],
  templateUrl: './responder-condiciones.html',
  styleUrl: './responder-condiciones.css',
})
export class ResponderCondiciones {
  readonly solicitud = input.required<SolicitudTurista>();
  /** El plazo terminó: ya no se puede decidir mientras se consulta el estado real. */
  readonly plazoVencido = input(false);
  /** La solicitud con su estado nuevo, tras confirmar o declinar. */
  readonly respondida = output<SolicitudTurista>();
  /** La solicitud cambió por su lado: la pantalla debe volver a consultarla. */
  readonly reconsultar = output<void>();

  private readonly servicio = inject(SolicitudesService);
  private readonly avisos = inject(AvisosService);
  private readonly dialogo = viewChild.required(DialogoConfirmacion);
  private readonly clave = new ClaveDeIdempotencia();

  /** La decisión que se está enviando, o null. */
  protected readonly enviando = signal<DecisionCondiciones | null>(null);
  protected readonly error = signal<TextoVivo | null>(null);

  protected confirmar(): void {
    this.responder('CONFIRMAR');
  }

  protected pedirDeclinar(): void {
    this.dialogo().abrir();
  }

  protected declinar(): void {
    this.responder('DECLINAR');
  }

  private responder(decision: DecisionCondiciones): void {
    if (this.enviando() || this.plazoVencido()) {
      return;
    }
    this.error.set(null);
    this.enviando.set(decision);
    this.servicio
      .responder(this.solicitud().id, decision, this.clave.para({ decision }))
      .subscribe({
        next: (solicitud) => {
          this.enviando.set(null);
          this.clave.reiniciar();
          this.avisos.mostrar(
            traducir(
              decision === 'CONFIRMAR'
                ? 'solicitudes.decision.confirmada'
                : 'solicitudes.decision.declinada',
            ),
          );
          this.respondida.emit(solicitud);
        },
        error: (fallo: unknown) => {
          this.enviando.set(null);
          const problema = aErrorApi(fallo);
          this.error.set(() => problema.mensaje);
          if (problema.codigo && CODIGOS_DE_ESTADO_CAMBIADO.has(problema.codigo)) {
            // La clave de esta decisión ya no sirve: el estado cambió y lo que sigue es otra operación.
            this.clave.reiniciar();
            // Al reconsultar, este bloque puede desaparecer (la solicitud ya no está atendida): el aviso deja el motivo.
            this.avisos.mostrar(problema.mensaje);
            this.reconsultar.emit();
          }
        },
      });
  }
}
