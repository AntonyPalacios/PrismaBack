# PrismaBack — backend de Sistema Prisma v2

> Acuerdos del proyecto (fuente de verdad): [`docs/AGREEMENTS.md`](docs/AGREEMENTS.md).
> Es una copia de `prisma-v2/CLAUDE.md`; si cambia la maestra, se actualiza esta copia y la de PrismaFront.
> Este archivo solo agrega lo propio de este repo y no repite las reglas.

## Qué es
API REST de Prisma: Spring Boot 4.1 · Java 21 · PostgreSQL · Flyway · MapStruct · JWT en cookie httpOnly.
La v1 (hecha sin IA) vive en la rama `legacy`.

## Cómo correrlo
```bash
export JAVA_HOME="/c/Users/anton/.jdks/corretto-21.0.9"   # el java por defecto es 17
docker compose up -d                                      # Postgres 16 local
./mvnw spring-boot:run                                    # :8080, Swagger en /swagger-ui.html
./mvnw verify                                             # tests *IT con Testcontainers (requiere Docker)
```
Secretos en `.env` (no versionado; plantilla en `.env.example`). Nunca leer ni escribir sus valores.

## Convenciones clave de este repo
- Un paquete por módulo de negocio (`catalog`, `security`, `users`…) con `entidad → Repository → Service/impl → Controller`, `dto/` (records) y `mapper/` (MapStruct). `catalog` es la plantilla.
- El esquema solo cambia con migraciones Flyway nuevas (`V<n>__...sql`); nunca `ddl-auto: update`.
- Errores en RFC 7807 con `detail` en español para el usuario final y `errorId`.
- `@PreAuthorize` explícito en cada endpoint de escritura. `/api/**` sin sesión responde 401.
- Tests de integración `*IT` contra Postgres real (Testcontainers), no H2.

## Flujo de trabajo
Antes de tocar código: tarjeta → issue en inglés → rama ligada (`gh issue develop`) → código → PR. Ver la skill `prisma-flow` en `prisma-v2/.claude/skills/`.
