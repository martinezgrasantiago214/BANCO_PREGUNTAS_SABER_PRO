# Documento de Arquitectura — Iteración 2

**Banco de Preguntas Saber PRO**
Ingeniería de Software II · Universidad del Cauca · 2026.2
Integrantes: _(completar)_

## 1. Introducción

En la iteración 1 se construyó un monolito de escritorio en 3 capas con MVC, principios SOLID y algunos patrones GoF, y el atributo de calidad principal fue la **modificabilidad**.

En esta iteración el sistema se refactoriza a una **arquitectura de microservicios orientada a eventos** para atender el atributo de calidad **escalabilidad**. El objetivo es que el sistema soporte los picos de trabajo de las convocatorias (muchos autores enviando preguntas a revisión al mismo tiempo y cientos de notificaciones por enviar). Para lograrlo, cada parte del sistema debe poder crecer de forma independiente, sin degradar las operaciones interactivas.

Este documento describe:

- las historias de usuario implementadas,
- el escenario de calidad de escalabilidad,
- el diseño de contextos delimitados,
- la arquitectura según el modelo C4,
- los patrones de diseño aplicados y las principales decisiones de arquitectura.

## 2. Historias de usuario de la iteración 2

Los criterios de aceptación se expresan con el formato *Dado / Cuando / Entonces*.

### HU01 — Crear pregunta de selección múltiple con única respuesta

*Yo como autor de preguntas necesito crear preguntas de selección múltiple con única respuesta, acordes a los principios del Diseño Centrado en Evidencia, para alimentar el banco de preguntas Saber Pro.*

- **CA1.** Dado un autor autenticado, cuando registra Contexto, Pregunta directa, 4 opciones, Respuesta correcta, Justificación, Bibliografía, Competencia, Tema, Subtema y Nivel de dificultad, entonces la pregunta se guarda en estado **Borrador**.
- **CA2.** Cuando falta algún campo, la pregunta directa tiene menos de 10 caracteres, las opciones no son exactamente 4 o se repiten, o la respuesta correcta no coincide con una opción, entonces el sistema **no guarda** la pregunta y muestra **todos** los errores juntos (validación estructural).
- **CA3.** La respuesta correcta se marca junto a una de las 4 opciones, por lo que siempre hay exactamente una correcta.

### HU02 — Enviar a revisión

*Yo como autor necesito cambiar el estado de mis preguntas de "Borrador" a "Pendiente de revisión" para que el administrador les asigne al menos un revisor.*

- **CA1.** Dada una pregunta propia en Borrador, cuando la envía a revisión, entonces queda en **Pendiente de revisión** y se notifica a los administradores.
- **CA2.** Solo el autor puede enviarla, y solo desde Borrador. Cualquier otra transición se rechaza con un mensaje claro.
- **CA3.** Cada estado se visualiza con su color, con una leyenda en el listado: Borrador en amarillo, Pendiente en azul, En revisión en morado, Aprobada en verde y Rechazada en rojo.

### HU03 — Listar y editar mis preguntas

*Yo como autor necesito listar las preguntas que he creado para poder más adelante verlas y editarlas.*

- **CA1.** El listado muestra únicamente las preguntas del autor, paginadas (8 por página) y ordenadas de la más reciente a la más antigua.
- **CA2.** Se puede filtrar por estado, tema y palabra clave (busca en el enunciado, el contexto y el subtema).
- **CA3.** Se puede ver el detalle completo, incluidas las observaciones de los revisores.
- **CA4.** Solo se editan preguntas en Borrador o Rechazada, y la edición aplica la misma validación estructural. Al editar una Rechazada, la pregunta vuelve a Borrador.

### HU04 — Asignar revisores

*Yo como administrador necesito asignar revisores (entre 1 y 3) a las preguntas en estado "Pendiente de revisión" para que sean revisadas por otros docentes.*

- **CA1.** El administrador ve, paginadas, las preguntas pendientes de asignación.
- **CA2.** Debe seleccionar entre **1 y 3** revisores distintos, que existan y tengan el rol Revisor. El autor no puede revisar su propia pregunta.
- **CA3.** Al asignar, la pregunta pasa a **En revisión** y cada revisor recibe un **correo** de notificación. El autor también es informado.
- **CA4.** Solo un usuario con rol Administrador puede asignar.

### HU05 — Revisar preguntas asignadas (nueva)

*Yo como revisor necesito listar las preguntas que me han sido asignadas para leerlas, dejar la evaluación y cambiar el estado a Aprobada o Rechazada.*

- **CA1.** El revisor ve sus preguntas asignadas y las puede filtrar por *pendientes por evaluar*, *ya evaluadas* o *todas*.
- **CA2.** Puede leer la pregunta completa: contexto, enunciado, opciones con la correcta marcada, justificación, bibliografía y clasificación.
- **CA3.** Califica de 1 a 5 cada criterio de la rúbrica (alineación con la competencia, claridad, plausibilidad de los distractores y justificación/bibliografía) y emite su concepto **Aprobada** o **Rechazada**. Cada revisor evalúa una sola vez.
- **CA4.** Puede dejar **observaciones** para el autor, quien las lee en el detalle de su pregunta.
- **CA5.** La decisión final se calcula con la política configurada. Por defecto, un rechazo rechaza la pregunta y se requieren todas las aprobaciones para aprobarla.
- **CA6.** Se **notifica a los implicados**: el autor recibe cada evaluación, cada observación y la decisión final, y los revisores y administradores reciben la decisión final.

## 3. Planificación del Sprint 2

_(Insertar pantallazo del tablero Jira/Trello con las tareas del Sprint 2, su estado y el responsable de cada una.)_

## 4. Escenario de calidad: escalabilidad

| Elemento | Descripción |
|----------|-------------|
| **Contexto** | Cierre de convocatoria Saber PRO. El sistema se despliega en contenedores con una instancia por microservicio, y el envío de correos tarda unos 300 ms por mensaje (latencia típica de SMTP). |
| **Fuente** | Unos 300 docentes (autores, administradores y revisores) trabajando al mismo tiempo. |
| **Estímulo** | Durante 3 minutos llegan 20 operaciones por segundo de "crear pregunta + enviar a revisión" (unos 3.600 envíos). Cada envío genera eventos que producen notificaciones. |
| **Artefacto** | question-service, review-service, notification-service y RabbitMQ. |
| **Respuesta** | Las solicitudes interactivas se responden sin esperar el envío de correos, que se procesa de forma asíncrona por la cola. Ante el pico, el operador agrega réplicas de notification-service y review-service (`docker compose up --scale`), que se registran solas en Eureka y compiten por la misma cola. No se modifica código ni se reinician los demás servicios. |
| **Medida de la respuesta** | (1) p95 de `submit-for-review` **< 500 ms** con **< 1 %** de errores (k6). (2) Tiempo de vaciado de la cola `notification-service.events` **≈ 3 veces menor** con 3 réplicas que con 1 (consola de RabbitMQ). (3) Agregar una réplica toma **< 1 minuto** y no pierde mensajes, porque las colas son durables, el ACK es manual y hay DLQ. |
| **Resultado esperado** | El sistema absorbe el pico sin degradar la experiencia de los autores y la capacidad de notificación crece de forma casi lineal con el número de réplicas. |

**Cómo se mide:**

1. Ejecutar `k6 run docs/carga/k6-escalabilidad.js` con `MAIL_SIMULATED_LATENCY_MS=300` y una sola réplica. Registrar el p95 y el tiempo que tarda la cola en volver a 0.
2. Repetir con `--scale notification-service=3`.
3. Comparar los resultados (tabla y gráfica del panel *Queues* de RabbitMQ).

**Tácticas de arquitectura aplicadas:**

| Táctica | Cómo se aplica |
|---------|----------------|
| Comunicación asíncrona | Eventos sobre RabbitMQ. |
| Competing consumers | Varias réplicas consumen la misma cola. |
| Descubrimiento y balanceo | Eureka con `lb://` en el gateway y en los clientes REST. |
| Base de datos por servicio | Cada servicio tiene su propia base. |
| Servicios sin estado de sesión | La identidad viaja en `X-User-Login`. |
| Consumidores idempotentes | Se deduplica por `eventId` y las transiciones repetidas se ignoran. |
| Control de concurrencia optimista | `@Version` en los agregados. |
| Reintentos + Dead Letter Queue | Los mensajes que fallan tras los reintentos quedan en una DLQ. |
| Paginación en todos los listados | Ningún listado carga todos los registros en memoria. |

## 5. Diseño de contextos delimitados

Diagrama: [`diagramas/05-contextos-delimitados.puml`](diagramas/05-contextos-delimitados.puml)

Se tomó como base la Figura 1 del enunciado y se ajustó al alcance de esta iteración:

| Contexto (Figura 1) | Decisión | Microservicio |
|---------------------|----------|---------------|
| Gestión de Usuarios (Identity & Access) | Se implementa | `user-service` |
| Gestión de Preguntas | Se implementa | `question-service` |
| Ciclo de Vida de Preguntas | **Se fusiona** con Gestión de Preguntas | `question-service` (patrón State) |
| Validación de Preguntas | **Se fusiona** con Gestión de Preguntas | `question-service` (Chain of Responsibility) |
| Revisión por Pares | Se implementa | `review-service` |
| — | **Contexto nuevo de soporte**: Notificaciones | `notification-service` |
| Simulacros, Seguimiento Académico, Administración y Trazabilidad | Iteraciones futuras (no hay HU en este corte) | — |

**Justificación de las fusiones:**

- **Ciclo de Vida.** El estado es parte del invariante del agregado Pregunta. Por ejemplo, solo se edita en Borrador o Rechazada. Separarlo exigiría transacciones distribuidas para operaciones que deben ser atómicas.
- **Validación Estructural.** HU01 exige validar *al momento de grabar*, es decir, de forma síncrona. Un servicio aparte solo agregaría latencia y un punto de falla sin ganancia de escalabilidad, porque la validación es CPU pura y escala junto con question-service.

**Relaciones entre contextos:**

- question-service y review-service se integran solo por eventos. Los contratos están en `common-events` y funcionan como *Published Language*.
- review-service guarda una **proyección local** de los datos de la pregunta que recibe en el evento, así que opera aunque question-service no esté disponible.
- review-service y notification-service consultan a user-service por REST con una relación *Customer/Supplier*, para validar roles y resolver correos.
- Los contextos futuros, como Auditoría, se suscriben a los mismos eventos (por ejemplo, con un binding `#`) sin modificar los servicios existentes.

**Eventos de integración** (exchange `saberpro.events`, tipo *topic*):

| Evento | Routing key | Productor | Consumidores |
|--------|-------------|-----------|--------------|
| `QuestionSubmittedForReviewEvent` | `question.submitted` | question | review, notification |
| `ReviewersAssignedEvent` | `review.reviewers-assigned` | review | question, notification |
| `ReviewEvaluationSubmittedEvent` | `review.evaluation-submitted` | review | notification |
| `ObservationAddedEvent` | `review.observation-added` | review | notification |
| `ReviewCompletedEvent` | `review.completed` | review | question, notification |

## 6. Arquitectura y diseño (modelo C4)

Los diagramas fuente están en [`docs/diagramas`](diagramas). Son PlantUML con la librería estándar C4 y se renderizan con el plugin PlantUML de IntelliJ o VS Code, o en plantuml.com.

### 6.1 Nivel 1 — Contexto

`01-c4-contexto.puml`. Tres tipos de usuario (autor, administrador y revisor) usan el sistema, que se apoya en un servidor SMTP externo.

### 6.2 Nivel 2 — Contenedores

`02-c4-contenedores.puml`. Muestra el **frontend y el backend separados y comunicados por una API REST**:

- El **frontend de escritorio** (Swing) solo conoce el **API Gateway** (`:8080`).
- El gateway enruta por nombre lógico (`lb://servicio`) a los 4 microservicios, que se descubren mediante **Eureka**.
- Los servicios se comunican de forma asíncrona por **RabbitMQ** y cada uno tiene su propia **base de datos PostgreSQL**.
- La API se documenta con **Swagger/OpenAPI**. El gateway publica un Swagger UI unificado en `/swagger-ui.html`, con un selector para cada servicio.

### 6.3 Nivel 3 — Componentes

`03-c4-componentes-question-service.puml` y `04-c4-componentes-review-service.puml`.

Todos los servicios siguen una **arquitectura hexagonal (puertos y adaptadores)**:

- `domain`: agregados, value objects, reglas y patrones. Es Java puro y no importa Spring.
- `application`: casos de uso (`QuestionService`, `ReviewService`…) y **puertos** (`QuestionRepositoryPort`, `ReviewEventPublisherPort`, `UserDirectoryPort`…).
- `infrastructure`: adaptadores de entrada (REST controllers y listeners AMQP) y de salida (JPA, publicadores RabbitMQ y clientes REST), además de la configuración, donde se ensamblan las dependencias.

### 6.4 Flujo dinámico

`06-secuencia-hu02-hu05.puml`. Muestra la secuencia completa: el envío a revisión, la asignación, la evaluación, la decisión y las notificaciones.

### 6.5 Frontend

El paquete `frontend-desktop` aplica el **micro-patrón MVC**:

- **Vistas:** clases Swing en `view`.
- **Controladores:** `SessionController`, `AuthorController` y `ReviewController`. Ejecutan las llamadas HTTP fuera del hilo de Swing.
- **Modelo:** los DTOs y `AppEvents`, que actúa como Subject del patrón Observer.

Cuando una acción cambia datos, o cuando llega una notificación nueva producida por los eventos del backend, todas las ventanas abiertas se refrescan solas.

## 7. Patrones de diseño implementados

| # | Patrón (GoF) | Tipo | Problema que resuelve | Implementación |
|---|--------------|------|-----------------------|----------------|
| 1 | **Builder** | Creacional | La pregunta tiene 14 atributos y se construye en dos lugares (creación y rehidratación desde la BD). Un constructor telescópico sería ilegible y propenso a errores. | `Question.builder()…option(texto, correcta)…build()`. `build()` verifica que no haya más de una opción correcta. Lo usan `MultipleChoiceQuestionPlugin` y `QuestionRepositoryAdapter`. |
| 2 | **Factory Method** | Creacional | Habrá varios tipos de pregunta (caso, multimedia…) y el servicio no debe hacer `new` de cada uno. | `QuestionTypePlugin` (Creator) define `generate()`, que valida y luego llama al método fábrica abstracto `createQuestion()`. `MultipleChoiceQuestionPlugin` lo implementa. Los plugins se cargan por reflexión desde `plugins.properties` (microkernel del primer corte). |
| 3 | **Adapter** | Estructural | La aplicación debe enviar correos sin depender de la API de Spring Mail, y poder cambiar a un modo simulado para la demo. | `EmailSenderPort` (Target) tiene dos implementaciones. `SmtpEmailSenderAdapter` adapta `JavaMailSender` (Adaptee). `OutboxFileEmailSenderAdapter` adapta el sistema de archivos. Se elige por configuración (`MAIL_MODE`). |
| 4 | **Facade** | Estructural | El frontend tendría que conocer 4 servicios, sus rutas, DTOs y la cabecera de identidad. | `SaberProApiFacade` expone operaciones por caso de uso (`myQuestions`, `assignReviewers`, `reviewWorkspace`…) y oculta los 4 clientes HTTP y la sesión. También compone datos de varios servicios, por ejemplo la pregunta junto con su revisión. |
| 5 | **State** | Comportamiento | Las reglas del ciclo de vida (qué se puede hacer en cada estado) estaban dispersas en `if/switch`. | La interfaz `QuestionState` tiene 5 estados concretos y cada uno devuelve el estado siguiente o rechaza la transición. `Question` delega en su estado. Agregar un estado no modifica la entidad (OCP). |
| 6 | **Strategy** | Comportamiento | Con 1 a 3 revisores hay que combinar sus conceptos, y la regla académica puede cambiar. | La interfaz `DecisionPolicy` tiene dos implementaciones: `UnanimousApprovalPolicy` (por defecto) y `MajorityPolicy`. Se selecciona con `REVIEW_DECISION_POLICY` sin tocar el agregado. |
| 7 | Chain of Responsibility | Comportamiento (adicional) | La validación estructural tiene varias reglas independientes y debe reportar todos los errores. | `QuestionValidationHandler` con 5 eslabones (`ContentRule`, `OptionsRule`, `CorrectAnswerRule`, `ClassificationRule`, `EvidenceMetadataRule`) enlazados con `linkWith()`. |
| 8 | Template Method | Comportamiento (adicional) | Todas las notificaciones siguen el mismo algoritmo y solo cambian los destinatarios y el texto. | `NotificationTemplate.compose()` es final. Las subclases implementan `recipients()`, `subject()` y `body()`, con 5 plantillas, una por evento. |
| 9 | Observer | Comportamiento (adicional) | Las ventanas abiertas deben refrescarse cuando cambian los datos. | `AppEvents` (Subject) y `AppObserver`, implementado por las vistas. |

Diagrama de clases de los patrones: [`diagramas/07-clases-patrones.puml`](diagramas/07-clases-patrones.puml)

### Principios SOLID

| Principio | Cómo se aplica |
|-----------|----------------|
| **SRP** | Controller, servicio de aplicación, agregado y adaptador tienen responsabilidades separadas. Cada regla de validación es una clase. |
| **OCP** | Nuevos estados, políticas de decisión, plugins, reglas, plantillas de notificación o proveedores de correo se agregan con clases nuevas, sin modificar las existentes. |
| **LSP** | Cualquier `QuestionState`, `DecisionPolicy` o `EmailSenderPort` es intercambiable. Las pruebas usan *fakes* que sustituyen a los adaptadores reales. |
| **ISP** | Los puertos son pequeños y específicos (`UserDirectoryPort` tiene un solo método en review-service). |
| **DIP** | La aplicación depende de puertos. Los adaptadores se conectan en las clases `*BeansConfig`. |

## 8. Decisiones de arquitectura (resumen)

| Decisión | Alternativa descartada | Motivo |
|----------|------------------------|--------|
| RabbitMQ con exchange *topic* | Kafka | El volumen es moderado y RabbitMQ resuelve bien el patrón *competing consumers* con DLQ y una consola visual. Kafka añade complejidad operativa que no se justifica. |
| Eureka + Spring Cloud Gateway | URLs fijas | Permite agregar réplicas sin reconfigurar y da un punto único de entrada con Swagger agregado. |
| Proyección local en review-service | Consultar question-service por REST | Evita el acoplamiento temporal: la revisión funciona aunque question-service esté caído. |
| Identidad por cabecera `X-User-Login` | JWT | Mantiene el alcance del corte. JWT validado en el gateway queda como trabajo futuro (atributo de seguridad). |
| Swing como frontend | Web | Continuidad con la iteración 1 y reutilización de MVC. La separación frontend/backend la garantiza la API REST. |
| Outbox de archivos para correo en la demo | Exigir SMTP | La demo no depende de credenciales. El cambio a SMTP real es solo de configuración (Adapter). |

**Limitaciones conocidas:**

- La publicación del evento ocurre después de guardar en la base, sin *transactional outbox*.
- No hay autenticación con token.
- Ambos puntos se documentan como trabajo futuro.

## 9. Pruebas unitarias

Se ejecutan con `mvn test`. Hay pruebas para **todas las entidades de dominio y servicios de aplicación** de los 4 microservicios: 92 casos que cubren estados, transiciones válidas e inválidas, reglas de validación, políticas de decisión, idempotencia y permisos. Hay además pruebas de la lógica del cliente.

Se usan *fakes* en memoria (`testdoubles`) gracias a la inversión de dependencias, sin necesidad de base de datos ni broker.

## 10. Enlaces

- Repositorio Git: _(URL)_
- Video de sustentación: _(URL de YouTube)_
