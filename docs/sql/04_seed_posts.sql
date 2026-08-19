SET NAMES utf8mb4;
START TRANSACTION;

-- ============================================================
-- 게시글 13개 (유저 13명이 작성, FREE/INTERVIEW_REVIEW/JOB_INFO/RESUME 분배)
-- ※ 01_seed_users.sql을 먼저 실행해야 합니다.
-- ============================================================

INSERT INTO post (user_id, title, content, category, view_count, deleted, created_at)
VALUES ((SELECT id FROM users WHERE username = 'dummy.user01@example.com'),
        '다들 스터디 몇 개나 하고 계신가요?', '저는 지금 알고리즘 스터디 하나 하고 있는데 다른 분들은 몇 개씩 병행하시는지 궁금해요.', 'FREE', 24, 0, NOW());

INSERT INTO post (user_id, title, content, category, view_count, deleted, created_at)
VALUES ((SELECT id FROM users WHERE username = 'dummy.user02@example.com'),
        '카카오 1차 면접 후기 공유합니다', 'CS 위주로 질문 받았고 프로젝트 관련해서도 꼬리질문이 많았어요. 준비하시는 분들께 도움 됐으면 합니다.', 'INTERVIEW_REVIEW', 156, 0, NOW());

INSERT INTO post (user_id, title, content, category, view_count, deleted, created_at)
VALUES ((SELECT id FROM users WHERE username = 'dummy.user03@example.com'),
        '이번 주 상반기 공채 마감 정리', '이번 주에 마감되는 공채 리스트 정리해봤어요. 놓치지 마세요!', 'JOB_INFO', 132, 0, NOW());

INSERT INTO post (user_id, title, content, category, view_count, deleted, created_at)
VALUES ((SELECT id FROM users WHERE username = 'dummy.user04@example.com'),
        '경력기술서 첨삭 부탁드려요', '이직 준비 중인데 경력기술서 한번 봐주실 분 계신가요?', 'RESUME', 41, 0, NOW());

INSERT INTO post (user_id, title, content, category, view_count, deleted, created_at)
VALUES ((SELECT id FROM users WHERE username = 'dummy.user05@example.com'),
        '취준 스터디 같이 하실 분 구해요', '주 2회 온라인으로 진행할 예정이에요. 관심 있으신 분 댓글 남겨주세요.', 'FREE', 19, 0, NOW());

INSERT INTO post (user_id, title, content, category, view_count, deleted, created_at)
VALUES ((SELECT id FROM users WHERE username = 'dummy.user06@example.com'),
        '네이버 백엔드 최종면접 후기', '임원면접까지 갔던 경험 공유드려요. 인성 질문 위주였습니다.', 'INTERVIEW_REVIEW', 210, 0, NOW());

INSERT INTO post (user_id, title, content, category, view_count, deleted, created_at)
VALUES ((SELECT id FROM users WHERE username = 'dummy.user07@example.com'),
        '정보처리기사 실기 일정 안내', '이번 회차 실기 시험 일정이랑 접수 기간 정리했어요.', 'JOB_INFO', 88, 0, NOW());

INSERT INTO post (user_id, title, content, category, view_count, deleted, created_at)
VALUES ((SELECT id FROM users WHERE username = 'dummy.user08@example.com'),
        '포트폴리오 링크 남겨요, 피드백 환영', '3개월 동안 준비한 포트폴리오입니다. 솔직한 의견 부탁드려요.', 'RESUME', 65, 0, NOW());

INSERT INTO post (user_id, title, content, category, view_count, deleted, created_at)
VALUES ((SELECT id FROM users WHERE username = 'dummy.user09@example.com'),
        '스터디 카페 추천해주세요', '강남/역삼 쪽에서 스터디하기 좋은 카페 있을까요?', 'FREE', 12, 0, NOW());

INSERT INTO post (user_id, title, content, category, view_count, deleted, created_at)
VALUES ((SELECT id FROM users WHERE username = 'dummy.user10@example.com'),
        '토스 실무진 면접 후기', '기술 질문보다 협업 경험 위주로 물어보셨어요. 참고하세요.', 'INTERVIEW_REVIEW', 178, 0, NOW());

INSERT INTO post (user_id, title, content, category, view_count, deleted, created_at)
VALUES ((SELECT id FROM users WHERE username = 'dummy.user11@example.com'),
        'IT 기업 신입 공채 모음', '이번 달 신입 개발자 공채 올라온 곳들 정리했습니다.', 'JOB_INFO', 245, 0, NOW());

INSERT INTO post (user_id, title, content, category, view_count, deleted, created_at)
VALUES ((SELECT id FROM users WHERE username = 'dummy.user12@example.com'),
        '이력서 한 줄 요약 어떻게 쓰세요?', '경력기술서 맨 위 한 줄 요약이 항상 고민이네요. 팁 있으면 공유해주세요.', 'RESUME', 33, 0, NOW());

INSERT INTO post (user_id, title, content, category, view_count, deleted, created_at)
VALUES ((SELECT id FROM users WHERE username = 'dummy.user13@example.com'),
        '오늘도 출첵합니다', '취준 100일째, 오늘도 열심히 준비했습니다. 다들 화이팅!', 'FREE', 9, 0, NOW());

COMMIT;
