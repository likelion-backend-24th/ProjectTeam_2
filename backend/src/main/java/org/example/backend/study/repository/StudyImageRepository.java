package org.example.backend.study.repository;

import org.example.backend.study.entity.StudyImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudyImageRepository extends JpaRepository<StudyImage, Long> {
    List<StudyImage> findAllByStudyIdOrderByImageOrder(Long studyId);
    void deleteAllByStudyId(Long studyId);
}