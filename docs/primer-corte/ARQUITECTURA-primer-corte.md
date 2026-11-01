# Documento de arquitectura - Banco de Preguntas Saber PRO (Primer corte)

Material base para el documento de arquitectura y el video de sustentación.
Las secciones siguen el orden que pide el enunciado de entregables. Portada,
pantallazos, tablero del sprint y URLs los completa el equipo.

---

## 1. Introducción

Aplicación de escritorio Java (Swing) para construir y administrar un banco
de preguntas que apoya la preparación de los estudiantes para las pruebas
Saber Pro. Es un **monolito en capas** (presentación, dominio/servicio y
acceso a datos, más una capa transversal) que usa el **micro patrón MVC con
Observer**. La creación de preguntas se resuelve con un **Microkernel** y un
pipeline de **Tuberías y Filtros**. Se aplican los principios **SOLID** y
varios patrones **GoF** para favorecer la modificabilidad.

En esta primera iteración se implementan las 4 historias de usuario de alto
valor del primer corte (HU01 a HU04).

---

## 2. Historias de usuario y criterios de aceptación

### HU01 - Crear pregunta de selección múltiple con única respuesta

> Yo como **autor de preguntas** necesito crear preguntas de selección
> múltiple con única respuesta acorde a los principios del Diseño Centrado en
> Evidencia para alimentar el banco de preguntas Saber Pro.

| # | Criterio de aceptación |
|---|------------------------|
| CA1 | **Dado** un autor autenticado, **cuando** abre "Crear pregunta", **entonces** el formulario pide Contexto, Pregunta directa, 4 opciones, Respuesta correcta, Justificación, Bibliografía, Competencia, Tema, Subtema y Nivel de dificultad. |
| CA2 | **Dado** un formulario completo y consistente, **cuando** pulsa "Guardar", **entonces** la pregunta se guarda en estado **Borrador** y queda asociada al autor. |
| CA3 | **Dado** un formulario incompleto o inconsistente, **cuando** pulsa "Guardar", **entonces** el sistema aplica la validación estructural, **no** guarda la pregunta y muestra el motivo exacto. |
| CA4 | La validación estructural rechaza: contexto vacío o pregunta directa demasiado corta; distinto de 4 opciones, opciones vacías o repetidas; clasificación incompleta; respuesta correcta que no esté entre las opciones; justificación o bibliografía vacías. |
| CA5 | La respuesta correcta solo se puede elegir entre las 4 opciones escritas (lista desplegable). |

*Dónde está:* `CreateQuestionFrame` → `QuestionService.crearPregunta()` →
`QuestionMicrokernel` → `MultipleChoiceQuestionPlugin` → 5 filtros de
`microkernel/pipeline/filters`.

### HU02 - Enviar pregunta a revisión (estados con colores)

> Yo como **autor de preguntas** necesito cambiar el estado de mis preguntas
> de "borrador" a "Pendiente de revisión" para que el administrador les
> asigne al menos un revisor.

| # | Criterio de aceptación |
|---|------------------------|
| CA1 | **Dada** una pregunta propia en **Borrador**, **cuando** el autor la selecciona y pulsa "Enviar a revisión", **entonces** pasa a **Pendiente de revisión**. |
| CA2 | **Dada** una pregunta que **no** está en Borrador, **cuando** intenta enviarla, **entonces** el sistema lo impide y explica por qué. |
| CA3 | Un autor **no** puede cambiar el estado de preguntas de otro autor. |
| CA4 | Cada estado se muestra con **su propio color** en la tabla, y hay una **leyenda** de colores: Borrador (amarillo), Pendiente de revisión (azul), En revisión (morado), Aprobada (verde), Rechazada (rojo), Eliminada (gris). |
| CA5 | Al cambiar el estado, la vista "Mis preguntas" se actualiza sola (patrón Observer). |

*Dónde está:* `MyQuestionsFrame` → `QuestionService.marcarPendienteDeRevision()`;
colores en `QuestionState.getColor()`.

### HU03 - Listar mis preguntas con paginación y filtros

> Yo como **autor de preguntas** necesito listar las preguntas que he creado
> para poder más adelante verlas y editarlas.

| # | Criterio de aceptación |
|---|------------------------|
| CA1 | El autor ve **solo sus** preguntas, ordenadas de la más reciente a la más antigua. |
| CA2 | La lista está **paginada** (5 por página) con botones Anterior/Siguiente y el texto "Página X de Y (N preguntas)"; no se puede avanzar más allá de la última página. |
| CA3 | Se puede **filtrar** por estado, por tema y por palabra clave (busca en la pregunta y en el contexto), y los filtros se combinan. |
| CA4 | Con **"Ver detalle"** (o doble clic) se ven todos los campos de la pregunta, con la respuesta correcta marcada. |
| CA5 | Con **"Editar"** se abre el formulario precargado; solo se editan preguntas en **Borrador**, y los cambios pasan por la misma validación estructural de HU01. |

*Dónde está:* `MyQuestionsFrame` → `QuestionService.listarMisPreguntas()` /
`actualizarPregunta()` → `SQLiteQuestionRepository.search()`.

### HU04 - Asignar revisores y notificar por correo

> Yo como **administrador** necesito asignar al menos un revisor de las
> preguntas en estado "Pendiente de revisión" para que las preguntas sean
> revisadas por otros docentes.

| # | Criterio de aceptación |
|---|------------------------|
| CA1 | El administrador ve **solo** las preguntas en estado **Pendiente de revisión**. |
| CA2 | Puede seleccionar **uno o varios** revisores; si no selecciona ninguno, el sistema no permite asignar. |
| CA3 | Al asignar, la pregunta pasa a **En revisión** y guarda los revisores asignados. |
| CA4 | El sistema **envía un correo** a cada revisor asignado. Si no hay servidor de correo configurado, el correo queda registrado en la bandeja local `~/BancoPreguntasSaberPro/outbox/`. |
| CA5 | El administrador ve, para cada revisor, si el correo se **envió** o se **simuló**. |

*Dónde está:* `AssignReviewersFrame` → `QuestionService.asignarRevisores()` →
`SmtpEmailService` (SMTP con STARTTLS/SSL, solo JDK).

---

## 3. Prototipos y test de usabilidad

**Prototipos:** pegar un pantallazo de cada ventana (Login, Menú, Crear
pregunta, Mis preguntas con la leyenda de colores, Detalle, Asignar
revisores). Si se hicieron bocetos antes de programar, mostrarlos junto al
pantallazo final.

**Test de usabilidad (propuesta):** pedir a 3-5 compañeros que hagan estas
tareas sin ayuda y registrar tiempo, errores y comentarios.

| Tarea | Éxito si... | Tiempo | Errores | Comentarios |
|-------|-------------|--------|---------|-------------|
| T1. Iniciar sesión como autor1 y crear una pregunta completa | Aparece "Pregunta creada" | | | |
| T2. Intentar guardar una pregunta sin bibliografía | Entiende el mensaje de error | | | |
| T3. Encontrar sus preguntas del tema "X" | Usa el filtro de tema | | | |
| T4. Editar una pregunta en borrador | Guarda el cambio | | | |
| T5. Enviar una pregunta a revisión e identificar su nuevo estado por el color | Reconoce el azul | | | |
| T6. Como admin, asignar dos revisores | Ve el mensaje de correos | | | |

Al final, cada participante califica de 1 a 5: facilidad de uso, claridad
de los mensajes y utilidad de los colores. Reportar el promedio y 2-3
mejoras identificadas.

---

## 4. Atributos de calidad de la iteración

| Atributo | Por qué es relevante | Cómo se atiende |
|----------|----------------------|-----------------|
| **Modificabilidad** (principal) | Se esperan nuevos tipos de pregunta, reglas de validación y cambios de almacenamiento. | Microkernel con plugins por Reflexión, pipeline de filtros, interfaces inyectadas (DIP). |
| **Usabilidad** | Los docentes deben ver el estado de un vistazo y encontrar sus preguntas. | Colores por estado, leyenda, paginación, filtros, mensajes de error claros. |
| **Seguridad** | Hay usuarios con roles y contraseñas. | Contraseñas cifradas con PBKDF2 y sal; política de contraseñas; menú según rol. |
| **Confiabilidad** | Guardar preguntas no debe fallar en silencio. | SQLite en modo WAL con `busy_timeout`; errores visibles; el correo cae a la bandeja local si SMTP falla. |
| **Testeabilidad** | La rúbrica exige pruebas del dominio. | Servicios con dependencias inyectadas y dobles en memoria para las pruebas. |

### Escenario de calidad de modificabilidad

| Parte | Descripción |
|-------|-------------|
| **Fuente** | Equipo de desarrollo. |
| **Estímulo** | Se solicita un nuevo tipo de pregunta (por ejemplo, "pregunta con imagen") con reglas de validación propias. |
| **Artefacto** | Microkernel de preguntas (`QuestionMicrokernel`, plugins y pipeline). |
| **Entorno** | Tiempo de desarrollo, con el sistema ya en funcionamiento. |
| **Respuesta** | Se crea una clase que implementa `QuestionPlugin` (con su propio pipeline, reutilizando filtros existentes o agregando uno nuevo) y se registra en `plugins.properties`. El núcleo, los servicios y las demás capas **no se modifican**. |
| **Medición de la respuesta** | 0 líneas modificadas en `QuestionMicrokernel` y `QuestionService`; 1 clase nueva + 1 línea en `plugins.properties`; menos de 4 horas de trabajo; todas las pruebas existentes siguen pasando. |
| **Resultado esperado** | El nuevo tipo queda disponible sin riesgo de romper los tipos existentes. Ya se demostró con `CaseQuestionPlugin` y `MultimediaQuestionPlugin`, agregados sin tocar el núcleo. |

---

## 5. Arquitectura y diseño (modelo C4 + UML)

Los diagramas están en `docs/diagramas/`: el `.puml` es el fuente (PlantUML con
C4-PlantUML) y el `.png` la imagen lista para el documento.

| Diagrama | Archivo | Qué muestra |
|----------|---------|-------------|
| C4 nivel 1 - Contexto | `c4-contexto.png` | Actores (autor, administrador, revisor), el sistema y el servidor de correo. |
| C4 nivel 2 - Contenedores | `c4-contenedores.png` | Aplicación de escritorio (Swing), API REST (Spring Boot), base de datos SQLite y servidor SMTP. |
| C4 nivel 3 - Componentes | `c4-componentes.png` | Dentro de la app de escritorio: vistas, servicios, microkernel, plugins, pipeline, repositorios y correo. |
| UML - Clases | `clases.png` | Microkernel + Tuberías y Filtros, y el servicio con Observer e interfaces inyectadas. |

### Capas

```
presentation   Vistas Swing (MVC: Vista) + AppContext
service        QuestionService, UserService (MVC: Controlador), correo, políticas
domain         Entidades: Question, User, estados, roles (MVC: Modelo)
access         Repositorios SQLite detrás de interfaces
infra          Observer / Subject (capa transversal)
microkernel    Núcleo, plugins y pipeline de validación
api            API REST (Taller de microservicios)
```

---

## 6. Patrones de diseño y principios SOLID

| Patrón | Tipo | Dónde | Para qué en este problema |
|--------|------|-------|---------------------------|
| **Observer** | GoF comportamiento | `infra/Subject`, `infra/Observer`, `MyQuestionsFrame` | Cuando cambia una pregunta (se crea, se edita, cambia de estado), la vista "Mis preguntas" se refresca sola sin que el servicio conozca las ventanas. |
| **MVC** | Micro patrón arquitectónico | `presentation` / `service` / `domain` | Separa lo que se ve (Swing) de la lógica y los datos. |
| **Strategy** | GoF comportamiento | `IPasswordPolicy` → `DefaultPasswordPolicy`; `IPasswordHasher` → `PBKDF2PasswordHasher`; `IEmailService` → `SmtpEmailService` | Cambiar la política de contraseñas, el algoritmo de cifrado o el medio de notificación sin tocar `UserService` ni `QuestionService`. |
| **Factory Method + Singleton** | GoF creacional | `QuestionRepositoryFactory`, `UserRepositoryFactory` | Un único punto que decide qué repositorio crear (archivo o memoria). |
| **Repository** | Patrón de acceso a datos | `IQuestionRepository`, `IUserRepository` | Los servicios no saben que hay SQLite detrás. |
| **Microkernel (plug-in)** | Arquitectónico | `QuestionMicrokernel` + `plugins.properties` + Reflexión | Agregar tipos de pregunta sin modificar el núcleo. |
| **Tuberías y Filtros** | Arquitectónico | `QuestionPipeline` + 5 filtros | Validación estructural de HU01 por etapas independientes y reutilizables. |
| **Adapter** | GoF estructural | `CreateQuestionFrame.SimpleDocListener` | Adapta los 3 métodos de `DocumentListener` a una sola acción. |
| **DTO** | Patrón de diseño | `QuestionRequest`, `OperationResult`, `PagedResult` | Transportar datos entre capas sin exponer las entidades. |

| Principio | Evidencia |
|-----------|-----------|
| **S**RP | `User` solo guarda datos; validar (`IPasswordPolicy`), cifrar (`IPasswordHasher`) y persistir (`IUserRepository`) están en clases distintas. Cada filtro valida una sola cosa. |
| **O**CP | Nuevos tipos de pregunta y nuevos filtros se agregan sin modificar el núcleo ni el pipeline. |
| **L**SP | Cualquier `IQuestionRepository` (SQLite o el doble en memoria de las pruebas) funciona igual en `QuestionService`. |
| **I**SP | Interfaces pequeñas: `Observer` (1 método), `QuestionFilter` (2), `IEmailService`, `IPasswordPolicy`. |
| **D**IP | `QuestionService` y `UserService` reciben interfaces por constructor; `AppContext` arma las piezas concretas. |

---

## 7. Pruebas unitarias automatizadas

Ejecutar con `mvn test` (o desde la pestaña *Testing* de VS Code). 13 clases de
prueba en `src/test/java`, que cubren todas las entidades y servicios:

| Área | Clases de prueba |
|------|------------------|
| Entidades del dominio | `QuestionTest`, `UserTest`, `QuestionRequestTest`, `EnumsTest` |
| Servicios | `QuestionServiceTest` (HU01-HU04 de punta a punta), `UserServiceTest`, `DefaultPasswordPolicyTest`, `PBKDF2PasswordHasherTest`, `SmtpEmailServiceTest`, `SupportClassesTest` |
| Microkernel y pipeline | `QuestionMicrokernelTest`, `QuestionPipelineTest` |
| Transversal | `SubjectTest` (Observer) |

Las pruebas de servicios usan dobles en memoria
(`testdoubles/InMemory*RepositoryFake`), así no dependen de SQLite.

---

## 8. Enlaces

- Video de YouTube: *(completar)*
- Repositorio Git: *(completar)*
- Tablero del Sprint 1 (Jira/Trello): *(pantallazo)*
