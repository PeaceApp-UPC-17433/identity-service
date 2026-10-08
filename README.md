# identity-service

BC-01 *Identity & Access* de PeaceApp: registro, autenticación, perfil, zonas de interés y
jurisdicción institucional. Implementa US-01, US-02, US-03 y US-26 del informe
(ver `PeaceApp-Report`, secciones 1.3.4 y 2.4).

## Requisitos

- Java 21
- Maven 3.9+
- Docker (opcional, para levantar junto a Postgres)

## Ejecutar en local

Sin Docker, usa una base H2 en memoria por defecto:

```bash
mvn spring-boot:run
```

Con Docker Compose (Postgres real):

```bash
docker compose up --build
```

El servicio queda disponible en `http://localhost:8081`. Swagger UI en
`http://localhost:8081/swagger-ui.html`.

## Endpoints

| Método | Ruta | Historia | Descripción |
| --- | --- | --- | --- |
| POST | /api/v1/auth/register | US-01 | Registro de usuario |
| POST | /api/v1/auth/login | US-02 | Inicio de sesión (devuelve JWT) |
| GET | /api/v1/users/me | US-03 / US-05 | Perfil propio, zonas de interés y reputación |
| POST | /api/v1/users/me/interest-zones | US-03 | Agregar zona de interés |

## Pendiente (bloqueado por diagramas/decisiones de arquitectura aún no cerradas)

- Contrato OpenAPI formal (sección 2.7 del informe, aún vacía).
- Publicación real de eventos de dominio (`UserRegistered`, `UserVerified`,
  `InterestZoneUpdated`, `AccountLocked`) hacia el broker definido en CA-01.
- Propagación de claims desde el API Gateway (CA-04) una vez que TS-04 esté implementado.
- Verificación de correo (US-01 solo crea la cuenta; falta el flujo de verificación).
