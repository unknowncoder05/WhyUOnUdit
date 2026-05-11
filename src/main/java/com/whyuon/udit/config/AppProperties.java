package com.whyuon.udit.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private boolean loadOnStartup = true;
    private String jsonSource = "classpath:whyuon.json";
    private String corsAllowedOrigins = "http://localhost:8081";

    public boolean isLoadOnStartup() {
        return loadOnStartup;
    }

    public void setLoadOnStartup(boolean loadOnStartup) {
        this.loadOnStartup = loadOnStartup;
    }

    public String getJsonSource() {
        return jsonSource;
    }

    public void setJsonSource(String jsonSource) {
        this.jsonSource = jsonSource;
    }

    public String getCorsAllowedOrigins() {
        return corsAllowedOrigins;
    }

    public void setCorsAllowedOrigins(String corsAllowedOrigins) {
        this.corsAllowedOrigins = corsAllowedOrigins;
    }
}
