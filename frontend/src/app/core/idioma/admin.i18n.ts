import { diccionario } from './diccionario';

/**
 * Textos del panel del administrador (P05): su menú lateral y las pantallas de prestadores y RNT, moderación, cuentas internas,
 * comisión y pagos, zonas, alertas de sesgo y pendientes técnicos. Los nombres de las pantallas están en `app.i18n.ts`
 * (`app.titulo.*`), porque son también el título de la pestaña del navegador. Los textos de cada pantalla se suman aquí.
 */
export const ADMIN = diccionario(
  {
    // Panel (menú lateral)
    'admin.panel.control': 'Panel del administrador',

    // Alertas de sesgo (RET-02)
    'admin.sesgo.intro':
      'La recomendación no debe mostrar a un prestador mucho más de lo que le corresponde por su peso en el catálogo. Cada día se compara, en los últimos 30 días, el porcentaje de apariciones de cada prestador con su porcentaje de las ofertas visibles; si el índice pasa de 1,5, se abre una alerta. Una alerta se cierra sola cuando el índice baja.',
    'admin.sesgo.cargando': 'Cargando las alertas…',
    'admin.sesgo.vacioTitulo': 'Sin alertas abiertas',
    'admin.sesgo.vacioAyuda':
      'Ningún prestador aparece en las recomendaciones más de lo que le corresponde.',
    'admin.sesgo.prestadorSinNombre': 'Prestador {codigo}',
    'admin.sesgo.indice': 'Índice {indice}',
    'admin.sesgo.explicacion': 'Aparece {veces} veces lo que le corresponde.',
    'admin.sesgo.porcentajeApariciones': 'Parte de las apariciones',
    'admin.sesgo.porcentajeOfertas': 'Parte de las ofertas visibles',
    'admin.sesgo.apariciones': {
      uno: '{n} aparición en {recomendaciones} recomendaciones',
      otros: '{n} apariciones en {recomendaciones} recomendaciones',
    },
    'admin.sesgo.abierta': 'Abierta',
    'admin.sesgo.actualizada': 'Actualizada',

    // P05.1 Prestadores y RNT
    'admin.prestadores.intro':
      'Los prestadores registrados, su estado frente al RNT y el operador de disponibilidad que atiende sus solicitudes. Un prestador sin operador rechaza las solicitudes, así que asigna uno antes de que llegue la primera.',
    'admin.prestadores.filtrar': 'Filtrar prestadores',
    'admin.prestadores.filtro.todos': 'Todos',
    'admin.prestadores.filtro.sinOperador': 'Sin operador',
    'admin.prestadores.total': { uno: '{n} prestador', otros: '{n} prestadores' },
    'admin.prestadores.cargando': 'Cargando los prestadores…',
    'admin.prestadores.vacioTitulo': 'No hay prestadores',
    'admin.prestadores.vacioAyuda': 'Aquí aparecerán los prestadores cuando registren su RNT.',
    'admin.prestadores.vacioSinOperadorTitulo': 'Todos tienen operador',
    'admin.prestadores.vacioSinOperadorAyuda': 'Ningún prestador está esperando que le asignes un operador.',
    'admin.prestadores.caption': 'Prestadores registrados',
    'admin.prestadores.columna.prestador': 'Prestador',
    'admin.prestadores.columna.estadoRnt': 'Estado del RNT',
    'admin.prestadores.columna.operador': 'Operador',
    'admin.prestadores.columna.registrado': 'Registrado',
    'admin.prestadores.rnt': 'RNT n.º {numero}',
    'admin.prestadores.sinRazonSocial': 'Sin razón social en el RNT',
    'admin.prestadores.operadorDe': 'Operador de {prestador}',
    'admin.prestadores.elige': 'Elige un operador',
    'admin.prestadores.desactivado': '{nombre} (desactivado)',
    'admin.prestadores.desconocido': 'Operador {codigo}',
    'admin.prestadores.asignar': 'Asignar',
    'admin.prestadores.asignando': 'Asignando…',
    'admin.prestadores.asignado': '{operador} atenderá las solicitudes nuevas de {prestador}.',
    'admin.prestadores.aviso':
      'La asignación rige para las solicitudes que se envíen desde ahora; las que ya están abiertas siguen con el operador que tenían.',
    'admin.prestadores.sinOperadores':
      'No hay operadores activos para asignar. Crea uno en Cuentas internas.',
    'admin.prestadores.errorOperadores':
      'No pudimos leer los operadores, así que por ahora no se puede asignar. Vuelve a cargar la página.',
    'admin.rnt.estado.ACTIVO': 'Activo',
    'admin.rnt.estado.INACTIVO': 'Inactivo',
    'admin.rnt.estado.NO_ENCONTRADO': 'No encontrado',
    'admin.rnt.estado.VERIFICACION_PENDIENTE': 'Verificación pendiente',

    // Sincronizar con el RNT (RF-76 a RF-78)
    'admin.rnt.sync.titulo': 'Sincronizar con el RNT',
    'admin.rnt.sync.texto':
      'Compara a cada prestador con el RNT. También corre sola cada lunes a las 3:00 (hora de Colombia). El prestador que pierde la habilitación sale del catálogo y se cierran sus solicitudes sin pagar; el que la recupera vuelve con sus ofertas aprobadas.',
    'admin.rnt.sync.boton': 'Sincronizar ahora',
    'admin.rnt.sync.sincronizando': 'Sincronizando…',
    'admin.rnt.sync.espera':
      'Esto puede tardar un poco. Puedes seguir en el panel; el resultado aparecerá aquí.',
    'admin.rnt.sync.dialogoTitulo': '¿Sincronizar con el RNT ahora?',
    'admin.rnt.sync.dialogoMensaje':
      'Los prestadores que ya no estén habilitados saldrán del catálogo y sus solicitudes sin pagar se cerrarán. La sincronización puede tardar.',
    'admin.rnt.sync.dialogoAccion': 'Sincronizar',
    'admin.rnt.sync.resultado': 'Resultado de la sincronización',
    'admin.rnt.sync.revisados': 'Revisados',
    'admin.rnt.sync.retirados': 'Retirados del catálogo',
    'admin.rnt.sync.restituidos': 'Restituidos',
    'admin.rnt.sync.sinRespuesta': 'Sin respuesta del RNT',
    'admin.rnt.sync.sinCambios': 'No hubo cambios en el catálogo.',
    'admin.rnt.sync.interrumpida':
      'El RNT dejó de responder y la sincronización se detuvo. Cada prestador conserva su último estado.',
    'admin.rnt.sync.conservaron':
      'Algunos prestadores conservaron su último estado porque el RNT no respondió.',
    'admin.rnt.sync.terminada':
      'Sincronización con el RNT terminada: {revisados} revisados, {retirados} retirados, {restituidos} restituidos.',

    // P05.2 Moderación (RF-79, RF-80)
    'admin.moderacion.intro':
      'Revisa lo que los prestadores y los turistas enviaron antes de que se publique. Decides aquí; el catálogo aplica la decisión en unos segundos. Un rechazo le llega al prestador o al turista con su motivo.',
    'admin.moderacion.cola': 'Elegir la cola de moderación',
    'admin.moderacion.cola.ofertas': 'Ofertas',
    'admin.moderacion.cola.opiniones': 'Opiniones',
    'admin.moderacion.totalOfertas': { uno: '{n} oferta pendiente', otros: '{n} ofertas pendientes' },
    'admin.moderacion.totalOpiniones': { uno: '{n} opinión pendiente', otros: '{n} opiniones pendientes' },
    'admin.moderacion.cargando': 'Cargando la cola…',
    'admin.moderacion.actualizar': 'Actualizar la lista',
    'admin.moderacion.vacioOfertasTitulo': 'No hay ofertas por revisar',
    'admin.moderacion.vacioOfertasAyuda':
      'Cuando un prestador publique o edite una oferta, aparecerá aquí.',
    'admin.moderacion.vacioOpinionesTitulo': 'No hay opiniones por revisar',
    'admin.moderacion.vacioOpinionesAyuda': 'Cuando un turista envíe una opinión, aparecerá aquí.',
    'admin.moderacion.oferta.version': 'Versión {numero}',
    'admin.moderacion.oferta.nueva': 'Oferta nueva',
    'admin.moderacion.oferta.edicion': 'Edición de una oferta publicada',
    'admin.moderacion.oferta.enviada': 'Enviada {fecha}',
    'admin.moderacion.oferta.sinRnt':
      'El prestador no tiene el RNT activo: no se puede aprobar esta oferta, solo rechazarla.',
    'admin.moderacion.oferta.leyenda':
      'Las líneas marcadas cambiaron frente a la versión publicada.',
    'admin.moderacion.cambio': 'Cambió',
    'admin.moderacion.fotos': { uno: '{n} foto', otros: '{n} fotos' },
    'admin.moderacion.fotoAlt': 'Foto {n} de {total}',
    'admin.moderacion.fotosCambiaron': 'Las fotos cambiaron.',
    'admin.moderacion.opinion.sobre': 'Sobre {oferta}',
    'admin.moderacion.opinion.sinNombre': 'Oferta {codigo}',
    'admin.moderacion.opinion.calificacion': 'Calificación: {n} de 5',
    'admin.moderacion.opinion.enviada': 'Enviada {fecha}',
    'admin.moderacion.si': 'Sí',
    'admin.moderacion.no': 'No',
    'admin.moderacion.actividades': { uno: '{n} actividad', otros: '{n} actividades' },
    'admin.moderacion.campo.nombre': 'Nombre',
    'admin.moderacion.campo.descripcion': 'Descripción',
    'admin.moderacion.campo.precio': 'Precio de referencia',
    'admin.moderacion.campo.zona': 'Zona',
    'admin.moderacion.campo.capacidad': 'Capacidad máxima',
    'admin.moderacion.campo.horarios': 'Horarios',
    'admin.moderacion.campo.comoLlegar': 'Cómo llegar',
    'admin.moderacion.campo.ubicacion': 'Ubicación',
    'admin.moderacion.campo.tipoActividad': 'Tipo de actividad',
    'admin.moderacion.campo.nivelEsfuerzo': 'Nivel de esfuerzo',
    'admin.moderacion.campo.accesible': 'Accesible en silla de ruedas',
    'admin.moderacion.campo.horariosSalida': 'Horas de salida',
    'admin.moderacion.campo.puntoEncuentro': 'Punto de encuentro',
    'admin.moderacion.campo.duracion': 'Duración',
    'admin.moderacion.campo.modalidad': 'Modalidad',
    'admin.moderacion.campo.idiomas': 'Idiomas de atención',
    'admin.moderacion.campo.actividades': 'Actividades incluidas',
    'admin.moderacion.campo.tipoUnidad': 'Tipo de unidad',
    'admin.moderacion.aprobar': 'Aprobar',
    'admin.moderacion.rechazar': 'Rechazar',
    'admin.moderacion.motivo': 'Motivo del rechazo',
    'admin.moderacion.motivoAyudaOferta':
      'Se le notifica al prestador con este texto. ({largo} de {maximo})',
    'admin.moderacion.motivoAyudaOpinion':
      'Se le notifica al turista con este texto. ({largo} de {maximo})',
    'admin.moderacion.confirmarRechazo': 'Confirmar rechazo',
    'admin.moderacion.enviando': 'Enviando…',
    'admin.moderacion.error.motivoRequerido': 'Escribe el motivo del rechazo.',
    'admin.moderacion.error.motivoLargo': 'El motivo admite hasta {maximo} caracteres.',
    'admin.moderacion.enviada.APROBAR': 'Aprobación enviada',
    'admin.moderacion.enviada.RECHAZAR': 'Rechazo enviado',
    'admin.moderacion.enviada.texto':
      'La decisión llega al catálogo en unos segundos, por la bandeja de salida. Actualiza la lista para ver que ya no aparece.',
    'admin.moderacion.aviso.APROBAR':
      'Enviaste la aprobación. El catálogo la aplicará en unos segundos.',
    'admin.moderacion.aviso.RECHAZAR':
      'Enviaste el rechazo con su motivo. El catálogo lo aplicará en unos segundos.',
  },
  {
    'admin.panel.control': 'Administrator panel',

    'admin.sesgo.intro':
      "The recommendations should not show a provider much more than its weight in the catalog deserves. Every day, over the last 30 days, each provider's share of appearances is compared with its share of the visible offers; if the index goes above 1.5, an alert opens. An alert closes by itself when the index drops.",
    'admin.sesgo.cargando': 'Loading the alerts…',
    'admin.sesgo.vacioTitulo': 'No open alerts',
    'admin.sesgo.vacioAyuda': 'No provider appears in the recommendations more than it should.',
    'admin.sesgo.prestadorSinNombre': 'Provider {codigo}',
    'admin.sesgo.indice': 'Index {indice}',
    'admin.sesgo.explicacion': 'Appears {veces} times its fair share.',
    'admin.sesgo.porcentajeApariciones': 'Share of appearances',
    'admin.sesgo.porcentajeOfertas': 'Share of visible offers',
    'admin.sesgo.apariciones': {
      uno: '{n} appearance in {recomendaciones} recommendations',
      otros: '{n} appearances in {recomendaciones} recommendations',
    },
    'admin.sesgo.abierta': 'Opened',
    'admin.sesgo.actualizada': 'Updated',

    'admin.prestadores.intro':
      'The registered providers, their status in the RNT and the availability operator who handles their requests. A provider without an operator rejects requests, so assign one before the first arrives.',
    'admin.prestadores.filtrar': 'Filter providers',
    'admin.prestadores.filtro.todos': 'All',
    'admin.prestadores.filtro.sinOperador': 'No operator',
    'admin.prestadores.total': { uno: '{n} provider', otros: '{n} providers' },
    'admin.prestadores.cargando': 'Loading the providers…',
    'admin.prestadores.vacioTitulo': 'There are no providers',
    'admin.prestadores.vacioAyuda': 'Providers will appear here when they register their RNT.',
    'admin.prestadores.vacioSinOperadorTitulo': 'Everyone has an operator',
    'admin.prestadores.vacioSinOperadorAyuda': 'No provider is waiting for you to assign an operator.',
    'admin.prestadores.caption': 'Registered providers',
    'admin.prestadores.columna.prestador': 'Provider',
    'admin.prestadores.columna.estadoRnt': 'RNT status',
    'admin.prestadores.columna.operador': 'Operator',
    'admin.prestadores.columna.registrado': 'Registered',
    'admin.prestadores.rnt': 'RNT no. {numero}',
    'admin.prestadores.sinRazonSocial': 'No business name in the RNT',
    'admin.prestadores.operadorDe': 'Operator of {prestador}',
    'admin.prestadores.elige': 'Choose an operator',
    'admin.prestadores.desactivado': '{nombre} (deactivated)',
    'admin.prestadores.desconocido': 'Operator {codigo}',
    'admin.prestadores.asignar': 'Assign',
    'admin.prestadores.asignando': 'Assigning…',
    'admin.prestadores.asignado': '{operador} will handle the new requests from {prestador}.',
    'admin.prestadores.aviso':
      'The assignment applies to requests sent from now on; those already open stay with the operator they had.',
    'admin.prestadores.sinOperadores':
      'There are no active operators to assign. Create one in Internal accounts.',
    'admin.prestadores.errorOperadores':
      "We couldn't load the operators, so assigning is not possible for now. Reload the page.",
    'admin.rnt.estado.ACTIVO': 'Active',
    'admin.rnt.estado.INACTIVO': 'Inactive',
    'admin.rnt.estado.NO_ENCONTRADO': 'Not found',
    'admin.rnt.estado.VERIFICACION_PENDIENTE': 'Verification pending',

    'admin.rnt.sync.titulo': 'Synchronize with the RNT',
    'admin.rnt.sync.texto':
      'Compares each provider against the RNT. It also runs by itself every Monday at 3:00 (Colombia time). A provider that loses its authorization leaves the catalog and its unpaid requests are closed; one that recovers it comes back with its approved offers.',
    'admin.rnt.sync.boton': 'Synchronize now',
    'admin.rnt.sync.sincronizando': 'Synchronizing…',
    'admin.rnt.sync.espera':
      'This may take a while. You can keep using the panel; the result will appear here.',
    'admin.rnt.sync.dialogoTitulo': 'Synchronize with the RNT now?',
    'admin.rnt.sync.dialogoMensaje':
      'Providers that are no longer authorized will leave the catalog and their unpaid requests will be closed. The synchronization may take a while.',
    'admin.rnt.sync.dialogoAccion': 'Synchronize',
    'admin.rnt.sync.resultado': 'Synchronization result',
    'admin.rnt.sync.revisados': 'Reviewed',
    'admin.rnt.sync.retirados': 'Removed from the catalog',
    'admin.rnt.sync.restituidos': 'Restored',
    'admin.rnt.sync.sinRespuesta': 'No answer from the RNT',
    'admin.rnt.sync.sinCambios': 'There were no changes in the catalog.',
    'admin.rnt.sync.interrumpida':
      'The RNT stopped responding and the synchronization stopped. Each provider keeps its last status.',
    'admin.rnt.sync.conservaron':
      'Some providers kept their last status because the RNT did not respond.',
    'admin.rnt.sync.terminada':
      'Synchronization with the RNT finished: {revisados} reviewed, {retirados} removed, {restituidos} restored.',

    'admin.moderacion.intro':
      'Review what providers and tourists submitted before it is published. You decide here; the catalog applies the decision within a few seconds. A rejection reaches the provider or tourist with its reason.',
    'admin.moderacion.cola': 'Choose the moderation queue',
    'admin.moderacion.cola.ofertas': 'Offers',
    'admin.moderacion.cola.opiniones': 'Reviews',
    'admin.moderacion.totalOfertas': { uno: '{n} offer pending', otros: '{n} offers pending' },
    'admin.moderacion.totalOpiniones': { uno: '{n} review pending', otros: '{n} reviews pending' },
    'admin.moderacion.cargando': 'Loading the queue…',
    'admin.moderacion.actualizar': 'Refresh the list',
    'admin.moderacion.vacioOfertasTitulo': 'There are no offers to review',
    'admin.moderacion.vacioOfertasAyuda':
      'When a provider publishes or edits an offer, it will appear here.',
    'admin.moderacion.vacioOpinionesTitulo': 'There are no reviews to review',
    'admin.moderacion.vacioOpinionesAyuda': 'When a tourist submits a review, it will appear here.',
    'admin.moderacion.oferta.version': 'Version {numero}',
    'admin.moderacion.oferta.nueva': 'New offer',
    'admin.moderacion.oferta.edicion': 'Edit of a published offer',
    'admin.moderacion.oferta.enviada': 'Submitted {fecha}',
    'admin.moderacion.oferta.sinRnt':
      "The provider's RNT is not active: this offer cannot be approved, only rejected.",
    'admin.moderacion.oferta.leyenda':
      'The marked lines changed from the published version.',
    'admin.moderacion.cambio': 'Changed',
    'admin.moderacion.fotos': { uno: '{n} photo', otros: '{n} photos' },
    'admin.moderacion.fotoAlt': 'Photo {n} of {total}',
    'admin.moderacion.fotosCambiaron': 'The photos changed.',
    'admin.moderacion.opinion.sobre': 'About {oferta}',
    'admin.moderacion.opinion.sinNombre': 'Offer {codigo}',
    'admin.moderacion.opinion.calificacion': 'Rating: {n} of 5',
    'admin.moderacion.opinion.enviada': 'Submitted {fecha}',
    'admin.moderacion.si': 'Yes',
    'admin.moderacion.no': 'No',
    'admin.moderacion.actividades': { uno: '{n} activity', otros: '{n} activities' },
    'admin.moderacion.campo.nombre': 'Name',
    'admin.moderacion.campo.descripcion': 'Description',
    'admin.moderacion.campo.precio': 'Reference price',
    'admin.moderacion.campo.zona': 'Area',
    'admin.moderacion.campo.capacidad': 'Maximum capacity',
    'admin.moderacion.campo.horarios': 'Opening hours',
    'admin.moderacion.campo.comoLlegar': 'How to get there',
    'admin.moderacion.campo.ubicacion': 'Location',
    'admin.moderacion.campo.tipoActividad': 'Activity type',
    'admin.moderacion.campo.nivelEsfuerzo': 'Effort level',
    'admin.moderacion.campo.accesible': 'Wheelchair accessible',
    'admin.moderacion.campo.horariosSalida': 'Departure times',
    'admin.moderacion.campo.puntoEncuentro': 'Meeting point',
    'admin.moderacion.campo.duracion': 'Duration',
    'admin.moderacion.campo.modalidad': 'Mode',
    'admin.moderacion.campo.idiomas': 'Service languages',
    'admin.moderacion.campo.actividades': 'Included activities',
    'admin.moderacion.campo.tipoUnidad': 'Unit type',
    'admin.moderacion.aprobar': 'Approve',
    'admin.moderacion.rechazar': 'Reject',
    'admin.moderacion.motivo': 'Reason for rejection',
    'admin.moderacion.motivoAyudaOferta':
      'The provider is notified with this text. ({largo} of {maximo})',
    'admin.moderacion.motivoAyudaOpinion':
      'The tourist is notified with this text. ({largo} of {maximo})',
    'admin.moderacion.confirmarRechazo': 'Confirm rejection',
    'admin.moderacion.enviando': 'Sending…',
    'admin.moderacion.error.motivoRequerido': 'Enter the reason for the rejection.',
    'admin.moderacion.error.motivoLargo': 'The reason allows up to {maximo} characters.',
    'admin.moderacion.enviada.APROBAR': 'Approval sent',
    'admin.moderacion.enviada.RECHAZAR': 'Rejection sent',
    'admin.moderacion.enviada.texto':
      'The decision reaches the catalog in a few seconds, through the outbox. Refresh the list to see it disappear.',
    'admin.moderacion.aviso.APROBAR':
      'You sent the approval. The catalog will apply it in a few seconds.',
    'admin.moderacion.aviso.RECHAZAR':
      'You sent the rejection with its reason. The catalog will apply it in a few seconds.',
  },
);
