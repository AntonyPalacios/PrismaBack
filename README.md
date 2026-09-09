# Prisma backend v2

Spring Boot 3.4 · Java 21 · PostgreSQL · Flyway · MapStruct · JWT-in-cookie

## Antes de arrancar

Tu terminal por defecto tiene Java 17 en el PATH, pero este proyecto es
Java 21. Ya tienes Corretto 21 instalado (vía IntelliJ) en
`C:\Users\anton\.jdks\corretto-21.0.9` — apunta `JAVA_HOME` ahí antes de
correr Maven desde la terminal, o simplemente abre el proyecto en
IntelliJ y selecciona ese SDK para el módulo.

```bash
export JAVA_HOME="/c/Users/anton/.jdks/corretto-21.0.9"
```

## Levantar todo en local

1. Copia `.env.example` a `.env` y complétalo (secreto JWT, credenciales de Google OAuth2, password de la BD).
2. Levanta Postgres: `docker compose up -d`
3. Corre el backend: `./mvnw spring-boot:run`
4. Swagger UI: http://localhost:8080/swagger-ui.html

## Tests

Los tests de integración (`*IT`) usan Testcontainers — necesitan Docker
corriendo localmente:

```bash
./mvnw verify
```

## Estructura

Cada módulo de negocio vive en su propio paquete, con las capas de
siempre adentro (`entidad`, `Repository`, `Service`/`impl`, `Controller`,
`dto/`, `mapper/`). El módulo `catalog/` (Área, Curso, Tipo de examen) es
la plantilla completa a copiar para los módulos que faltan:

- [x] `catalog` — áreas, cursos, tipos de examen
- [x] `users` — usuarios y roles (solo entidades por ahora)
- [x] `security` — login Google + JWT en cookie httpOnly
- [ ] `academic` — ciclos y etapas
- [ ] `students` — alumnos, apoderados, matrícula por etapa
- [ ] `tutoring` — historial de asignación de tutor
- [ ] `exams` — exámenes y resultados
- [ ] `goals` — metas por área
- [ ] `imports` — carga de Excel con cola de homologación
- [ ] `logbook` — bitácora psicológica

Migraciones Flyway en `src/main/resources/db/migration` — el esquema
completo ya está en `V1__init_schema.sql` / `V2__seed_reference_data.sql`
aunque el código de los módulos de arriba todavía no exista; se van
implementando uno a uno sobre esas tablas.
