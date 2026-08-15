package org.example.backend.expert.service;

import org.example.backend.common.exception.BusinessException;
import org.example.backend.expert.dto.request.FeedbackCreateRequest;
import org.example.backend.expert.dto.request.FeedbackMessageRequest;
import org.example.backend.expert.dto.response.ExpertFeedbackListResponse;
import org.example.backend.expert.dto.response.FeedbackMessageResponse;
import org.example.backend.expert.dto.response.FeedbackResponse;
import org.example.backend.expert.dto.response.MyFeedbackListResponse;
import org.example.backend.expert.entity.ExpertProfile;
import org.example.backend.expert.entity.Feedback;
import org.example.backend.expert.entity.FeedbackMessage;
import org.example.backend.expert.entity.FeedbackStatus;
import org.example.backend.expert.exception.ExpertErrorCode;
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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.example.backend.expert.entity.FeedbackCloseReason;
import org.example.backend.user.entity.AccountStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.PageImpl;

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
        requester.setSubscribed(true);
        requester.setStatus(AccountStatus.ACTIVE);

        expertUser = new User();
        expertUser.setId(2L);
        expertUser.setRole(Role.EXPERT);
        expertUser.setStatus(AccountStatus.ACTIVE);

        approvedExpertProfile = ExpertProfile.builder()
                .user(expertUser).introduction("5년차 백엔드 개발자").build();
        approvedExpertProfile.approve();
    }

    @Test
    void createFeedback_미승인전문가면_예외() {
        ExpertProfile pending = ExpertProfile.builder()
                .user(expertUser).introduction("신입 지원자").build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(requester));
        when(expertProfileRepository.findById(99L)).thenReturn(Optional.of(pending));

        FeedbackCreateRequest request = new FeedbackCreateRequest();
        request.setExpertProfileId(99L);

        BusinessException e = assertThrows(BusinessException.class,
                () -> feedbackService.createFeedback(1L, request));
        assertThat(e.getErrorCode()).isEqualTo(ExpertErrorCode.FEEDBACK_EXPERT_NOT_APPROVED);
    }

    @Test
    void createFeedback_비구독자면_예외() {
        requester.setSubscribed(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(requester));

        FeedbackCreateRequest request = new FeedbackCreateRequest();
        request.setExpertProfileId(99L);

        BusinessException e = assertThrows(BusinessException.class,
                () -> feedbackService.createFeedback(1L, request));
        assertThat(e.getErrorCode()).isEqualTo(ExpertErrorCode.SUBSCRIPTION_REQUIRED);

        verify(expertProfileRepository, never()).findById(any());
    }

    @Test
    void createFeedback_정상이면_스레드와첫메시지_저장() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(requester));
        when(expertProfileRepository.findById(99L)).thenReturn(Optional.of(approvedExpertProfile));
        when(feedbackRepository.save(any(Feedback.class))).thenAnswer(inv -> inv.getArgument(0));

        FeedbackCreateRequest request = new FeedbackCreateRequest();
        request.setExpertProfileId(99L);
        request.setTopic("포트폴리오 피드백 요청");
        request.setContent("이력서 첨삭 부탁드려요");

        FeedbackResponse response = feedbackService.createFeedback(1L, request);

        assertThat(response.getStatus()).isEqualTo(FeedbackStatus.PENDING);
        assertThat(response.getTopic()).isEqualTo("포트폴리오 피드백 요청");
    }

    @Test
    void getFeedback_존재하면_상세반환() {
        Feedback feedback = Feedback.builder()
                .requester(requester).expertProfile(approvedExpertProfile).topic("포트폴리오 피드백 요청").build();
        when(feedbackRepository.findById(1L)).thenReturn(Optional.of(feedback));

        FeedbackResponse response = feedbackService.getFeedback(requester.getId(), 1L);

        assertThat(response.getStatus()).isEqualTo(FeedbackStatus.PENDING);
        assertThat(response.getRequesterId()).isEqualTo(1L);
        assertThat(response.getTopic()).isEqualTo("포트폴리오 피드백 요청");
    }

    @Test
    void getFeedback_당사자가아니면_예외() {
        Feedback feedback = Feedback.builder()
                .requester(requester).expertProfile(approvedExpertProfile).build();
        when(feedbackRepository.findById(1L)).thenReturn(Optional.of(feedback));

        BusinessException e = assertThrows(BusinessException.class,
                () -> feedbackService.getFeedback(999L, 1L));
        assertThat(e.getErrorCode()).isEqualTo(ExpertErrorCode.FEEDBACK_ACCESS_DENIED);
    }

    @Test
    void getMessages_당사자면_목록반환() {
        Feedback feedback = Feedback.builder()
                .requester(requester).expertProfile(approvedExpertProfile).build();
        when(feedbackRepository.findById(1L)).thenReturn(Optional.of(feedback));
        when(feedbackMessageRepository.findByFeedbackIdOrderByCreatedAtAsc(1L))
                .thenReturn(List.of(
                        FeedbackMessage.builder().feedback(feedback).sender(requester).content("첫 메시지").build()
                ));

        List<FeedbackMessageResponse> response = feedbackService.getMessages(requester.getId(), 1L);

        assertThat(response).hasSize(1);
    }

    @Test
    void getMessages_당사자가아니면_예외() {
        Feedback feedback = Feedback.builder()
                .requester(requester).expertProfile(approvedExpertProfile).build();
        when(feedbackRepository.findById(1L)).thenReturn(Optional.of(feedback));

        BusinessException e = assertThrows(BusinessException.class,
                () -> feedbackService.getMessages(999L, 1L));
        assertThat(e.getErrorCode()).isEqualTo(ExpertErrorCode.FEEDBACK_ACCESS_DENIED);

        verify(feedbackMessageRepository, never()).findByFeedbackIdOrderByCreatedAtAsc(any());
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

        BusinessException e = assertThrows(BusinessException.class,
                () -> feedbackService.addMessage(999L, 1L, new FeedbackMessageRequest()));
        assertThat(e.getErrorCode()).isEqualTo(ExpertErrorCode.FEEDBACK_ACCESS_DENIED);

        verify(feedbackMessageRepository, never()).save(any());
    }

    @Test
    void getMyFeedbacks_요약목록_expertNickname_포함() {
        expertUser.setNickname("김전문");
        Feedback feedback = Feedback.builder()
                .requester(requester).expertProfile(approvedExpertProfile).topic("포트폴리오 피드백 요청").build();
        Pageable pageable = PageRequest.of(0, 20);
        when(feedbackRepository.findByRequesterId(1L, pageable))
                .thenReturn(new PageImpl<>(List.of(feedback), pageable, 1));

        MyFeedbackListResponse response = feedbackService.getMyFeedbacks(1L, pageable);

        assertThat(response.getFeedbacks()).hasSize(1);
        assertThat(response.getFeedbacks().get(0).getExpertNickname()).isEqualTo("김전문");
        assertThat(response.getFeedbacks().get(0).getTopic()).isEqualTo("포트폴리오 피드백 요청");
        assertThat(response.getFeedbacks().get(0).getStatus()).isEqualTo(FeedbackStatus.PENDING);
        assertThat(response.getTotalElements()).isEqualTo(1);
    }

    @Test
    void createFeedback_같은전문가와_열린스레드가있으면_예외() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(requester));
        when(expertProfileRepository.findById(99L)).thenReturn(Optional.of(approvedExpertProfile));
        when(feedbackRepository.existsByRequesterIdAndExpertProfileIdAndClosedAtIsNull(1L, 99L)).thenReturn(true);

        FeedbackCreateRequest request = new FeedbackCreateRequest();
        request.setExpertProfileId(99L);

        BusinessException e = assertThrows(BusinessException.class,
                () -> feedbackService.createFeedback(1L, request));
        assertThat(e.getErrorCode()).isEqualTo(ExpertErrorCode.FEEDBACK_ALREADY_OPEN);

        verify(feedbackRepository, never()).save(any());
    }

    @Test
    void createFeedback_열린스레드가5개면_예외() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(requester));
        when(expertProfileRepository.findById(99L)).thenReturn(Optional.of(approvedExpertProfile));
        when(feedbackRepository.countByRequesterIdAndClosedAtIsNull(1L)).thenReturn(5L);

        FeedbackCreateRequest request = new FeedbackCreateRequest();
        request.setExpertProfileId(99L);

        BusinessException e = assertThrows(BusinessException.class,
                () -> feedbackService.createFeedback(1L, request));
        assertThat(e.getErrorCode()).isEqualTo(ExpertErrorCode.FEEDBACK_OPEN_LIMIT_EXCEEDED);

        verify(feedbackRepository, never()).save(any());
    }

    @Test
    void closeThread_요청자본인이면_성공() {
        Feedback feedback = Feedback.builder()
                .requester(requester).expertProfile(approvedExpertProfile).topic("포트폴리오 피드백 요청").build();
        when(feedbackRepository.findById(1L)).thenReturn(Optional.of(feedback));

        FeedbackResponse response = feedbackService.closeThread(requester.getId(), 1L);

        assertThat(feedback.isClosed()).isTrue();
        assertThat(feedback.getClosedBy()).isEqualTo(FeedbackCloseReason.REQUESTER_CLOSED);
        assertThat(response.getClosedBy()).isEqualTo(FeedbackCloseReason.REQUESTER_CLOSED);
    }

    @Test
    void closeThread_요청자가아니면_예외() {
        Feedback feedback = Feedback.builder()
                .requester(requester).expertProfile(approvedExpertProfile).build();
        when(feedbackRepository.findById(1L)).thenReturn(Optional.of(feedback));

        BusinessException e = assertThrows(BusinessException.class,
                () -> feedbackService.closeThread(expertUser.getId(), 1L));
        assertThat(e.getErrorCode()).isEqualTo(ExpertErrorCode.FEEDBACK_ACCESS_DENIED);
        assertThat(feedback.isClosed()).isFalse();
    }

    @Test
    void closeThread_이미닫힌스레드면_예외() {
        Feedback feedback = Feedback.builder()
                .requester(requester).expertProfile(approvedExpertProfile).build();
        feedback.close(FeedbackCloseReason.REQUESTER_CLOSED);
        when(feedbackRepository.findById(1L)).thenReturn(Optional.of(feedback));

        BusinessException e = assertThrows(BusinessException.class,
                () -> feedbackService.closeThread(requester.getId(), 1L));
        assertThat(e.getErrorCode()).isEqualTo(ExpertErrorCode.FEEDBACK_CLOSED);
    }

    @Test
    void closeThreadsByExpertProfile_전문가박탈시_열린스레드모두닫힘() {
        Feedback openThread = Feedback.builder()
                .requester(requester).expertProfile(approvedExpertProfile).build();
        when(feedbackRepository.findByExpertProfileIdAndClosedAtIsNull(any()))
                .thenReturn(List.of(openThread));

        feedbackService.closeThreadsByExpertProfile(approvedExpertProfile);

        assertThat(openThread.isClosed()).isTrue();
        assertThat(openThread.getClosedBy()).isEqualTo(FeedbackCloseReason.EXPERT_REVOKED);
    }

    @Test
    void addMessage_요청자구독만료시_예외() {
        requester.setSubscribed(false);
        Feedback feedback = Feedback.builder()
                .requester(requester).expertProfile(approvedExpertProfile).build();
        when(feedbackRepository.findById(1L)).thenReturn(Optional.of(feedback));
        when(userRepository.findById(1L)).thenReturn(Optional.of(requester));

        FeedbackMessageRequest request = new FeedbackMessageRequest();
        request.setContent("구독 만료 후 메시지 시도");

        BusinessException e = assertThrows(BusinessException.class,
                () -> feedbackService.addMessage(1L, 1L, request));
        assertThat(e.getErrorCode()).isEqualTo(ExpertErrorCode.FEEDBACK_SUBSCRIPTION_EXPIRED);

        verify(feedbackMessageRepository, never()).save(any());
    }

    @Test
    void createFeedback_요청자탈퇴시_예외() {
        requester.setStatus(AccountStatus.WITHDRAWN);
        when(userRepository.findById(1L)).thenReturn(Optional.of(requester));

        FeedbackCreateRequest request = new FeedbackCreateRequest();
        request.setExpertProfileId(99L);

        BusinessException e = assertThrows(BusinessException.class,
                () -> feedbackService.createFeedback(1L, request));
        assertThat(e.getErrorCode()).isEqualTo(ExpertErrorCode.FEEDBACK_USER_INACTIVE);

        verify(expertProfileRepository, never()).findById(any());
    }

    @Test
    void createFeedback_전문가탈퇴시_예외() {
        expertUser.setStatus(AccountStatus.WITHDRAWN);
        when(userRepository.findById(1L)).thenReturn(Optional.of(requester));
        when(expertProfileRepository.findById(99L)).thenReturn(Optional.of(approvedExpertProfile));

        FeedbackCreateRequest request = new FeedbackCreateRequest();
        request.setExpertProfileId(99L);

        BusinessException e = assertThrows(BusinessException.class,
                () -> feedbackService.createFeedback(1L, request));
        assertThat(e.getErrorCode()).isEqualTo(ExpertErrorCode.FEEDBACK_USER_INACTIVE);

        verify(feedbackRepository, never()).save(any());
    }

    @Test
    void addMessage_상대방탈퇴시_예외() {
        expertUser.setStatus(AccountStatus.WITHDRAWN);
        Feedback feedback = Feedback.builder()
                .requester(requester).expertProfile(approvedExpertProfile).build();
        when(feedbackRepository.findById(1L)).thenReturn(Optional.of(feedback));
        when(userRepository.findById(1L)).thenReturn(Optional.of(requester));

        FeedbackMessageRequest request = new FeedbackMessageRequest();
        request.setContent("상대방 탈퇴 후 메시지 시도");

        BusinessException e = assertThrows(BusinessException.class,
                () -> feedbackService.addMessage(1L, 1L, request));
        assertThat(e.getErrorCode()).isEqualTo(ExpertErrorCode.FEEDBACK_USER_INACTIVE);

        verify(feedbackMessageRepository, never()).save(any());
    }

    @Test
    void getExpertFeedbacks_페이지네이션_정상반환() {
        Feedback feedback = Feedback.builder()
                .requester(requester).expertProfile(approvedExpertProfile).topic("포트폴리오 피드백 요청").build();
        Pageable pageable = PageRequest.of(0, 20);
        when(feedbackRepository.findByExpertProfileId(99L, pageable))
                .thenReturn(new PageImpl<>(List.of(feedback), pageable, 1));

        ExpertFeedbackListResponse response = feedbackService.getExpertFeedbacks(99L, pageable);

        assertThat(response.getFeedbacks()).hasSize(1);
        assertThat(response.getFeedbacks().get(0).getTopic()).isEqualTo("포트폴리오 피드백 요청");
        assertThat(response.getTotalElements()).isEqualTo(1);
        assertThat(response.getPage()).isEqualTo(0);
    }

    @Test
    void getMyExpertFeedbacks_전문가프로필로_전환후_조회() {
        Feedback feedback = Feedback.builder()
                .requester(requester).expertProfile(approvedExpertProfile).topic("포트폴리오 피드백 요청").build();
        Pageable pageable = PageRequest.of(0, 20);
        when(expertProfileRepository.findByUserId(2L)).thenReturn(Optional.of(approvedExpertProfile));
        when(feedbackRepository.findByExpertProfileId(any(), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(feedback), pageable, 1));

        ExpertFeedbackListResponse response = feedbackService.getMyExpertFeedbacks(2L, pageable);

        assertThat(response.getFeedbacks()).hasSize(1);
        assertThat(response.getFeedbacks().get(0).getTopic()).isEqualTo("포트폴리오 피드백 요청");
    }

    @Test
    void addMessage_답변후_요청자가_추가질문하면_PENDING으로_전환() {
        Feedback feedback = Feedback.builder()
                .requester(requester).expertProfile(approvedExpertProfile).build();
        feedback.markAnswered();

        when(feedbackRepository.findById(1L)).thenReturn(Optional.of(feedback));
        when(userRepository.findById(1L)).thenReturn(Optional.of(requester));
        when(feedbackMessageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        FeedbackMessageRequest request = new FeedbackMessageRequest();
        request.setContent("추가로 궁금한 게 생겼어요");

        feedbackService.addMessage(1L, 1L, request);

        assertThat(feedback.getStatus()).isEqualTo(FeedbackStatus.PENDING);
    }
}
