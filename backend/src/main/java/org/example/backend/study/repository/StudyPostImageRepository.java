package org.example.backend.study.repository;

import org.example.backend.study.entity.StudyPostImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudyPostImageRepository extends JpaRepository<StudyPostImage, Long> {
    List<StudyPostImage> findAllByStudyPostIdOrderByImageOrder(Long studyPostId);
    void deleteAllByStudyPostId(Long studyPostId);
}