# Mini-WHYUON

Practicas UDIT con WHYUON. Backend en **Java 17 + Spring Boot 3** que cubre los 3 desafios del enunciado.

## Stack

- Java 17
- Spring Boot 3.3
- Spring Data JPA + H2 (base de datos en memoria)
- Swagger UI (springdoc-openapi)

## Estructura

```
mini-whyuon/
├── src/
│   ├── main/
│   │   ├── java/com/whyuon/udit/   Codigo de la aplicacion
│   │   └── resources/              application.yml + datos
│   └── test/                        Tests
├── pom.xml                          Dependencias Maven
└── README.md
```

## Como arrancar

```bash
mvn spring-boot:run
```

- API:        http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html
- H2 console: http://localhost:8080/h2-console

## Desafios

### Desafio 1 - Listar publicaciones desde el JSON

Leer `whyuon.json` (con publicaciones de YouTube y blogs) y exponer un endpoint que devuelva la lista con titulo, plataforma, fecha y enlace.

### Desafio 2 - Persistir en base de datos

Las publicaciones se almacenan en H2 mediante JPA. El listado lee de la BD.

### Desafio 3 - Generar informes

Endpoint que descarga un fichero CSV / TXT / Excel con todas las publicaciones, ordenadas para que el uso sea comodo.

## Tests

```bash
mvn test
```
