package org.example.backend.study.repository;

import org.example.backend.study.entity.StudyPostComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudyPostCommentRepository extends JpaRepository<StudyPostComment, Long> {

    List<StudyPostComment> findAllByStudyPostId(Long postId);
}
