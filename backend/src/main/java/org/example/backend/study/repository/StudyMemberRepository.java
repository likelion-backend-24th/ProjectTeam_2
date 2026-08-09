package org.example.backend.study.repository;

import org.example.backend.study.entity.StudyMember;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudyMemberRepository extends JpaRepository<StudyMember, Long> {
    int countByStudyId(Long studyId);
    Optional<StudyMember> findByStudyIdAndUserId(Long studyId, Long userId);
    int countByUserId(Long userId);
    List<StudyMember> findByStudyId(Long id);
    Page<StudyMember> findByUserId(Long userId, Pageable pageable);
    void deleteByStudyId(Long id);

    @Query("""
        SELECT sm.study.id AS studyId, COUNT(sm) AS memberCount
        FROM StudyMember sm
        WHERE sm.study.id IN :studyIds
        GROUP BY sm.study.id
        """)
    List<StudyMemberCountProjection> countByStudyIds(@Param("studyIds") List<Long> studyIds);

    interface StudyMemberCountProjection {
        Long getStudyId();
        Long getMemberCount();
    }
}
