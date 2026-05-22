package com.whyuon.udit.service;

import com.whyuon.udit.model.Publication;
import com.whyuon.udit.model.PublicationImage;
import com.whyuon.udit.repository.PublicationImageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

/**
 * Descarga la imagen apuntada por una URL y la guarda en BD asociada a una
 * publicación. Si la descarga falla (404, timeout, etc.) no propaga el error:
 * se logea un warning y la publicación queda sin imagen archivada.
 *
 * Pensado para preservar miniaturas de YouTube y portadas de blog aunque el
 * origen las borre algún día.
 */
@Service
public class ImageArchiveService {

    private static final Logger log = LoggerFactory.getLogger(ImageArchiveService.class);

    private static final int CONNECT_TIMEOUT_MS = 5_000;
    private static final int READ_TIMEOUT_MS = 10_000;
    private static final int MAX_BYTES = 5 * 1024 * 1024;   // 5 MB

    private final PublicationImageRepository imageRepository;

    public ImageArchiveService(PublicationImageRepository imageRepository) {
        this.imageRepository = imageRepository;
    }

    /**
     * Intenta archivar la imagen. No lanza excepciones para no romper la
     * carga del JSON; el peor caso es que la publicación se quede sin
     * imagen y el frontend caiga al fallback (avatar del autor o degradado).
     */
    @Transactional
    public void archive(Publication publication, String imageUrl) {
        if (publication == null || imageUrl == null || imageUrl.isBlank()) {
            return;
        }
        if (imageRepository.existsByPublicationId(publication.getId())) {
            return;  // ya archivada, no la volvemos a descargar
        }
        try {
            DownloadedImage downloaded = download(imageUrl);
            imageRepository.save(new PublicationImage(publication, imageUrl, downloaded.mimeType(), downloaded.bytes()));
            log.debug("Imagen archivada para publication {} desde {}", publication.getId(), imageUrl);
        } catch (Exception e) {
            log.warn("No se pudo archivar la imagen de la publication {} ({}): {}",
                    publication.getId(), imageUrl, e.getMessage());
        }
    }

    private DownloadedImage download(String url) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
        conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
        conn.setReadTimeout(READ_TIMEOUT_MS);
        conn.setRequestProperty("User-Agent", "Mini-WHYUON/1.0");
        conn.setInstanceFollowRedirects(true);

        int status = conn.getResponseCode();
        if (status < 200 || status >= 300) {
            conn.disconnect();
            throw new IOException("HTTP " + status + " al descargar " + url);
        }

        String mimeType = conn.getContentType();
        try (InputStream in = conn.getInputStream()) {
            byte[] bytes = in.readNBytes(MAX_BYTES);
            if (in.read() != -1) {
                throw new IOException("La imagen supera el tamano maximo permitido (" + MAX_BYTES + " bytes)");
            }
            return new DownloadedImage(bytes, mimeType);
        } finally {
            conn.disconnect();
        }
    }

    private record DownloadedImage(byte[] bytes, String mimeType) {}
}
