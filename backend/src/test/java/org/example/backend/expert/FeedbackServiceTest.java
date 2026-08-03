package org.example.backend.expert.service;

import org.example.backend.expert.dto.FeedbackCreateRequest;
import org.example.backend.expert.dto.FeedbackMessageRequest;
import org.example.backend.expert.dto.FeedbackResponse;
import org.example.backend.expert.entity.ExpertProfile;
import org.example.backend.expert.entity.Feedback;
import org.example.backend.expert.entity.FeedbackStatus;
import org.example.backend.expert.exception.FeedbackAccessDeniedException;
import org.example.backend.expert.exception.SubscriptionRequiredException;
import org.example.backend.expert.repository.ExpertProfileRepository;
import org.example.backend.expert.repository.FeedbackMessageRepository;
import org.example.backend.expert.repository.FeedbackRepository;
import org.example.backend.user.entity.Role;
import org.example.backend.user.entity.User;
import org.example.backend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeedbackServiceTest {

    @Mock
    private FeedbackRepository feedbackRepository;
    @Mock
    private FeedbackMessageRepository feedbackMessageRepository;
    @Mock
    private ExpertProfileRepository expertProfileRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private FeedbackService feedbackService;

    private User requester;
    private User expertUser;
    private ExpertProfile approvedExpertProfile;

    @BeforeEach
    void setUp() {
        requester = new User();
        requester.setId(1L);
        requester.setRole(Role.USER);
        requester.setSubscribed(true); // 기본값: 구독 중. 비구독 테스트에서만 false로 덮어씀

        expertUser = new User();
        expertUser.setId(2L);
        expertUser.setRole(Role.EXPERT);

        approvedExpertProfile = ExpertProfile.builder()
                .user(expertUser).career("5년").certification("정보처리기사").build();
        approvedExpertProfile.approve();
    }

    @Test
    void createFeedback_미승인전문가면_예외() {
        ExpertProfile pending = ExpertProfile.builder()
                .user(expertUser).career("1년").certification(null).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(requester));
        when(expertProfileRepository.findById(99L)).thenReturn(Optional.of(pending));

        FeedbackCreateRequest request = new FeedbackCreateRequest();
        request.setExpertProfileId(99L);

        assertThrows(FeedbackAccessDeniedException.class,
                () -> feedbackService.createFeedback(1L, request));
    }

    @Test
    void createFeedback_비구독자면_예외() {
        requester.setSubscribed(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(requester));

        FeedbackCreateRequest request = new FeedbackCreateRequest();
        request.setExpertProfileId(99L);

        assertThrows(SubscriptionRequiredException.class,
                () -> feedbackService.createFeedback(1L, request));

        verify(expertProfileRepository, never()).findById(any());
    }

    @Test
    void createFeedback_정상이면_스레드와첫메시지_저장() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(requester));
        when(expertProfileRepository.findById(99L)).thenReturn(Optional.of(approvedExpertProfile));
        when(feedbackRepository.save(any(Feedback.class))).thenAnswer(inv -> inv.getArgument(0));

        FeedbackCreateRequest request = new FeedbackCreateRequest();
        request.setExpertProfileId(99L);
        request.setContent("이력서 첨삭 부탁드려요");

        FeedbackResponse response = feedbackService.createFeedback(1L, request);

        assertThat(response.getStatus()).isEqualTo(FeedbackStatus.PENDING);
    }

    @Test
    void getFeedback_존재하면_상세반환() {
        Feedback feedback = Feedback.builder()
                .requester(requester).expertProfile(approvedExpertProfile).build();
        when(feedbackRepository.findById(1L)).thenReturn(Optional.of(feedback));

        FeedbackResponse response = feedbackService.getFeedback(1L);

        assertThat(response.getStatus()).isEqualTo(FeedbackStatus.PENDING);
        assertThat(response.getRequesterId()).isEqualTo(1L);
    }

    @Test
    void addMessage_전문가가_답변하면_ANSWERED로_전환() {
        Feedback feedback = Feedback.builder()
                .requester(requester).expertProfile(approvedExpertProfile).build();
        when(feedbackRepository.findById(1L)).thenReturn(Optional.of(feedback));
        when(userRepository.findById(2L)).thenReturn(Optional.of(expertUser));
        when(feedbackMessageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        FeedbackMessageRequest request = new FeedbackMessageRequest();
        request.setContent("이렇게 수정해보세요");

        feedbackService.addMessage(2L, 1L, request);

        assertThat(feedback.getStatus()).isEqualTo(FeedbackStatus.ANSWERED);
    }

    @Test
    void addMessage_요청자가_추가질문하면_상태유지() {
        Feedback feedback = Feedback.builder()
                .requester(requester).expertProfile(approvedExpertProfile).build();
        when(feedbackRepository.findById(1L)).thenReturn(Optional.of(feedback));
        when(userRepository.findById(1L)).thenReturn(Optional.of(requester));
        when(feedbackMessageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        FeedbackMessageRequest request = new FeedbackMessageRequest();
        request.setContent("추가로 궁금한 게 있어요");

        feedbackService.addMessage(1L, 1L, request);

        assertThat(feedback.getStatus()).isEqualTo(FeedbackStatus.PENDING);
    }

    @Test
    void addMessage_요청자도담당전문가도아니면_예외() {
        Feedback feedback = Feedback.builder()
                .requester(requester).expertProfile(approvedExpertProfile).build();
        User stranger = new User();
        stranger.setId(999L);
        stranger.setRole(Role.USER);

        when(feedbackRepository.findById(1L)).thenReturn(Optional.of(feedback));
        when(userRepository.findById(999L)).thenReturn(Optional.of(stranger));

        assertThrows(FeedbackAccessDeniedException.class,
                () -> feedbackService.addMessage(999L, 1L, new FeedbackMessageRequest()));

        verify(feedbackMessageRepository, never()).save(any());
    }

    @Test
    void getMyFeedbacks_요약목록_expertNickname_포함() {
        expertUser.setNickname("김전문");
        Feedback feedback = Feedback.builder()
                .requester(requester).expertProfile(approvedExpertProfile).build();
        when(feedbackRepository.findByRequesterId(1L)).thenReturn(java.util.List.of(feedback));

        var response = feedbackService.getMyFeedbacks(1L);

        assertThat(response.getFeedbacks()).hasSize(1);
        assertThat(response.getFeedbacks().get(0).getExpertNickname()).isEqualTo("김전문");
        assertThat(response.getFeedbacks().get(0).getStatus()).isEqualTo(FeedbackStatus.PENDING);
    }
}