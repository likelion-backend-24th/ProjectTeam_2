SET NAMES utf8mb4;
START TRANSACTION;

-- ============================================================
-- 스터디 15개 (유저 15명이 각각 리더, 리더는 study_member로도 등록)
-- ※ 01_seed_users.sql을 먼저 실행해야 합니다.
-- username으로 user_id를 매번 조회하는 방식이라 순서 상관없이 안전합니다.
-- ============================================================

-- 1
SET @u = (SELECT id FROM users WHERE username = 'dummy.user01@example.com');
INSERT INTO study (leader_id, title, description, category, capacity, recruit_start, recruit_end, deleted, created_at)
VALUES (@u, '코딩테스트 대비 알고리즘 스터디', '매주 프로그래머스 문제 풀이하고 서로 코드 리뷰하는 스터디입니다.', 'IT_DEVELOPMENT', 6, CURDATE(), DATE_ADD(CURDATE(), INTERVAL 14 DAY), 0, NOW());
SET @s = LAST_INSERT_ID();
INSERT INTO study_member (study_id, user_id, joined_at) VALUES (@s, @u, NOW());

-- 2
SET @u = (SELECT id FROM users WHERE username = 'dummy.user02@example.com');
INSERT INTO study (leader_id, title, description, category, capacity, recruit_start, recruit_end, deleted, created_at)
VALUES (@u, 'SQLD 자격증 취득반', '한 달 안에 SQLD 자격증 같이 준비해요. 인강 진도 맞춰서 스터디 진행합니다.', 'CERTIFICATE', 5, CURDATE(), DATE_ADD(CURDATE(), INTERVAL 14 DAY), 0, NOW());
SET @s = LAST_INSERT_ID();
INSERT INTO study_member (study_id, user_id, joined_at) VALUES (@s, @u, NOW());

-- 3
SET @u = (SELECT id FROM users WHERE username = 'dummy.user03@example.com');
INSERT INTO study (leader_id, title, description, category, capacity, recruit_start, recruit_end, deleted, created_at)
VALUES (@u, 'UX 리서치 스터디', '유저 인터뷰 설계부터 리서치 리포트 작성까지 함께 실습합니다.', 'ETC', 5, CURDATE(), DATE_ADD(CURDATE(), INTERVAL 14 DAY), 0, NOW());
SET @s = LAST_INSERT_ID();
INSERT INTO study_member (study_id, user_id, joined_at) VALUES (@s, @u, NOW());

-- 4
SET @u = (SELECT id FROM users WHERE username = 'dummy.user04@example.com');
INSERT INTO study (leader_id, title, description, category, capacity, recruit_start, recruit_end, deleted, created_at)
VALUES (@u, '프론트엔드 실전 프로젝트반', 'React + TypeScript로 실전 미니 프로젝트 만들어보는 스터디예요.', 'IT_DEVELOPMENT', 6, CURDATE(), DATE_ADD(CURDATE(), INTERVAL 14 DAY), 0, NOW());
SET @s = LAST_INSERT_ID();
INSERT INTO study_member (study_id, user_id, joined_at) VALUES (@s, @u, NOW());

-- 5
SET @u = (SELECT id FROM users WHERE username = 'dummy.user05@example.com');
INSERT INTO study (leader_id, title, description, category, capacity, recruit_start, recruit_end, deleted, created_at)
VALUES (@u, '자소서 첨삭 스터디', '서로 자기소개서 돌려 읽고 피드백 주고받는 모임입니다.', 'ETC', 6, CURDATE(), DATE_ADD(CURDATE(), INTERVAL 14 DAY), 0, NOW());
SET @s = LAST_INSERT_ID();
INSERT INTO study_member (study_id, user_id, joined_at) VALUES (@s, @u, NOW());

-- 6
SET @u = (SELECT id FROM users WHERE username = 'dummy.user06@example.com');
INSERT INTO study (leader_id, title, description, category, capacity, recruit_start, recruit_end, deleted, created_at)
VALUES (@u, '백엔드 스프링 스터디', 'Spring Boot로 미니 프로젝트 만들면서 실무 감각 익히는 스터디예요.', 'IT_DEVELOPMENT', 6, CURDATE(), DATE_ADD(CURDATE(), INTERVAL 14 DAY), 0, NOW());
SET @s = LAST_INSERT_ID();
INSERT INTO study_member (study_id, user_id, joined_at) VALUES (@s, @u, NOW());

-- 7
SET @u = (SELECT id FROM users WHERE username = 'dummy.user07@example.com');
INSERT INTO study (leader_id, title, description, category, capacity, recruit_start, recruit_end, deleted, created_at)
VALUES (@u, '정보처리기사 실기 스터디', '실기 시험 D-30, 기출문제 풀이 위주로 스터디 진행합니다.', 'CERTIFICATE', 5, CURDATE(), DATE_ADD(CURDATE(), INTERVAL 14 DAY), 0, NOW());
SET @s = LAST_INSERT_ID();
INSERT INTO study_member (study_id, user_id, joined_at) VALUES (@s, @u, NOW());

-- 8
SET @u = (SELECT id FROM users WHERE username = 'dummy.user08@example.com');
INSERT INTO study (leader_id, title, description, category, capacity, recruit_start, recruit_end, deleted, created_at)
VALUES (@u, '오픽 IH 목표 스터디', '매주 화/목 화상으로 모여서 오픽 스크립트 연습합니다.', 'LANGUAGE', 4, CURDATE(), DATE_ADD(CURDATE(), INTERVAL 14 DAY), 0, NOW());
SET @s = LAST_INSERT_ID();
INSERT INTO study_member (study_id, user_id, joined_at) VALUES (@s, @u, NOW());

-- 9
SET @u = (SELECT id FROM users WHERE username = 'dummy.user09@example.com');
INSERT INTO study (leader_id, title, description, category, capacity, recruit_start, recruit_end, deleted, created_at)
VALUES (@u, 'PM 취업 준비반', '기획 직무 지원자들끼리 케이스 스터디하고 모의 면접 진행해요.', 'JOB_PREP', 6, CURDATE(), DATE_ADD(CURDATE(), INTERVAL 14 DAY), 0, NOW());
SET @s = LAST_INSERT_ID();
INSERT INTO study_member (study_id, user_id, joined_at) VALUES (@s, @u, NOW());

-- 10
SET @u = (SELECT id FROM users WHERE username = 'dummy.user10@example.com');
INSERT INTO study (leader_id, title, description, category, capacity, recruit_start, recruit_end, deleted, created_at)
VALUES (@u, '독서 모임 겸 커리어 토크', '커리어/자기계발 책 읽고 이야기 나누는 가벼운 모임입니다.', 'ETC', 8, CURDATE(), DATE_ADD(CURDATE(), INTERVAL 14 DAY), 0, NOW());
SET @s = LAST_INSERT_ID();
INSERT INTO study_member (study_id, user_id, joined_at) VALUES (@s, @u, NOW());

-- 11
SET @u = (SELECT id FROM users WHERE username = 'dummy.user11@example.com');
INSERT INTO study (leader_id, title, description, category, capacity, recruit_start, recruit_end, deleted, created_at)
VALUES (@u, '재무제표 스터디', '재무제표 읽는 법부터 실전 분석까지 같이 공부해요.', 'CERTIFICATE', 5, CURDATE(), DATE_ADD(CURDATE(), INTERVAL 14 DAY), 0, NOW());
SET @s = LAST_INSERT_ID();
INSERT INTO study_member (study_id, user_id, joined_at) VALUES (@s, @u, NOW());

-- 12
SET @u = (SELECT id FROM users WHERE username = 'dummy.user12@example.com');
INSERT INTO study (leader_id, title, description, category, capacity, recruit_start, recruit_end, deleted, created_at)
VALUES (@u, 'ADsP 자격증 스터디', '데이터분석 준전문가 자격증 2주 완성 목표로 같이 준비해요.', 'CERTIFICATE', 6, CURDATE(), DATE_ADD(CURDATE(), INTERVAL 14 DAY), 0, NOW());
SET @s = LAST_INSERT_ID();
INSERT INTO study_member (study_id, user_id, joined_at) VALUES (@s, @u, NOW());

-- 13
SET @u = (SELECT id FROM users WHERE username = 'dummy.user13@example.com');
INSERT INTO study (leader_id, title, description, category, capacity, recruit_start, recruit_end, deleted, created_at)
VALUES (@u, '비즈니스 영어회화 스터디', '실무에서 바로 쓰는 이메일/미팅 영어 표현 연습합니다.', 'LANGUAGE', 5, CURDATE(), DATE_ADD(CURDATE(), INTERVAL 14 DAY), 0, NOW());
SET @s = LAST_INSERT_ID();
INSERT INTO study_member (study_id, user_id, joined_at) VALUES (@s, @u, NOW());

-- 14
SET @u = (SELECT id FROM users WHERE username = 'dummy.user14@example.com');
INSERT INTO study (leader_id, title, description, category, capacity, recruit_start, recruit_end, deleted, created_at)
VALUES (@u, '면접 스피치 연습 모임', '모의 면접 녹화하고 서로 피드백 주는 스터디입니다.', 'JOB_PREP', 6, CURDATE(), DATE_ADD(CURDATE(), INTERVAL 14 DAY), 0, NOW());
SET @s = LAST_INSERT_ID();
INSERT INTO study_member (study_id, user_id, joined_at) VALUES (@s, @u, NOW());

-- 15
SET @u = (SELECT id FROM users WHERE username = 'dummy.user15@example.com');
INSERT INTO study (leader_id, title, description, category, capacity, recruit_start, recruit_end, deleted, created_at)
VALUES (@u, '포트폴리오 크리틱 모임', '디자인/개발 포트폴리오 서로 리뷰해주는 모임이에요.', 'ETC', 8, CURDATE(), DATE_ADD(CURDATE(), INTERVAL 14 DAY), 0, NOW());
SET @s = LAST_INSERT_ID();
INSERT INTO study_member (study_id, user_id, joined_at) VALUES (@s, @u, NOW());

COMMIT;