-- =====================================================================
-- MINI-WHYUON · Esquema MySQL
-- ---------------------------------------------------------------------
-- Modelo normalizado en 3 tablas:
--   channels        canales/blogs origen (YouTube o blog)
--   authors         autores de blog (los canales de YouTube no tienen autor)
--   publications    cada video o post individual, apunta a channels y opcionalmente a authors
-- =====================================================================

CREATE TABLE IF NOT EXISTS channels (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    platform     VARCHAR(20)  NOT NULL,                       -- 'youtube' | 'blog'
    external_id  VARCHAR(255) NOT NULL,                       -- channel_id del JSON
    name         VARCHAR(255) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_channels_platform_external (platform, external_id)
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
    author_id         BIGINT       NULL,                      -- NULL para YouTube
    url               VARCHAR(500) NOT NULL,
    image_url         VARCHAR(500),
    date_published    DATETIME     NOT NULL,
    description       TEXT,
    publication_id    VARCHAR(255),                           -- solo YouTube (id de video)
    format            VARCHAR(20),                            -- solo YouTube: 'video' | 'short'
    duration_seconds  INT,                                    -- solo YouTube
    PRIMARY KEY (id),
    UNIQUE KEY uk_publications_url (url),
    KEY idx_publications_date (date_published),
    CONSTRAINT fk_publications_channel
        FOREIGN KEY (channel_id) REFERENCES channels (id),
    CONSTRAINT fk_publications_author
        FOREIGN KEY (author_id)  REFERENCES authors  (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
