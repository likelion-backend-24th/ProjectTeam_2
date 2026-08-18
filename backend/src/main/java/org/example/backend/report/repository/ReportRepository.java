package org.example.backend.report.repository;

import org.example.backend.report.entity.Report;
import org.example.backend.report.entity.ReportStatus;
import org.example.backend.report.entity.ReportTargetType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReportRepository extends JpaRepository<Report, Long> {

    // 반려/삭제 처리된 예전 신고는 무시하고, 아직 PENDING인 중복 신고만 막을 때 사용
    boolean existsByTargetTypeAndTargetIdAndReporterIdAndStatus(
            ReportTargetType targetType, Long targetId, Long reporterId, ReportStatus status);

    Page<Report> findAllByStatus(ReportStatus status, Pageable pageable);

    // 관리자가 신고된 피드백 스레드만 열람할 수 있도록 게이트할 때 사용 (신고자 무관하게 존재 여부만 확인)
    boolean existsByTargetTypeAndTargetId(ReportTargetType targetType, Long targetId);
}