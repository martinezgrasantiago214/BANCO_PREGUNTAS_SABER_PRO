# Banco de Preguntas Saber PRO

Proyecto **unificado** a partir de los tres repositorios entregados
(`GestionUsuariosSOLID`, `TallerCapasMVC`, `lisw2_t5_g4`), reescrito como una
sola aplicacion de escritorio Java Swing que implementa las 4 historias de
usuario del primer corte, siguiendo la arquitectura exigida por los talleres
de la materia:

- **Arquitectura en capas** (`presentation` / `service` / `domain` / `access` / `infra`).
- **Micro patron MVC + Observer** (Taller de Capas y MVC).
- **Microkernel + Tuberias y Filtros**, con carga de plugins por **Reflexion**
  (Taller de Microkernel).
- **Principios SOLID** en la gestion de usuarios (Taller SOLID), reutilizados
  para autenticar autores, administradores y revisores.
- **API REST con Spring Boot** (Taller de Microservicios): expone el mismo
  banco de preguntas por HTTP (GET, POST, PUT, DELETE), reutilizando los
  servicios, el microkernel y la base de datos de la aplicacion de escritorio.

## Como se cubre cada historia de usuario

| HU | Descripcion | Donde esta implementada |
|----|-------------|--------------------------|
| HU01 | Crear preguntas de seleccion multiple con unica respuesta, con Contexto, Pregunta directa, 4 distractores, Respuesta correcta, Justificacion, Bibliografia, Competencia, Tema, Subtema y Nivel de dificultad. Al grabar se aplica validacion estructural. | `presentation/CreateQuestionFrame` -> `service/QuestionService.crearPregunta()` -> `microkernel/core/QuestionMicrokernel` -> `microkernel/plugins/MultipleChoiceQuestionPlugin` -> pipeline de 5 filtros en `microkernel/pipeline/filters` (HU03) |
| HU02 | Cambiar el estado de "borrador" a "Pendiente de revision", visualizando los estados con colores. | `presentation/MyQuestionsFrame` (columna de estado coloreada, `QuestionState.getColor()`) -> `service/QuestionService.marcarPendienteDeRevision()` |
| HU03 | Listar las preguntas propias, con paginacion y filtros. | `presentation/MyQuestionsFrame` (filtros de estado/tema/palabra clave + paginacion) -> `service/QuestionService.listarMisPreguntas()` -> `access/SQLiteQuestionRepository.search()` |
| HU04 | El administrador asigna al menos un revisor a preguntas "Pendiente de revision"; el sistema envia un correo de notificacion. | `presentation/AssignReviewersFrame` -> `service/QuestionService.asignarRevisores()` -> `service/SmtpEmailService` |

La capa de usuarios (login, registro, roles Administrador / Autor de
preguntas / Revisor) es la del Taller SOLID, adaptada al mismo paquete base;
sigue aplicando SRP, OCP, ISP y DIP (interfaces `IUserRepository`,
`IPasswordPolicy`, `IPasswordHasher` inyectadas por constructor).

## Arquitectura Microkernel + Tuberias y Filtros (HU01/HU03)

- **Nucleo (`QuestionMicrokernel`)**: guarda el banco de preguntas en un
  `Map<String, Question>`, lee `plugins.properties` y **carga cada plugin por
  Reflexion** (`Class.forName(...).getDeclaredConstructor().newInstance()`),
  sin conocer sus clases concretas.
- **Plugins** (contrato comun `QuestionPlugin`): `MultipleChoiceQuestionPlugin`
  (principal, exigido por HU01 y con el pipeline completo de 5 filtros),
  `CaseQuestionPlugin` y `MultimediaQuestionPlugin` (demuestran que se pueden
  agregar nuevos tipos de pregunta sin tocar el nucleo).
- **Pipeline (Tuberias y Filtros)**: `ContentValidationFilter`,
  `OptionsValidationFilter`, `ClassificationFilter`,
  `CorrectAnswerValidationFilter` y `MetadataValidationFilter` (justificacion,
  bibliografia y autor). El pipeline se detiene en el primer filtro que
  falla y expone el mensaje de error a la interfaz grafica.

## Estructura de paquetes

```
co.unicauca.saberpro
├── domain          Entidades: User, Question, Role, QuestionState, DifficultyLevel...
├── access           IUserRepository/SQLiteUserRepository, IQuestionRepository/SQLiteQuestionRepository
├── service           UserService, QuestionService, IEmailService/SmtpEmailService, PagedResult...
├── microkernel
│    ├── core          QuestionMicrokernel (nucleo, reflexion)
│    ├── common         Contrato QuestionPlugin
│    ├── pipeline        QuestionFilter/QuestionPipeline + filtros
│    └── plugins         MultipleChoice / Case / Multimedia
├── api              API REST (Spring Boot): controller / dto / exception / config
├── infra             Observer, Subject (patron Observer)
└── presentation       LoginFrame, RegisterFrame, MainFrame, CreateQuestionFrame,
                        MyQuestionsFrame, AssignReviewersFrame
```

## Como ejecutar

Requiere JDK 17+ y Maven (con acceso a Internet para descargar
`sqlite-jdbc` y `junit-jupiter` la primera vez).

```bash
mvn compile exec:java
# o, para generar un jar ejecutable con dependencias:
mvn package
java -jar target/BancoPreguntasSaberPro-jar-with-dependencies.jar
```

Al primer arranque se crea la base de datos SQLite en una ubicacion **fija**,
`~/BancoPreguntasSaberPro/bancopreguntas.db` (dentro de la carpeta personal
del usuario del sistema operativo, gestionada por `access/AppPaths`), sin
importar desde que carpeta se ejecute la aplicacion (terminal, IDE, jar,
etc.). Asi los datos siempre son los mismos entre una ejecucion y otra. Se
siembran ademas 4 usuarios de prueba (`access/DataInitializer`):

| Usuario | Contrasena | Rol |
|---------|------------|-----|
| admin  | Admin123!   | Administrador |
| autor1 | Autor123!   | Autor de preguntas |
| rev1   | Revisor123! | Revisor |
| rev2   | Revisor123! | Revisor |

### Si "Mis preguntas" no muestra datos

La capa de acceso a datos ya **no falla en silencio**: si la base de datos
no pudo abrirse o una consulta fallo (por ejemplo, si el jar se ejecuto sin
el driver `sqlite-jdbc` empaquetado), aparece un cuadro de dialogo con el
error real apenas arranca la aplicacion o al abrir "Mis preguntas" /
"Asignar revisores", en vez de dejar la tabla vacia sin explicacion. Si ve
ese dialogo, la causa mas probable es que el jar se genero/ejecuto sin las
dependencias incluidas: use `mvn package` (no `mvn compile`) y ejecute el
jar `-jar-with-dependencies.jar` indicado arriba, o `mvn compile exec:java`.
El detalle completo de cualquier error tambien se imprime siempre en la
consola.

## API REST (Taller de Microservicios)

La API es una **segunda capa de presentacion** (HTTP/JSON) sobre las mismas
capas `service` / `microkernel` / `access`. No duplica logica: el
`QuestionController` llama a `QuestionService`, que a su vez usa el
microkernel (con su pipeline de filtros) y `SQLiteQuestionRepository`. Ambas
aplicaciones comparten la base de datos `~/BancoPreguntasSaberPro/bancopreguntas.db`,
asi que una pregunta creada por Postman aparece en "Mis preguntas" del
escritorio (al volver a abrir o refrescar la ventana) y viceversa.

```
co.unicauca.saberpro.api
├── BancoPreguntasApiApplication   @SpringBootApplication (main de la API)
├── config/ApiConfig               Beans: repositorios, microkernel, servicios
├── controller/                    QuestionController, UserController, PluginController
├── dto/                           QuestionDTO (entrada), QuestionResponseDTO, ApiErrorDTO...
└── exception/                     ApiExceptionHandler (@RestControllerAdvice), 400/404
```

### Ejecutar la API

```bash
mvn spring-boot:run
```

o, desde el IDE, ejecutar `co.unicauca.saberpro.api.BancoPreguntasApiApplication`.
Queda escuchando en `http://localhost:8080`. La app Swing se sigue
ejecutando igual que antes (`mvn compile exec:java` / `MainApp`).

### Endpoints

| Metodo | Ruta | Descripcion | Respuesta |
|--------|------|-------------|-----------|
| GET | `/api/questions?author=&state=&topic=&keyword=&page=1&size=10` | Listar con filtros y paginacion (HU03) | 200 |
| GET | `/api/questions/{id}` | Consultar una pregunta | 200 / 404 |
| POST | `/api/questions` | Crear: pasa por el microkernel y el pipeline de filtros (HU01) | 201 / 400 |
| PUT | `/api/questions/{id}` | Editar (solo el autor y en estado Borrador; se revalida con el pipeline) | 200 / 400 / 404 |
| DELETE | `/api/questions/{id}` | Eliminacion logica (estado `ELIMINADA`) | 200 / 400 / 404 |
| PATCH | `/api/questions/{id}/submit-review` | Enviar a revision (HU02). Body: `{"authorLogin":"autor1"}` | 200 / 400 |
| PATCH | `/api/questions/{id}/reviewers` | Asignar revisores (HU04). Body: `{"reviewers":["rev1"]}` | 200 / 400 |
| GET | `/api/users`, `/api/users/reviewers` | Usuarios (sin hash de contrasena) | 200 |
| GET | `/api/plugins` | Plugins cargados por Reflexion | 200 |

Ejemplo de body para POST/PUT:

```json
{
  "type": "MULTIPLE_CHOICE",
  "context": "Una empresa necesita que su banco de preguntas admita nuevos tipos...",
  "directQuestion": "¿Que patron permite agregar funcionalidad mediante plugins?",
  "distractors": ["Microkernel", "Cliente-Servidor", "Capas", "Broker"],
  "correctAnswer": "Microkernel",
  "justification": "El patron Microkernel separa un nucleo minimo de sus plugins.",
  "bibliography": "Richards, M. Software Architecture Patterns. O'Reilly, 2015.",
  "competence": "Arquitectura de software",
  "topic": "Patrones arquitectonicos",
  "subtopic": "Microkernel",
  "difficultyLevel": "MEDIO",
  "authorLogin": "autor1"
}
```

En `postman/BancoPreguntas-API.postman_collection.json` hay una coleccion
lista para importar con las 13 peticiones en orden de demostracion (el `id`
creado por el POST se guarda automaticamente en la variable `questionId`).

### Decisiones de diseno

- **Sin JPA sobre la entidad de dominio.** El taller advierte que anotar la
  entidad de negocio con JPA rompe el SRP. Aqui `Question` sigue siendo un
  POJO de dominio y la persistencia continua detras de `IQuestionRepository`
  (DIP), por lo que la API no requirio cambiar la capa de acceso a datos.
- **DTOs propios de la API** (`QuestionDTO`, `QuestionResponseDTO`): el JSON
  no depende de la forma interna de las entidades.
- **DELETE logico**: usa el estado `ELIMINADA` que ya existia en el ciclo de
  vida del dominio, conservando trazabilidad.
- **Metodos `synchronized` en los controladores**: el repositorio SQLite
  comparte una unica conexion JDBC y Tomcat atiende en varios hilos.

## Notificacion por correo (HU04)

`SmtpEmailService` implementa un cliente SMTP minimo escrito solo con
`java.net.Socket` (sin dependencias externas). Para enviar correos reales,
copie `src/main/resources/mail.properties.example` a
`src/main/resources/mail.properties` y complete host/usuario/clave. **Si ese
archivo no existe** (configuracion por defecto), cada notificacion se
registra igualmente como un archivo `.txt` en la carpeta `outbox/` y en
consola, de modo que HU04 sea verificable sin depender de un servidor SMTP.

## Pruebas unitarias

`src/test/java` contiene pruebas JUnit 5 para:
- Cada filtro del pipeline y su combinacion completa (`QuestionPipelineTest`).
- El nucleo del microkernel, incluida la carga por reflexion (`QuestionMicrokernelTest`).
- `UserService` (registro, politica de contrasenas, login, listado de revisores).
- `QuestionService`, cubriendo extremo a extremo las 4 historias de usuario
  (`QuestionServiceTest`).
- Reglas basicas de la entidad `Question` (`QuestionTest`).

Ejecutar con `mvn test`. Las pruebas usan dobles en memoria
(`testdoubles/InMemoryUserRepositoryFake`, `InMemoryQuestionRepositoryFake`)
para no depender de SQLite.

> Nota: en el entorno donde se genero esta entrega no hubo acceso a Maven
> Central, por lo que la logica completa (microkernel, pipeline, servicios,
> las 4 HU de punta a punta) se verifico manualmente compilando con `javac`
> y ejecutando un arnes de pruebas equivalente sin dependencias externas
> (21/21 verificaciones correctas). Al compilar con `mvn test` en un entorno
> con Internet, estas mismas pruebas quedan disponibles como JUnit 5.

## Origen del codigo reutilizado

- **GestionUsuariosSOLID**: `Role`, `User`, `IUserRepository`,
  `SQLiteUserRepository`, `IPasswordPolicy`/`DefaultPasswordPolicy`,
  `IPasswordHasher`/`PBKDF2PasswordHasher`, `UserService`, `OperationResult`.
- **TallerCapasMVC**: patron `Observer`/`Subject`, idea de capas
  `presentation/domain/access/infra` y la nocion de notificar vistas ante un
  cambio de estado (usada aqui en `MyQuestionsFrame`).
- **lisw2_t5_g4**: `QuestionMicrokernel` (con reflexion), `QuestionPlugin`,
  `QuestionPipeline`/`QuestionFilter` y los 4 filtros de validacion
  propuestos en el taller (mas un quinto filtro de metadatos agregado para
  cubrir todos los campos de HU01).
