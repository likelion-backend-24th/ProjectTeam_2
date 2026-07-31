package org.example.backend.study.repository;

import org.example.backend.study.entity.Study;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StudyRepository extends JpaRepository<Study, Long> {
    Page<Study> findByTitleContaining(String keyword, Pageable pageable);
}
