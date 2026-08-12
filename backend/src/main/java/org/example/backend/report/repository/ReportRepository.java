package org.example.backend.report.repository;

import org.example.backend.report.entity.Report;
import org.example.backend.report.entity.ReportStatus;
import org.example.backend.report.entity.ReportTargetType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReportRepository extends JpaRepository<Report, Long> {

    boolean existsByTargetTypeAndTargetIdAndReporterId(
            ReportTargetType targetType, Long targetId, Long reporterId);

    Page<Report> findAllByStatus(ReportStatus status, Pageable pageable);
}