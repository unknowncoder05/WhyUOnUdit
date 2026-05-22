package com.whyuon.udit.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Registro de auditoría de cada ejecución del importador del JSON. Permite
 * responder a "¿cuándo se cargaron estos datos?" y detectar importaciones
 * fallidas o parciales.
 */
@Entity
@Table(name = "import_runs")
public class ImportRun {

    public enum Status { OK, PARTIAL, ERROR }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String source;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @Column(name = "items_inserted", nullable = false)
    private int itemsInserted;

    @Column(name = "items_skipped", nullable = false)
    private int itemsSkipped;

    @Column(name = "items_failed", nullable = false)
    private int itemsFailed;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    public ImportRun() {
    }

    public ImportRun(String source) {
        this.source = source;
        this.startedAt = LocalDateTime.now();
        this.itemsInserted = 0;
        this.itemsSkipped = 0;
        this.itemsFailed = 0;
        this.status = Status.OK.name();
    }

    public void finishOk(int inserted, int skipped) {
        this.finishedAt = LocalDateTime.now();
        this.itemsInserted = inserted;
        this.itemsSkipped = skipped;
        this.status = (itemsFailed == 0 ? Status.OK : Status.PARTIAL).name();
    }

    public void finishError(String message) {
        this.finishedAt = LocalDateTime.now();
        this.status = Status.ERROR.name();
        this.errorMessage = message;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }

    public LocalDateTime getFinishedAt() { return finishedAt; }
    public void setFinishedAt(LocalDateTime finishedAt) { this.finishedAt = finishedAt; }

    public int getItemsInserted() { return itemsInserted; }
    public void setItemsInserted(int itemsInserted) { this.itemsInserted = itemsInserted; }

    public int getItemsSkipped() { return itemsSkipped; }
    public void setItemsSkipped(int itemsSkipped) { this.itemsSkipped = itemsSkipped; }

    public int getItemsFailed() { return itemsFailed; }
    public void setItemsFailed(int itemsFailed) { this.itemsFailed = itemsFailed; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
