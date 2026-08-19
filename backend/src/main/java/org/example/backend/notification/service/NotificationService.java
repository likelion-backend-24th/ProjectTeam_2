package org.example.backend.notification.service;

import lombok.RequiredArgsConstructor;
import org.example.backend.common.exception.BusinessException;
import org.example.backend.notification.dto.NotificationCountResponse;
import org.example.backend.notification.dto.NotificationResponse;
import org.example.backend.notification.entity.Notification;
import org.example.backend.notification.entity.NotificationTargetType;
import org.example.backend.notification.exception.NotificationErrorCode;
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
        // 이제 여기서 markAllAsReadByReceiverId 호출 삭제 — 열람만으로는 읽음 처리 안 함
        return notificationRepository
                .findAllByReceiverIdAndIsReadFalseOrderByCreatedAtDesc(userId, pageable)
                .map(NotificationResponse::from);
    }

    // 추가: 알림 하나를 클릭했을 때 그것만 읽음 처리
    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new BusinessException(NotificationErrorCode.NOTIFICATION_NOT_FOUND));

        if (!notification.getReceiver().getId().equals(userId)) {
            throw new BusinessException(NotificationErrorCode.NOTIFICATION_ACCESS_DENIED);
        }

        notification.setRead(true);   // 엔티티에 @Setter 있어서 바로 저장(더티체킹으로 커밋 시 반영)
    }

}