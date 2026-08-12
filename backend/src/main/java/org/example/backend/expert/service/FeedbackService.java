package org.example.backend.expert.service;

import lombok.RequiredArgsConstructor;
import org.example.backend.common.exception.BusinessException;
import org.example.backend.expert.dto.request.FeedbackCreateRequest;
import org.example.backend.expert.dto.request.FeedbackMessageRequest;
import org.example.backend.expert.dto.response.FeedbackMessageResponse;
import org.example.backend.expert.dto.response.FeedbackResponse;
import org.example.backend.expert.dto.response.MyFeedbackListResponse;
import org.example.backend.expert.dto.response.MyFeedbackSummaryResponse;
import org.example.backend.expert.entity.ExpertProfile;
import org.example.backend.expert.entity.Feedback;
import org.example.backend.expert.entity.FeedbackMessage;
import org.example.backend.expert.exception.ExpertErrorCode;
import org.example.backend.expert.repository.ExpertProfileRepository;
import org.example.backend.expert.repository.FeedbackMessageRepository;
import org.example.backend.expert.repository.FeedbackRepository;
import org.example.backend.user.entity.User;
import org.example.backend.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final FeedbackMessageRepository feedbackMessageRepository;
    private final ExpertProfileRepository expertProfileRepository;
    private final UserRepository userRepository;

    @Transactional
    public FeedbackResponse createFeedback(Long requesterId, FeedbackCreateRequest request) {
        User requester = userRepository.findById(requesterId)
                .orElseThrow(() -> new BusinessException(ExpertErrorCode.USER_NOT_FOUND));
        if (!requester.isSubscribed()) {
            throw new BusinessException(ExpertErrorCode.SUBSCRIPTION_REQUIRED);
        }
        ExpertProfile expertProfile = expertProfileRepository.findById(request.getExpertProfileId())
                .orElseThrow(() -> new BusinessException(ExpertErrorCode.EXPERT_PROFILE_NOT_FOUND));
        if (!expertProfile.isApproved()) {
            throw new BusinessException(ExpertErrorCode.FEEDBACK_EXPERT_NOT_APPROVED);
        }

        Feedback feedback = Feedback.builder()
                .requester(requester)
                .expertProfile(expertProfile)
                .topic(request.getTopic())
                .build();

        feedbackRepository.save(feedback);

        feedbackMessageRepository.save(
                FeedbackMessage.builder()
                        .feedback(feedback)
                        .sender(requester)
                        .content(request.getContent())
                        .build()
        );

        return FeedbackResponse.from(feedback);
    }

    public FeedbackResponse getFeedback(Long callerId, Long feedbackId) {
        Feedback feedback = getFeedbackOrThrow(feedbackId);
        validateFeedbackAccess(feedback, callerId);
        return FeedbackResponse.from(feedback);
    }

    @Transactional
    public FeedbackMessageResponse addMessage(Long senderId, Long feedbackId, FeedbackMessageRequest request) {
        Feedback feedback = getFeedbackOrThrow(feedbackId);
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new BusinessException(ExpertErrorCode.USER_NOT_FOUND));

        boolean isRequester = feedback.getRequester().getId().equals(senderId);
        boolean isExpertAnswering = feedback.getExpertProfile().getUser().getId().equals(senderId);
        if (!isRequester && !isExpertAnswering) {
            throw new BusinessException(ExpertErrorCode.FEEDBACK_ACCESS_DENIED);
        }

        FeedbackMessage message = feedbackMessageRepository.save(
                FeedbackMessage.builder()
                        .feedback(feedback)
                        .sender(sender)
                        .content(request.getContent())
                        .build()
        );

        // 전문가가 지금 답변하고 있다면 → 답변완료 처리한다.
        if (isExpertAnswering) {
            feedback.markAnswered();
        }

        return FeedbackMessageResponse.from(message);
    }

    public List<FeedbackMessageResponse> getMessages(Long callerId, Long feedbackId) {
        Feedback feedback = getFeedbackOrThrow(feedbackId);
        validateFeedbackAccess(feedback, callerId);
        return feedbackMessageRepository.findByFeedbackIdOrderByCreatedAtAsc(feedbackId)
                .stream().map(message -> FeedbackMessageResponse.from(message)).toList();
    }

    public MyFeedbackListResponse getMyFeedbacks(Long requesterId) {
        List<MyFeedbackSummaryResponse> summaries = feedbackRepository.findByRequesterId(requesterId)
                .stream().map(feedback -> MyFeedbackSummaryResponse.from(feedback)).toList();
        return MyFeedbackListResponse.from(summaries);
    }
// 특정 전문가 1명에게 들어온 문의 스레드 리턴
//    전문가 프로필 id를 지정해서, 그 전문가에게 들어온 문의 스레드를 전부 조회하는 기능
    public List<FeedbackResponse> getExpertFeedbacks(Long expertProfileId) {
        return feedbackRepository.findByExpertProfileId(expertProfileId)
                .stream().map(feedback -> FeedbackResponse.from(feedback)).toList();
    }
// 전문가 본인이 받은 문의 목록
//    로그인한 전문가 본인이 "내가 받은 문의함" 화면을 열었을 때 쓰는 기능
    public List<FeedbackResponse> getMyExpertFeedbacks(Long expertUserId) {
        ExpertProfile expertProfile = expertProfileRepository.findByUserId(expertUserId)
                .orElseThrow(() -> new BusinessException(ExpertErrorCode.EXPERT_PROFILE_NOT_FOUND));
        return getExpertFeedbacks(expertProfile.getId());
    }

    private Feedback getFeedbackOrThrow(Long id) {
        return feedbackRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ExpertErrorCode.FEEDBACK_NOT_FOUND));
    }

    private void validateFeedbackAccess(Feedback feedback, Long callerId) {
        boolean isRequester = feedback.getRequester().getId().equals(callerId);
        boolean isExpert = feedback.getExpertProfile().getUser().getId().equals(callerId);
        if (!isRequester && !isExpert) {
            throw new BusinessException(ExpertErrorCode.FEEDBACK_ACCESS_DENIED);
        }
    }
}