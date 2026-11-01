# Banco de Preguntas Saber PRO — Segundo corte (microservicios)

Refactorizacion del monolito del primer corte a una solucion distribuida con
**microservicios** y **arquitectura orientada a eventos** (RabbitMQ), con el
**frontend (Java Swing) separado del backend** y comunicado por **API REST**
a traves de un **API Gateway**.

> El codigo del monolito del primer corte sigue en el historial de git (commit
> `74ab78d` y anteriores) y su documentacion en [`docs/primer-corte/`](docs/primer-corte).

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

**Opcion C: VS Code con un clic.** Encender RabbitMQ una vez (`brew services start rabbitmq`
o `docker compose up rabbitmq`), abrir *Run and Debug* (`Cmd/Ctrl + Shift + D`), elegir
**"Banco de Preguntas (todo el sistema)"** y darle Play: arranca los 4 servicios, el gateway y
el frontend (configuracion en `.vscode/launch.json`). Esperar unos 20 segundos antes de iniciar sesion.

- Swagger UI unificado: http://localhost:8080/swagger-ui.html
- Consola de RabbitMQ: http://localhost:15672 (guest / guest)
- Si el puerto 8080 esta ocupado: `PORT=8090` para el gateway y
  `-Dsaberpro.gateway.url=http://localhost:8090` para el frontend.
- Sin RabbitMQ los servicios arrancan y el API responde, pero los eventos no
  se entregan (el health de los servicios con eventos aparece `DOWN`).
- Datos: `~/BancoPreguntasSaberPro/` (`user-service.db`, `question-service.db`,
  `review-service.db`, `outbox/`). Usuarios de prueba: `admin/Admin123!`, `autor1/Autor123!`,
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

## Historias de usuario implementadas

Las 5 historias del segundo corte estan completas, en `main` y en estado *Listo* en Jira.
Los issues de GitHub de cada subtarea estan cerrados.

| HU | Jira | Que hace | Donde |
|----|------|----------|-------|
| HU01 Crear preguntas | PSP-6 (PSP-14 a PSP-19) | El autor crea preguntas de seleccion multiple. La fabrica del tipo (Factory Method) aplica la validacion estructural completa y responde **400 con todos los errores juntos**; la pregunta valida queda en Borrador y se publica `question.created`. | `QuestionApplicationService.create`, `QuestionPipeline.validateAll`, `CreateQuestionFrame` |
| HU02 Cambiar estado | PSP-8 (PSP-20 a PSP-23) | Boton "Enviar a revision": Borrador -> Pendiente de revision (patron State). Solo el autor (403) y solo desde Borrador (409). Publica `question.submitted`. Estados con colores y leyenda. | `QuestionApplicationService.submitForReview`, `MyQuestionsFrame` |
| HU03 Listar mis preguntas | PSP-9 (PSP-24 a PSP-29) | Listado del autor con busqueda (enunciado, contexto y tema), filtros por estado y tema, paginacion y edicion de Borradores (403 si no es el autor, 409 si no esta en Borrador). | `QuestionApplicationService.search / update`, `MyQuestionsFrame` |
| HU04 Asignar revisores | PSP-11 (PSP-30 a PSP-35) | El review-service registra las preguntas pendientes a partir de `question.submitted`; el administrador asigna de 1 a 3 revisores (validados contra user-service) y se publica `review.assigned`, con lo que el notification-service envia un correo a cada revisor. | `ReviewApplicationService.registerPendingQuestion / assignReviewers`, `ReviewPolicy`, `NotificationService.onReviewersAssigned`, `AssignReviewersFrame` |
| HU05 Evaluar preguntas asignadas (nueva) | PSP-44 (PSP-45 a PSP-50) | El revisor lista sus preguntas asignadas, las lee, las aprueba o rechaza y deja observaciones (obligatorias al rechazar). **El primer revisor asignado que evalua decide el estado.** Se publica `review.evaluated`: el question-service cambia el estado y se notifica al autor y a los administradores. | `ReviewApplicationService.listAssignedTo / evaluate`, `EvaluationPolicy`, `QuestionApplicationService.applyEvaluation`, `AssignedQuestionsFrame`, `EvaluationDialog` |

Flujo de punta a punta: crear (HU01) -> enviar a revision (HU02) -> asignar revisores (HU04)
-> evaluar (HU05); el autor ve el resultado y las observaciones en "Mis preguntas" (HU03).

## Pruebas

`mvn test` ejecuta **207 pruebas** JUnit 5: entidades y reglas del dominio, servicios
de aplicacion de cada microservicio, patrones, repositorios SQLite, los codigos HTTP del
API (200, 201, 400, 403, 404 y 409) y la logica del frontend. No necesitan RabbitMQ ni
servidor de correo.

| Modulo | Pruebas |
|--------|---------|
| common | 15 |
| user-service | 20 |
| question-service | 94 |
| review-service | 42 |
| notification-service | 17 |
| frontend | 19 |
