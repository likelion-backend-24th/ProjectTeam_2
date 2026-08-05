package org.example.backend.expert.service;

import lombok.RequiredArgsConstructor;
import org.example.backend.expert.dto.FeedbackCreateRequest;
import org.example.backend.expert.dto.FeedbackMessageRequest;
import org.example.backend.expert.dto.FeedbackMessageResponse;
import org.example.backend.expert.dto.FeedbackResponse;
import org.example.backend.expert.dto.MyFeedbackListResponse;
import org.example.backend.expert.dto.MyFeedbackSummaryResponse;
import org.example.backend.expert.entity.ExpertProfile;
import org.example.backend.expert.entity.Feedback;
import org.example.backend.expert.entity.FeedbackMessage;
import org.example.backend.expert.exception.ExpertProfileNotFoundException;
import org.example.backend.expert.exception.FeedbackAccessDeniedException;
import org.example.backend.expert.exception.FeedbackNotFoundException;
import org.example.backend.expert.exception.SubscriptionRequiredException;
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

    // F-30: 구독자가 전문가를 지정해 질문 등록 (스레드 생성 + 첫 메시지). POST /api/feedbacks
    // 구독 여부는 users.is_subscribed 캐시 플래그로 확인 (subscription 테이블이 source of truth,
    // is_subscribed는 F-28 신청/취소 시 함께 갱신되는 조회 성능용 캐시)
    @Transactional
    public FeedbackResponse createFeedback(Long requesterId, FeedbackCreateRequest request) {
        User requester = userRepository.findById(requesterId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다."));
        if (!requester.isSubscribed()) {
            throw new SubscriptionRequiredException("구독자만 이용할 수 있습니다.");
        }
        ExpertProfile expertProfile = expertProfileRepository.findById(request.getExpertProfileId())
                .orElseThrow(() -> new ExpertProfileNotFoundException("존재하지 않는 전문가입니다."));
        if (!expertProfile.isApproved()) {
            throw new FeedbackAccessDeniedException("승인된 전문가가 아닙니다.");
        }

        Feedback feedback = Feedback.builder()
                .requester(requester)
                .expertProfile(expertProfile)
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

    public FeedbackResponse getFeedback(Long feedbackId) {
        Feedback feedback = getFeedbackOrThrow(feedbackId);
        return FeedbackResponse.from(feedback);
    }

    @Transactional
    public FeedbackMessageResponse addMessage(Long senderId, Long feedbackId, FeedbackMessageRequest request) {
        Feedback feedback = getFeedbackOrThrow(feedbackId);
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다."));

        boolean isRequester = feedback.getRequester().getId().equals(senderId);
        boolean isExpertAnswering = feedback.getExpertProfile().getUser().getId().equals(senderId);
        if (!isRequester && !isExpertAnswering) {
            throw new FeedbackAccessDeniedException("이 문의의 요청자 또는 담당 전문가만 메시지를 남길 수 있습니다.");
        }

        FeedbackMessage message = feedbackMessageRepository.save(
                FeedbackMessage.builder()
                        .feedback(feedback)
                        .sender(sender)
                        .content(request.getContent())
                        .build()
        );

        if (isExpertAnswering) {
            feedback.markAnswered();
        }

        return FeedbackMessageResponse.from(message);
    }

    public List<FeedbackMessageResponse> getMessages(Long feedbackId) {
        return feedbackMessageRepository.findByFeedbackIdOrderByCreatedAtAsc(feedbackId)
                .stream().map(FeedbackMessageResponse::from).toList();
    }

    public MyFeedbackListResponse getMyFeedbacks(Long requesterId) {
        List<MyFeedbackSummaryResponse> summaries = feedbackRepository.findByRequesterId(requesterId)
                .stream().map(MyFeedbackSummaryResponse::from).toList();
        return MyFeedbackListResponse.from(summaries);
    }

    public List<FeedbackResponse> getExpertFeedbacks(Long expertProfileId) {
        return feedbackRepository.findByExpertProfileId(expertProfileId)
                .stream().map(FeedbackResponse::from).toList();
    }

    public List<FeedbackResponse> getMyExpertFeedbacks(Long expertUserId) {
        ExpertProfile expertProfile = expertProfileRepository.findByUserId(expertUserId)
                .orElseThrow(() -> new ExpertProfileNotFoundException("전문가 프로필이 없습니다."));
        return getExpertFeedbacks(expertProfile.getId());
    }

    private Feedback getFeedbackOrThrow(Long id) {
        return feedbackRepository.findById(id)
                .orElseThrow(() -> new FeedbackNotFoundException("존재하지 않는 문의입니다."));
    }
}
//