import { EstadoIntento, IntentoPago } from '../../../core/api/modelos-nucleo';
import { establecerIdioma } from '../../../core/idioma/idioma';
import {
  CLASE_DE_INTENTO,
  ESPERA_LENTA_MS,
  ESPERA_RAPIDA_MS,
  ETIQUETA_INTENTO,
  INTENTOS_MAXIMOS,
  MOTIVO_INTENTO,
  VENTANA_RAPIDA_MS,
  avisoDeIntentos,
  centavosAPesos,
  esperaDeReconsulta,
  estaConfirmandoPago,
  intentoAprobado,
  intentoEnCurso,
  intentosRestantes,
  mostrarIntentos,
  intentosUsados,
  puedePagar,
  urlDeCheckoutSegura,
} from './pagos';
import { Plazo, plazoRestante } from './plazo';

const AHORA = Date.parse('2026-10-26T15:00:00Z');
const HORA = 3_600_000;

function intento(numero: number, estado: EstadoIntento): IntentoPago {
  return {
    id: `2b7e3f10-5c4d-4a8e-9f61-0d2c3b4a5e6${numero}`,
    numero,
    estado,
    referencia: `ST-${numero}`,
    montoCentavos: 30000000,
    creadoEn: '2026-10-26T17:05:00Z',
  };
}

const VIGENTE: Plazo = plazoRestante(new Date(AHORA + 2 * HORA).toISOString(), AHORA);
const VENCIDO: Plazo = plazoRestante(new Date(AHORA - HORA).toISOString(), AHORA);
const SIN_PLAZO: Plazo = plazoRestante(undefined, AHORA);

describe('intentos de pago', () => {
  it('el límite son tres intentos con resultado de la pasarela (RF-58)', () => {
    expect(INTENTOS_MAXIMOS).toBe(3);
  });

  it('sin intentos quedan los tres', () => {
    expect(intentosUsados([])).toBe(0);
    expect(intentosRestantes([])).toBe(3);
  });

  it('un rechazo y un error cuentan; uno abandonado o en curso no', () => {
    const intentos = [
      intento(1, 'RECHAZADO'),
      intento(2, 'ERROR'),
      intento(3, 'ABANDONADO'),
      intento(4, 'EN_CURSO'),
    ];

    expect(intentosUsados(intentos)).toBe(2);
    expect(intentosRestantes(intentos)).toBe(1);
  });

  it('con tres rechazos no queda ninguno y nunca baja de cero', () => {
    const tres = [intento(1, 'RECHAZADO'), intento(2, 'RECHAZADO'), intento(3, 'ERROR')];

    expect(intentosRestantes(tres)).toBe(0);
    expect(intentosRestantes([...tres, intento(4, 'RECHAZADO')])).toBe(0);
  });

  it('el intento en curso es el más reciente que sigue abierto', () => {
    expect(intentoEnCurso([intento(1, 'RECHAZADO'), intento(2, 'EN_CURSO')])?.numero).toBe(2);
    expect(intentoEnCurso([intento(1, 'RECHAZADO'), intento(2, 'ABANDONADO')])).toBeNull();
    expect(intentoEnCurso([])).toBeNull();
  });

  it('el intento aprobado trae la referencia del pago', () => {
    expect(intentoAprobado([intento(1, 'RECHAZADO'), intento(2, 'APROBADO')])?.numero).toBe(2);
    expect(intentoAprobado([intento(1, 'RECHAZADO')])).toBeNull();
  });
});

describe('puedePagar', () => {
  it('una solicitud confirmada, con intentos y plazo, se puede pagar', () => {
    expect(puedePagar('CONFIRMADA', [], VIGENTE)).toBe(true);
  });

  it('con el pago fallido se puede reintentar mientras queden intentos y plazo', () => {
    expect(puedePagar('PAGO_FALLIDO', [intento(1, 'RECHAZADO')], VIGENTE)).toBe(true);
    expect(puedePagar('PAGO_FALLIDO', [intento(1, 'RECHAZADO'), intento(2, 'ERROR')], VIGENTE)).toBe(true);
  });

  it('sin intentos no se puede', () => {
    const tres = [intento(1, 'RECHAZADO'), intento(2, 'RECHAZADO'), intento(3, 'RECHAZADO')];

    expect(puedePagar('PAGO_FALLIDO', tres, VIGENTE)).toBe(false);
  });

  it('con el plazo terminado no se puede', () => {
    expect(puedePagar('CONFIRMADA', [], VENCIDO)).toBe(false);
    expect(puedePagar('PAGO_FALLIDO', [intento(1, 'RECHAZADO')], VENCIDO)).toBe(false);
  });

  it('si el plazo no viene lo decide el servidor', () => {
    expect(puedePagar('CONFIRMADA', [], SIN_PLAZO)).toBe(true);
  });

  it('con un intento en curso se puede: abrir el pago devuelve ese mismo intento', () => {
    expect(puedePagar('CONFIRMADA', [intento(1, 'EN_CURSO')], VIGENTE)).toBe(true);
  });

  it.each(['PENDIENTE', 'ATENDIDA', 'NO_DISPONIBLE', 'SIN_RESPUESTA', 'DECLINADA', 'PAGADA', 'VENCIDA'] as const)(
    'una solicitud %s no se paga',
    (estado) => {
      expect(puedePagar(estado, [], VIGENTE)).toBe(false);
    },
  );
});

describe('avisoDeIntentos', () => {
  beforeEach(() => establecerIdioma('es', { recordar: false }));

  it('con los tres intentos libres dice cuántos quedan, sin aviso de último', () => {
    const aviso = avisoDeIntentos([]);

    expect(aviso.restantes).toBe(3);
    expect(aviso.texto).toBe('Te quedan 3 intentos de pago');
    expect(aviso.ultimo).toBeNull();
  });

  it('con dos intentos todavía no avisa del último', () => {
    const aviso = avisoDeIntentos([intento(1, 'RECHAZADO')]);

    expect(aviso.texto).toBe('Te quedan 2 intentos de pago');
    expect(aviso.ultimo).toBeNull();
  });

  it('antes del último intento avisa que si se rechaza la solicitud vencerá, y usa el singular', () => {
    const aviso = avisoDeIntentos([intento(1, 'RECHAZADO'), intento(2, 'ERROR')]);

    expect(aviso.restantes).toBe(1);
    expect(aviso.texto).toBe('Te queda 1 intento de pago');
    expect(aviso.ultimo).toBe(
      'Este es tu último intento: si el pago se rechaza, la solicitud vencerá.',
    );
  });

  it('sin intentos lo dice y no repite el aviso del último', () => {
    const aviso = avisoDeIntentos([intento(1, 'RECHAZADO'), intento(2, 'RECHAZADO'), intento(3, 'RECHAZADO')]);

    expect(aviso.restantes).toBe(0);
    expect(aviso.texto).toBe('No te quedan intentos de pago.');
    expect(aviso.ultimo).toBeNull();
  });

  it('también está en inglés, con plural', () => {
    establecerIdioma('en', { recordar: false });

    expect(avisoDeIntentos([]).texto).toBe('You have 3 payment attempts left');
    const ultimo = avisoDeIntentos([intento(1, 'RECHAZADO'), intento(2, 'RECHAZADO')]);
    expect(ultimo.texto).toBe('You have 1 payment attempt left');
    expect(ultimo.ultimo).toBe(
      'This is your last attempt: if the payment is rejected, the request will expire.',
    );
  });
});

describe('textos de los intentos', () => {
  beforeEach(() => establecerIdioma('es', { recordar: false }));

  it('cada estado tiene su nombre, su motivo y su color', () => {
    const estados: EstadoIntento[] = ['EN_CURSO', 'APROBADO', 'RECHAZADO', 'ERROR', 'ABANDONADO'];

    for (const estado of estados) {
      expect(ETIQUETA_INTENTO[estado]).toBeTruthy();
      expect(MOTIVO_INTENTO[estado]).toBeTruthy();
      expect(CLASE_DE_INTENTO[estado]).toMatch(/^etiqueta--/);
    }
  });

  it('el motivo del error dice que puede reintentar y que la confirmación del banco puede tardar', () => {
    expect(MOTIVO_INTENTO.ERROR).toBe(
      'Hubo un error al procesar el pago. Puedes intentarlo de nuevo. Si ves un cobro en tu banco, la confirmación puede tardar unos minutos.',
    );
  });

  it('el motivo del rechazo aclara que no se cobró nada', () => {
    expect(MOTIVO_INTENTO.RECHAZADO).toContain('No se te cobró nada');
  });

  it('el motivo del error también está en inglés', () => {
    establecerIdioma('en', { recordar: false });

    expect(MOTIVO_INTENTO.ERROR).toBe(
      'There was an error processing the payment. You can try again. If you see a charge from your bank, the confirmation may take a few minutes.',
    );
    expect(ETIQUETA_INTENTO.ERROR).toBe('Failed');
  });
});

describe('urlDeCheckoutSegura', () => {
  it('acepta la pasarela simulada del prototipo (http://localhost)', () => {
    expect(
      urlDeCheckoutSegura({ url: 'http://localhost:8080/api/nucleo/pasarela-simulada/ST-1?monto=30000000' }),
    ).toBe('http://localhost:8080/api/nucleo/pasarela-simulada/ST-1?monto=30000000');
  });

  it('acepta el Web Checkout de Wompi', () => {
    const url =
      'https://checkout.wompi.co/p/?public-key=pub_test_X&currency=COP&amount-in-cents=30000000&reference=ST-1&signature:integrity=9a1f';

    expect(urlDeCheckoutSegura({ url })).toBe(url);
  });

  it.each([
    'javascript:alert(1)',
    'data:text/html,<script>1</script>',
    'ftp://pasarela.example/pago',
    '//pasarela.example/pago',
    '/api/nucleo/pasarela-simulada/ST-1',
    'checkout sin protocolo',
    'https://usuario:clave@pasarela.example/pago',
    '',
    '   ',
  ])('rechaza %j', (url) => {
    expect(urlDeCheckoutSegura({ url })).toBeNull();
  });

  it('rechaza un checkout ausente', () => {
    expect(urlDeCheckoutSegura(null)).toBeNull();
    expect(urlDeCheckoutSegura(undefined)).toBeNull();
  });
});

describe('esperaDeReconsulta', () => {
  it('los primeros 2 minutos reconsulta cada 3 segundos', () => {
    expect(ESPERA_RAPIDA_MS).toBe(3_000);
    expect(esperaDeReconsulta(0)).toBe(3_000);
    expect(esperaDeReconsulta(60_000)).toBe(3_000);
    expect(esperaDeReconsulta(VENTANA_RAPIDA_MS - 1)).toBe(3_000);
  });

  it('después de 2 minutos reconsulta cada 15 segundos', () => {
    expect(VENTANA_RAPIDA_MS).toBe(120_000);
    expect(ESPERA_LENTA_MS).toBe(15_000);
    expect(esperaDeReconsulta(VENTANA_RAPIDA_MS)).toBe(15_000);
    expect(esperaDeReconsulta(10 * 60_000)).toBe(15_000);
  });
});

describe('estaConfirmandoPago', () => {
  it('una solicitud sin pagar con un intento en curso está esperando la confirmación', () => {
    expect(estaConfirmandoPago('CONFIRMADA', [intento(1, 'EN_CURSO')])).toBe(true);
    expect(estaConfirmandoPago('PAGO_FALLIDO', [intento(1, 'RECHAZADO'), intento(2, 'EN_CURSO')])).toBe(true);
  });

  it('sin un intento en curso no espera nada', () => {
    expect(estaConfirmandoPago('CONFIRMADA', [])).toBe(false);
    expect(estaConfirmandoPago('PAGO_FALLIDO', [intento(1, 'RECHAZADO')])).toBe(false);
  });

  it('una solicitud pagada o cerrada ya no espera, aunque quede un intento marcado en curso', () => {
    expect(estaConfirmandoPago('PAGADA', [intento(1, 'EN_CURSO')])).toBe(false);
    expect(estaConfirmandoPago('VENCIDA', [intento(1, 'EN_CURSO')])).toBe(false);
  });
});

describe('centavosAPesos', () => {
  it('convierte los centavos de la pasarela a pesos', () => {
    expect(centavosAPesos(30000000)).toBe(300000);
    expect(centavosAPesos(0)).toBe(0);
  });
});

describe('mostrarIntentos', () => {
  it.each(['CONFIRMADA', 'PAGO_FALLIDO', 'PAGADA', 'VENCIDA'] as const)('con la solicitud %s se piden los intentos', (estado) => {
    expect(mostrarIntentos(estado)).toBe(true);
  });

  it.each(['PENDIENTE', 'ATENDIDA', 'NO_DISPONIBLE', 'SIN_RESPUESTA', 'DECLINADA'] as const)(
    'con la solicitud %s no se piden',
    (estado) => {
      expect(mostrarIntentos(estado)).toBe(false);
    },
  );
});
