package com.whyuon.udit.model;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

/**
 * Bytes de la imagen de una publicación, archivada en BD para que sobreviva
 * a borrados en el origen (YouTube, blog). Una imagen por publicación.
 */
@Entity
@Table(
    name = "publication_images",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_publication_images_publication",
        columnNames = "publication_id"
    )
)
public class PublicationImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "publication_id", nullable = false)
    private Publication publication;

    @Column(name = "source_url", length = 500)
    private String sourceUrl;

    @Column(name = "mime_type", length = 50)
    private String mimeType;

    /**
     * Los bytes en sí. Marcado como LAZY para que JPA no cargue megas de
     * datos cada vez que se accede a la entidad sin necesitarlos.
     */
    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "image_bytes", nullable = false, columnDefinition = "LONGBLOB")
    private byte[] imageBytes;

    @Column(name = "downloaded_at", nullable = false)
    private LocalDateTime downloadedAt;

    public PublicationImage() {
    }

    public PublicationImage(Publication publication, String sourceUrl, String mimeType, byte[] imageBytes) {
        this.publication = publication;
        this.sourceUrl = sourceUrl;
        this.mimeType = mimeType;
        this.imageBytes = imageBytes;
        this.downloadedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Publication getPublication() { return publication; }
    public void setPublication(Publication publication) { this.publication = publication; }

    public String getSourceUrl() { return sourceUrl; }
    public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }

    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }

    public byte[] getImageBytes() { return imageBytes; }
    public void setImageBytes(byte[] imageBytes) { this.imageBytes = imageBytes; }

    public LocalDateTime getDownloadedAt() { return downloadedAt; }
    public void setDownloadedAt(LocalDateTime downloadedAt) { this.downloadedAt = downloadedAt; }
}
