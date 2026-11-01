# Banco de Preguntas Saber PRO — Segundo corte (microservicios)

Refactorizacion del monolito del primer corte a una solucion distribuida con
**microservicios** y **arquitectura orientada a eventos** (RabbitMQ), con el
**frontend (Java Swing) separado del backend** y comunicado por **API REST**
a traves de un **API Gateway**.

> El codigo del monolito sigue en el historial de git (commit `74ab78d` y
> anteriores). Lo reutilizable se migro a los servicios; las pantallas que aun
> no existen se pueden consultar alli, por ejemplo:
> `git show 74ab78d:src/main/java/co/unicauca/saberpro/presentation/MyQuestionsFrame.java`

## Documentacion

- Documento de arquitectura del segundo corte: [`docs/ARQUITECTURA.md`](docs/ARQUITECTURA.md),
  tambien en [Word](docs/Documento-Arquitectura-Segundo-Corte.docx) y
  [PDF](docs/Documento-Arquitectura-Segundo-Corte.pdf).
- Diagramas C4, de contextos delimitados, de secuencia y de clases: [`docs/diagramas/`](docs/diagramas)
  (fuente PlantUML `.puml` e imagen `.png`).
- Documentacion del primer corte (monolito): [`docs/primer-corte/`](docs/primer-corte).

## Arquitectura

```
 frontend (Swing) ──REST──> api-gateway :8080 ──┬──> user-service :8081          (Gestion de Usuarios)
                                                ├──> question-service :8082      (Gestion, Validacion y Ciclo de vida de Preguntas)
                                                └──> review-service :8083        (Revision por Pares)

 question-service ──┐                         ┌──> review-service        (question.submitted)
 review-service   ──┴─> RabbitMQ (saberpro.events, topic) ─┼──> question-service      (review.evaluated)
                                                           └──> notification-service :8084 (review.assigned, review.evaluated)
```

| Modulo | Que contiene |
|--------|--------------|
| `common` | Contratos compartidos: eventos de dominio, enums del API (`QuestionStatus` con su color, `DifficultyLevel`, `Role`, `EvaluationVerdict`), reglas de los campos (`QuestionField`), bus de eventos (Observer) y manejo de errores HTTP. |
| `user-service` | Login, registro y consulta de usuarios (SQLite propio). |
| `question-service` | Preguntas: Builder, fabricas por tipo, validacion estructural, estados (State), SQLite propio. Publica `question.created` / `question.submitted`, consume `review.evaluated`. |
| `review-service` | Asignacion de revisores (HU04) y evaluaciones (HU05). Consume `question.submitted`, publica `review.assigned` / `review.evaluated`. |
| `notification-service` | Escucha eventos y envia correos (SMTP o bandeja de salida en archivo). Escala con varias instancias sobre las mismas colas. |
| `api-gateway` | Spring Cloud Gateway: unico punto de entrada y Swagger UI unificado. |
| `frontend` | Cliente Swing; solo conoce el gateway (via `BancoPreguntasFacade`). |

Cada servicio es dueno de sus datos: ninguno lee la base de datos de otro; se
comunican por REST (consultas) o por eventos (cambios).

## Como ejecutar

Requisitos: JDK 17+ y Maven. Para los eventos, Docker (RabbitMQ).

```bash
mvn package -DskipTests          # compila todo y genera los jar

# Opcion A: todo el backend en Docker
docker compose up --build

# Opcion B: desde el IDE / consola
docker compose up rabbitmq       # solo el broker
java -jar user-service/target/user-service-2.0.0.jar
java -jar question-service/target/question-service-2.0.0.jar
java -jar review-service/target/review-service-2.0.0.jar
java -jar notification-service/target/notification-service-2.0.0.jar
java -jar api-gateway/target/api-gateway-2.0.0.jar

# Frontend (con el gateway arriba)
java -jar frontend/target/frontend-2.0.0.jar
# o: mvn -pl frontend exec:java
```

- Swagger UI unificado: http://localhost:8080/swagger-ui.html
- Consola de RabbitMQ: http://localhost:15672 (guest / guest)
- Si el puerto 8080 esta ocupado: `PORT=8090` para el gateway y
  `-Dsaberpro.gateway.url=http://localhost:8090` para el frontend.
- Sin RabbitMQ los servicios arrancan y el API responde, pero los eventos no
  se entregan (el health de los servicios con eventos aparece `DOWN`).
- Datos: `~/BancoPreguntasSaberPro/` (`user-service.db`, `question-service.db`,
  `outbox/`). Usuarios de prueba: `admin/Admin123!`, `autor1/Autor123!`,
  `rev1/Revisor123!`, `rev2/Revisor123!`.
- Correo real: copiar `notification-service/src/main/resources/mail.properties.example`
  como `mail.properties` (esta en `.gitignore`).
- Postman: `postman/BancoPreguntas-API.postman_collection.json` (18 peticiones via gateway).

## Patrones de diseno GoF

| Tipo | Patron | Donde |
|------|--------|-------|
| Creacional | **Builder** | `question-service/.../domain/Question.java` (`Question.builder()`) |
| Creacional | **Factory Method** | `question-service/.../domain/factory/QuestionFactory.java` y sus subclases (`createQuestion`, `createValidationPipeline`), cargadas por reflexion en `QuestionFactoryRegistry` |
| Estructural | **Adapter** | `notification-service/.../channel/SmtpEmailChannelAdapter.java` adapta `SmtpMailClient` a `NotificationChannel` |
| Estructural | **Facade** | `frontend/.../facade/BancoPreguntasFacade.java` |
| Comportamiento | **State** | `question-service/.../domain/state/` (Borrador, PendienteRevision, Aprobada, Rechazada, Eliminada) |
| Comportamiento | **Observer** | `common/.../events/DomainEventBus.java` (observadores: log y `RabbitEventForwarder` hacia el broker); en el frontend, `Subject`/`Observer` para refrescar pantallas |

## Estado de los issues de Jira

Hechos:

- **PSP-14** y **PSP-15**: formulario `frontend/.../ui/CreateQuestionFrame.java`
  y reglas `common/.../contract/QuestionField.java`.
- **PSP-8 (HU02), subtareas PSP-20 a PSP-23**: el autor envia su pregunta de Borrador a
  Pendiente de revision con el boton "Enviar a revision" de "Mis preguntas".
  `QuestionApplicationService.submitForReview` valida el autor (403), el patron State decide
  la transicion (una no permitida responde **409**) y se publica `question.submitted`, que el
  review-service consume para listar la pregunta como pendiente de asignar (HU04). Cada estado
  se ve con su color y hay una leyenda. Pruebas en `SubmitForReviewTest` y `QuestionControllerHu02Test`.
- **PSP-6 (HU01), subtareas PSP-16 a PSP-19**: crear preguntas. `QuestionApplicationService.create`
  normaliza los datos, elige la fabrica del tipo (Factory Method), aplica la validacion
  estructural completa y responde **400 con todos los errores juntos** (`QuestionPipeline.validateAll`),
  guarda la pregunta en Borrador y publica `question.created`. El formulario muestra los errores
  del servidor como lista. Pruebas en `CreateQuestionTest` y `QuestionControllerHu01Test`.
- **PSP-9 (HU03), subtareas PSP-24 a PSP-29**: listado, busqueda (enunciado, contexto y tema),
  filtros, paginacion y edicion de las preguntas del autor (403 si no es el autor, 409 si no
  esta en Borrador). Backend en `QuestionApplicationService.search / update` y pantalla
  `frontend/.../ui/MyQuestionsFrame.java` (menu "Mis preguntas" del autor).
  Pruebas en `ListAndEditQuestionsTest`, `QuestionControllerHu03Test` y `MyQuestionsQueryTest`.
- **PSP-44 (HU05, PSP-45 a PSP-50)**: el revisor lista sus preguntas asignadas,
  las lee, las evalua y deja observaciones; la pregunta pasa a Aprobada o
  Rechazada y se notifica al autor y a los administradores.
  - Backend: `ReviewApplicationService` (`listAssignedTo`, `evaluate`, `listEvaluations`),
    `EvaluationPolicy`, `QuestionApplicationService.applyEvaluation` y
    `NotificationService.onQuestionEvaluated`.
  - Frontend: `AssignedQuestionsFrame`, `QuestionDetailDialog` (tambien muestra las
    observaciones, para el autor) y `EvaluationDialog`.
  - Reglas: solo se evalua una pregunta asignada al revisor y Pendiente de
    revision; **el primer revisor asignado que evalua decide el estado** y despues
    la pregunta no admite mas evaluaciones; al rechazar, las observaciones son
    obligatorias (maximo 2000 caracteres).
  - Para probarla de punta a punta: crear la pregunta (HU01), enviarla a revision (HU02)
    y asignarle revisores (HU04).

Lo pendiente esta marcado en el codigo con `TODO(PSP-xx)` y, en el API,
responde **501 Not Implemented** con el issue. Para encontrarlos:
`git grep -n "TODO(PSP-"`.

| Issue | Que falta | Donde |
|-------|-----------|-------|
| PSP-30 | Registrar/listar preguntas pendientes desde el evento | `ReviewApplicationService.registerPendingQuestion / listPendingQuestions` |
| PSP-31 a PSP-33 | Asignar 1 a 3 revisores (`ReviewPolicy` ya valida) y persistir | `ReviewApplicationService.assignReviewers`, `InMemoryReviewRepository` |
| PSP-34 | Correo a los revisores | `NotificationService.onReviewersAssigned` |
| PSP-35 | Pruebas | `review-service/src/test` |

En el frontend, `BancoPreguntasFacade` ya expone todas las operaciones; solo
faltan las pantallas (ver los `TODO` en `MainFrame`).

## Pruebas

`mvn test` ejecuta 122 pruebas JUnit 5 en los 7 modulos (entidades, servicios,
patrones, repositorios SQLite, cliente HTTP y fachada del frontend).
