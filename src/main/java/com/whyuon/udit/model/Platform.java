package com.whyuon.udit.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Plataforma (YouTube, blog, ...). Tabla maestra para evitar duplicar el
 * texto en cada fila de channels y permitir añadir nuevas sin tocar código.
 */
@Entity
@Table(
    name = "platforms",
    uniqueConstraints = @UniqueConstraint(name = "uk_platforms_code", columnNames = "code")
)
public class Platform {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String code;

    @Column(name = "display_name", nullable = false, length = 50)
    private String displayName;

    public Platform() {
    }

    public Platform(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
}
