-- =====================================================================
-- MINI-WHYUON · Esquema MySQL
-- ---------------------------------------------------------------------
-- Modelo normalizado en 6 tablas:
--   platforms          catálogo de plataformas (youtube, blog, ...) - tabla maestra
--   channels           canales/blogs origen, referencia a una plataforma
--   authors            autores de blog (los canales de YouTube no tienen autor)
--   publications       cada vídeo o post individual
--   publication_images bytes de la imagen archivada (LONGBLOB)
--   import_runs        registro de cada importación del JSON (auditoría)
-- =====================================================================

CREATE TABLE IF NOT EXISTS platforms (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    code          VARCHAR(20)  NOT NULL,                       -- identificador interno
    display_name  VARCHAR(50)  NOT NULL,                       -- nombre legible
    PRIMARY KEY (id),
    UNIQUE KEY uk_platforms_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Seed con las dos plataformas que vienen en el JSON. Si llega TikTok o Twitch
-- en el futuro, basta un INSERT más, sin tocar código.
INSERT INTO platforms (code, display_name)
SELECT 'youtube', 'YouTube'
WHERE NOT EXISTS (SELECT 1 FROM platforms WHERE code = 'youtube');

INSERT INTO platforms (code, display_name)
SELECT 'blog', 'Blog'
WHERE NOT EXISTS (SELECT 1 FROM platforms WHERE code = 'blog');

CREATE TABLE IF NOT EXISTS channels (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    platform_id  BIGINT       NOT NULL,                        -- FK a platforms
    external_id  VARCHAR(255) NOT NULL,                        -- channel_id del JSON
    name         VARCHAR(255) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_channels_platform_external (platform_id, external_id),
    CONSTRAINT fk_channels_platform
        FOREIGN KEY (platform_id) REFERENCES platforms (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS authors (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    name       VARCHAR(255) NOT NULL,
    url        VARCHAR(500) NOT NULL,
    image_url  VARCHAR(500),
    PRIMARY KEY (id),
    UNIQUE KEY uk_authors_url (url)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS publications (
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    channel_id        BIGINT       NOT NULL,
    author_id         BIGINT       NULL,                       -- NULL para YouTube
    url               VARCHAR(500) NOT NULL,
    image_url         VARCHAR(500),
    date_published    DATETIME     NOT NULL,
    description       TEXT,
    publication_id    VARCHAR(255),                            -- solo YouTube (id de vídeo)
    format            VARCHAR(20),                             -- solo YouTube: 'video' | 'short'
    duration_seconds  INT,                                     -- solo YouTube
    PRIMARY KEY (id),
    UNIQUE KEY uk_publications_url (url),
    KEY idx_publications_date (date_published),
    CONSTRAINT fk_publications_channel
        FOREIGN KEY (channel_id) REFERENCES channels (id),
    CONSTRAINT fk_publications_author
        FOREIGN KEY (author_id)  REFERENCES authors  (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- Archivado de imágenes: los bytes de la miniatura/portada viven en BD
-- aunque el origen (YouTube, blog) borre el contenido. Una imagen por
-- publicación.
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS publication_images (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    publication_id  BIGINT       NOT NULL,
    source_url      VARCHAR(500),                              -- URL de la que se descargó
    mime_type       VARCHAR(50),                               -- p.ej. image/jpeg, image/png
    image_bytes     LONGBLOB     NOT NULL,
    downloaded_at   DATETIME     NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_publication_images_publication (publication_id),
    CONSTRAINT fk_publication_images_publication
        FOREIGN KEY (publication_id) REFERENCES publications (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- Auditoría: cada carga del JSON deja una fila aquí.
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS import_runs (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    source          VARCHAR(500) NOT NULL,                     -- ruta o classpath origen
    started_at      DATETIME     NOT NULL,
    finished_at     DATETIME,
    items_inserted  INT          NOT NULL DEFAULT 0,
    items_skipped   INT          NOT NULL DEFAULT 0,
    items_failed    INT          NOT NULL DEFAULT 0,
    status          VARCHAR(20)  NOT NULL,                     -- OK | PARTIAL | ERROR
    error_message   TEXT,
    PRIMARY KEY (id),
    KEY idx_import_runs_started_at (started_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
