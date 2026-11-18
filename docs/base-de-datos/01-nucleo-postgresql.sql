-- =====================================================================================================================
-- Samarian Tour · Núcleo transaccional · Base de datos PostgreSQL 17
-- Esquema completo y datos base: el resultado de aplicar las migraciones de Flyway V1 a V17
-- (servicios/nucleo/src/main/resources/db/migration), generado el 17 de noviembre de 2026.
--
-- Uso normal: no hace falta correr este script. Al arrancar, el núcleo aplica las migraciones solo (Flyway), también
-- en Docker Compose. Este archivo sirve para revisar el esquema o crear la base a mano:
--   createdb -U postgres nucleo
--   psql -U postgres -d nucleo -f 01-nucleo-postgresql.sql
--
-- Lo que este script NO crea:
--   - La cuenta del administrador inicial. La crea la migración V9, que es en Java porque guarda el nombre cifrado con
--     la clave de cada instalación (NUCLEO_CIFRADO_CLAVE): admin@samariantour.co. Ver 03-administrador-inicial.md.
--   - La tabla flyway_schema_history. Si la base se crea con este script y después arranca el núcleo, Flyway intentará
--     aplicar las migraciones otra vez: para usarlas juntas, cree la base vacía y deje que el núcleo la prepare.
--   - Los datos de la demostración (prestadores, cuentas demo): los carga el perfil "demo" del núcleo al arrancar.
-- =====================================================================================================================

--
-- PostgreSQL database dump
--

\restrict R0AGewoNuAjcf0NFunIt5qrhMCeOvG7uLGbi0gnucjWQYGMIAASUril3MGsVNQx

-- Dumped from database version 17.11 (Debian 17.11-1.pgdg13+2)
-- Dumped by pg_dump version 17.11 (Debian 17.11-1.pgdg13+2)

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET transaction_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: aceptaciones_politica; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.aceptaciones_politica (
    usuario_id uuid NOT NULL,
    version character varying(20) NOT NULL,
    fecha_aceptacion timestamp with time zone NOT NULL
);


--
-- Name: alertas_sesgo; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.alertas_sesgo (
    id uuid NOT NULL,
    prestador_id uuid NOT NULL,
    indice numeric(8,2) NOT NULL,
    porcentaje_apariciones numeric(5,2) NOT NULL,
    porcentaje_ofertas numeric(5,2) NOT NULL,
    apariciones bigint NOT NULL,
    recomendaciones bigint NOT NULL,
    abierta_en timestamp with time zone NOT NULL,
    actualizada_en timestamp with time zone NOT NULL,
    cerrada_en timestamp with time zone
);


--
-- Name: auditoria_prestadores; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.auditoria_prestadores (
    id uuid NOT NULL,
    prestador_id uuid NOT NULL,
    accion character varying(50) NOT NULL,
    detalle character varying(300) NOT NULL,
    ocurrido_en timestamp with time zone NOT NULL
);


--
-- Name: bandeja_salida; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.bandeja_salida (
    id_aviso character varying(200) NOT NULL,
    destino character varying(20) NOT NULL,
    cuerpo jsonb NOT NULL,
    estado character varying(20) NOT NULL,
    creado_en timestamp with time zone NOT NULL,
    proximo_intento timestamp with time zone NOT NULL,
    intentos integer NOT NULL,
    enviado_en timestamp with time zone,
    ultimo_error character varying(1000),
    CONSTRAINT bandeja_salida_destino CHECK (((destino)::text = ANY ((ARRAY['OFERTAS'::character varying, 'OPINIONES'::character varying])::text[]))),
    CONSTRAINT bandeja_salida_estado CHECK (((estado)::text = ANY ((ARRAY['PENDIENTE'::character varying, 'ENVIADO'::character varying, 'RECHAZADO'::character varying])::text[])))
);


--
-- Name: claves_idempotencia; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.claves_idempotencia (
    usuario_id uuid NOT NULL,
    clave uuid NOT NULL,
    operacion character varying(100) NOT NULL,
    huella character varying(64) NOT NULL,
    codigo_respuesta integer,
    cuerpo_respuesta text,
    creada_en timestamp with time zone NOT NULL,
    expira_en timestamp with time zone NOT NULL
);


--
-- Name: claves_idempotencia_pagos; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.claves_idempotencia_pagos (
    usuario_id uuid NOT NULL,
    clave uuid NOT NULL,
    operacion character varying(100) NOT NULL,
    huella character varying(64) NOT NULL,
    codigo_respuesta integer,
    cuerpo_respuesta text,
    creada_en timestamp with time zone NOT NULL,
    expira_en timestamp with time zone NOT NULL
);


--
-- Name: codigos_un_uso; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.codigos_un_uso (
    id uuid NOT NULL,
    usuario_id uuid NOT NULL,
    proposito character varying(30) NOT NULL,
    hash_codigo character varying(100) NOT NULL,
    emitido_en timestamp with time zone NOT NULL,
    expira_en timestamp with time zone NOT NULL,
    usado_en timestamp with time zone,
    intentos_fallidos integer DEFAULT 0 NOT NULL,
    revision bigint NOT NULL,
    CONSTRAINT codigos_un_uso_proposito CHECK (((proposito)::text = ANY ((ARRAY['VERIFICACION_WHATSAPP'::character varying, 'RESTABLECER_CONTRASENA'::character varying, 'ESTABLECER_CONTRASENA'::character varying])::text[])))
);


--
-- Name: configuracion_comision; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.configuracion_comision (
    id uuid NOT NULL,
    porcentaje numeric(5,2) NOT NULL,
    vigente_desde timestamp with time zone NOT NULL,
    administrador_id uuid,
    CONSTRAINT configuracion_comision_porcentaje CHECK (((porcentaje >= (0)::numeric) AND (porcentaje < (100)::numeric)))
);


--
-- Name: cuentas_cobro; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.cuentas_cobro (
    id uuid NOT NULL,
    prestador_id uuid NOT NULL,
    tipo character varying(20) NOT NULL,
    entidad character varying(100) NOT NULL,
    numero_cifrado text NOT NULL,
    titular_cifrado text NOT NULL,
    vigente_desde timestamp with time zone NOT NULL,
    vigente_hasta timestamp with time zone,
    CONSTRAINT cuentas_cobro_tipo CHECK (((tipo)::text = ANY ((ARRAY['AHORROS'::character varying, 'CORRIENTE'::character varying, 'NEQUI'::character varying, 'DAVIPLATA'::character varying])::text[])))
);


--
-- Name: decisiones_moderacion; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.decisiones_moderacion (
    id uuid NOT NULL,
    tipo character varying(10) NOT NULL,
    moderado_id character varying(50) NOT NULL,
    numero_version integer,
    decision character varying(10) NOT NULL,
    motivo character varying(1000),
    administrador_id uuid NOT NULL,
    decidida_en timestamp with time zone NOT NULL,
    id_aviso character varying(200) NOT NULL,
    CONSTRAINT decisiones_moderacion_decision CHECK (((decision)::text = ANY ((ARRAY['APROBAR'::character varying, 'RECHAZAR'::character varying])::text[]))),
    CONSTRAINT decisiones_moderacion_tipo CHECK (((tipo)::text = ANY ((ARRAY['OFERTA'::character varying, 'OPINION'::character varying])::text[]))),
    CONSTRAINT decisiones_moderacion_version CHECK ((((tipo)::text = 'OFERTA'::text) = (numero_version IS NOT NULL)))
);


--
-- Name: dispersiones; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.dispersiones (
    id uuid NOT NULL,
    solicitud_id uuid NOT NULL,
    prestador_id uuid NOT NULL,
    monto_bruto bigint NOT NULL,
    porcentaje_comision numeric(5,2) NOT NULL,
    comision bigint NOT NULL,
    costo_transaccional bigint NOT NULL,
    monto_neto bigint NOT NULL,
    estado character varying(20) NOT NULL,
    intentos integer NOT NULL,
    ultima_causa character varying(500),
    cuenta_destino character varying(20),
    id_pasarela character varying(100),
    creada_en timestamp with time zone NOT NULL,
    proximo_intento timestamp with time zone,
    ejecutada_en timestamp with time zone,
    aviso_enviado_en timestamp with time zone,
    aviso_intentado_en timestamp with time zone,
    reactivaciones integer DEFAULT 0 NOT NULL,
    CONSTRAINT dispersiones_estado CHECK (((estado)::text = ANY ((ARRAY['PENDIENTE'::character varying, 'EXITOSA'::character varying, 'FALLIDA'::character varying])::text[]))),
    CONSTRAINT dispersiones_montos CHECK (((monto_bruto > 0) AND (comision >= 0) AND (costo_transaccional >= 0) AND (monto_neto >= 0)))
);


--
-- Name: eventos_pasarela; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.eventos_pasarela (
    id uuid NOT NULL,
    clave_unica character varying(150) NOT NULL,
    transaccion_id character varying(100) NOT NULL,
    referencia character varying(255) NOT NULL,
    estado character varying(20) NOT NULL,
    monto_centavos bigint NOT NULL,
    cuerpo text NOT NULL,
    recibido_en timestamp with time zone NOT NULL,
    procesado_en timestamp with time zone,
    resultado character varying(50),
    procesamiento character varying(20) DEFAULT 'PENDIENTE'::character varying NOT NULL,
    intentos_procesamiento integer DEFAULT 0 NOT NULL,
    ultimo_error character varying(500),
    siguiente_intento_en timestamp with time zone NOT NULL,
    aviso_enviado_en timestamp with time zone,
    aviso_intentado_en timestamp with time zone,
    CONSTRAINT eventos_pasarela_estado CHECK (((estado)::text = ANY ((ARRAY['PENDIENTE'::character varying, 'APROBADA'::character varying, 'RECHAZADA'::character varying, 'ANULADA'::character varying, 'ERROR'::character varying])::text[]))),
    CONSTRAINT eventos_pasarela_procesamiento CHECK (((procesamiento)::text = ANY ((ARRAY['PENDIENTE'::character varying, 'PROCESADO'::character varying, 'FALLIDO'::character varying])::text[])))
);


--
-- Name: exposiciones; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.exposiciones (
    id uuid NOT NULL,
    consulta_id uuid NOT NULL,
    registrada_en timestamp with time zone NOT NULL,
    origen character varying(20) NOT NULL,
    motor character varying(20) NOT NULL,
    oferta_id character varying(50) NOT NULL,
    prestador_id uuid NOT NULL,
    posicion integer NOT NULL,
    CONSTRAINT exposiciones_motor CHECK (((motor)::text = ANY ((ARRAY['REGLAS'::character varying, 'IA'::character varying])::text[]))),
    CONSTRAINT exposiciones_origen CHECK (((origen)::text = ANY ((ARRAY['RECOMENDACION'::character varying, 'ALTERNATIVAS'::character varying])::text[]))),
    CONSTRAINT exposiciones_posicion CHECK ((posicion >= 1))
);


--
-- Name: intentos_pago; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.intentos_pago (
    id uuid NOT NULL,
    solicitud_id uuid NOT NULL,
    turista_id uuid NOT NULL,
    numero integer NOT NULL,
    referencia character varying(255) NOT NULL,
    monto_centavos bigint NOT NULL,
    estado character varying(20) NOT NULL,
    transaccion_id character varying(100),
    creado_en timestamp with time zone NOT NULL,
    resuelto_en timestamp with time zone,
    revision bigint NOT NULL,
    abandonar_en timestamp with time zone NOT NULL,
    CONSTRAINT intentos_pago_estado CHECK (((estado)::text = ANY ((ARRAY['EN_CURSO'::character varying, 'APROBADO'::character varying, 'RECHAZADO'::character varying, 'ERROR'::character varying, 'ABANDONADO'::character varying])::text[]))),
    CONSTRAINT intentos_pago_monto CHECK ((monto_centavos > 0)),
    CONSTRAINT intentos_pago_numero CHECK ((numero >= 1))
);


--
-- Name: notificaciones_solicitud; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.notificaciones_solicitud (
    id uuid NOT NULL,
    solicitud_id uuid NOT NULL,
    destinatario_tipo character varying(20) NOT NULL,
    destinatario_id uuid NOT NULL,
    canal character varying(20) NOT NULL,
    tipo character varying(40) NOT NULL,
    datos jsonb NOT NULL,
    estado character varying(20) NOT NULL,
    intentos integer NOT NULL,
    proximo_intento timestamp with time zone NOT NULL,
    creada_en timestamp with time zone NOT NULL,
    enviada_en timestamp with time zone,
    ultimo_error character varying(500),
    CONSTRAINT notificaciones_solicitud_canal CHECK (((canal)::text = ANY ((ARRAY['WHATSAPP'::character varying, 'PLATAFORMA'::character varying])::text[]))),
    CONSTRAINT notificaciones_solicitud_destinatario CHECK (((destinatario_tipo)::text = ANY ((ARRAY['TURISTA'::character varying, 'PRESTADOR'::character varying, 'OPERADOR'::character varying])::text[]))),
    CONSTRAINT notificaciones_solicitud_estado CHECK (((estado)::text = ANY ((ARRAY['PENDIENTE'::character varying, 'ENVIADA'::character varying, 'FALLIDA'::character varying])::text[])))
);


--
-- Name: pagos_tardios; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.pagos_tardios (
    id uuid NOT NULL,
    solicitud_id uuid NOT NULL,
    intento_id uuid NOT NULL,
    referencia character varying(255) NOT NULL,
    transaccion_id character varying(100) NOT NULL,
    monto_centavos bigint NOT NULL,
    registrado_en timestamp with time zone NOT NULL,
    aviso_enviado_en timestamp with time zone,
    motivo character varying(20) NOT NULL,
    aviso_intentado_en timestamp with time zone,
    CONSTRAINT pagos_tardios_motivo CHECK (((motivo)::text = ANY ((ARRAY['PAGO_TARDIO'::character varying, 'MONTO_DISTINTO'::character varying])::text[])))
);


--
-- Name: politicas_datos; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.politicas_datos (
    version character varying(20) NOT NULL,
    fecha_publicacion date NOT NULL,
    url_texto character varying(300) NOT NULL
);


--
-- Name: prestadores; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.prestadores (
    id uuid NOT NULL,
    numero_rnt character varying(10) NOT NULL,
    tipo character varying(30) NOT NULL,
    numero_empleados integer,
    modalidades character varying(100),
    idiomas character varying(100),
    razon_social character varying(300),
    municipio character varying(100),
    categoria_rnt character varying(200),
    persona_natural_rnt boolean DEFAULT false NOT NULL,
    estado_rnt character varying(30) NOT NULL,
    visto_en_rnt boolean DEFAULT false NOT NULL,
    ultima_sincronizacion timestamp with time zone,
    sincronizacion_tomada_en timestamp with time zone,
    whatsapp_cifrado text,
    whatsapp_verificado boolean DEFAULT false NOT NULL,
    whatsapp_codigo_hash character varying(100),
    whatsapp_codigo_emitido timestamp with time zone,
    whatsapp_codigo_intentos integer DEFAULT 0 NOT NULL,
    operador_id uuid,
    habilitado_en_catalogo boolean DEFAULT false NOT NULL,
    version_habilitacion bigint DEFAULT 0 NOT NULL,
    fecha_registro timestamp with time zone NOT NULL,
    revision bigint NOT NULL,
    suprimido_en timestamp with time zone,
    CONSTRAINT prestadores_estado_rnt CHECK (((estado_rnt)::text = ANY ((ARRAY['ACTIVO'::character varying, 'INACTIVO'::character varying, 'NO_ENCONTRADO'::character varying, 'VERIFICACION_PENDIENTE'::character varying])::text[]))),
    CONSTRAINT prestadores_tipo CHECK (((tipo)::text = ANY ((ARRAY['ALOJAMIENTO'::character varying, 'RESTAURANTE'::character varying, 'AGENCIA_OPERADORA'::character varying, 'GUIA_INDEPENDIENTE'::character varying])::text[])))
);


--
-- Name: revisiones_sesgo; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.revisiones_sesgo (
    dia date NOT NULL,
    revisada_en timestamp with time zone NOT NULL
);


--
-- Name: rnt_simulado_bajas; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.rnt_simulado_bajas (
    numero_rnt character varying(10) NOT NULL
);


--
-- Name: sesiones; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.sesiones (
    id uuid NOT NULL,
    usuario_id uuid NOT NULL,
    hash_renovacion character varying(100) NOT NULL,
    creada_en timestamp with time zone NOT NULL,
    expira_en timestamp with time zone NOT NULL,
    revocada_en timestamp with time zone,
    revision bigint NOT NULL
);


--
-- Name: solicitudes; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.solicitudes (
    id uuid NOT NULL,
    turista_id uuid NOT NULL,
    oferta_id character varying(50) NOT NULL,
    tipo_oferta character varying(20) NOT NULL,
    nombre_oferta character varying(300) NOT NULL,
    prestador_id uuid NOT NULL,
    zona_id character varying(50) NOT NULL,
    fecha_inicio date NOT NULL,
    hora_salida time without time zone,
    personas integer NOT NULL,
    nota_turista character varying(500),
    operador_id uuid NOT NULL,
    estado character varying(20) NOT NULL,
    precio_base bigint,
    politica_cancelacion character varying(1000),
    nota_operador character varying(1000),
    vence_en timestamp with time zone,
    limite_vencimiento timestamp with time zone,
    intento_pago_en_curso boolean NOT NULL,
    intentos_pago_fallidos integer NOT NULL,
    creada_en timestamp with time zone NOT NULL,
    actualizada_en timestamp with time zone NOT NULL,
    revision bigint NOT NULL,
    hora_reserva time without time zone,
    fecha_salida date,
    numero_unidades integer,
    aparte_hasta timestamp with time zone,
    categoria_prestador character varying(20),
    CONSTRAINT solicitudes_categoria_prestador CHECK (((categoria_prestador)::text = ANY ((ARRAY['GUIA_INDEPENDIENTE'::character varying, 'MICRO_PEQUENO'::character varying, 'MEDIANO'::character varying, 'GRANDE'::character varying])::text[]))),
    CONSTRAINT solicitudes_estado CHECK (((estado)::text = ANY ((ARRAY['PENDIENTE'::character varying, 'ATENDIDA'::character varying, 'NO_DISPONIBLE'::character varying, 'SIN_RESPUESTA'::character varying, 'CONFIRMADA'::character varying, 'DECLINADA'::character varying, 'PAGO_FALLIDO'::character varying, 'PAGADA'::character varying, 'VENCIDA'::character varying])::text[]))),
    CONSTRAINT solicitudes_fecha_salida CHECK (((fecha_salida IS NULL) OR (fecha_salida > fecha_inicio))),
    CONSTRAINT solicitudes_intentos CHECK (((intentos_pago_fallidos >= 0) AND (intentos_pago_fallidos <= 3))),
    CONSTRAINT solicitudes_numero_unidades CHECK (((numero_unidades IS NULL) OR (numero_unidades >= 1))),
    CONSTRAINT solicitudes_personas CHECK ((personas >= 1)),
    CONSTRAINT solicitudes_precio CHECK (((precio_base IS NULL) OR (precio_base > 0))),
    CONSTRAINT solicitudes_tipo_oferta CHECK (((tipo_oferta)::text = ANY ((ARRAY['ACTIVIDAD'::character varying, 'PAQUETE'::character varying, 'ALOJAMIENTO'::character varying, 'RESTAURANTE'::character varying])::text[])))
);


--
-- Name: solicitudes_supresion; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.solicitudes_supresion (
    id uuid NOT NULL,
    usuario_id uuid NOT NULL,
    fecha_solicitud timestamp with time zone NOT NULL,
    estado character varying(20) NOT NULL,
    fecha_ejecucion timestamp with time zone,
    CONSTRAINT solicitudes_supresion_ejecucion CHECK ((((estado)::text = 'EJECUTADA'::text) = (fecha_ejecucion IS NOT NULL))),
    CONSTRAINT solicitudes_supresion_estado CHECK (((estado)::text = ANY ((ARRAY['SOLICITADA'::character varying, 'EJECUTADA'::character varying])::text[])))
);


--
-- Name: transiciones_solicitud; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.transiciones_solicitud (
    id uuid NOT NULL,
    solicitud_id uuid NOT NULL,
    orden integer NOT NULL,
    estado_origen character varying(20),
    estado_destino character varying(20) NOT NULL,
    ocurrida_en timestamp with time zone NOT NULL,
    actor character varying(20) NOT NULL,
    actor_id uuid,
    motivo character varying(500),
    CONSTRAINT transiciones_solicitud_actor CHECK (((actor)::text = ANY ((ARRAY['TURISTA'::character varying, 'OPERADOR'::character varying, 'SISTEMA'::character varying, 'PAGOS'::character varying])::text[])))
);


--
-- Name: usuarios; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.usuarios (
    id uuid NOT NULL,
    rol character varying(30) NOT NULL,
    nombre_cifrado text NOT NULL,
    correo character varying(254) NOT NULL,
    hash_contrasena character varying(100),
    estado character varying(20) NOT NULL,
    intentos_fallidos integer DEFAULT 0 NOT NULL,
    bloqueada_hasta timestamp with time zone,
    fecha_registro timestamp with time zone NOT NULL,
    whatsapp_cifrado text,
    whatsapp_verificado boolean DEFAULT false NOT NULL,
    revision bigint NOT NULL,
    CONSTRAINT usuarios_estado CHECK (((estado)::text = ANY ((ARRAY['ACTIVA'::character varying, 'DESACTIVADA'::character varying, 'SUPRIMIDA'::character varying])::text[]))),
    CONSTRAINT usuarios_rol CHECK (((rol)::text = ANY ((ARRAY['TURISTA'::character varying, 'PRESTADOR'::character varying, 'ADMINISTRADOR'::character varying, 'OPERADOR_DISPONIBILIDAD'::character varying, 'GESTOR_DESTINO'::character varying])::text[]))),
    CONSTRAINT usuarios_whatsapp_solo_turista CHECK ((((rol)::text = 'TURISTA'::text) OR (whatsapp_cifrado IS NULL)))
);


--
-- Data for Name: aceptaciones_politica; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- Data for Name: alertas_sesgo; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- Data for Name: auditoria_prestadores; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- Data for Name: bandeja_salida; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- Data for Name: claves_idempotencia; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- Data for Name: claves_idempotencia_pagos; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- Data for Name: codigos_un_uso; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- Data for Name: configuracion_comision; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.configuracion_comision (id, porcentaje, vigente_desde, administrador_id) VALUES ('00000000-0000-0000-0000-000000000004', 4.00, '2026-01-01 00:00:00+00', NULL);


--
-- Data for Name: cuentas_cobro; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- Data for Name: decisiones_moderacion; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- Data for Name: dispersiones; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- Data for Name: eventos_pasarela; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- Data for Name: exposiciones; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- Data for Name: intentos_pago; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- Data for Name: notificaciones_solicitud; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- Data for Name: pagos_tardios; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- Data for Name: politicas_datos; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.politicas_datos (version, fecha_publicacion, url_texto) VALUES ('v1', '2026-10-28', '/api/nucleo/politica-datos/v1');


--
-- Data for Name: prestadores; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- Data for Name: revisiones_sesgo; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- Data for Name: rnt_simulado_bajas; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- Data for Name: sesiones; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- Data for Name: solicitudes; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- Data for Name: solicitudes_supresion; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- Data for Name: transiciones_solicitud; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- Data for Name: usuarios; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- Name: aceptaciones_politica aceptaciones_politica_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.aceptaciones_politica
    ADD CONSTRAINT aceptaciones_politica_pkey PRIMARY KEY (usuario_id, version);


--
-- Name: alertas_sesgo alertas_sesgo_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.alertas_sesgo
    ADD CONSTRAINT alertas_sesgo_pkey PRIMARY KEY (id);


--
-- Name: auditoria_prestadores auditoria_prestadores_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.auditoria_prestadores
    ADD CONSTRAINT auditoria_prestadores_pkey PRIMARY KEY (id);


--
-- Name: bandeja_salida bandeja_salida_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bandeja_salida
    ADD CONSTRAINT bandeja_salida_pkey PRIMARY KEY (id_aviso);


--
-- Name: claves_idempotencia_pagos claves_idempotencia_pagos_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.claves_idempotencia_pagos
    ADD CONSTRAINT claves_idempotencia_pagos_pkey PRIMARY KEY (usuario_id, clave);


--
-- Name: claves_idempotencia claves_idempotencia_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.claves_idempotencia
    ADD CONSTRAINT claves_idempotencia_pkey PRIMARY KEY (usuario_id, clave);


--
-- Name: codigos_un_uso codigos_un_uso_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.codigos_un_uso
    ADD CONSTRAINT codigos_un_uso_pkey PRIMARY KEY (id);


--
-- Name: configuracion_comision configuracion_comision_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.configuracion_comision
    ADD CONSTRAINT configuracion_comision_pkey PRIMARY KEY (id);


--
-- Name: cuentas_cobro cuentas_cobro_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cuentas_cobro
    ADD CONSTRAINT cuentas_cobro_pkey PRIMARY KEY (id);


--
-- Name: decisiones_moderacion decisiones_moderacion_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.decisiones_moderacion
    ADD CONSTRAINT decisiones_moderacion_pkey PRIMARY KEY (id);


--
-- Name: decisiones_moderacion decisiones_moderacion_unica; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.decisiones_moderacion
    ADD CONSTRAINT decisiones_moderacion_unica UNIQUE NULLS NOT DISTINCT (tipo, moderado_id, numero_version);


--
-- Name: dispersiones dispersiones_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dispersiones
    ADD CONSTRAINT dispersiones_pkey PRIMARY KEY (id);


--
-- Name: dispersiones dispersiones_solicitud; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dispersiones
    ADD CONSTRAINT dispersiones_solicitud UNIQUE (solicitud_id);


--
-- Name: eventos_pasarela eventos_pasarela_clave; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.eventos_pasarela
    ADD CONSTRAINT eventos_pasarela_clave UNIQUE (clave_unica);


--
-- Name: eventos_pasarela eventos_pasarela_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.eventos_pasarela
    ADD CONSTRAINT eventos_pasarela_pkey PRIMARY KEY (id);


--
-- Name: exposiciones exposiciones_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.exposiciones
    ADD CONSTRAINT exposiciones_pkey PRIMARY KEY (id);


--
-- Name: intentos_pago intentos_pago_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.intentos_pago
    ADD CONSTRAINT intentos_pago_pkey PRIMARY KEY (id);


--
-- Name: intentos_pago intentos_pago_referencia; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.intentos_pago
    ADD CONSTRAINT intentos_pago_referencia UNIQUE (referencia);


--
-- Name: notificaciones_solicitud notificaciones_solicitud_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.notificaciones_solicitud
    ADD CONSTRAINT notificaciones_solicitud_pkey PRIMARY KEY (id);


--
-- Name: pagos_tardios pagos_tardios_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pagos_tardios
    ADD CONSTRAINT pagos_tardios_pkey PRIMARY KEY (id);


--
-- Name: pagos_tardios pagos_tardios_transaccion; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pagos_tardios
    ADD CONSTRAINT pagos_tardios_transaccion UNIQUE (transaccion_id);


--
-- Name: politicas_datos politicas_datos_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.politicas_datos
    ADD CONSTRAINT politicas_datos_pkey PRIMARY KEY (version);


--
-- Name: prestadores prestadores_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.prestadores
    ADD CONSTRAINT prestadores_pkey PRIMARY KEY (id);


--
-- Name: revisiones_sesgo revisiones_sesgo_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.revisiones_sesgo
    ADD CONSTRAINT revisiones_sesgo_pkey PRIMARY KEY (dia);


--
-- Name: rnt_simulado_bajas rnt_simulado_bajas_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.rnt_simulado_bajas
    ADD CONSTRAINT rnt_simulado_bajas_pkey PRIMARY KEY (numero_rnt);


--
-- Name: sesiones sesiones_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sesiones
    ADD CONSTRAINT sesiones_pkey PRIMARY KEY (id);


--
-- Name: solicitudes solicitudes_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.solicitudes
    ADD CONSTRAINT solicitudes_pkey PRIMARY KEY (id);


--
-- Name: solicitudes_supresion solicitudes_supresion_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.solicitudes_supresion
    ADD CONSTRAINT solicitudes_supresion_pkey PRIMARY KEY (id);


--
-- Name: solicitudes_supresion solicitudes_supresion_usuario_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.solicitudes_supresion
    ADD CONSTRAINT solicitudes_supresion_usuario_id_key UNIQUE (usuario_id);


--
-- Name: transiciones_solicitud transiciones_solicitud_orden; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.transiciones_solicitud
    ADD CONSTRAINT transiciones_solicitud_orden UNIQUE (solicitud_id, orden);


--
-- Name: transiciones_solicitud transiciones_solicitud_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.transiciones_solicitud
    ADD CONSTRAINT transiciones_solicitud_pkey PRIMARY KEY (id);


--
-- Name: usuarios usuarios_correo_unico; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.usuarios
    ADD CONSTRAINT usuarios_correo_unico UNIQUE (correo);


--
-- Name: usuarios usuarios_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.usuarios
    ADD CONSTRAINT usuarios_pkey PRIMARY KEY (id);


--
-- Name: alertas_sesgo_abierta; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX alertas_sesgo_abierta ON public.alertas_sesgo USING btree (prestador_id) WHERE (cerrada_en IS NULL);


--
-- Name: auditoria_prestadores_por_prestador; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX auditoria_prestadores_por_prestador ON public.auditoria_prestadores USING btree (prestador_id, ocurrido_en);


--
-- Name: bandeja_salida_pendientes; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX bandeja_salida_pendientes ON public.bandeja_salida USING btree (proximo_intento, creado_en) WHERE ((estado)::text = 'PENDIENTE'::text);


--
-- Name: claves_idempotencia_expiradas; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX claves_idempotencia_expiradas ON public.claves_idempotencia USING btree (expira_en);


--
-- Name: claves_idempotencia_pagos_expiradas; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX claves_idempotencia_pagos_expiradas ON public.claves_idempotencia_pagos USING btree (expira_en);


--
-- Name: codigos_un_uso_ultimo; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX codigos_un_uso_ultimo ON public.codigos_un_uso USING btree (usuario_id, proposito, emitido_en DESC);


--
-- Name: cuentas_cobro_una_vigente; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX cuentas_cobro_una_vigente ON public.cuentas_cobro USING btree (prestador_id) WHERE (vigente_hasta IS NULL);


--
-- Name: dispersiones_fallidas_sin_aviso; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX dispersiones_fallidas_sin_aviso ON public.dispersiones USING btree (aviso_intentado_en NULLS FIRST) WHERE (((estado)::text = 'FALLIDA'::text) AND (aviso_enviado_en IS NULL));


--
-- Name: dispersiones_pendientes; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX dispersiones_pendientes ON public.dispersiones USING btree (proximo_intento) WHERE ((estado)::text = 'PENDIENTE'::text);


--
-- Name: eventos_pasarela_fallidos_sin_aviso; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX eventos_pasarela_fallidos_sin_aviso ON public.eventos_pasarela USING btree (aviso_intentado_en NULLS FIRST) WHERE (((procesamiento)::text = 'FALLIDO'::text) AND (aviso_enviado_en IS NULL));


--
-- Name: eventos_pasarela_por_procesar; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX eventos_pasarela_por_procesar ON public.eventos_pasarela USING btree (siguiente_intento_en) WHERE ((procesamiento)::text = 'PENDIENTE'::text);


--
-- Name: exposiciones_por_fecha; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX exposiciones_por_fecha ON public.exposiciones USING btree (registrada_en);


--
-- Name: intentos_pago_por_abandonar; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX intentos_pago_por_abandonar ON public.intentos_pago USING btree (abandonar_en) WHERE ((estado)::text = 'EN_CURSO'::text);


--
-- Name: intentos_pago_solicitud; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX intentos_pago_solicitud ON public.intentos_pago USING btree (solicitud_id, numero);


--
-- Name: intentos_pago_uno_en_curso; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX intentos_pago_uno_en_curso ON public.intentos_pago USING btree (solicitud_id) WHERE ((estado)::text = 'EN_CURSO'::text);


--
-- Name: notificaciones_solicitud_avisos; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX notificaciones_solicitud_avisos ON public.notificaciones_solicitud USING btree (destinatario_id, creada_en DESC) WHERE ((canal)::text = 'PLATAFORMA'::text);


--
-- Name: notificaciones_solicitud_pendientes; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX notificaciones_solicitud_pendientes ON public.notificaciones_solicitud USING btree (proximo_intento, creada_en) WHERE ((estado)::text = 'PENDIENTE'::text);


--
-- Name: pagos_tardios_sin_aviso; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX pagos_tardios_sin_aviso ON public.pagos_tardios USING btree (aviso_intentado_en NULLS FIRST) WHERE (aviso_enviado_en IS NULL);


--
-- Name: prestadores_rnt_unico; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX prestadores_rnt_unico ON public.prestadores USING btree (numero_rnt) WHERE (suprimido_en IS NULL);


--
-- Name: sesiones_abiertas; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX sesiones_abiertas ON public.sesiones USING btree (usuario_id) WHERE (revocada_en IS NULL);


--
-- Name: solicitudes_abiertas_por_prestador; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX solicitudes_abiertas_por_prestador ON public.solicitudes USING btree (prestador_id) WHERE ((estado)::text = ANY ((ARRAY['PENDIENTE'::character varying, 'ATENDIDA'::character varying, 'CONFIRMADA'::character varying, 'PAGO_FALLIDO'::character varying])::text[]));


--
-- Name: solicitudes_cola; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX solicitudes_cola ON public.solicitudes USING btree (operador_id, estado, creada_en);


--
-- Name: solicitudes_no_duplicada; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX solicitudes_no_duplicada ON public.solicitudes USING btree (turista_id, oferta_id, fecha_inicio, hora_salida, hora_reserva) NULLS NOT DISTINCT WHERE ((estado)::text = ANY ((ARRAY['PENDIENTE'::character varying, 'ATENDIDA'::character varying, 'CONFIRMADA'::character varying, 'PAGO_FALLIDO'::character varying])::text[]));


--
-- Name: solicitudes_por_creacion; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX solicitudes_por_creacion ON public.solicitudes USING btree (creada_en);


--
-- Name: solicitudes_por_vencer; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX solicitudes_por_vencer ON public.solicitudes USING btree (limite_vencimiento) WHERE ((estado)::text = ANY ((ARRAY['PENDIENTE'::character varying, 'ATENDIDA'::character varying, 'CONFIRMADA'::character varying, 'PAGO_FALLIDO'::character varying])::text[]));


--
-- Name: solicitudes_turista; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX solicitudes_turista ON public.solicitudes USING btree (turista_id, creada_en DESC);


--
-- Name: aceptaciones_politica aceptaciones_politica_usuario_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.aceptaciones_politica
    ADD CONSTRAINT aceptaciones_politica_usuario_id_fkey FOREIGN KEY (usuario_id) REFERENCES public.usuarios(id);


--
-- Name: aceptaciones_politica aceptaciones_politica_version_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.aceptaciones_politica
    ADD CONSTRAINT aceptaciones_politica_version_fkey FOREIGN KEY (version) REFERENCES public.politicas_datos(version);


--
-- Name: auditoria_prestadores auditoria_prestadores_prestador_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.auditoria_prestadores
    ADD CONSTRAINT auditoria_prestadores_prestador_id_fkey FOREIGN KEY (prestador_id) REFERENCES public.prestadores(id);


--
-- Name: codigos_un_uso codigos_un_uso_usuario_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.codigos_un_uso
    ADD CONSTRAINT codigos_un_uso_usuario_id_fkey FOREIGN KEY (usuario_id) REFERENCES public.usuarios(id);


--
-- Name: cuentas_cobro cuentas_cobro_prestador_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cuentas_cobro
    ADD CONSTRAINT cuentas_cobro_prestador_id_fkey FOREIGN KEY (prestador_id) REFERENCES public.prestadores(id);


--
-- Name: notificaciones_solicitud notificaciones_solicitud_solicitud_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.notificaciones_solicitud
    ADD CONSTRAINT notificaciones_solicitud_solicitud_id_fkey FOREIGN KEY (solicitud_id) REFERENCES public.solicitudes(id);


--
-- Name: pagos_tardios pagos_tardios_intento_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pagos_tardios
    ADD CONSTRAINT pagos_tardios_intento_id_fkey FOREIGN KEY (intento_id) REFERENCES public.intentos_pago(id);


--
-- Name: prestadores prestadores_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.prestadores
    ADD CONSTRAINT prestadores_id_fkey FOREIGN KEY (id) REFERENCES public.usuarios(id);


--
-- Name: prestadores prestadores_operador_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.prestadores
    ADD CONSTRAINT prestadores_operador_id_fkey FOREIGN KEY (operador_id) REFERENCES public.usuarios(id);


--
-- Name: sesiones sesiones_usuario_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sesiones
    ADD CONSTRAINT sesiones_usuario_id_fkey FOREIGN KEY (usuario_id) REFERENCES public.usuarios(id);


--
-- Name: solicitudes_supresion solicitudes_supresion_usuario_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.solicitudes_supresion
    ADD CONSTRAINT solicitudes_supresion_usuario_id_fkey FOREIGN KEY (usuario_id) REFERENCES public.usuarios(id);


--
-- Name: transiciones_solicitud transiciones_solicitud_solicitud_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.transiciones_solicitud
    ADD CONSTRAINT transiciones_solicitud_solicitud_id_fkey FOREIGN KEY (solicitud_id) REFERENCES public.solicitudes(id);


--
-- PostgreSQL database dump complete
--

\unrestrict R0AGewoNuAjcf0NFunIt5qrhMCeOvG7uLGbi0gnucjWQYGMIAASUril3MGsVNQx

