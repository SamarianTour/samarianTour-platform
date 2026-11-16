import { diccionario } from './diccionario';

/**
 * Textos de las páginas legales del pie: términos y condiciones, protección al consumidor, PQRSF y la política de datos
 * (la política en sí la entrega el backend; aquí solo su marco).
 *
 * [SUPUESTO] Son textos de demostración de un prototipo académico, no revisados por un abogado (el aviso repite la frase de
 * la política del backend). No fijan plazos ni compromisos legales.
 * [SUPUESTO pendiente del equipo] PQRSF: el plazo de respuesta no está definido y por eso la página no promete ninguno.
 */
export const LEGAL = diccionario(
  {
    'legal.aviso': 'Texto de demostración del prototipo académico. No ha sido revisado por un abogado.',
    'legal.contacto': 'Para cualquier duda, escríbenos a',

    // Términos y condiciones
    'legal.terminos.titulo': 'Términos y condiciones',
    'legal.terminos.que.titulo': 'Qué es Samarian Tour',
    'legal.terminos.que.p1':
      'Samarian Tour es una plataforma de turismo sostenible en Santa Marta. Conecta a turistas con prestadores de servicios turísticos verificados ante el RNT (Registro Nacional de Turismo).',
    'legal.terminos.que.p2': 'Los servicios turísticos los presta cada prestador, no la plataforma.',
    'legal.terminos.solicitud.titulo': 'Una solicitud de disponibilidad no es una reserva',
    'legal.terminos.solicitud.p1':
      'Cuando envías una solicitud de disponibilidad, solo pides que se confirme si hay cupo. No hay reserva inmediata.',
    'legal.terminos.solicitud.p2':
      'Un operador de disponibilidad confirma el cupo con el prestador. Si hay cupo, tú decides si confirmas las condiciones y pagas.',
    'legal.terminos.pago.titulo': 'El pago',
    'legal.terminos.pago.p1': 'El pago se hace en una pasarela de pago externa a Samarian Tour.',
    'legal.terminos.comision.titulo': 'La comisión',
    'legal.terminos.comision.p1':
      'La comisión de la plataforma se descuenta al prestador. El turista no paga cargos adicionales a Samarian Tour.',

    // Protección al consumidor
    'legal.proteccion.titulo': 'Protección al consumidor',
    'legal.proteccion.precio.titulo': 'Información clara del precio',
    'legal.proteccion.precio.p1':
      'Cada oferta muestra su precio de referencia en pesos colombianos, con su unidad (por ejemplo, por persona). Pagas el precio acordado, sin cargos ocultos.',
    'legal.proteccion.estatuto.titulo': 'Tus derechos como consumidor',
    'legal.proteccion.estatuto.p1':
      'En Colombia, los derechos de los consumidores están regulados por el Estatuto del Consumidor (Ley 1480 de 2011).',
    'legal.proteccion.autoridad.titulo': 'Autoridad',
    'legal.proteccion.autoridad.p1':
      'La Superintendencia de Industria y Comercio (SIC) es la autoridad de protección al consumidor en Colombia.',
    'legal.proteccion.problema.titulo': '¿Tienes un problema?',
    'legal.proteccion.problema.p1':
      'Cuéntanos qué pasó por correo. También puedes acudir a la Superintendencia de Industria y Comercio.',

    // PQRSF
    'legal.pqrsf.titulo': 'PQRSF',
    'legal.pqrsf.letras.titulo': '¿Qué es cada letra?',
    'legal.pqrsf.p': 'Petición: pedir información o una gestión.',
    'legal.pqrsf.q': 'Queja: inconformidad con la atención de una persona.',
    'legal.pqrsf.r': 'Reclamo: inconformidad con un servicio o con un cobro.',
    'legal.pqrsf.s': 'Sugerencia: una idea para mejorar la plataforma.',
    'legal.pqrsf.f': 'Felicitación: reconocer algo que salió bien.',
    'legal.pqrsf.como.titulo': 'Cómo escribirnos',
    'legal.pqrsf.como.p1': 'Envía tu mensaje por correo electrónico a la dirección de contacto que aparece abajo.',
    'legal.pqrsf.datos.titulo': 'Qué datos incluir',
    'legal.pqrsf.datos.nombre': 'Tu nombre y el correo con el que te registraste, si tienes cuenta.',
    'legal.pqrsf.datos.tipo': 'El tipo: petición, queja, reclamo, sugerencia o felicitación.',
    'legal.pqrsf.datos.que': 'Qué pasó, con el mayor detalle posible.',
    'legal.pqrsf.datos.oferta': 'La oferta o el número de la solicitud, si aplica.',
    'legal.pqrsf.datos.fecha': 'La fecha en que ocurrió.',

    // Política de datos (el texto lo entrega el backend)
    'legal.politica.titulo': 'Política de tratamiento de datos',
    'legal.politica.ley': 'Ley 1581 de 2012',
  },
  {
    'legal.aviso': 'Demo text for the academic prototype. It has not been reviewed by a lawyer.',
    'legal.contacto': 'If you have any questions, write to us at',

    // Terms and conditions
    'legal.terminos.titulo': 'Terms and conditions',
    'legal.terminos.que.titulo': 'What Samarian Tour is',
    'legal.terminos.que.p1':
      'Samarian Tour is a sustainable tourism platform in Santa Marta. It connects tourists with tourism service providers verified with the RNT (National Tourism Registry).',
    'legal.terminos.que.p2': 'Each provider delivers its tourism services, not the platform.',
    'legal.terminos.solicitud.titulo': 'An availability request is not a booking',
    'legal.terminos.solicitud.p1':
      'When you send an availability request, you are only asking for availability to be confirmed. There is no instant booking.',
    'legal.terminos.solicitud.p2':
      'An availability operator confirms availability with the provider. If there is availability, you decide whether to accept the terms and pay.',
    'legal.terminos.pago.titulo': 'Payment',
    'legal.terminos.pago.p1': 'Payment is made through a payment gateway that is external to Samarian Tour.',
    'legal.terminos.comision.titulo': 'Commission',
    'legal.terminos.comision.p1':
      'The platform commission is deducted from the provider. Tourists pay no extra charges to Samarian Tour.',

    // Consumer protection
    'legal.proteccion.titulo': 'Consumer protection',
    'legal.proteccion.precio.titulo': 'Clear price information',
    'legal.proteccion.precio.p1':
      'Every offer shows its reference price in Colombian pesos, with its unit (for example, per person). You pay the agreed price, with no hidden charges.',
    'legal.proteccion.estatuto.titulo': 'Your rights as a consumer',
    'legal.proteccion.estatuto.p1':
      'In Colombia, consumer rights are regulated by the Consumer Statute (Law 1480 of 2011).',
    'legal.proteccion.autoridad.titulo': 'Authority',
    'legal.proteccion.autoridad.p1':
      'The Superintendence of Industry and Commerce (SIC) is the consumer protection authority in Colombia.',
    'legal.proteccion.problema.titulo': 'Having a problem?',
    'legal.proteccion.problema.p1':
      'Tell us what happened by email. You can also go to the Superintendence of Industry and Commerce.',

    // PQRSF
    'legal.pqrsf.titulo': 'PQRSF',
    'legal.pqrsf.letras.titulo': 'What does each letter mean?',
    'legal.pqrsf.p': 'P, request: asking for information or for an action.',
    'legal.pqrsf.q': 'Q, complaint: dissatisfaction with how a person treated you.',
    'legal.pqrsf.r': 'R, claim: dissatisfaction with a service or with a charge.',
    'legal.pqrsf.s': 'S, suggestion: an idea to improve the platform.',
    'legal.pqrsf.f': 'F, compliment: recognizing something that went well.',
    'legal.pqrsf.como.titulo': 'How to write to us',
    'legal.pqrsf.como.p1': 'Send your message by email to the contact address below.',
    'legal.pqrsf.datos.titulo': 'What to include',
    'legal.pqrsf.datos.nombre': 'Your name and the email you signed up with, if you have an account.',
    'legal.pqrsf.datos.tipo': 'The type: request, complaint, claim, suggestion or compliment.',
    'legal.pqrsf.datos.que': 'What happened, in as much detail as possible.',
    'legal.pqrsf.datos.oferta': 'The offer or the availability request number, if it applies.',
    'legal.pqrsf.datos.fecha': 'The date it happened.',

    // Data processing policy (the text comes from the backend)
    'legal.politica.titulo': 'Data processing policy',
    'legal.politica.ley': 'Law 1581 of 2012',
  },
);
