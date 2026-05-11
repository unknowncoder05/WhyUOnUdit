package com.whyuon.udit.loader;

import com.whyuon.udit.config.AppProperties;
import com.whyuon.udit.dto.ImportResultResponse;
import com.whyuon.udit.service.JsonImportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Carga whyuon.json al arrancar cuando la configuracion lo permite.
 */
@Component
public class JsonLoader implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(JsonLoader.class);

    private final AppProperties appProperties;
    private final JsonImportService jsonImportService;

    public JsonLoader(AppProperties appProperties, JsonImportService jsonImportService) {
        this.appProperties = appProperties;
        this.jsonImportService = jsonImportService;
    }

    @Override
    public void run(String... args) throws Exception {
        if (!appProperties.isLoadOnStartup()) {
            log.info("Carga inicial desactivada por configuracion.");
            return;
        }
        ImportResultResponse result = jsonImportService.importFromSource(appProperties.getJsonSource());
        log.info("Carga JSON terminada desde {}: {} publicaciones insertadas, {} ya existian.",
                result.source(), result.inserted(), result.skipped());
    }
}
