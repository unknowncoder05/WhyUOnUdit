# Mini-WHYUON

Practicas UDIT con WHYUON. Backend en **Java 17 + Spring Boot 3** que cubre los 3 desafios del enunciado.

## Stack

- Java 17
- Spring Boot 3.3
- Spring Data JPA + **MySQL 8**
- Swagger UI (springdoc-openapi)

## Estructura

```
mini-whyuon/
├── src/
│   ├── main/
│   │   ├── java/com/whyuon/udit/
│   │   │   ├── Application.java       Punto de entrada Spring Boot
│   │   │   ├── model/                 Entidades JPA (Channel, Author, Publication)
│   │   │   ├── repository/            Repositorios Spring Data
│   │   │   └── loader/JsonLoader.java Carga inicial del JSON a la BD
│   │   └── resources/
│   │       ├── application.yml        Configuracion (conexion MySQL)
│   │       ├── schema.sql             Esquema SQL de las 3 tablas
│   │       └── whyuon.json            Datos de prueba
│   └── test/                          Tests
├── whyuon.json                        Copia de referencia (no se usa en runtime)
├── pom.xml                            Dependencias Maven
└── README.md
```

## Modelo de datos

Tres tablas normalizadas para no repetir nombres de canal ni autores:

```
channels (id, platform, external_id, name)             UNIQUE(platform, external_id)
authors  (id, name, url, image_url)                    UNIQUE(url)
publications (
    id, channel_id → channels, author_id → authors (NULL en YouTube),
    url UNIQUE, image_url, date_published, description,
    publication_id, format, duration_seconds            -- columnas solo YouTube
)
```

Esquema completo en [src/main/resources/schema.sql](src/main/resources/schema.sql).

## Preparar MySQL

1. Tener MySQL 8 corriendo en `localhost:3306`.
2. Crear (opcional) la base de datos manualmente:
   ```sql
   CREATE DATABASE miniwhyuon CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```
   *(la URL JDBC tiene `createDatabaseIfNotExist=true`, asi que tambien se crea sola).*
3. Por defecto la app se conecta con `root` / `root`. Para cambiarlo, exporta variables antes de arrancar:
   ```bash
   export DB_USER=mi_usuario
   export DB_PASSWORD=mi_password
   ```

## Como arrancar

```bash
mvn spring-boot:run
```

Al arrancar:

1. Se crea la base de datos `miniwhyuon` si no existe.
2. Se ejecuta `schema.sql` (crea las 3 tablas si no existen).
3. `JsonLoader` lee `src/main/resources/whyuon.json` e inserta las publicaciones.
   - Es idempotente: si vuelves a arrancar no duplica nada (UNIQUE por URL).

Endpoints (cuando se anadan los controladores):

- API:        http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html

## Comprobar que se ha cargado bien

```sql
USE miniwhyuon;

SELECT COUNT(*) FROM publications;                                          -- 23
SELECT platform, COUNT(*) FROM publications p
  JOIN channels c ON c.id = p.channel_id
  GROUP BY platform;                                                        -- youtube 12, blog 11
SELECT name, COUNT(*) AS posts FROM channels c
  JOIN publications p ON p.channel_id = c.id
  GROUP BY name ORDER BY posts DESC;
```

## Desafios

### Desafio 1 - Listar publicaciones desde el JSON

Leer `whyuon.json` (con publicaciones de YouTube y blogs) y exponer un endpoint que devuelva la lista con titulo, plataforma, fecha y enlace.

### Desafio 2 - Persistir en base de datos  ✅

Las publicaciones se almacenan en MySQL mediante JPA. La carga inicial la hace `JsonLoader` al arrancar.

### Desafio 3 - Generar informes

Endpoint que descarga un fichero CSV / TXT / Excel con todas las publicaciones, ordenadas para que el uso sea comodo.

## Tests

```bash
mvn test
```
