CREATE TABLE `comment`
(
  id         BIGINT   NOT NULL AUTO_INCREMENT COMMENT 'ID',
  content    TEXT     NOT NULL COMMENT '댓글 내용',
  post_id    BIGINT   NOT NULL COMMENT '글 번호',
  user_id    BIGINT   NOT NULL COMMENT '댓글 작성자',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시간',
  updated_at DATETIME NULL     ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 시간',
  PRIMARY KEY (id)
) COMMENT '댓글';

CREATE TABLE expert_profile
(
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'ID',
    user_id       BIGINT       NOT NULL COMMENT '유저 ID',
    introduction  VARCHAR(500) NULL     COMMENT '소개글',
    status        VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT '심사 상태 (PENDING, APPROVED, REJECTED)',
    reject_reason VARCHAR(255) NULL     COMMENT '거절 이유',
    PRIMARY KEY (id)
) COMMENT '전문가';

ALTER TABLE expert_profile
    ADD CONSTRAINT UQ_expert_profile_user_id UNIQUE (user_id);

CREATE TABLE career
(
    id                BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'ID',
    expert_profile_id BIGINT       NOT NULL COMMENT '전문가 프로필 ID',
    company_name      VARCHAR(100) NOT NULL COMMENT '회사명',
    position          VARCHAR(100) NOT NULL COMMENT '직함',
    years             INT          NOT NULL COMMENT '경력 연차',
    job_field         VARCHAR(30)  NOT NULL COMMENT '직무 분야',
    PRIMARY KEY (id)
) COMMENT '전문가 경력';

CREATE TABLE certification
(
    id                BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'ID',
    expert_profile_id BIGINT       NOT NULL COMMENT '전문가 프로필 ID',
    name              VARCHAR(100) NOT NULL COMMENT '자격증명',
    issuer            VARCHAR(100) NOT NULL COMMENT '발급 기관',
    acquired_year     INT          NOT NULL COMMENT '취득 연도',
    PRIMARY KEY (id)
) COMMENT '전문가 자격증';

CREATE TABLE post
(
  id         BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'ID',
  title      VARCHAR(200) NOT NULL COMMENT '제목',
  content    TEXT         NOT NULL COMMENT '내용',
  category   VARCHAR(20)  NOT NULL COMMENT '카테고리',
  user_id    BIGINT       NOT NULL COMMENT '작성자',
  created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시간',
  updated_at DATETIME     NULL     ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 시간',
  PRIMARY KEY (id)
) COMMENT 'Q&A';

CREATE TABLE study
(
  id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'ID',
  title         VARCHAR(200) NOT NULL COMMENT '제목',
  description   TEXT         NULL     COMMENT '소개',
  capacity      INT          NOT NULL COMMENT '모집 인원',
  recruit_start DATE         NULL     COMMENT '시작일',
  recruit_end   DATE         NULL     COMMENT '마감일',
  leader_id     BIGINT       NOT NULL COMMENT '방장 ID',
  created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시간',
  PRIMARY KEY (id)
) COMMENT '스터디 모집';

CREATE TABLE study_post
(
  id         BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'ID',
  study_id   BIGINT       NOT NULL COMMENT '스터디 ID',
  user_id    BIGINT       NOT NULL COMMENT '작성자',
  title      VARCHAR(200) NOT NULL COMMENT '제목',
  content    TEXT         NOT NULL COMMENT '내용',
  created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시간',
  updated_at DATETIME     NULL     ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 시간',
  PRIMARY KEY (id)
) COMMENT '스터디 내 게시글';

CREATE TABLE study_post_comment
(
  id            BIGINT   NOT NULL AUTO_INCREMENT COMMENT 'ID',
  content       TEXT     NOT NULL COMMENT '댓글 내용',
  study_post_id BIGINT   NOT NULL COMMENT '스터디 게시글 ID',
  user_id       BIGINT   NOT NULL COMMENT '댓글 작성자',
  created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시간',
  updated_at    DATETIME NULL     ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 시간',
  PRIMARY KEY (id)
) COMMENT '스터디 게시글 댓글';

CREATE TABLE study_member
(
  id        BIGINT   NOT NULL AUTO_INCREMENT COMMENT 'ID',
  study_id  BIGINT   NOT NULL COMMENT '스터디 ID',
  user_id   BIGINT   NOT NULL COMMENT '멤버 ID',
  joined_at DATETIME NOT NULL COMMENT '가입 시간',
  PRIMARY KEY (id)
) COMMENT '스터디 멤버';

ALTER TABLE study_member
  ADD CONSTRAINT UQ_study_member_study_user UNIQUE (study_id, user_id);

CREATE TABLE subscription
(
  id         BIGINT      NOT NULL AUTO_INCREMENT COMMENT 'ID',
  user_id    BIGINT      NOT NULL COMMENT '유저 ID',
  status     VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '구독 상태 (ACTIVE, CANCELLED)',
  started_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '시작일',
  expired_at DATETIME    NULL     COMMENT '만료일',
  PRIMARY KEY (id)
) COMMENT '구독';

CREATE TABLE feedback
(
  id                BIGINT      NOT NULL AUTO_INCREMENT COMMENT 'ID',
  requester_id      BIGINT      NOT NULL COMMENT '질문자(구독자) ID',
  expert_profile_id BIGINT      NOT NULL COMMENT '담당 전문가 ID',
  status            VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '스레드 상태 (PENDING: 답변 대기, ANSWERED: 답변 완료)',
  created_at        DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시간',
  answered_at       DATETIME    NULL     COMMENT '전문가 최초 답변 시간',
  PRIMARY KEY (id)
) COMMENT '전문가 문의 스레드';

CREATE TABLE feedback_message
(
  id          BIGINT   NOT NULL AUTO_INCREMENT COMMENT 'ID',
  feedback_id BIGINT   NOT NULL COMMENT '문의 스레드 ID',
  sender_id   BIGINT   NOT NULL COMMENT '작성자 (질문자 또는 전문가)',
  content     TEXT     NOT NULL COMMENT '메시지 내용',
  created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시간',
  PRIMARY KEY (id)
) COMMENT '전문가 문의 메시지';

CREATE TABLE users
(
  id         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '유저 ID',
  username   VARCHAR(50)  NOT NULL COMMENT 'email',
  password   VARCHAR(255) NULL     COMMENT 'pw (소셜 전용 가입자는 NULL)',
  nickname   VARCHAR(50)  NOT NULL COMMENT '닉네임',
  role       VARCHAR(20)  NOT NULL DEFAULT 'USER' COMMENT '권한 (USER, EXPERT, ADMIN)',
  status     VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE' COMMENT '계정 상태 (ACTIVE, SUSPENDED, WITHDRAWN)',
  created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시간',
  PRIMARY KEY (id)
) COMMENT '회원';

ALTER TABLE users
  ADD CONSTRAINT UQ_username UNIQUE (username);

CREATE TABLE oauth_account
(
  id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'ID',
  user_id     BIGINT       NOT NULL COMMENT '유저 ID',
  provider    VARCHAR(20)  NOT NULL COMMENT '제공자 (KAKAO, GOOGLE)',
  provider_id VARCHAR(100) NOT NULL COMMENT '제공자 측 고유 회원 ID',
  linked_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '연동 시각',
  PRIMARY KEY (id)
) COMMENT '소셜 로그인 연동';

CREATE TABLE refresh_token
(
  id         BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'ID',
  user_id    BIGINT       NOT NULL COMMENT '유저 ID',
  token      VARCHAR(500) NOT NULL COMMENT '리프레시 토큰 값',
  expires_at DATETIME     NOT NULL COMMENT '만료 시간',
  created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '발급 시간',
  PRIMARY KEY (id)
) COMMENT '리프레시 토큰';

ALTER TABLE refresh_token
  ADD CONSTRAINT UQ_refresh_token_user_id UNIQUE (user_id);

ALTER TABLE oauth_account
  ADD CONSTRAINT UQ_oauth_account_provider UNIQUE (provider, provider_id);

ALTER TABLE post
  ADD CONSTRAINT FK_users_TO_post
    FOREIGN KEY (user_id)
    REFERENCES users (id);

ALTER TABLE comment
  ADD CONSTRAINT FK_post_TO_comment
    FOREIGN KEY (post_id)
    REFERENCES post (id)
    ON DELETE CASCADE;

ALTER TABLE comment
  ADD CONSTRAINT FK_users_TO_comment
    FOREIGN KEY (user_id)
    REFERENCES users (id);

ALTER TABLE study
  ADD CONSTRAINT FK_users_TO_study
    FOREIGN KEY (leader_id)
    REFERENCES users (id);

ALTER TABLE study_member
  ADD CONSTRAINT FK_study_TO_study_member
    FOREIGN KEY (study_id)
    REFERENCES study (id)
    ON DELETE CASCADE;

ALTER TABLE study_member
  ADD CONSTRAINT FK_users_TO_study_member
    FOREIGN KEY (user_id)
    REFERENCES users (id);

ALTER TABLE study_post
  ADD CONSTRAINT FK_study_TO_study_post
    FOREIGN KEY (study_id)
    REFERENCES study (id)
    ON DELETE CASCADE;

ALTER TABLE study_post
  ADD CONSTRAINT FK_users_TO_study_post
    FOREIGN KEY (user_id)
    REFERENCES users (id);

ALTER TABLE study_post_comment
  ADD CONSTRAINT FK_study_post_TO_study_post_comment
    FOREIGN KEY (study_post_id)
    REFERENCES study_post (id)
    ON DELETE CASCADE;

ALTER TABLE study_post_comment
  ADD CONSTRAINT FK_users_TO_study_post_comment
    FOREIGN KEY (user_id)
    REFERENCES users (id);

ALTER TABLE expert_profile
  ADD CONSTRAINT FK_users_TO_expert_profile
    FOREIGN KEY (user_id)
    REFERENCES users (id);

ALTER TABLE career
    ADD CONSTRAINT FK_expert_profile_TO_career
        FOREIGN KEY (expert_profile_id)
            REFERENCES expert_profile (id)
            ON DELETE CASCADE;

ALTER TABLE certification
    ADD CONSTRAINT FK_expert_profile_TO_certification
        FOREIGN KEY (expert_profile_id)
            REFERENCES expert_profile (id)
            ON DELETE CASCADE;


ALTER TABLE subscription
  ADD CONSTRAINT FK_users_TO_subscription
    FOREIGN KEY (user_id)
    REFERENCES users (id);

ALTER TABLE oauth_account
  ADD CONSTRAINT FK_users_TO_oauth_account
    FOREIGN KEY (user_id)
    REFERENCES users (id)
    ON DELETE CASCADE;

ALTER TABLE feedback
  ADD CONSTRAINT FK_users_TO_feedback
    FOREIGN KEY (requester_id)
    REFERENCES users (id);

ALTER TABLE feedback
  ADD CONSTRAINT FK_expert_profile_TO_feedback
    FOREIGN KEY (expert_profile_id)
    REFERENCES expert_profile (id);

ALTER TABLE feedback_message
  ADD CONSTRAINT FK_feedback_TO_feedback_message
    FOREIGN KEY (feedback_id)
    REFERENCES feedback (id)
    ON DELETE CASCADE;

ALTER TABLE feedback_message
  ADD CONSTRAINT FK_users_TO_feedback_message
    FOREIGN KEY (sender_id)
    REFERENCES users (id);

ALTER TABLE refresh_token
  ADD CONSTRAINT FK_users_TO_refresh_token
    FOREIGN KEY (user_id)
    REFERENCES users (id)
    ON DELETE CASCADE;