package org.example.backend.notification.repository;

import org.example.backend.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    long countByReceiverIdAndIsReadFalse(Long receiverId);

    Page<Notification> findAllByReceiverIdOrderByCreatedAtDesc(Long receiverId, Pageable pageable);

    // 추가: 확인 안 한 알림만 조회 (드롭다운에 표시할 목록용)
    Page<Notification> findAllByReceiverIdAndIsReadFalseOrderByCreatedAtDesc(Long receiverId, Pageable pageable);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.receiver.id = :receiverId AND n.isRead = false")
    void markAllAsReadByReceiverId(@Param("receiverId") Long receiverId);
}