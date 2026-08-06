package org.example.backend.expert.repository;

import org.example.backend.expert.entity.Certification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CertificationRepository extends JpaRepository<Certification, Long> {

    List<Certification> findByExpertProfileId(Long expertProfileId);
}