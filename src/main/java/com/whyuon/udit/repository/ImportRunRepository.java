package com.whyuon.udit.repository;

import com.whyuon.udit.model.ImportRun;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ImportRunRepository extends JpaRepository<ImportRun, Long> {

    List<ImportRun> findAllByOrderByStartedAtDesc();
}
