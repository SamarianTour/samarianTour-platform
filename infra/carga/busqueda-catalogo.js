// Prueba de carga de la búsqueda del catálogo (QAS-07, y la parte de QAS-02 que le toca al catálogo; DC-48).
//
// Cada usuario virtual busca con filtros combinados de tipo, zona y precio, a veces con texto o en la segunda página,
// y espera de 1 a 3 s antes de la siguiente búsqueda, como una persona que mira los resultados.
//
// Mide dos cosas:
//   - en el cliente (k6): la latencia y los errores de cada búsqueda, pasando por Nginx;
//   - en el servidor: al empezar y al terminar lee /actuator/prometheus de cada réplica y calcula, solo con las
//     búsquedas de la prueba, qué parte respondió en 2 s o menos y el p95 estimado. Es la medida de QAS-07.
//
// Se corre con infra/docker-compose.carga.yml (ver el README). Variables:
//   USUARIOS  usuarios concurrentes (500, QAS-07)
//   DURACION  tiempo sostenido con todos los usuarios (10m, QAS-02)
//   SUBIDA    tiempo para llegar a todos los usuarios (1m)
//   REPLICAS  réplicas del catálogo de las que se leen las métricas (2)

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Gauge } from 'k6/metrics';

const BASE = __ENV.BASE_URL || 'http://nginx:8080/api/catalogo';
const USUARIOS = Number(__ENV.USUARIOS || 500);
// Cada réplica por el nombre de su contenedor: por el nombre del servicio, k6 reutiliza la conexión y siempre
// llegaría a la misma.
const REPLICAS = (__ENV.REPLICAS || 'samarian-carga-catalogo-1,samarian-carga-catalogo-2').split(',');

const TIPOS = ['ACTIVIDAD', 'PAQUETE', 'ALOJAMIENTO', 'RESTAURANTE'];
const ZONAS = ['a00000000000000000000001', 'a00000000000000000000002', 'a00000000000000000000003',
    'a00000000000000000000004', 'a00000000000000000000005', 'a00000000000000000000006',
    'a00000000000000000000007'];
const TEXTOS = ['Minca', 'Tayrona', 'Taganga', 'paquete', 'senderismo', 'restaurante'];

// Medidas del servidor, calculadas al final con los contadores de las réplicas.
const dentroDe2s = new Gauge('servidor_busquedas_en_2s');
const p95Servidor = new Gauge('servidor_busqueda_p95_ms', true);
const busquedasServidor = new Gauge('servidor_busquedas');

export const options = {
    scenarios: {
        busqueda: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: __ENV.SUBIDA || '1m', target: USUARIOS },
                { duration: __ENV.DURACION || '10m', target: USUARIOS },
                { duration: '20s', target: 0 },
            ],
            gracefulRampDown: '10s',
        },
    },
    // Sin la etiqueta url, que repetiría los filtros de cada búsqueda.
    systemTags: ['status', 'method', 'name', 'scenario', 'check', 'error', 'error_code', 'expected_response'],
    thresholds: {
        // QAS-02: menos del 1 % de errores y al menos 50 peticiones por segundo
        'http_req_failed{nombre:busqueda}': ['rate<0.01'],
        'http_reqs{nombre:busqueda}': ['rate>=50'],
        // QAS-07: p95 <= 2 s; en el cliente incluye el paso por Nginx
        'http_req_duration{nombre:busqueda}': ['p(95)<=2000'],
        // QAS-07 medido en el servidor: al menos el 95 % de las búsquedas en 2 s o menos
        servidor_busquedas_en_2s: ['value>=0.95'],
    },
    summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
};

export function setup() {
    return { antes: metricasDeLasReplicas() };
}

export default function () {
    const filtros = {
        tipo: unoDe(TIPOS),
        zonaId: unoDe(ZONAS),
    };
    const minimo = 5000 * Math.floor(Math.random() * 40);
    filtros.precioMin = String(minimo);
    filtros.precioMax = String(minimo + 5000 * (20 + Math.floor(Math.random() * 120)));
    if (Math.random() < 0.2) {
        filtros.texto = unoDe(TEXTOS);
    }
    if (Math.random() < 0.15) {
        filtros.page = '1';
    }
    const consulta = Object.entries(filtros).map(([k, v]) => `${k}=${encodeURIComponent(v)}`).join('&');
    // name agrupa todas las búsquedas: sin él, cada combinación de filtros sería una serie distinta en k6.
    const respuesta = http.get(`${BASE}/ofertas?${consulta}`, { tags: { nombre: 'busqueda', name: '/ofertas' } });
    check(respuesta, {
        'responde 200': (r) => r.status === 200,
        'trae la página': (r) => r.status === 200 && r.json('contenido') !== undefined,
    });
    sleep(1 + Math.random() * 2);
}

export function teardown(datos) {
    const despues = metricasDeLasReplicas();
    const porUmbral = {};
    for (const [instancia, cubetas] of Object.entries(despues)) {
        const antes = datos.antes[instancia] || {};
        for (const [umbral, cantidad] of Object.entries(cubetas)) {
            porUmbral[umbral] = (porUmbral[umbral] || 0) + cantidad - (antes[umbral] || 0);
        }
    }
    const total = porUmbral['+Inf'] || 0;
    const en2s = porUmbral['2.0'] || 0;
    busquedasServidor.add(total);
    if (total > 0) {
        dentroDe2s.add(en2s / total);
        p95Servidor.add(percentil(porUmbral, total, 0.95) * 1000);
    }
    console.log(`Servidor: ${total} búsquedas en ${Object.keys(despues).length} réplicas; `
        + `${(100 * en2s / Math.max(total, 1)).toFixed(2)} % en 2 s o menos; por umbral: ${JSON.stringify(porUmbral)}`);
}

// Devuelve, por réplica, las búsquedas acumuladas en cada umbral (le) de /ofertas, sumando todos los estados.
function metricasDeLasReplicas() {
    const replicas = {};
    for (const replica of REPLICAS) {
        const respuesta = http.get(`http://${replica}:8080/api/catalogo/actuator/prometheus`,
            { tags: { nombre: 'metricas' } });
        if (respuesta.status !== 200) {
            throw new Error(`No se pudieron leer las métricas de ${replica}: ${respuesta.status} ${respuesta.error}`);
        }
        replicas[replica] = cubetasDeBusqueda(respuesta.body);
    }
    return replicas;
}

function cubetasDeBusqueda(texto) {
    const cubetas = {};
    for (const linea of texto.split('\n')) {
        if (!linea.startsWith('http_server_requests_seconds_bucket{') || !linea.includes('uri="/ofertas"')) {
            continue;
        }
        const umbral = linea.match(/le="([^"]+)"/)[1];
        cubetas[umbral] = (cubetas[umbral] || 0) + Number(linea.slice(linea.lastIndexOf(' ') + 1));
    }
    return cubetas;
}

// Percentil estimado por interpolación lineal dentro del umbral donde cae, como histogram_quantile de Prometheus.
function percentil(porUmbral, total, q) {
    const etiquetas = Object.keys(porUmbral).filter((u) => u !== '+Inf').sort((a, b) => Number(a) - Number(b));
    const objetivo = q * total;
    let anterior = 0;
    let acumuladoAnterior = 0;
    for (const etiqueta of etiquetas) {
        const umbral = Number(etiqueta);
        const acumulado = porUmbral[etiqueta];
        if (acumulado >= objetivo) {
            const dentro = acumulado - acumuladoAnterior;
            return anterior + (umbral - anterior) * (dentro === 0 ? 1 : (objetivo - acumuladoAnterior) / dentro);
        }
        anterior = umbral;
        acumuladoAnterior = acumulado;
    }
    // Más allá del último umbral (3 s) no se puede estimar.
    return Infinity;
}

function unoDe(lista) {
    return lista[Math.floor(Math.random() * lista.length)];
}

export function handleSummary(datos) {
    return {
        stdout: textSummary(datos),
        '/resultados/resumen.json': JSON.stringify(datos, null, 2),
    };
}

// Resumen corto en texto: lo que se compara con QAS-07 y QAS-02.
function textSummary(datos) {
    const m = datos.metrics;
    const valor = (nombre, campo) => (m[nombre] ? m[nombre].values[campo] : undefined);
    const umbrales = Object.entries(m).flatMap(([nombre, metrica]) => Object.entries(metrica.thresholds || {})
        .map(([regla, r]) => `  ${r.ok ? 'CUMPLE   ' : 'NO CUMPLE'} ${nombre}: ${regla}`));
    return [
        '',
        '== Búsqueda del catálogo (QAS-07, QAS-02) ==',
        `Búsquedas: ${valor('http_reqs{nombre:busqueda}', 'count')} `
            + `(${(valor('http_reqs{nombre:busqueda}', 'rate') || 0).toFixed(1)} por segundo)`,
        `Errores: ${((valor('http_req_failed{nombre:busqueda}', 'rate') || 0) * 100).toFixed(2)} %`,
        `Cliente, pasando por Nginx: p95 ${(valor('http_req_duration{nombre:busqueda}', 'p(95)') || 0).toFixed(0)} ms, `
            + `p99 ${(valor('http_req_duration{nombre:busqueda}', 'p(99)') || 0).toFixed(0)} ms, `
            + `máximo ${(valor('http_req_duration{nombre:busqueda}', 'max') || 0).toFixed(0)} ms`,
        `Servidor: ${valor('servidor_busquedas', 'value')} búsquedas, `
            + `${((valor('servidor_busquedas_en_2s', 'value') || 0) * 100).toFixed(2)} % en 2 s o menos, `
            + `p95 estimado ${(valor('servidor_busqueda_p95_ms', 'value') || 0).toFixed(0)} ms`,
        'Umbrales:',
        ...umbrales,
        '',
    ].join('\n');
}
