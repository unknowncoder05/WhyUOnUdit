package com.whyuon.udit.repository;

import com.whyuon.udit.model.Publication;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PublicationRepository extends JpaRepository<Publication, Long> {

    boolean existsByUrl(String url);
}
