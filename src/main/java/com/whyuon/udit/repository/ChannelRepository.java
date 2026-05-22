package com.whyuon.udit.repository;

import com.whyuon.udit.model.Channel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChannelRepository extends JpaRepository<Channel, Long> {

    Optional<Channel> findByPlatformCodeAndExternalId(String platformCode, String externalId);
}
