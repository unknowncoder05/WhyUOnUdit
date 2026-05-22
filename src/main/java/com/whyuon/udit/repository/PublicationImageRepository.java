package com.whyuon.udit.repository;

import com.whyuon.udit.model.PublicationImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PublicationImageRepository extends JpaRepository<PublicationImage, Long> {

    Optional<PublicationImage> findByPublicationId(Long publicationId);

    boolean existsByPublicationId(Long publicationId);
}
