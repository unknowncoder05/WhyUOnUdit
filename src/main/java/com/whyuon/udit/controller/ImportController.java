package com.whyuon.udit.controller;

import com.whyuon.udit.config.AppProperties;
import com.whyuon.udit.dto.ImportResultResponse;
import com.whyuon.udit.service.JsonImportService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/api/import")
public class ImportController {

    private final JsonImportService jsonImportService;
    private final AppProperties appProperties;

    public ImportController(JsonImportService jsonImportService, AppProperties appProperties) {
        this.jsonImportService = jsonImportService;
        this.appProperties = appProperties;
    }

    @PostMapping
    public ImportResultResponse importData(@RequestParam(required = false) String path) throws IOException {
        return jsonImportService.importFromSource(path == null || path.isBlank()
                ? appProperties.getJsonSource()
                : path);
    }
}
