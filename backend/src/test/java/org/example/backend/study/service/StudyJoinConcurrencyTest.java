package org.example.backend.study.service;

import org.example.backend.common.exception.BusinessException;
import org.example.backend.study.entity.Study;
import org.example.backend.study.entity.StudyCategory;
import org.example.backend.study.entity.StudyMember;
import org.example.backend.study.exception.StudyErrorCode;
import org.example.backend.study.repository.StudyMemberRepository;
import org.example.backend.study.repository.StudyRepository;
import org.example.backend.user.entity.AccountStatus;
import org.example.backend.user.entity.Role;
import org.example.backend.user.entity.User;
import org.example.backend.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 스터디 가입 정원 체크의 동시성 검증.
 * 각 스레드가 joinStudy() 안에서 자기 트랜잭션을 열어야 하므로 테스트에는 @Transactional을 붙이지 않는다.
 * (붙이면 미리 넣은 데이터가 커밋되지 않아 다른 스레드에서 보이지 않음) → 데이터는 @AfterEach에서 직접 정리.
 */
@SpringBootTest
@ActiveProfiles("test")
class StudyJoinConcurrencyTest {

    private static final int THREAD_COUNT = 10;

    @Autowired
    private StudyMemberService studyMemberService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private StudyRepository studyRepository;
    @Autowired
    private StudyMemberRepository studyMemberRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void tearDown() {
        // Study는 @SoftDelete라 JPA delete로는 행이 남아 users FK에 걸리므로 SQL로 직접 지운다.
        jdbcTemplate.execute("DELETE FROM study_member");
        jdbcTemplate.execute("DELETE FROM study");
        jdbcTemplate.execute("DELETE FROM users");
    }

    @Test
    void 정원이_1자리_남은_스터디에_10명이_동시에_가입하면_1명만_성공한다() throws InterruptedException {
        // given: 정원 2명, 방장이 이미 멤버라 1자리만 남은 스터디
        User leader = saveUser("leader");
        Study study = studyRepository.save(
                new Study("동시성 스터디", "설명", 2, LocalDate.now(), null, leader, StudyCategory.IT_DEVELOPMENT));
        studyMemberRepository.save(new StudyMember(study, leader));

        List<User> applicants = new ArrayList<>();
        for (int i = 0; i < THREAD_COUNT; i++) {
            applicants.add(saveUser("applicant" + i));
        }

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch startLatch = new CountDownLatch(1);           // 출발 신호
        CountDownLatch doneLatch = new CountDownLatch(THREAD_COUNT); // 전원 완료 대기

        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger capacityExceededCount = new AtomicInteger();
        List<Throwable> unexpected = new ArrayList<>();

        // when: 10명이 출발선에 모였다가 동시에 가입 요청
        for (User applicant : applicants) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    studyMemberService.joinStudy(applicant.getId(), study.getId());
                    successCount.incrementAndGet();
                } catch (BusinessException e) {
                    if (e.getErrorCode() == StudyErrorCode.STUDY_CAPACITY_EXCEEDED) {
                        capacityExceededCount.incrementAndGet();
                    } else {
                        synchronized (unexpected) { unexpected.add(e); }
                    }
                } catch (Throwable e) {
                    synchronized (unexpected) { unexpected.add(e); }
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean finished = doneLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        // then
        assertThat(finished).as("30초 안에 모든 요청이 끝나야 한다").isTrue();
        assertThat(unexpected).as("정원 초과 외의 예외는 없어야 한다").isEmpty();
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(capacityExceededCount.get()).isEqualTo(THREAD_COUNT - 1);
        assertThat(studyMemberRepository.countByStudyId(study.getId())).isEqualTo(2);
    }

    private User saveUser(String name) {
        User user = new User();
        user.setName(name);
        user.setUsername(name + "@test.com");
        user.setNickname(name);
        user.setRole(Role.USER);
        user.setStatus(AccountStatus.ACTIVE);
        user.setCreatedAt(LocalDateTime.now());
        user.setSubscribed(false);
        return userRepository.save(user);
    }
}
