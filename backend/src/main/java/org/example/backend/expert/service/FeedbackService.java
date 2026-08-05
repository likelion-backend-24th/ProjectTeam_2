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

/**
 * 구독자-전문가 1:1 문의 스레드(F-30)를 처리하는 서비스.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final FeedbackMessageRepository feedbackMessageRepository;
    private final ExpertProfileRepository expertProfileRepository;
    private final UserRepository userRepository;

    /**
     * F-30: 구독자가 전문가를 지정해 문의 스레드를 개설한다(스레드 생성 + 첫 메시지 저장).
     * 순서: (1) requester가 존재하는 유저인지 확인 (2) User.isSubscribed()로 구독 여부 확인,
     * 아니면 SubscriptionRequiredException(403) (3) 지정한 expertProfileId가 존재하는지 확인,
     * 없으면 ExpertProfileNotFoundException(404) (4) 해당 프로필이 APPROVED 상태인지 확인,
     * 아니면 FeedbackAccessDeniedException(403) (5) Feedback을 PENDING 상태로 생성 (6) 요청
     * content를 첫 FeedbackMessage로 저장.
     * 구독 여부는 users.is_subscribed 캐시 플래그로 확인한다(subscription 테이블이 실제
     * source of truth이고, is_subscribed는 F-28 구독 신청/취소 시 함께 갱신되는 조회용 캐시다).
     */
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

    // 스레드 단건 조회. 존재 여부만 확인할 뿐, 호출자가 이 스레드의 요청자인지 담당
    // 전문가인지를 검증하는 로직은 이 메서드에는 없다(컨트롤러에도 Authentication 파라미터가 없음).
    public FeedbackResponse getFeedback(Long feedbackId) {
        Feedback feedback = getFeedbackOrThrow(feedbackId);
        return FeedbackResponse.from(feedback);
    }

    /**
     * F-30: 스레드에 메시지를 추가한다.
     * senderId가 스레드의 requester인지(isRequester), 아니면 담당 전문가의 user인지
     * (isExpertAnswering)를 확인해서, 둘 다 아니면 FeedbackAccessDeniedException(403)을 던진다
     * - 이 메서드에서만 요청자/담당 전문가 여부를 실제로 검증한다.
     * 담당 전문가가 남긴 메시지라면 Feedback.markAnswered()를 호출해 스레드 상태를
     * PENDING -> ANSWERED로 전환한다(요청자가 메시지를 남기는 경우엔 상태가 바뀌지 않음).
     */
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

    // 스레드의 메시지를 작성 시각 오름차순으로 조회한다. getFeedback()과 마찬가지로
    // 호출자가 이 스레드의 당사자인지 검증하는 로직은 없다.
    public List<FeedbackMessageResponse> getMessages(Long feedbackId) {
        return feedbackMessageRepository.findByFeedbackIdOrderByCreatedAtAsc(feedbackId)
                .stream().map(FeedbackMessageResponse::from).toList();
    }

    // F-30: 구독자 본인이 개설한 스레드 목록(GET /api/feedbacks/me). requesterId로 필터링해서
    // MyFeedbackSummaryResponse(전문가 닉네임까지 포함한 요약 형태)로 변환한다.
    public MyFeedbackListResponse getMyFeedbacks(Long requesterId) {
        List<MyFeedbackSummaryResponse> summaries = feedbackRepository.findByRequesterId(requesterId)
                .stream().map(MyFeedbackSummaryResponse::from).toList();
        return MyFeedbackListResponse.from(summaries);
    }

    // 특정 전문가 프로필(expertProfileId)이 담당하는 스레드 전체를 조회한다.
    // getMyExpertFeedbacks()에서 로그인한 전문가의 프로필 id를 찾아 이 메서드를 호출하는 형태로 쓰인다.
    public List<FeedbackResponse> getExpertFeedbacks(Long expertProfileId) {
        return feedbackRepository.findByExpertProfileId(expertProfileId)
                .stream().map(FeedbackResponse::from).toList();
    }

    // 로그인한 유저(전문가)가 받은 문의 목록을 조회한다. userId로 본인의 ExpertProfile을
    // 먼저 찾고(없으면 전문가가 아니므로 ExpertProfileNotFoundException), 찾은 프로필 id로
    // getExpertFeedbacks()를 호출한다. GET /api/feedbacks/expert에서 사용되며, 이 엔드포인트는
    // API 명세 F-30 목록에는 없는 상태다.
    public List<FeedbackResponse> getMyExpertFeedbacks(Long expertUserId) {
        ExpertProfile expertProfile = expertProfileRepository.findByUserId(expertUserId)
                .orElseThrow(() -> new ExpertProfileNotFoundException("전문가 프로필이 없습니다."));
        return getExpertFeedbacks(expertProfile.getId());
    }

    // id로 Feedback을 조회하고, 없으면 FeedbackNotFoundException(404)을 던지는 공용 헬퍼.
    private Feedback getFeedbackOrThrow(Long id) {
        return feedbackRepository.findById(id)
                .orElseThrow(() -> new FeedbackNotFoundException("존재하지 않는 문의입니다."));
    }
}