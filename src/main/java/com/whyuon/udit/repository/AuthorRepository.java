package com.whyuon.udit.repository;

import com.whyuon.udit.model.Author;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AuthorRepository extends JpaRepository<Author, Long> {

    Optional<Author> findByUrl(String url);
}
