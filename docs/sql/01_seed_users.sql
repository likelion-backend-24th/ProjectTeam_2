SET NAMES utf8mb4;
START TRANSACTION;

-- ============================================================
-- 유저 15명 (role=USER)
-- 공통 비밀번호: Expert1234!
-- ============================================================

INSERT INTO users (username, password, nickname, name, role, status, is_subscribed, failed_login_attempts, created_at, terms_agree_at)
VALUES ('dummy.user01@example.com', '$2b$10$UxTIZyDU5nX6MkypUACBvO977BhZlVqP0yzW93WzR2leqj2R53GPq', '김민준', '김민준', 'USER', 'ACTIVE', 0, 0, NOW(), NOW());

INSERT INTO users (username, password, nickname, name, role, status, is_subscribed, failed_login_attempts, created_at, terms_agree_at)
VALUES ('dummy.user02@example.com', '$2b$10$UxTIZyDU5nX6MkypUACBvO977BhZlVqP0yzW93WzR2leqj2R53GPq', '이서준', '이서준', 'USER', 'ACTIVE', 0, 0, NOW(), NOW());

INSERT INTO users (username, password, nickname, name, role, status, is_subscribed, failed_login_attempts, created_at, terms_agree_at)
VALUES ('dummy.user03@example.com', '$2b$10$UxTIZyDU5nX6MkypUACBvO977BhZlVqP0yzW93WzR2leqj2R53GPq', '박서연', '박서연', 'USER', 'ACTIVE', 0, 0, NOW(), NOW());

INSERT INTO users (username, password, nickname, name, role, status, is_subscribed, failed_login_attempts, created_at, terms_agree_at)
VALUES ('dummy.user04@example.com', '$2b$10$UxTIZyDU5nX6MkypUACBvO977BhZlVqP0yzW93WzR2leqj2R53GPq', '최유진', '최유진', 'USER', 'ACTIVE', 0, 0, NOW(), NOW());

INSERT INTO users (username, password, nickname, name, role, status, is_subscribed, failed_login_attempts, created_at, terms_agree_at)
VALUES ('dummy.user05@example.com', '$2b$10$UxTIZyDU5nX6MkypUACBvO977BhZlVqP0yzW93WzR2leqj2R53GPq', '정하준', '정하준', 'USER', 'ACTIVE', 0, 0, NOW(), NOW());

INSERT INTO users (username, password, nickname, name, role, status, is_subscribed, failed_login_attempts, created_at, terms_agree_at)
VALUES ('dummy.user06@example.com', '$2b$10$UxTIZyDU5nX6MkypUACBvO977BhZlVqP0yzW93WzR2leqj2R53GPq', '강예은', '강예은', 'USER', 'ACTIVE', 0, 0, NOW(), NOW());

INSERT INTO users (username, password, nickname, name, role, status, is_subscribed, failed_login_attempts, created_at, terms_agree_at)
VALUES ('dummy.user07@example.com', '$2b$10$UxTIZyDU5nX6MkypUACBvO977BhZlVqP0yzW93WzR2leqj2R53GPq', '조민서', '조민서', 'USER', 'ACTIVE', 0, 0, NOW(), NOW());

INSERT INTO users (username, password, nickname, name, role, status, is_subscribed, failed_login_attempts, created_at, terms_agree_at)
VALUES ('dummy.user08@example.com', '$2b$10$UxTIZyDU5nX6MkypUACBvO977BhZlVqP0yzW93WzR2leqj2R53GPq', '윤도현', '윤도현', 'USER', 'ACTIVE', 0, 0, NOW(), NOW());

INSERT INTO users (username, password, nickname, name, role, status, is_subscribed, failed_login_attempts, created_at, terms_agree_at)
VALUES ('dummy.user09@example.com', '$2b$10$UxTIZyDU5nX6MkypUACBvO977BhZlVqP0yzW93WzR2leqj2R53GPq', '장수아', '장수아', 'USER', 'ACTIVE', 0, 0, NOW(), NOW());

INSERT INTO users (username, password, nickname, name, role, status, is_subscribed, failed_login_attempts, created_at, terms_agree_at)
VALUES ('dummy.user10@example.com', '$2b$10$UxTIZyDU5nX6MkypUACBvO977BhZlVqP0yzW93WzR2leqj2R53GPq', '임지호', '임지호', 'USER', 'ACTIVE', 0, 0, NOW(), NOW());

INSERT INTO users (username, password, nickname, name, role, status, is_subscribed, failed_login_attempts, created_at, terms_agree_at)
VALUES ('dummy.user11@example.com', '$2b$10$UxTIZyDU5nX6MkypUACBvO977BhZlVqP0yzW93WzR2leqj2R53GPq', '한소율', '한소율', 'USER', 'ACTIVE', 0, 0, NOW(), NOW());

INSERT INTO users (username, password, nickname, name, role, status, is_subscribed, failed_login_attempts, created_at, terms_agree_at)
VALUES ('dummy.user12@example.com', '$2b$10$UxTIZyDU5nX6MkypUACBvO977BhZlVqP0yzW93WzR2leqj2R53GPq', '오은우', '오은우', 'USER', 'ACTIVE', 0, 0, NOW(), NOW());

INSERT INTO users (username, password, nickname, name, role, status, is_subscribed, failed_login_attempts, created_at, terms_agree_at)
VALUES ('dummy.user13@example.com', '$2b$10$UxTIZyDU5nX6MkypUACBvO977BhZlVqP0yzW93WzR2leqj2R53GPq', '노하린', '노하린', 'USER', 'ACTIVE', 0, 0, NOW(), NOW());

INSERT INTO users (username, password, nickname, name, role, status, is_subscribed, failed_login_attempts, created_at, terms_agree_at)
VALUES ('dummy.user14@example.com', '$2b$10$UxTIZyDU5nX6MkypUACBvO977BhZlVqP0yzW93WzR2leqj2R53GPq', '배현우', '배현우', 'USER', 'ACTIVE', 0, 0, NOW(), NOW());

INSERT INTO users (username, password, nickname, name, role, status, is_subscribed, failed_login_attempts, created_at, terms_agree_at)
VALUES ('dummy.user15@example.com', '$2b$10$UxTIZyDU5nX6MkypUACBvO977BhZlVqP0yzW93WzR2leqj2R53GPq', '신아윤', '신아윤', 'USER', 'ACTIVE', 0, 0, NOW(), NOW());

COMMIT;
