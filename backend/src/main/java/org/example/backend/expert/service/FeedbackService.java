package org.example.backend.expert.service;

import lombok.RequiredArgsConstructor;
import org.example.backend.auth.service.EmailService;
import org.example.backend.common.exception.BusinessException;
import org.example.backend.common.file.FileStorageService;
import org.example.backend.common.file.ImageValidator;
import org.example.backend.expert.dto.request.FeedbackCreateRequest;
import org.example.backend.expert.dto.request.FeedbackMessageRequest;
import org.example.backend.expert.dto.response.FeedbackMessageResponse;
import org.example.backend.expert.dto.response.FeedbackResponse;
import org.example.backend.expert.dto.response.MyFeedbackListResponse;
import org.example.backend.expert.dto.response.MyFeedbackSummaryResponse;
import org.example.backend.expert.entity.*;
import org.example.backend.expert.exception.ExpertErrorCode;
import org.example.backend.expert.repository.ExpertProfileRepository;
import org.example.backend.expert.repository.FeedbackMessageImageRepository;
import org.example.backend.expert.repository.FeedbackMessageRepository;
import org.example.backend.expert.repository.FeedbackRepository;
import org.example.backend.user.entity.User;
import org.example.backend.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.example.backend.notification.entity.NotificationTargetType;
import org.example.backend.notification.service.NotificationService;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.LinkedHashMap;

import org.example.backend.user.entity.AccountStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.example.backend.expert.dto.response.ExpertFeedbackListResponse;
import org.example.backend.report.entity.ReportTargetType;
import org.example.backend.report.repository.ReportRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final FeedbackMessageRepository feedbackMessageRepository;
    private final ExpertProfileRepository expertProfileRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final NotificationService notificationService;
    private final FeedbackMessageImageRepository feedbackMessageImageRepository;
    private final FileStorageService fileStorageService;
    private final ImageValidator imageValidator;
    private final ReportRepository reportRepository;

    @Transactional
    public FeedbackResponse createFeedback(Long requesterId, FeedbackCreateRequest request,  List<MultipartFile> images) {
        User requester = userRepository.findByIdForUpdate(requesterId)
                .orElseThrow(() -> new BusinessException(ExpertErrorCode.USER_NOT_FOUND));
        if (requester.getStatus() != AccountStatus.ACTIVE) {
            throw new BusinessException(ExpertErrorCode.FEEDBACK_USER_INACTIVE);
        }
        if (!requester.isSubscribed()) {
            throw new BusinessException(ExpertErrorCode.SUBSCRIPTION_REQUIRED);
        }
        ExpertProfile expertProfile = expertProfileRepository.findById(request.getExpertProfileId())
                .orElseThrow(() -> new BusinessException(ExpertErrorCode.EXPERT_PROFILE_NOT_FOUND));
        if (!expertProfile.isApproved()) {
            throw new BusinessException(ExpertErrorCode.FEEDBACK_EXPERT_NOT_APPROVED);
        }
        if (expertProfile.getUser().getStatus() != AccountStatus.ACTIVE) {
            throw new BusinessException(ExpertErrorCode.FEEDBACK_USER_INACTIVE);
        }
        if (feedbackRepository.existsByRequesterIdAndExpertProfileIdAndClosedAtIsNull(requesterId, request.getExpertProfileId())) {
            throw new BusinessException(ExpertErrorCode.FEEDBACK_ALREADY_OPEN);
        }
        if (feedbackRepository.countByRequesterIdAndClosedAtIsNull(requesterId) >= 5) {
            throw new BusinessException(ExpertErrorCode.FEEDBACK_OPEN_LIMIT_EXCEEDED);
        }


        Feedback feedback = Feedback.builder()
                .requester(requester)
                .expertProfile(expertProfile)
                .topic(request.getTopic())
                .build();
        feedbackRepository.save(feedback);

        FeedbackMessage firstMessage = feedbackMessageRepository.save(
                FeedbackMessage.builder()
                        .feedback(feedback)
                        .sender(requester)
                        .content(request.getContent())
                        .build()
        );
        saveMessageImages(firstMessage, images);

        notificationService.notifyComment(
                expertProfile.getUser(),   // 알림 받을 사람 = 전문가
                requester,                   // 문의 보낸 사람
                NotificationTargetType.FEEDBACK,
                feedback.getId(),
                firstMessage.getId(),
                request.getContent()
        );

        return FeedbackResponse.from(feedback);
    }

    public FeedbackResponse getFeedback(Long callerId, Long feedbackId) {
        Feedback feedback = getFeedbackOrThrow(feedbackId);
        validateFeedbackAccess(feedback, callerId);
        return FeedbackResponse.from(feedback);
    }

    @Transactional
    public FeedbackMessageResponse addMessage(Long senderId, Long feedbackId, FeedbackMessageRequest request, List<MultipartFile> images) {
        Feedback feedback = getFeedbackOrThrow(feedbackId);
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new BusinessException(ExpertErrorCode.USER_NOT_FOUND));

        boolean isRequester = feedback.getRequester().getId().equals(senderId);
        boolean isExpertAnswering = feedback.getExpertProfile().getUser().getId().equals(senderId);
        if (!isRequester && !isExpertAnswering) {
            throw new BusinessException(ExpertErrorCode.FEEDBACK_ACCESS_DENIED);
        }
        if (feedback.isClosed()) {
            throw new BusinessException(ExpertErrorCode.FEEDBACK_CLOSED);
        }
        if (feedback.getRequester().getStatus() != AccountStatus.ACTIVE
                || feedback.getExpertProfile().getUser().getStatus() != AccountStatus.ACTIVE) {
            throw new BusinessException(ExpertErrorCode.FEEDBACK_USER_INACTIVE);
        }
        if (!feedback.getRequester().isSubscribed()) {
            throw new BusinessException(ExpertErrorCode.FEEDBACK_SUBSCRIPTION_EXPIRED);
        }

        FeedbackMessage message = feedbackMessageRepository.save(
                FeedbackMessage.builder()
                        .feedback(feedback)
                        .sender(sender)
                        .content(request.getContent())
                        .build()
        );

        List<String> imageUrls = saveMessageImages(message, images);

        User receiver = isExpertAnswering ? feedback.getRequester() : feedback.getExpertProfile().getUser();
        notificationService.notifyComment(
                receiver,
                sender,
                NotificationTargetType.FEEDBACK,
                feedback.getId(),
                message.getId(),
                request.getContent()
        );

        // 전문가가 지금 답변하고 있다면 → 답변완료 처리한다.
        if (isExpertAnswering) {
            feedback.markAnswered();
            emailService.sendFeedbackAnswered(feedback.getRequester().getUsername(), feedback.getTopic());
        } else if (isRequester) {
            feedback.markPending();
        }
        return FeedbackMessageResponse.from(message, imageUrls);
    }

    @Transactional
    public void closeThreadsByExpertProfile(ExpertProfile expertProfile) {
        List<Feedback> openThreads = feedbackRepository.findByExpertProfileIdAndClosedAtIsNull(expertProfile.getId());
        openThreads.forEach(feedback -> feedback.close(FeedbackCloseReason.EXPERT_REVOKED));
    }

    @Transactional
    public FeedbackResponse closeThread(Long requesterId, Long feedbackId) {
        Feedback feedback = getFeedbackOrThrow(feedbackId);
        if (!feedback.getRequester().getId().equals(requesterId)) {
            throw new BusinessException(ExpertErrorCode.FEEDBACK_ACCESS_DENIED);
        }
        if (feedback.isClosed()) {
            throw new BusinessException(ExpertErrorCode.FEEDBACK_CLOSED);
        }
        feedback.close(FeedbackCloseReason.REQUESTER_CLOSED);
        return FeedbackResponse.from(feedback);
    }


    public List<FeedbackMessageResponse> getMessages(Long callerId, Long feedbackId) {
        Feedback feedback = getFeedbackOrThrow(feedbackId);
        validateFeedbackAccess(feedback, callerId);
        List<FeedbackMessage> messages = feedbackMessageRepository.findByFeedbackIdOrderByCreatedAtAsc(feedbackId);
             Map<Long, List<String>> imageUrlsByMessageId = getImageUrlsByMessageIds(messages);
             return messages.stream()
                     .map(message -> FeedbackMessageResponse.from(message,
                             imageUrlsByMessageId.getOrDefault(message.getId(), List.of())))
                     .toList();
    }

    public MyFeedbackListResponse getMyFeedbacks(Long requesterId, Pageable pageable) {
        Page<MyFeedbackSummaryResponse> page = feedbackRepository.findByRequesterId(requesterId, pageable)
                .map(MyFeedbackSummaryResponse::from);
        return MyFeedbackListResponse.from(page);
    }

    // 특정 전문가 1명에게 들어온 문의 스레드 리턴
//    전문가 프로필 id를 지정해서, 그 전문가에게 들어온 문의 스레드를 전부 조회하는 기능
    public ExpertFeedbackListResponse getExpertFeedbacks(Long expertProfileId, Pageable pageable) {
        Page<FeedbackResponse> page = feedbackRepository.findByExpertProfileId(expertProfileId, pageable)
                .map(FeedbackResponse::from);
        return ExpertFeedbackListResponse.from(page);
    }
// 전문가 본인이 받은 문의 목록
//    로그인한 전문가 본인이 "내가 받은 문의함" 화면을 열었을 때 쓰는 기능

    public ExpertFeedbackListResponse getMyExpertFeedbacks(Long expertUserId, Pageable pageable) {
        ExpertProfile expertProfile = expertProfileRepository.findByUserId(expertUserId)
                .orElseThrow(() -> new BusinessException(ExpertErrorCode.EXPERT_PROFILE_NOT_FOUND));
        return getExpertFeedbacks(expertProfile.getId(), pageable);
    }

    private Feedback getFeedbackOrThrow(Long id) {
        return feedbackRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ExpertErrorCode.FEEDBACK_NOT_FOUND));
    }

    // report 패키지에서 피드백 스레드 신고 시 신고자가 당사자(요청자/담당 전문가)인지 확인할 때 사용
    public boolean isParticipant(Long feedbackId, Long userId) {
        return feedbackRepository.findById(feedbackId)
                .map(feedback -> feedback.getRequester().getId().equals(userId)
                        || feedback.getExpertProfile().getUser().getId().equals(userId))
                .orElse(false);
    }

    // 관리자용 - 당사자 여부와 무관하게, 이 스레드에 대한 신고가 실제로 존재할 때만 열람을 허용한다
    public FeedbackResponse getFeedbackForAdmin(Long feedbackId) {
        Feedback feedback = getFeedbackOrThrow(feedbackId);
        if (!reportRepository.existsByTargetTypeAndTargetId(ReportTargetType.FEEDBACK, feedbackId)) {
            throw new BusinessException(ExpertErrorCode.FEEDBACK_NOT_REPORTED);
        }
        return FeedbackResponse.from(feedback);
    }

    // 관리자용 - 위와 동일한 게이트로, 신고된 스레드의 메시지(이미지 포함)를 조회한다
    public List<FeedbackMessageResponse> getMessagesForAdmin(Long feedbackId) {
        getFeedbackOrThrow(feedbackId);
        if (!reportRepository.existsByTargetTypeAndTargetId(ReportTargetType.FEEDBACK, feedbackId)) {
            throw new BusinessException(ExpertErrorCode.FEEDBACK_NOT_REPORTED);
        }
        List<FeedbackMessage> messages = feedbackMessageRepository.findByFeedbackIdOrderByCreatedAtAsc(feedbackId);
        Map<Long, List<String>> imageUrlsByMessageId = getImageUrlsByMessageIds(messages);
        return messages.stream()
                .map(message -> FeedbackMessageResponse.from(message,
                        imageUrlsByMessageId.getOrDefault(message.getId(), List.of())))
                .toList();
    }

    private void validateFeedbackAccess(Feedback feedback, Long callerId) {
        boolean isRequester = feedback.getRequester().getId().equals(callerId);
        boolean isExpert = feedback.getExpertProfile().getUser().getId().equals(callerId);
        if (!isRequester && !isExpert) {
            throw new BusinessException(ExpertErrorCode.FEEDBACK_ACCESS_DENIED);
        }
    }

    private List<String> saveMessageImages(FeedbackMessage message, List<MultipartFile> images) {
        if (images == null || images.isEmpty()) {
            return List.of();
        }

        List<String> imageUrls = new ArrayList<>();
        for (int i = 0; i < images.size(); i++) {
            MultipartFile file = images.get(i);
            imageValidator.validate(file);
            String url = fileStorageService.upload(file, "feedback-messages");
            feedbackMessageImageRepository.save(new FeedbackMessageImage(message, url, file.getOriginalFilename(), i));
            imageUrls.add(url);
        }
        return imageUrls;
    }

    // 메시지 목록 조회 시, 메시지마다 이미지를 개별 조회하지 않고 한 번에 배치로 가져와 messageId별로 묶는다 (N+1 방지)
    private Map<Long, List<String>> getImageUrlsByMessageIds(List<FeedbackMessage> messages) {
             List<Long> messageIds = messages.stream().map(FeedbackMessage::getId).toList();
             if (messageIds.isEmpty()) {
                     return Map.of();
                 }
             return feedbackMessageImageRepository.findAllByFeedbackMessageIdInOrderByImageOrder(messageIds).stream()
                     .collect(Collectors.groupingBy(
                             image -> image.getFeedbackMessage().getId(),
                             LinkedHashMap::new,
                             Collectors.mapping(FeedbackMessageImage::getImageUrl, Collectors.toList())
                     ));

    }
}