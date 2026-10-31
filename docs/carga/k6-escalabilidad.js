// Escenario de calidad de ESCALABILIDAD - prueba de carga con k6 (https://k6.io)
// Ejecutar:  k6 run docs/carga/k6-escalabilidad.js
// Cada iteracion: un autor crea una pregunta y la envia a revision.
// Cada envio genera eventos que disparan notificaciones (trabajo asincrono).
import http from 'k6/http';
import { check } from 'k6';

const API = __ENV.API || 'http://localhost:8080';
const AUTHORS = ['autor1', 'autor2'];

export const options = {
  scenarios: {
    cierre_convocatoria: {
      executor: 'ramping-arrival-rate',
      startRate: 2,
      timeUnit: '1s',
      preAllocatedVUs: 50,
      maxVUs: 200,
      stages: [
        { target: 20, duration: '1m' },   // subida
        { target: 20, duration: '3m' },   // pico sostenido
        { target: 0, duration: '30s' },
      ],
    },
  },
  thresholds: {
    'http_req_duration{name:submit}': ['p(95)<500'],
    'http_req_failed': ['rate<0.01'],
  },
};

export default function () {
  const author = AUTHORS[Math.floor(Math.random() * AUTHORS.length)];
  const headers = { 'Content-Type': 'application/json', 'X-User-Login': author };
  const body = JSON.stringify({
    type: 'MULTIPLE_CHOICE',
    context: 'Contexto de prueba de carga ' + __VU + '-' + __ITER,
    statement: '¿Cuál alternativa favorece la escalabilidad horizontal?',
    options: ['Monolito', 'Microservicios con eventos', 'Dos capas', 'Mainframe'],
    correctAnswer: 'Microservicios con eventos',
    justification: 'Permite escalar cada servicio por separado.',
    bibliography: 'Richards & Ford, 2020.',
    competence: 'Diseño de software', topic: 'Arquitectura', subtopic: 'Escalabilidad', difficulty: 'MEDIO',
  });
  const created = http.post(`${API}/api/questions`, body, { headers, tags: { name: 'create' } });
  if (!check(created, { 'creada 201': (r) => r.status === 201 })) return;
  const id = created.json('id');
  const submitted = http.post(`${API}/api/questions/${id}/submit-for-review`, null, { headers, tags: { name: 'submit' } });
  check(submitted, { 'enviada 200': (r) => r.status === 200 });
}
