package org.example.backend.notification.service;

import lombok.RequiredArgsConstructor;
import org.example.backend.notification.dto.NotificationCountResponse;
import org.example.backend.notification.dto.NotificationResponse;
import org.example.backend.notification.entity.Notification;
import org.example.backend.notification.entity.NotificationTargetType;
import org.example.backend.notification.repository.NotificationRepository;
import org.example.backend.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    @Transactional
    public void notifyComment(User receiver, User commenter, NotificationTargetType targetType, Long targetId, Long commentId, String commentContent) {
        // 본인 글에 본인이 댓글 단 경우엔 알림을 만들지 않음
        if (receiver.getId().equals(commenter.getId())) {
            return;
        }
        String preview = commentContent.length() > 15 ? commentContent.substring(0, 15) + "..." : commentContent;
        notificationRepository.save(new Notification(receiver, targetType, targetId, commentId, preview));
    }
    public NotificationCountResponse getUnreadCount(Long userId) {
        long count = notificationRepository.countByReceiverIdAndIsReadFalse(userId);
        return new NotificationCountResponse(count);
    }

    @Transactional
    public Page<NotificationResponse> getNotifications(Long userId, Pageable pageable) {
        Page<NotificationResponse> notifications = notificationRepository
                .findAllByReceiverIdAndIsReadFalseOrderByCreatedAtDesc(userId, pageable)   // 여기만 변경
                .map(NotificationResponse::from);

        // 목록을 조회하는 이 시점에 안 읽은 알림을 전부 읽음 처리
        // -> 다음 번 조회부터는 방금 보여준 것들이 자동으로 목록에서 빠짐
        notificationRepository.markAllAsReadByReceiverId(userId);

        return notifications;
    }
}