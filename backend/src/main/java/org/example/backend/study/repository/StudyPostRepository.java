package org.example.backend.study.repository;

import org.example.backend.study.entity.StudyPost;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudyPostRepository extends JpaRepository<StudyPost, Long> {
    List<StudyPost> findAllByStudyIdOrderByPinnedDescCreatedAtDesc(Long id);
}
