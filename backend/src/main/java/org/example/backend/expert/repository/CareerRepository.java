package org.example.backend.expert.repository;

import org.example.backend.expert.entity.Career;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CareerRepository extends JpaRepository<Career, Long> {

    List<Career> findByExpertProfileId(Long expertProfileId);
}