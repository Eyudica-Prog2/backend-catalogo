# backend-catalogo

Servicio del catálogo de profesionales de Turnos. Mantiene una **copia propia** del catálogo
que publica la catedra, la sincroniza de forma incremental y expone la búsqueda a los demás
servicios y a la app.

Implementación en **inglés** (clases, comentarios, esquema de BD, logs); esta documentación en
español. Los contratos de la catedra (endpoints, campos JSON, `code`, topics) se respetan
literalmente: `INTEGRATION_REFERENCE-v2.md` (§13–§18) y `PROJECT_STATEMENT-v1.md`.

## Arquitectura

Hexagonal con *vertical slicing*: cada slice es un paquete con su modelo de dominio (puro,
sin Spring), sus casos de uso, sus puertos y su infraestructura.

| Slice | Responsabilidad |
|---|---|
| `catalog` | profesionales, categorías, horarios, búsqueda y sincronización |
| `catedra` | cliente HTTP de la catedra, integración técnica y contratos |
| `user` | registro, login y cuenta del usuario final |
| `shared` | seguridad (JWT), errores, configuración y salud |

Stack: Java 21, Spring Boot 3.5, PostgreSQL (el esquema lo posee **Flyway**, `ddl-auto: validate`),
Redis y Kafka de la catedra como fuentes externas. Nunca H2/SQLite. La API es stateless y
responde `application/problem+json` en cada error, con un `code` estable.

## Sincronización del catálogo

### Fuentes

- **Snapshot** (API técnica de la catedra): copia completa de las tres colecciones.
- **Redis** (namespace `CATEDRA_REDIS_READ_NAMESPACE`, p. ej. `catedra:sync:*`):
  - `current-version` y `oldest-available-version` → ventana de versiones;
  - `metadata` (Hash) → la misma ventana con `publishedAt`;
  - `professional-categories`, `professionals`, `weekly-schedules` (Hash: campo = id,
    valor = JSON) → estado vigente de cada registro;
  - `changes:{version}` (String JSON) → ids afectados por esa versión, nunca entidades completas.
- **Kafka** (topic `CATEDRA_KAFKA_CATALOG_TOPIC`, grupo `CATEDRA_KAFKA_CONSUMER_GROUP_ID`):
  evento `CatalogUpdated` de `schemaVersion=1` que **solo notifica**: los datos siempre salen
  de Redis o de un snapshot.

### Algoritmo incremental

Se ejecuta en `POST /api/internal/sync/run`, en cada mensaje de Kafka y en cada tick del
scheduler:

1. Se lee la ventana publicada en Redis.
2. Si la versión local no está sincronizada o cae fuera de `[oldestAvailableVersion, currentVersion]`
   → **no se intenta avanzar**: se aplica un snapshot completo (recuperación ante discontinuidad;
   el `changes:{version}` ausente ni siquiera se lee).
3. Si la versión local ya es `current-version` → `ALREADY_CURRENT`.
4. En otro caso se aplican `local+1 … current` **en orden y cada versión en su propia
   transacción**: los cambios de una versión se persisten juntos y recién entonces avanza la
   versión local, de modo que reprocesar la misma versión no altera el resultado.
5. Para cada id afectado se lee el estado vigente del Hash correspondiente y se aplica,
   incluido `enabled=false` como baja lógica.

### Recuperación

- **Kafka entrega al menos una vez**: el `eventId` se guarda en la tabla `processed_event`
  **después** de persistir los efectos; un eventId repetido no repite efectos y confirma su
  offset. Los offsets se confirman con `enable-auto-commit: false` y `ack-mode: record`,
  es decir, solo después de persistir.
- **Scheduler periódico** (`APP_SYNC_SCHEDULER_*`, ver `.env.example`) vuelve a comparar la versión
  local con `current-version` en Redis: si se pierde una notificación, la copia se pone al día
  sola en el próximo tick.
- Diagnóstico auxiliar en `alumnos:{groupId}:diagnostics:last-sync` (versión, instante y
  resultado del último intento). Es **solo auxiliar**: nunca es fuente autoritativa.

### Estados del último intento

`lastSyncResult` puede ser `SNAPSHOT_APPLIED`, `INCREMENTAL_APPLIED`, `ALREADY_CURRENT` o
`FAILED`. Con `FAILED`, `lastError` / `lastErrorAt` describen el intento y la copia local
sigue sirviendo la versión `appliedVersion`.

## Endpoints

| Método | Ruta | Acceso | Respuesta |
|---|---|---|---|
| GET | `/api/internal/health` | público | sonda de vida |
| POST | `/api/register` | público | `201` sin cuerpo |
| POST | `/api/authenticate` | público | `200` con `{id_token, token_type, expires_in}` |
| GET | `/api/account` | token | perfil del usuario autenticado, sin password |
| GET | `/api/sync/status` | token | versión local, ventana (`current`/`oldest`), último resultado y último error |
| POST | `/api/internal/sync/run` | token | aplica ahora las versiones pendientes |
| POST | `/api/internal/sync/force` | token | fuerza un snapshot completo |
| GET | `/api/professionals` | token | búsqueda con filtros (categoría, nombre, estado, disponibilidad) |
| GET | `/api/professionals/{id}` | token | detalle con horarios |
| GET | `/api/professional-categories` | token | categorías vigentes |
| GET | `/api/internal/professionals/{id}` | token | contrato interno consumido por `backend-turnos` |
| GET | `/api/internal/professionals/{id}/weekly-schedules` | token | contrato interno consumido por `backend-turnos` |
| GET | `/api/internal/integration-status` | token | estado de la integración con la catedra |

Todo lo no listado como público queda protegido (**denegar por defecto**) y exige un token
válido con la autoridad `ROLE_USER`: un token sin ese rol responde `403 FORBIDDEN`, y uno sin
token, con firma rota o con otro issuer responde `401 UNAUTHORIZED`.

### Autenticación de usuario final

- `POST /api/register`: body `{login, password, firstName, lastName, email, imageUrl?, langKey}`.
  Validaciones de dominio y de Jakarta (login 3–50 con patrón JHipster, password 4–100,
  nombres hasta 50, email válido y único, langKey 2–10, imageUrl opcional ≤254). El usuario
  nace activo (`activated=true`), con `authorities: [ROLE_USER]`, sin verificación por correo.
  Errores: `VALIDATION_ERROR`, `USERNAME_ALREADY_EXISTS`, `EMAIL_ALREADY_EXISTS`.
- `POST /api/authenticate`: body `{username, password, rememberMe}` → `200` con
  `{id_token, token_type, expires_in}`. JWT **HS256** firmado con `APP_JWT_SECRET`, issuer
  `backend-catalogo` y claims `sub` (login), `publicId` (UUID estable) y `authorities`.
  `rememberMe` solo alarga la vida del token (`APP_JWT_REMEMBER_ME_EXPIRY_SECONDS`).
  Credenciales inválidas → `401 UNAUTHORIZED` con el mismo detalle para un login inexistente
  y para una contraseña errónea (sin enumerar usuarios).
- `GET /api/account`: perfil del usuario autenticado; la identidad sale del `sub` del token
  ya verificado, nunca de un valor enviado por el cliente, y la respuesta no contiene
  password ni hash.

La contraseña se guarda **nunca en texto plano**: hash BCrypt en `jhi_user.password_hash`
(tablas `jhi_user`, `jhi_authority`, `jhi_user_authority`, migración Flyway `V3`). Ni
contraseñas ni tokens se escriben en los logs.

## Configuración

Todo valor sale del entorno: `.env.example` documenta cada variable (`cp .env.example .env`).
Las de esta iteración:

- `APP_JWT_SECRET` (≥ 32 caracteres), `APP_JWT_EXPIRY_SECONDS`,
  `APP_JWT_REMEMBER_ME_EXPIRY_SECONDS`;
- `CATEDRA_REDIS_HOST/PORT/USERNAME/PASSWORD`, `CATEDRA_REDIS_READ_NAMESPACE`,
  `CATEDRA_REDIS_WRITE_NAMESPACE` (namespace privado `alumnos:{groupId}:*`);
- `CATEDRA_KAFKA_BOOTSTRAP_SERVERS`, `CATEDRA_KAFKA_CONSUMER_GROUP_ID`,
  `CATEDRA_KAFKA_CATALOG_TOPIC`;
- `APP_SYNC_SCHEDULER_ENABLED`, `APP_SYNC_SCHEDULER_INITIAL_DELAY_MS`,
  `APP_SYNC_SCHEDULER_FIXED_DELAY_MS`, `APP_SYNC_KAFKA_LISTENER_ENABLED`;
- `CORS_ALLOWED_ORIGINS` (lista separada por comas; vacío = sin acceso cross-origin, por
  defecto seguro para clientes Android).

## Ejecución

```bash
cp .env.example .env   # completar credenciales
docker compose up --build
```

`docker-compose.yml` levanta PostgreSQL 16 y el servicio. Redis y Kafka pertenecen a la
catedra y no se levantan localmente: sus coordenadas llegan por entorno.

## Pruebas

```bash
./mvnw test
```

- Testcontainers con **PostgreSQL y Redis reales**; sin H2, sin mocks del feed incremental:
  el adaptador de Redis se ejercita contra el contenedor.
- El consumer de Kafka y el scheduler quedan **desactivados** en los tests
  (`app.sync.kafka.listener-enabled=false`, `app.sync.scheduler.enabled=false`) para que nada
  mueva el estado entre dos aserciones: el listener se prueba invocándolo con exactamente el
  cuerpo que entregaría el broker.
- Cobertura de la iteración: aplicación de 2 versiones en orden, idempotencia de una versión
  repetida, discontinuidad (`local=3`, `current=7`, `oldest=4`) → snapshot sin leer
  `changes:4`, evento Kafka duplicado procesado una sola vez, registro/login/JWT (claims,
  401, duplicados) y autorización 401/403.
