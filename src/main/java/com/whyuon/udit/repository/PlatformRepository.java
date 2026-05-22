package com.whyuon.udit.repository;

import com.whyuon.udit.model.Platform;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PlatformRepository extends JpaRepository<Platform, Long> {

    Optional<Platform> findByCode(String code);
}
