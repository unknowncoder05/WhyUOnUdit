# Mini-WHYUON

Practicas UDIT con WHYUON. Aplicacion completa que cubre los **3 desafios** del briefing:

1. **Listar publicaciones** desde un JSON con datos simulados (YouTube + blogs).
2. **Persistir las publicaciones** en una base de datos MySQL.
3. **Generar un informe** descargable en CSV listo para abrir en Excel.

## Stack

| Capa | Tecnologia |
|---|---|
| Backend | Java 17 + Spring Boot 3.3 + Spring Data JPA + Jackson |
| Base de datos | MySQL 8 |
| Frontend | HTML + CSS + JavaScript (sin framework) servido por Nginx |
| Orquestacion | Docker Compose (3 servicios) |
| Documentacion API | Swagger UI (springdoc-openapi) |

## Estructura del proyecto

```
mini-whyuon/
├── docker-compose.yml       Orquesta backend + db + frontend
├── Dockerfile               Imagen del backend
├── Makefile                 Comandos rapidos (make up, make down, ...)
├── pom.xml                  Dependencias Maven
├── src/main/
│   ├── java/com/whyuon/udit/
│   │   ├── Application.java         Punto de entrada Spring Boot
│   │   ├── config/                  AppProperties, WebConfig (CORS)
│   │   ├── controller/              PublicationController, ImportController
│   │   ├── dto/                     Respuestas REST (records)
│   │   ├── loader/JsonLoader.java   Carga inicial del JSON al arrancar
│   │   ├── model/                   Entidades JPA (Channel, Author, Publication)
│   │   ├── repository/              Repositorios Spring Data
│   │   └── service/                 PublicationService, JsonImportService
│   └── resources/
│       ├── application.yml          Configuracion Spring
│       ├── schema.sql               DDL de las 3 tablas
│       └── whyuon.json              Datos de prueba (23 publicaciones)
└── frontend/
    ├── Dockerfile                   Imagen Nginx
    ├── index.html
    ├── css/style.css
    ├── js/script.js
    └── img/                          Iconos y banner
```

## Modelo de datos

Tres tablas normalizadas, FKs y `UNIQUE` para deduplicar:

```
channels (id, platform, external_id, name)              UNIQUE(platform, external_id)
authors  (id, name, url, image_url)                     UNIQUE(url)
publications (
    id, channel_id → channels, author_id → authors (NULL para YouTube),
    url UNIQUE, image_url, date_published, description,
    publication_id, format, duration_seconds            -- solo YouTube
)
```

Esquema completo: [src/main/resources/schema.sql](src/main/resources/schema.sql).

## Como arrancar

### Requisitos
- Docker Desktop (con Docker Compose v2)
- Git
- (Opcional) Para arrancar el backend sin Docker: Java 17 + Maven 3.9+

### Levantar todo con Docker

```bash
make up
```

Equivale a `docker compose up --build -d`. Al terminar:

| Servicio | URL |
|---|---|
| Frontend | http://localhost:3000 |
| Backend / API | http://localhost:8000/api |
| Swagger UI | http://localhost:8000/swagger-ui.html |
| MySQL (cliente externo) | localhost:3307 (`root` / `root`, BD `miniwhyuon`) |

La primera vez tarda unos minutos porque descarga las imagenes base y compila el backend. Las siguientes son rapidas.

### Comandos `make` disponibles

| Comando | Que hace |
|---|---|
| `make up` | Construye y arranca los 3 contenedores en background |
| `make down` | Para y elimina los contenedores (los datos de MySQL se conservan) |
| `make logs` | Muestra logs en vivo |
| `make load-data` | Llama al endpoint de reimportacion del JSON |

### Que pasa al arrancar

1. MySQL arranca y queda `healthy`.
2. Backend espera a MySQL y arranca.
3. Spring ejecuta `schema.sql` (crea las tablas si no existen).
4. `JsonLoader` lee `src/main/resources/whyuon.json` e inserta las publicaciones.
   - **Idempotente**: si vuelves a arrancar no duplica nada (UNIQUE por `url`).
5. Frontend (Nginx) sirve `index.html` y consume la API.

## Endpoints principales

| Metodo | Ruta | Descripcion |
|---|---|---|
| GET | `/api/publications?page=0&size=12` | Listado paginado, ordenado por fecha descendente |
| GET | `/api/channels` | Resumen de canales con conteo de publicaciones |
| GET | `/api/reports/publications.csv` | Descarga del informe en CSV (UTF-8 + BOM, separador `;`) |
| POST | `/api/import?path=...` | Reimporta el JSON (classpath o ruta del filesystem) |

Documentacion interactiva: http://localhost:8000/swagger-ui.html

## Configuracion (variables de entorno)

El backend respeta estas variables (con sus defaults):

| Variable | Default | Descripcion |
|---|---|---|
| `DB_HOST` / `DB_PORT` / `DB_NAME` | `localhost` / `3306` / `miniwhyuon` | Conexion MySQL |
| `DB_USER` / `DB_PASSWORD` | `root` / `root` | Credenciales MySQL |
| `LOAD_DATA_ON_STARTUP` | `true` | Si es `false`, el `JsonLoader` no carga datos al arrancar |
| `WHYUON_JSON_PATH` | `classpath:whyuon.json` | Origen del JSON (classpath o ruta de fichero) |
| `CORS_ALLOWED_ORIGINS` | `*` | Origenes permitidos por CORS (separados por coma) |

## Comprobar que se ha cargado bien

```sql
USE miniwhyuon;

SELECT COUNT(*) FROM publications;                            -- 23

SELECT platform, COUNT(*) AS posts
FROM publications p JOIN channels c ON c.id = p.channel_id
GROUP BY platform;                                            -- youtube 12, blog 11

SELECT c.name, COUNT(*) AS posts
FROM channels c JOIN publications p ON p.channel_id = c.id
GROUP BY c.name ORDER BY posts DESC;
```

## Convenciones del equipo

- **Commits**: Conventional Commits (`feat:`, `fix:`, `chore:`, `docs:`, `refactor:`, `build:`).
- **Ramas**: una por feature, nombradas `feat/<tarea>`, `fix/<bug>`, `chore/<tarea>`.
- **PRs**: siempre contra `main`, con descripcion del *por que* (no solo *que*).
- **Despues del merge**: borrar la rama tanto en local como en remoto.

## Estado de los desafios

| # | Desafio | Estado |
|---|---|---|
| 1 | Listar publicaciones del JSON | ✅ Endpoint REST + cards visuales en el frontend |
| 2 | Persistir en BD | ✅ MySQL 8 con 3 tablas normalizadas |
| 3 | Generar informe | ✅ CSV con BOM UTF-8 + separador `;` (compatible Excel) |

## Notas

- `.claude/` esta en `.gitignore`: cada desarrollador con Claude Code tiene su propia configuracion local que no se versiona.
- El backend usa `Hibernate ddl-auto: none` y `schema.sql` para que el esquema este bajo control de version, no autogenerado.
