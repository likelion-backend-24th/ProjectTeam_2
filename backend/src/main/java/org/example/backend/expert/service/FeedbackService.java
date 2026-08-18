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
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import org.example.backend.user.entity.AccountStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.example.backend.expert.dto.response.ExpertFeedbackListResponse;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final FeedbackMessageRepository feedbackMessageRepository;
    private final ExpertProfileRepository expertProfileRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final FeedbackMessageImageRepository feedbackMessageImageRepository;
    private final FileStorageService fileStorageService;
    private final ImageValidator imageValidator;

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
        return feedbackMessageRepository.findByFeedbackIdOrderByCreatedAtAsc(feedbackId)
                .stream().map(message -> FeedbackMessageResponse.from(message, getImageUrls(message.getId()))).toList();    }

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

    private List<String> getImageUrls(Long messageId) {
        return feedbackMessageImageRepository.findAllByFeedbackMessageIdOrderByImageOrder(messageId).stream()
                .map(FeedbackMessageImage::getImageUrl)
                .toList();
    }
}