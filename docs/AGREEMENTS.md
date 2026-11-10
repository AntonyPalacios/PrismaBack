# Sistema Prisma v2 — Contexto general

> Archivo de contexto para cualquier persona o agente que trabaje en este proyecto.
> Es la **fuente única de acuerdos**. Si un acuerdo cambia, se cambia aquí (y se anota en el historial al final).
> Última revisión: 2026-11-09 — **borrador pendiente de revisión** (ver sección "Por revisar").

---

## 1. Qué es Prisma

Web app para **Academia Prisma**, academia preuniversitaria en Perú que prepara alumnos para el examen de admisión PUCP.
Los tutores (psicólogos) acompañan a los alumnos con simulacros semanales, siguen la evolución de sus puntajes, fijan metas de mejora y registran sesiones en una bitácora.

- **Organización del tiempo:** Ciclo → Etapas. Un solo ciclo `ACTIVE` a la vez. Los alumnos se reagrupan entre tutores y el historial de quién tuteló a quién es **inmutable** (append-only).
- **Dominio de exámenes:** 2 áreas (Lectura, Matemática) y 6 cursos (Lectura, Números y Operaciones, Álgebra, Geometría, Trigonometría, Estadística). 3 tipos de examen:
  - Ciencias: sin Estadística.
  - Letras: todos los cursos.
  - Arquitectura: sin Trigonometría ni Estadística.
- **Carreras** (Ciencias, Letras, Arquitectura, Biomédica, Derecho, Artes): cada una apunta a un tipo de examen.
- **Roles:** `ADMIN`, `TUTOR`, `COORDINADOR` (todo el módulo de exámenes salvo la recarga forzada, que es solo de Admin).
- **Admin permanente (inmutable):** `apalaciosc0109@gmail.com` (Antony Palacios Coronado) es siempre `ADMIN`. Se siembra por migración y ningún flujo de la app puede desactivarlo ni quitarle ese rol.
- **Primer tutor:** `silvia.valleg@prisma.edu.pe` (Silvia Valle Gómez) (sembrado junto al admin).
- **Usuarios finales:** tutores, coordinadores y Silvia (administración). La UI va en español.
- **Fase 2 (fuera del MVP):** reportes PDF con IA para apoderados, asistencia.

Documentos de origen (solo lectura): `C:\Users\anton\Documents\Silvia\` ("Contexto Prisma.docx", "Documento de Especificación de Requisitos Funcionales.docx", Excels reales de exámenes). Checklist de requerimientos: `Silvia\requisitos-funcionales-prisma-v2.md`.

---

## 2. Cómo trabajamos: Organic Driven Development + Vertical Slices

Desde 2026-11-09 dejamos el flujo SDD (spec → endpoints → código) por un desarrollo **orgánico por rebanadas verticales**:

1. **Una rebanada = un resultado visible para el usuario**, con back + front + tests, funcionando de punta a punta. Nada de "todo el backend primero".
2. **El software que funciona es la fuente de verdad.** Las specs de `specs/` son **referencia y backlog** (reglas de negocio ya negociadas), no un contrato que haya que mantener sincronizado.
3. **Crecer de a poco:** se construye lo mínimo que cumple el resultado. Se refactoriza cuando el código lo pide, no antes.
4. **Antes de cada rebanada** se escribe una tarjeta corta: resultado esperado, criterios de aceptación y reglas de negocio que aplican (copiadas o enlazadas desde la spec). Se confirma con Antony antes de codificar.
5. **Cambios de diseño o prototipo (Claude Design, etc.)** no reescriben specs.
6. **Todo cambio pedido se categoriza y se acuerda** antes de decidir si se hace o no:

   | Cat. | Tipo | Ejemplos | Cómo se decide |
   |---|---|---|---|
   | **A** | Visual / UX | Textos, colores, disposición | Se decide dentro de la rebanada |
   | **B** | Comportamiento | Filtros, orden, flujos nuevos sin tocar reglas | Se acuerda y se anota en la tarjeta / issue |
   | **C** | Regla de negocio o contrato API/datos | Nueva validación, campo nuevo, endpoint que cambia | Acuerdo explícito + entrada en "Historial de cambios" |
   | **D** | Acuerdo / arquitectura | Stack, convenciones, estructura | Se modifica este archivo, con aprobación expresa de Antony |

7. **Orden antes que código:** ningún cambio se codifica sin antes tener la tarjeta acordada, un **issue en GitHub** (formato estándar, en inglés) y una **rama ligada a ese issue** con checkout local. El procedimiento completo está en la skill `prisma-flow` (`.claude/skills/prisma-flow`).
8. **Ante la duda, preguntar.** Antony prefiere que se le consulte antes de asumir.

---

## 3. Estructura y repositorios

```
Sistema Prisma/
├── prisma-v2/                 ← proyecto actual (abrir las sesiones de Claude aquí)
│   ├── CLAUDE.md              ← este archivo (fuente de acuerdos)
│   ├── .claude/skills/        ← skills del proyecto (prisma-flow)
│   ├── backend/               → GitHub AntonyPalacios/PrismaBack (main = v2)
│   ├── frontend/              → GitHub AntonyPalacios/PrismaFront (main = v1 todavía)
│   └── specs/backend|frontend ← specs SDD (referencia, no contrato)
└── PRISMA/                    ← proyecto v1 hecho sin IA. SOLO LECTURA
    ├── prismaBack/            (v1 en rama `legacy`)
    └── prisma-front/          (v1 en `main` y `legacy`)
```

- **Dos repos separados:** PrismaBack y PrismaFront. Una rebanada back+front = dos issues enlazados entre sí y dos PRs.
- **Este archivo se versiona copiándolo** en cada repo como `docs/AGREEMENTS.md`. Cada repo tiene además un `CLAUDE.md` corto con lo propio de ese repo que apunta a `docs/AGREEMENTS.md`. La copia maestra es `prisma-v2/CLAUDE.md`; al cambiarla se actualizan ambas copias.
- **Nunca editar nada dentro de `PRISMA/`.** Se puede leer para comparar patrones.
- La v1 vive en la rama `legacy` de cada repo. En PrismaFront, la v1 se pasa a `legacy` al crear el proyecto Vite en la primera rebanada de front.

---

## 4. Acuerdos

### 4.1 Generales
- Monorepo lógico `prisma-v2/` con `backend/` y `frontend/`. Código en **inglés** (clases, variables, rutas, carpetas). Texto visible al usuario en **español**.
- Organización **por módulo de negocio**, igual en back y front: `catalog`, `academic`, `students`, `tutoring`, `exams`, `imports`, `goals`, `logbook`, `dashboards`, `users`/`security`.
- Toda escritura es todo-o-nada (`@Transactional`).
- Nombres de personas se muestran como **"Apellidos, Nombres"** y los listados se ordenan por apellido paterno, apellido materno y nombres, ignorando mayúsculas y tildes.
- **Git:** tarjeta → issue estándar en inglés (`[Feature]`, `[Fix]`, `[Refactor]`, `[Chore]`, `[Test]`, `[Docs]`) → rama `<n°-issue>-<descripcion>` ligada al issue (`gh issue develop`) → Conventional Commits en inglés → PR con `Closes #n`. No se hace commit sin preguntar y Claude nunca hace merge. Detalle en la skill `prisma-flow`.

### 4.2 Backend
- Java 21, **Spring Boot 4.1.x** (migrado 2026-11-09, PrismaBack#53), Maven. JDK local: `C:\Users\anton\.jdks\corretto-21.0.9` (el `java` por defecto de la terminal es 17).
- PostgreSQL 16 (Docker local). **Flyway** es dueño del esquema; `ddl-auto: validate`, nunca `update`. Todo cambio de esquema es una migración nueva.
- Capas dentro de cada módulo: entidad → `Repository` → `Service`/`impl` → `Controller`, con `dto/` y `mapper/`. El módulo `catalog` es la plantilla.
- DTOs como `record` sin Lombok. Lombok solo en entidades JPA.
- Mapeo con **MapStruct**; campos derivados vía `@Mapping(expression=…)` o métodos default.
- **Errores:** RFC 7807 Problem Details. El `detail` va en lenguaje simple para el usuario final y cada error trae un id para rastrearlo en el log. Nunca se exponen stack traces.
- **Auth:** solo Google OAuth2. El JWT viaja en cookie `httpOnly` + `Secure`, nunca en la URL ni en `localStorage`. El registro es administrado: solo entran correos que ya existen como `app_user` activo. CSRF con cookie legible + header `X-XSRF-TOKEN`.
- **Autorización** con `@PreAuthorize` en cada endpoint de escritura. El Tutor solo accede a sus alumnos asignados.
- **Auditoría** `created_by/at` y `updated_by/at` automática en tablas mutables.
- API REST: `/api/v1/<recurso-en-plural-en-ingles>`, JSON camelCase.
  - Desactivar = `DELETE` que responde `204` (borrado lógico).
  - `GET …/current` cuando existe la noción de "el vigente".
  - Cambios de estado con `PATCH …/status`.
  - Cargas masivas en dos pasos: `bulk-preview` y luego `bulk`.
- Tests de integración con Testcontainers (`*IT`, se corren con `./mvnw verify`).

### 4.3 Frontend
- **JavaScript** (sin TypeScript, reconfirmado 2026-11-09) + **Vite** + **MUI** (DataGrid/DatePicker). Mobile first, responsive a desktop.
- **RTK Query** como único estado de servidor. Prohibido copiar su caché a slices de Redux (fue el bug de v1).
- Formularios: **react-hook-form + zod**. Los schemas reflejan las reglas de negocio del backend.
- Gráficos: **Recharts**.
- Carpetas por módulo (`src/students/`, `src/exams/`…), cada una con sus componentes, endpoints RTK Query y schemas.
- CSRF automático en un `baseQuery` propio: lee la cookie y envía el header en cada mutación. El front nunca toca el JWT.
- Paleta del logo:
  - `primary #002F6C`, `secondary #0059B3`
  - `success #8DC63F`, `warning #F7941D`, `error #ED1C24`
  - `textPrimary #000`, `textSecondary #D6D6D6`
- Alertas: patrón `MyAlert` (Snackbar + Alert, abajo a la derecha, se oculta a los 3 s).
- CRUD: patrón `useActionType` + `useModal` + `MyActionButtons` + `DeleteConfirmation`, reimplementado (no copiado).
- **Prototipo de pantallas** (Claude Design, 2026-11-06): https://claude.ai/artifact/1PjLbVSgVjzKkajc1T5wsf. Toda pantalla nueva **se construye siguiendo lo que ya está diseñado ahí** (es criterio de aceptación de cada rebanada de front). Si el diseño debe cambiar, se decide en el momento dentro de la rebanada (categoría A/B) y no se reescriben specs.

### 4.4 Dominio: Excel de resultados (hoja `EVA`)
- Encabezados en las filas 9 y 11; los datos empiezan en la fila 12.
- Por alumno: mérito, DNI, nombre, triples B/M/BL por curso, puntajes por área, total y `TUTOR(A)` (solo el primer nombre).
- **No asistió:** total = 80 y BL = total de preguntas en todos los cursos. Se importa con `attended=false` y se excluye de estadísticas.
- **Filas basura** (nombre, DNI y tutor iguales a un número pequeño): se saltan sin pasar por la cola, pero se reportan en el conteo.
- **Tutor:** match contra tutores activos; si no hay match va a la cola de revisión. Las colisiones de nombre se resuelven en el Excel, no en el sistema.
- La columna "Geometría y Trigonometría" de Arquitectura es un error de la plantilla: corresponde solo a Geometría.
- Una carga duplicada se bloquea a nivel de examen completo. La recarga forzada es solo de Admin.
- Ningún lote de importación escribe datos reales hasta que un humano resuelve todas las filas ambiguas y confirma.

---

## 5. Estado actual (2026-11-09)

Backend en Spring Boot 4.1.2 (PrismaBack#53, mergeado). Rebanada de login en PR: PrismaBack#55 + PrismaFront#37 (verificada con Google real por Antony).

| Módulo | Backend | Frontend |
|---|---|---|
| security / login (Google, `/me`, logout, 401, CSRF) | ✅ (PR #55) | ✅ login, layout, sesión (PR #37) |
| catalog (áreas, cursos, tipos de examen) | ✅ CRUD + 1 test IT (Áreas) | ❌ ← **siguiente rebanada: Áreas** |
| users (gestión de usuarios y roles) | 🟡 entidades + seed (admin y Silvia) | ❌ |
| academic, students, tutoring, exams, imports, goals, logbook, dashboards | ❌ (el esquema ya existe en V1) | ❌ |

Pendientes de esquema:
- Seed del usuario "System" (para jobs automáticos).
- Eliminar `stage.sequence`.
- Evitar solapamiento de fechas en ciclo y etapa.

---

## 6. Comandos

```bash
# Backend (desde prisma-v2/backend)
export JAVA_HOME="/c/Users/anton/.jdks/corretto-21.0.9"
docker compose up -d          # Postgres local
./mvnw spring-boot:run        # API en :8080, Swagger en /swagger-ui.html
./mvnw verify                 # tests (requiere Docker)
```

```bash
# Frontend (desde prisma-v2/frontend) — necesita el backend en :8080
npm install
npm run dev                   # http://localhost:5173 (proxy /api -> :8080)
npm test && npm run lint && npm run build
```

---

## 7. Por revisar (abierto — decidir con Antony)

- [ ] **Alcance de `COORDINADOR`:** creció dos veces. Revalidar el límite actual (pospuesto por Antony: se ve más adelante, a más tardar al llegar a la rebanada de exámenes).

Resueltos el 2026-11-09: Spring Boot → 4.1.x · se mantiene JavaScript · dos repos con copia de este archivo · front v1 a `legacy` en la primera rebanada de front · ODD confirmado con categorías A–D · skill `prisma-flow` reemplaza a `github-flow` · skills SDD `spec-lite` y `spec-endpoints` archivadas en `~/.claude/skills-archive/` (se pueden restaurar moviéndolas de vuelta a `~/.claude/skills/`) · este archivo copiado como `docs/AGREEMENTS.md` en PrismaBack (#56) y PrismaFront (#38).

---

## Historial de cambios
- **2026-11-09** — (cat. C) Antony es admin permanente e inmutable; Silvia es el primer tutor. Ambos se siembran en la rebanada de login.
- **2026-11-09** — Revisión con Antony (cat. D): Spring Boot 4.1.x, JS se mantiene, dos repos + copia `docs/AGREEMENTS.md`, categorías de cambio A–D, regla "issue + rama antes de código" con la skill `prisma-flow`.
- **2026-11-09** — Creación. Se consolidan los acuerdos dispersos en memorias de Claude (decisiones back y front, contexto Excel, aislamiento de v1) y se adopta ODD + vertical slices.
- **2026-11-06** — Desde el prototipo de diseño se acordó para alumnos: filtro por tutor y "sin tutor", tutor y etapa vigentes en el listado, y orden por apellidos (spec REQ-STUDENTS-01).
