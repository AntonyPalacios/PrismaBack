# Prisma backend v2

Spring Boot 4.1 · Java 21 · PostgreSQL · Flyway · MapStruct · JWT-in-cookie

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

## Levantar el sistema completo con Docker (sin Java ni Node)

Para una máquina que solo tiene Docker (por ejemplo, la del trabajo). Levanta
base de datos, backend y frontend juntos en http://localhost:8080. Es
independiente del `docker-compose.yml` de desarrollo: puede correr al mismo
tiempo y usa su propia base de datos.

1. Instala Docker Desktop y ábrelo.
2. Clona los dos repos uno al lado del otro, con estos nombres de carpeta:
   ```bash
   git clone https://github.com/AntonyPalacios/PrismaBack.git backend
   git clone https://github.com/AntonyPalacios/PrismaFront.git frontend
   ```
   Si usas otros nombres, pon `FRONTEND_DIR=../<carpeta-del-front>` en el `.env`.
3. En `backend/`, copia `.env.example` a `.env` y complétalo: credenciales de
   Google, `DB_PASSWORD` y un `JWT_SECRET` nuevo (`openssl rand -base64 64`).
   No mandes el `.env` por chat ni lo subas al repo.
4. En Google Cloud Console, el OAuth client debe tener el redirect URI
   `http://localhost:8080/login/oauth2/code/google` (el mismo de desarrollo).
5. Desde `backend/`:
   ```bash
   docker compose -f docker-compose.full.yml up -d --build
   ```
   La primera vez tarda unos minutos (descarga dependencias y compila).
6. Abre http://localhost:8080 e inicia sesión con Google.

Comandos útiles (desde `backend/`):

```bash
docker compose -f docker-compose.full.yml logs -f backend             # ver logs
git pull && docker compose -f docker-compose.full.yml up -d --build   # actualizar (haz git pull también en frontend/)
docker compose -f docker-compose.full.yml down                        # apagar (conserva los datos)
docker compose -f docker-compose.full.yml down -v                     # apagar y BORRAR la base de datos
```

La base de datos arranca vacía, solo con lo que siembra Flyway (Antony y Silvia).
El día a día del desarrollo no cambia: IntelliJ + `npm run dev` siguen igual.

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
