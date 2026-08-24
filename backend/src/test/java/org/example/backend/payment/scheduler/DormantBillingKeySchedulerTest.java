package org.example.backend.payment.scheduler;

import org.example.backend.payment.entity.BillingKey;
import org.example.backend.payment.repository.BillingKeyRepository;
import org.example.backend.payment.service.PaymentService;
import org.example.backend.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DormantBillingKeySchedulerTest {

    @Mock
    private BillingKeyRepository billingKeyRepository;
    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private DormantBillingKeyScheduler scheduler;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
    }

    private BillingKey dormantCard(User owner) {
        return BillingKey.builder()
                .user(owner)
                .billingKeyToken("bk-token")
                .issuedAt(LocalDateTime.now().minusDays(200))
                .build();
    }

    @Test
    void 미사용카드는_사용자가_직접삭제할때와_같은_경로로_정리된다() {
        when(billingKeyRepository.findDormant(any()))
                .thenReturn(List.of(dormantCard(user)));

        scheduler.deleteDormantBillingKeys();

        verify(paymentService).deleteBillingKey(1L);
    }

    @Test
    void 한건이_실패해도_나머지는_계속_정리한다() {
        User other = new User();
        other.setId(2L);
        when(billingKeyRepository.findDormant(any()))
                .thenReturn(List.of(dormantCard(user), dormantCard(other)));
        doThrow(new RuntimeException("PG 오류")).when(paymentService).deleteBillingKey(1L);

        scheduler.deleteDormantBillingKeys();

        verify(paymentService).deleteBillingKey(2L);
    }

    @Test
    void 정리대상이_없으면_아무것도_호출하지_않는다() {
        when(billingKeyRepository.findDormant(any())).thenReturn(List.of());

        scheduler.deleteDormantBillingKeys();

        verifyNoInteractions(paymentService);
    }
}
