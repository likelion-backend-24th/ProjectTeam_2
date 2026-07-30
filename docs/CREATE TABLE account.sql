CREATE TABLE account (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    nickname VARCHAR(50) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'USER',        -- USER, EXPERT, ADMIN
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',     -- ACTIVE, WITHDRAWN
    created_at DATETIME NOT NULL
);

CREATE TABLE post (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    category VARCHAR(50),
    account_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    FOREIGN KEY (account_id) REFERENCES account(id)
);

CREATE TABLE comment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    content TEXT NOT NULL,
    post_id BIGINT NOT NULL,
    account_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL,
    FOREIGN KEY (post_id) REFERENCES post(id),
    FOREIGN KEY (account_id) REFERENCES account(id)
);

CREATE TABLE study (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(200) NOT NULL,
    description TEXT,                -- 소개글/모집글
    capacity INT NOT NULL,
    recruit_start DATE,
    recruit_end DATE,
    leader_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL,
    FOREIGN KEY (leader_id) REFERENCES account(id)
);

CREATE TABLE study_member (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    study_id BIGINT NOT NULL,
    account_id BIGINT NOT NULL,
    joined_at DATETIME NOT NULL,
    FOREIGN KEY (study_id) REFERENCES study(id),
    FOREIGN KEY (account_id) REFERENCES account(id),
    UNIQUE (study_id, account_id)
);

CREATE TABLE study_chat (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    study_id BIGINT NOT NULL,
    account_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    created_at DATETIME NOT NULL,
    FOREIGN KEY (study_id) REFERENCES study(id),
    FOREIGN KEY (account_id) REFERENCES account(id)
);
CREATE TABLE expert_profile (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    account_id BIGINT NOT NULL UNIQUE,
    career TEXT,
    certification TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',   -- PENDING, APPROVED, REJECTED
    reject_reason VARCHAR(255),
    FOREIGN KEY (account_id) REFERENCES account(id)
);

CREATE TABLE subscription (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    account_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,        -- ACTIVE, CANCELLED
    started_at DATETIME NOT NULL,
    expired_at DATETIME,
    FOREIGN KEY (account_id) REFERENCES account(id)
);