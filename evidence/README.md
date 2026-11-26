# Evidencia de contenedorización de identity-service

Verificación local del 30 de octubre de 2026 (America/Lima).

## Resultado

- Construcción de imagen correcta.
- Docker Compose: `healthy`.
- Usuario: `uid=100(appuser) gid=101(appgroup)`.
- `GET /actuator/health`: HTTP 200, `status: UP`.
- Base de datos: PostgreSQL 16 Alpine.

## Reproducción

```sh
docker compose -p peaceapp-evidence up --build -d
docker compose -p peaceapp-evidence ps
docker compose -p peaceapp-evidence exec -T identity-service id
docker compose -p peaceapp-evidence exec -T identity-service wget -S -O - http://127.0.0.1:8081/actuator/health
```

`containers.json`, `runtime-user.txt`, `health-response.txt` y `startup.log` conservan las salidas reales. `docker-validation.png` es un resumen visual de esas salidas, no una captura de terminal.

## CI

La figura `ci-validation.png` resume datos de la API de GitHub Actions para la [ejecución #38001955510](https://github.com/PeaceApp-UPC-17433/identity-service/actions/runs/38001955510), correspondiente al PR #10 del 9 de octubre. No corresponde a una nueva ejecución del Dockerfile ni acredita cobertura JaCoCo.

El build Docker conserva `-DskipTests`; la validación de salud no reemplaza las pruebas unitarias de CI.
