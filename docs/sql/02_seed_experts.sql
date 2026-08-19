SET NAMES utf8mb4;
START TRANSACTION;

-- ============================================================
-- 전문가 12명 (role=EXPERT, expert_profile APPROVED, career/certification 포함)
-- 공통 비밀번호: Expert1234!
-- ============================================================

-- 1. 김도윤 - 백엔드 개발자
INSERT INTO users (username, password, nickname, name, role, status, is_subscribed, failed_login_attempts, created_at, terms_agree_at)
VALUES ('dummy.expert01@example.com', '$2b$10$UxTIZyDU5nX6MkypUACBvO977BhZlVqP0yzW93WzR2leqj2R53GPq', '김도윤', '김도윤', 'EXPERT', 'ACTIVE', 0, 0, NOW(), NOW());
SET @uid = LAST_INSERT_ID();
INSERT INTO expert_profile (user_id, introduction, status, approved_at)
VALUES (@uid, '7년차 백엔드 개발자입니다. 대규모 트래픽 서비스 설계와 장애 대응 경험을 바탕으로 실전 조언 드립니다.', 'APPROVED', NOW());
SET @pid = LAST_INSERT_ID();
INSERT INTO career (expert_profile_id, company_name, position, years, job_field) VALUES
(@pid, '카카오', '백엔드 개발자', 7, 'IT_DEVELOPMENT'),
(@pid, '라인플러스', '서버 개발자', 2, 'IT_DEVELOPMENT');
INSERT INTO certification (expert_profile_id, name, issuer, acquired_year)
VALUES (@pid, '정보처리기사', '한국산업인력공단', 2019);

-- 2. 이서연 - 프론트엔드 개발자
INSERT INTO users (username, password, nickname, name, role, status, is_subscribed, failed_login_attempts, created_at, terms_agree_at)
VALUES ('dummy.expert02@example.com', '$2b$10$UxTIZyDU5nX6MkypUACBvO977BhZlVqP0yzW93WzR2leqj2R53GPq', '이서연', '이서연', 'EXPERT', 'ACTIVE', 0, 0, NOW(), NOW());
SET @uid = LAST_INSERT_ID();
INSERT INTO expert_profile (user_id, introduction, status, approved_at)
VALUES (@uid, '5년차 프론트엔드 개발자입니다. React/TypeScript 기반 서비스 개발과 취업 포트폴리오 첨삭을 도와드려요.', 'APPROVED', NOW());
SET @pid = LAST_INSERT_ID();
INSERT INTO career (expert_profile_id, company_name, position, years, job_field)
VALUES (@pid, '네이버', '프론트엔드 개발자', 5, 'IT_DEVELOPMENT');
INSERT INTO certification (expert_profile_id, name, issuer, acquired_year)
VALUES (@pid, 'SQLD', '한국데이터산업진흥원', 2020);

-- 3. 박지훈 - UX 디자이너
INSERT INTO users (username, password, nickname, name, role, status, is_subscribed, failed_login_attempts, created_at, terms_agree_at)
VALUES ('dummy.expert03@example.com', '$2b$10$UxTIZyDU5nX6MkypUACBvO977BhZlVqP0yzW93WzR2leqj2R53GPq', '박지훈', '박지훈', 'EXPERT', 'ACTIVE', 0, 0, NOW(), NOW());
SET @uid = LAST_INSERT_ID();
INSERT INTO expert_profile (user_id, introduction, status, approved_at)
VALUES (@uid, '6년차 UX 디자이너입니다. 유저 리서치부터 프로토타이핑까지 실무 프로세스를 공유합니다.', 'APPROVED', NOW());
SET @pid = LAST_INSERT_ID();
INSERT INTO career (expert_profile_id, company_name, position, years, job_field)
VALUES (@pid, '토스', 'UX 디자이너', 6, 'DESIGN_UX');
INSERT INTO certification (expert_profile_id, name, issuer, acquired_year)
VALUES (@pid, 'GTQ 1급', '한국생산성본부', 2018);

-- 4. 최유나 - 프로덕트 디자이너
INSERT INTO users (username, password, nickname, name, role, status, is_subscribed, failed_login_attempts, created_at, terms_agree_at)
VALUES ('dummy.expert04@example.com', '$2b$10$UxTIZyDU5nX6MkypUACBvO977BhZlVqP0yzW93WzR2leqj2R53GPq', '최유나', '최유나', 'EXPERT', 'ACTIVE', 0, 0, NOW(), NOW());
SET @uid = LAST_INSERT_ID();
INSERT INTO expert_profile (user_id, introduction, status, approved_at)
VALUES (@uid, '4년차 프로덕트 디자이너입니다. 0에서 1을 만드는 초기 스타트업 디자인 경험이 많아요.', 'APPROVED', NOW());
SET @pid = LAST_INSERT_ID();
INSERT INTO career (expert_profile_id, company_name, position, years, job_field)
VALUES (@pid, '배달의민족', '프로덕트 디자이너', 4, 'DESIGN_UX');

-- 5. 정민재 - 퍼포먼스 마케터
INSERT INTO users (username, password, nickname, name, role, status, is_subscribed, failed_login_attempts, created_at, terms_agree_at)
VALUES ('dummy.expert05@example.com', '$2b$10$UxTIZyDU5nX6MkypUACBvO977BhZlVqP0yzW93WzR2leqj2R53GPq', '정민재', '정민재', 'EXPERT', 'ACTIVE', 0, 0, NOW(), NOW());
SET @uid = LAST_INSERT_ID();
INSERT INTO expert_profile (user_id, introduction, status, approved_at)
VALUES (@uid, '8년차 퍼포먼스 마케터입니다. 데이터 기반 광고 최적화와 마케팅 직무 취업 상담 가능합니다.', 'APPROVED', NOW());
SET @pid = LAST_INSERT_ID();
INSERT INTO career (expert_profile_id, company_name, position, years, job_field) VALUES
(@pid, '쿠팡', '퍼포먼스 마케터', 8, 'MARKETING'),
(@pid, '우아한형제들', '그로스 마케터', 3, 'MARKETING');
INSERT INTO certification (expert_profile_id, name, issuer, acquired_year)
VALUES (@pid, '구글 애널리틱스 인증(GAIQ)', 'Google', 2022);

-- 6. 한소희 - 브랜드 마케터
INSERT INTO users (username, password, nickname, name, role, status, is_subscribed, failed_login_attempts, created_at, terms_agree_at)
VALUES ('dummy.expert06@example.com', '$2b$10$UxTIZyDU5nX6MkypUACBvO977BhZlVqP0yzW93WzR2leqj2R53GPq', '한소희', '한소희', 'EXPERT', 'ACTIVE', 0, 0, NOW(), NOW());
SET @uid = LAST_INSERT_ID();
INSERT INTO expert_profile (user_id, introduction, status, approved_at)
VALUES (@uid, '3년차 브랜드 마케터입니다. SNS 채널 운영과 콘텐츠 기획 실무를 상담해 드려요.', 'APPROVED', NOW());
SET @pid = LAST_INSERT_ID();
INSERT INTO career (expert_profile_id, company_name, position, years, job_field)
VALUES (@pid, '무신사', '브랜드 마케터', 3, 'MARKETING');

-- 7. 강태윤 - 전략기획 매니저
INSERT INTO users (username, password, nickname, name, role, status, is_subscribed, failed_login_attempts, created_at, terms_agree_at)
VALUES ('dummy.expert07@example.com', '$2b$10$UxTIZyDU5nX6MkypUACBvO977BhZlVqP0yzW93WzR2leqj2R53GPq', '강태윤', '강태윤', 'EXPERT', 'ACTIVE', 0, 0, NOW(), NOW());
SET @uid = LAST_INSERT_ID();
INSERT INTO expert_profile (user_id, introduction, status, approved_at)
VALUES (@uid, '9년차 전략기획 매니저입니다. 신사업 기획, 경영진 보고 자료 작성 노하우를 공유합니다.', 'APPROVED', NOW());
SET @pid = LAST_INSERT_ID();
INSERT INTO career (expert_profile_id, company_name, position, years, job_field)
VALUES (@pid, '현대카드', '전략기획 매니저', 9, 'MANAGEMENT_STRATEGY');
INSERT INTO certification (expert_profile_id, name, issuer, acquired_year)
VALUES (@pid, 'PMP', 'PMI', 2020);

-- 8. 윤서아 - 사업기획 담당자
INSERT INTO users (username, password, nickname, name, role, status, is_subscribed, failed_login_attempts, created_at, terms_agree_at)
VALUES ('dummy.expert08@example.com', '$2b$10$UxTIZyDU5nX6MkypUACBvO977BhZlVqP0yzW93WzR2leqj2R53GPq', '윤서아', '윤서아', 'EXPERT', 'ACTIVE', 0, 0, NOW(), NOW());
SET @uid = LAST_INSERT_ID();
INSERT INTO expert_profile (user_id, introduction, status, approved_at)
VALUES (@uid, '5년차 사업기획 담당자입니다. 신입/주니어 대상 사업기획 직무 이해와 서류 준비를 도와드려요.', 'APPROVED', NOW());
SET @pid = LAST_INSERT_ID();
INSERT INTO career (expert_profile_id, company_name, position, years, job_field)
VALUES (@pid, 'LG전자', '사업기획 담당자', 5, 'MANAGEMENT_STRATEGY');

-- 9. 서준혁 - 재무/회계
INSERT INTO users (username, password, nickname, name, role, status, is_subscribed, failed_login_attempts, created_at, terms_agree_at)
VALUES ('dummy.expert09@example.com', '$2b$10$UxTIZyDU5nX6MkypUACBvO977BhZlVqP0yzW93WzR2leqj2R53GPq', '서준혁', '서준혁', 'EXPERT', 'ACTIVE', 0, 0, NOW(), NOW());
SET @uid = LAST_INSERT_ID();
INSERT INTO expert_profile (user_id, introduction, status, approved_at)
VALUES (@uid, '6년차 재무 담당자입니다. 회계/재무 직무 취업 준비와 공인회계사 자격증 상담을 도와드려요.', 'APPROVED', NOW());
SET @pid = LAST_INSERT_ID();
INSERT INTO career (expert_profile_id, company_name, position, years, job_field)
VALUES (@pid, '삼성전자', '재무 담당자', 6, 'FINANCE_ACCOUNTING');
INSERT INTO certification (expert_profile_id, name, issuer, acquired_year)
VALUES (@pid, '공인회계사(CPA)', '금융감독원', 2016);

-- 10. 오하은 - 회계 담당자
INSERT INTO users (username, password, nickname, name, role, status, is_subscribed, failed_login_attempts, created_at, terms_agree_at)
VALUES ('dummy.expert10@example.com', '$2b$10$UxTIZyDU5nX6MkypUACBvO977BhZlVqP0yzW93WzR2leqj2R53GPq', '오하은', '오하은', 'EXPERT', 'ACTIVE', 0, 0, NOW(), NOW());
SET @uid = LAST_INSERT_ID();
INSERT INTO expert_profile (user_id, introduction, status, approved_at)
VALUES (@uid, '4년차 회계 담당자입니다. 자격증 준비와 실무 회계 처리 경험을 나눠드려요.', 'APPROVED', NOW());
SET @pid = LAST_INSERT_ID();
INSERT INTO career (expert_profile_id, company_name, position, years, job_field)
VALUES (@pid, 'KB증권', '회계 담당자', 4, 'FINANCE_ACCOUNTING');
INSERT INTO certification (expert_profile_id, name, issuer, acquired_year)
VALUES (@pid, '전산세무 1급', '한국세무사회', 2021);

-- 11. 서준호 - 영업기획 담당자
INSERT INTO users (username, password, nickname, name, role, status, is_subscribed, failed_login_attempts, created_at, terms_agree_at)
VALUES ('dummy.expert11@example.com', '$2b$10$UxTIZyDU5nX6MkypUACBvO977BhZlVqP0yzW93WzR2leqj2R53GPq', '서준호', '서준호', 'EXPERT', 'ACTIVE', 0, 0, NOW(), NOW());
SET @uid = LAST_INSERT_ID();
INSERT INTO expert_profile (user_id, introduction, status, approved_at)
VALUES (@uid, '6년차 영업기획 담당자입니다. B2B 영업 전략과 파트너십 협상 경험을 공유합니다.', 'APPROVED', NOW());
SET @pid = LAST_INSERT_ID();
INSERT INTO career (expert_profile_id, company_name, position, years, job_field)
VALUES (@pid, 'SK텔레콤', '영업기획 담당자', 6, 'SALES_CS');

-- 12. 백지민 - 고객성공매니저
INSERT INTO users (username, password, nickname, name, role, status, is_subscribed, failed_login_attempts, created_at, terms_agree_at)
VALUES ('dummy.expert12@example.com', '$2b$10$UxTIZyDU5nX6MkypUACBvO977BhZlVqP0yzW93WzR2leqj2R53GPq', '백지민', '백지민', 'EXPERT', 'ACTIVE', 0, 0, NOW(), NOW());
SET @uid = LAST_INSERT_ID();
INSERT INTO expert_profile (user_id, introduction, status, approved_at)
VALUES (@uid, '5년차 고객성공매니저(CSM)입니다. CS 직무 이해와 실무 커뮤니케이션 노하우를 알려드려요.', 'APPROVED', NOW());
SET @pid = LAST_INSERT_ID();
INSERT INTO career (expert_profile_id, company_name, position, years, job_field)
VALUES (@pid, '우아한형제들', '고객성공매니저', 5, 'SALES_CS');
INSERT INTO certification (expert_profile_id, name, issuer, acquired_year)
VALUES (@pid, 'CS Leaders(관리사)', '한국생산성본부', 2020);

COMMIT;
