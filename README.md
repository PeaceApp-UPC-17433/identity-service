# identity-service

BC-01 *Identity & Access* de PeaceApp: registro y verificación de correo, autenticación con bloqueo,
sesiones con refresh token, perfil, zonas de interés, roles y jurisdicción institucional.
Implementa US-01, US-02, US-03 y US-26 del informe (secciones 1.3.4 y 2.4).

## Levantar el servicio (recomendado: Docker)

Solo necesitas Docker. Java, Maven y Postgres van dentro de los contenedores, así no hay problemas de versiones.

```bash
docker compose up --build
```

| Qué | Dónde |
| --- | --- |
| API | http://localhost:8081 |
| Documentación (Scalar) | http://localhost:8081/scalar |
| Contrato OpenAPI | http://localhost:8081/v3/api-docs |
| Health | http://localhost:8081/actuator/health |
| Postgres | `localhost:5433`, base `identitydb`, usuario/clave `identity` |
| Admin inicial | `admin@peaceapp.pe` / `Admin12345` |

Postgres se expone en el **5433** del host para no chocar con un Postgres local en 5432.
Para cambiar puertos o credenciales: `cp .env.example .env` y edita `.env` (no se sube al repo).

Para apagar: `docker compose down`. Para borrar también los datos: `docker compose down -v`.

## Ambientes (perfiles de Spring)

| Archivo | Perfil | Uso |
| --- | --- | --- |
| `application.yml` | todos | Configuración común. Documentación **apagada** por defecto. |
| `application-dev.yml` | `dev` (por defecto) | Local y docker-compose. Valores por defecto listos, Scalar y OpenAPI activos, token de verificación visible en la respuesta. |
| `application-prod.yml` | `prod` | Kubernetes. Todo por variables de entorno **sin valores por defecto**; sin Scalar ni `/v3/api-docs`. |
| `src/test/resources/application-test.yml` | `test` | Pruebas automatizadas con H2 en modo PostgreSQL. |

En producción se activa con `SPRING_PROFILES_ACTIVE=prod`, y son obligatorias `DB_URL`, `DB_USERNAME`,
`DB_PASSWORD` y `JWT_SECRET` (mínimo 32 caracteres). Si falta alguna, el servicio no arranca.

## Desarrollo sin Docker para el servicio

Levanta solo la base y corre el servicio desde el IDE o con el Maven Wrapper:

```bash
docker compose up -d postgres
./mvnw spring-boot:run        # Windows: mvnw.cmd spring-boot:run
```

`mvnw` es el **Maven Wrapper**: descarga Maven 3.9.9 la primera vez, así todos usan la misma versión
sin instalar Maven. Requiere JDK 21 o superior.

## Pruebas

```bash
./mvnw test
```

32 pruebas: reglas del dominio (`UserTest`), el flujo HTTP completo (`IdentityFlowIntegrationTest`)
y la documentación por ambiente (`ApiDocsProfileTest`).

## Endpoints

| Método | Ruta | Historia | Auth |
| --- | --- | --- | --- |
| POST | `/api/v1/auth/register` | US-01 | pública |
| POST | `/api/v1/auth/verify-email` | US-01 | pública |
| POST | `/api/v1/auth/resend-verification` | US-01 | pública |
| POST | `/api/v1/auth/login` | US-02 | pública |
| POST | `/api/v1/auth/refresh` | RNF-11 | pública (refresh token) |
| POST | `/api/v1/auth/logout` | RNF-11 | pública (refresh token) |
| GET / PATCH | `/api/v1/users/me` | US-03 | Bearer |
| GET / POST | `/api/v1/users/me/interest-zones` | US-03 | Bearer |
| DELETE | `/api/v1/users/me/interest-zones/{zoneId}` | US-03 | Bearer |
| POST | `/api/v1/admin/institutional-accounts` | US-26 | ADMINISTRADOR |
| PUT | `/api/v1/admin/users/{userId}/role` | RNF-12 | ADMINISTRADOR |
| POST | `/api/v1/admin/users/{userId}/unlock` | RNF-11 | ADMINISTRADOR |

Todos los errores siguen RFC 7807 (Problem Details, CA-07).

## Flujo de prueba rápido

```bash
B=http://localhost:8081/api/v1

# 1. Registro (en dev la respuesta incluye verificationToken)
curl -s -X POST $B/auth/register -H 'Content-Type: application/json' \
  -d '{"email":"ana@peaceapp.pe","password":"Clave12345"}'

# 2. Verificar el correo con el token recibido
curl -s -X POST $B/auth/verify-email -H 'Content-Type: application/json' -d '{"token":"<verificationToken>"}'

# 3. Login -> accessToken + refreshToken
curl -s -X POST $B/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"ana@peaceapp.pe","password":"Clave12345"}'

# 4. Perfil y zonas de interés
curl -s $B/users/me -H "Authorization: Bearer <accessToken>"
curl -s -X POST $B/users/me/interest-zones -H "Authorization: Bearer <accessToken>" \
  -H 'Content-Type: application/json' \
  -d '{"label":"Casa","latitude":-12.12,"longitude":-77.03,"radiusMeters":500}'
```

O hazlo desde Scalar (`/scalar`): ejecuta el login, copia el `accessToken` y pégalo en la autenticación Bearer.

## Reglas de negocio implementadas

- **Contraseña:** de 8 a 72 caracteres, con al menos una letra y un número. Se guarda con BCrypt.
- **Correo:** se normaliza (`A@X.PE` y `a@x.pe` son la misma cuenta). Hay que verificarlo antes de iniciar sesión; el token dura 24 h.
- **Bloqueo (RNF-11):** 5 intentos fallidos consecutivos bloquean la cuenta 15 min. Un administrador puede desbloquearla antes. El mensaje es siempre "Credenciales invalidas", así no se revela si la cuenta existe o está bloqueada.
- **Sesión:** access token JWT de 60 min y refresh token de 7 días que se rota en cada uso. Si alguien reusa un refresh token ya rotado, se revocan todas las sesiones del usuario.
- **Zonas de interés:** máximo 10 por usuario, radio entre 100 y 5000 m, coordenadas validadas.
- **US-26:** solo un ADMINISTRADOR crea cuentas institucionales, y siempre con jurisdicción. El JWT lleva los claims `role` y `jurisdiction` para que el API Gateway los propague (CA-04).

## Eventos de dominio (outbox)

Los eventos se guardan en la tabla `outbox_events` dentro de la misma transacción que el cambio:
`UserRegistered`, `VerificationRequested`, `UserVerified`, `InterestZoneUpdated`, `AccountLocked` y `UserRoleChanged`.
Cuando se defina el broker (CA-01), un relay los publicará y marcará `published_at`.

## Pendiente

- Relay del outbox hacia el broker (depende de la sección 2.6 del informe).
- Envío del correo de verificación, a cargo de notification-service (BC-08) a partir de `UserRegistered`.
- Consumir `ReputationChanged` de validation-service para actualizar `reputationScore`.
- Evaluar firmar el JWT con RS256 y publicar JWKS para que el Gateway no comparta el secreto.
