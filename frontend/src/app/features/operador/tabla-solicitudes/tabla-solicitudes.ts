import { Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { SolicitudCola } from '../../../core/api/modelos-nucleo';
import { tablaDeTextos } from '../../../core/idioma/tabla-de-textos';
import { traducir } from '../../../core/idioma/traducir';
import { FechaPipe } from '../../../shared/fecha.pipe';
import { TIPO_OFERTA } from '../../../shared/etiquetas';
import { Icono } from '../../../shared/icono/icono';
import { TPipe } from '../../../core/idioma/t.pipe';
import { CLASE_DE_ESTADO_SOLICITUD, ETIQUETA_ESTADO } from '../../turista/solicitudes/estado-solicitud';
import { normalizarHora } from '../../turista/solicitudes/peticion-solicitud';
import { plazoRestante } from '../../turista/solicitudes/plazo';
import { duracionTexto } from '../../turista/solicitudes/texto-plazo';
import { NivelUrgencia, codigoCorto, nivelDeUrgencia } from '../cola';

/** `cola`: las pendientes con su urgencia y su plazo. `historial`: las ya tramitadas con su estado. */
export type ModoTabla = 'cola' | 'historial';

/**
 * La tabla de solicitudes del operador (P02.4 y su historial): una fila por solicitud, con un enlace a atenderla. No pide
 * datos: recibe las filas ya ordenadas. Las urgentes (6 h o menos, RF-36) se resaltan con color, con el texto "Urgente"
 * y con un icono, para que no dependan solo del color. Sin datos del turista: el operador gestiona con el prestador.
 */
@Component({
  selector: 'app-tabla-solicitudes-operador',
  imports: [RouterLink, Icono, TPipe, FechaPipe],
  templateUrl: './tabla-solicitudes.html',
  styleUrl: './tabla-solicitudes.css',
})
export class TablaSolicitudesOperador {
  readonly solicitudes = input.required<readonly SolicitudCola[]>();
  readonly modo = input<ModoTabla>('cola');
  /** El momento actual, para el plazo y la urgencia; la pantalla lo renueva con su reloj. */
  readonly ahora = input(Date.now());

  protected readonly etiquetaEstado = ETIQUETA_ESTADO;
  protected readonly claseDeEstado = CLASE_DE_ESTADO_SOLICITUD;
  protected readonly etiquetaTipo = TIPO_OFERTA;
  protected readonly etiquetaUrgencia = tablaDeTextos<NivelUrgencia>({
    urgente: 'operador.urgencia.urgente',
    'en-plazo': 'operador.urgencia.enPlazo',
    vencida: 'operador.urgencia.vencida',
  });
  protected readonly claseDeUrgencia: Readonly<Record<NivelUrgencia, string>> = {
    urgente: 'etiqueta--peligro',
    'en-plazo': 'etiqueta--neutra',
    vencida: 'etiqueta--aviso',
  };
  protected readonly codigo = codigoCorto;

  protected nivel(solicitud: SolicitudCola): NivelUrgencia {
    return nivelDeUrgencia(solicitud, this.ahora());
  }

  /** Cuánto falta para que venza, o que ya venció; null si la solicitud no tiene plazo. */
  protected plazo(solicitud: SolicitudCola): string | null {
    const plazo = plazoRestante(solicitud.venceEn, this.ahora());
    if (plazo.nivel === 'sin-plazo') {
      return null;
    }
    return plazo.nivel === 'vencido'
      ? traducir('operador.tabla.plazoVencido')
      : traducir('operador.tabla.restante', { tiempo: duracionTexto(plazo) });
  }

  /** La hora de salida o de la reserva, sin segundos. */
  protected hora(solicitud: SolicitudCola): string | null {
    const hora = solicitud.horaSalida ?? solicitud.horaReserva;
    return hora ? normalizarHora(hora) : null;
  }
}
