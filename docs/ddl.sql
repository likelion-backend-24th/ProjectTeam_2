-- MySQL Workbench Forward Engineering

SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0;
SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0;
SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION';

-- -----------------------------------------------------
-- Schema mydb
-- -----------------------------------------------------
-- -----------------------------------------------------
-- Schema prep2getherdb
-- -----------------------------------------------------

-- -----------------------------------------------------
-- Schema prep2getherdb
-- -----------------------------------------------------
CREATE SCHEMA IF NOT EXISTS `prep2getherdb` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci ;
USE `prep2getherdb` ;

-- -----------------------------------------------------
-- Table `prep2getherdb`.`users`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `prep2getherdb`.`users` (
  `is_subscribed` BIT(1) NOT NULL DEFAULT b'0',
  `created_at` DATETIME(6) NOT NULL,
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `withdrawn_at` DATETIME(6) NULL DEFAULT NULL,
  `nickname` VARCHAR(50) NOT NULL,
  `username` VARCHAR(50) NOT NULL,
  `name` VARCHAR(100) NOT NULL,
  `password` VARCHAR(255) NULL DEFAULT NULL,
  `role` ENUM('ADMIN', 'EXPERT', 'USER') NOT NULL DEFAULT 'USER',
  `status` ENUM('ACTIVE', 'SUSPENDED', 'WITHDRAWN') NOT NULL DEFAULT 'ACTIVE',
  PRIMARY KEY (`id`),
  UNIQUE INDEX `UK2ty1xmrrgtn89xt7kyxx6ta7h` (`nickname` ASC) VISIBLE,
  UNIQUE INDEX `UKr43af9ap4edm43mmtq01oddj6` (`username` ASC) VISIBLE)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci;


-- -----------------------------------------------------
-- Table `prep2getherdb`.`expert_profile`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `prep2getherdb`.`expert_profile` (
  `approved_at` DATETIME(6) NULL DEFAULT NULL,
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `introduction` VARCHAR(500) NULL DEFAULT NULL,
  `reject_reason` VARCHAR(255) NULL DEFAULT NULL,
  `status` ENUM('APPROVED', 'PENDING', 'REJECTED') NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE INDEX `UK6oirtpi7rp4545alfp5gxlv8l` (`user_id` ASC) VISIBLE,
  CONSTRAINT `FKb958nvcyhwo1armmgy157doqx`
    FOREIGN KEY (`user_id`)
    REFERENCES `prep2getherdb`.`users` (`id`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci;


-- -----------------------------------------------------
-- Table `prep2getherdb`.`career`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `prep2getherdb`.`career` (
  `years` INT NOT NULL,
  `expert_profile_id` BIGINT NOT NULL,
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `company_name` VARCHAR(100) NOT NULL,
  `position` VARCHAR(100) NOT NULL,
  `job_field` ENUM('DESIGN_UX', 'ETC', 'FINANCE_ACCOUNTING', 'IT_DEVELOPMENT', 'MANAGEMENT_STRATEGY', 'MARKETING', 'SALES_CS') NOT NULL,
  PRIMARY KEY (`id`),
  INDEX `FKmhb8r0ki8aexds53mxdhyl41r` (`expert_profile_id` ASC) VISIBLE,
  CONSTRAINT `FKmhb8r0ki8aexds53mxdhyl41r`
    FOREIGN KEY (`expert_profile_id`)
    REFERENCES `prep2getherdb`.`expert_profile` (`id`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci;


-- -----------------------------------------------------
-- Table `prep2getherdb`.`certification`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `prep2getherdb`.`certification` (
  `acquired_year` INT NOT NULL,
  `expert_profile_id` BIGINT NOT NULL,
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `issuer` VARCHAR(100) NOT NULL,
  `name` VARCHAR(100) NOT NULL,
  PRIMARY KEY (`id`),
  INDEX `FK8d83y2e6c8lefh9afs06mk642` (`expert_profile_id` ASC) VISIBLE,
  CONSTRAINT `FK8d83y2e6c8lefh9afs06mk642`
    FOREIGN KEY (`expert_profile_id`)
    REFERENCES `prep2getherdb`.`expert_profile` (`id`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci;


-- -----------------------------------------------------
-- Table `prep2getherdb`.`post`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `prep2getherdb`.`post` (
  `deleted` BIT(1) NOT NULL COMMENT 'Soft-delete indicator',
  `created_at` DATETIME(6) NOT NULL,
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `updated_at` DATETIME(6) NULL DEFAULT NULL,
  `user_id` BIGINT NOT NULL,
  `view_count` BIGINT NOT NULL,
  `title` VARCHAR(200) NOT NULL,
  `content` TEXT NOT NULL,
  `category` ENUM('FREE', 'INTERVIEW_REVIEW', 'JOB_INFO', 'RESUME') NOT NULL,
  PRIMARY KEY (`id`),
  INDEX `FK7ky67sgi7k0ayf22652f7763r` (`user_id` ASC) VISIBLE,
  CONSTRAINT `FK7ky67sgi7k0ayf22652f7763r`
    FOREIGN KEY (`user_id`)
    REFERENCES `prep2getherdb`.`users` (`id`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci;


-- -----------------------------------------------------
-- Table `prep2getherdb`.`comment`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `prep2getherdb`.`comment` (
  `deleted` BIT(1) NOT NULL COMMENT 'Soft-delete indicator',
  `created_at` DATETIME(6) NOT NULL,
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `post_id` BIGINT NOT NULL,
  `updated_at` DATETIME(6) NULL DEFAULT NULL,
  `user_id` BIGINT NOT NULL,
  `content` TEXT NOT NULL,
  PRIMARY KEY (`id`),
  INDEX `FKs1slvnkuemjsq2kj4h3vhx7i1` (`post_id` ASC) VISIBLE,
  INDEX `FKqm52p1v3o13hy268he0wcngr5` (`user_id` ASC) VISIBLE,
  CONSTRAINT `FKqm52p1v3o13hy268he0wcngr5`
    FOREIGN KEY (`user_id`)
    REFERENCES `prep2getherdb`.`users` (`id`),
  CONSTRAINT `FKs1slvnkuemjsq2kj4h3vhx7i1`
    FOREIGN KEY (`post_id`)
    REFERENCES `prep2getherdb`.`post` (`id`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci;


-- -----------------------------------------------------
-- Table `prep2getherdb`.`email_verification`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `prep2getherdb`.`email_verification` (
  `verified` BIT(1) NOT NULL,
  `code` VARCHAR(6) NOT NULL,
  `created_at` DATETIME(6) NOT NULL,
  `expires_at` DATETIME(6) NOT NULL,
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `email` VARCHAR(50) NOT NULL,
  PRIMARY KEY (`id`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci;


-- -----------------------------------------------------
-- Table `prep2getherdb`.`feedback`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `prep2getherdb`.`feedback` (
  `answered_at` DATETIME(6) NULL DEFAULT NULL,
  `closed_at` DATETIME(6) NULL DEFAULT NULL,
  `created_at` DATETIME(6) NOT NULL,
  `expert_profile_id` BIGINT NOT NULL,
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `requester_id` BIGINT NOT NULL,
  `topic` VARCHAR(100) NOT NULL,
  `closed_by` ENUM('EXPERT_REVOKED', 'REQUESTER_CLOSED') NULL DEFAULT NULL,
  `status` ENUM('ANSWERED', 'PENDING') NOT NULL,
  PRIMARY KEY (`id`),
  INDEX `FK3s834qthb3dh584ab8k1w9txj` (`expert_profile_id` ASC) VISIBLE,
  INDEX `FK1prd2nl8lgco4wjkkfiohyfb9` (`requester_id` ASC) VISIBLE,
  CONSTRAINT `FK1prd2nl8lgco4wjkkfiohyfb9`
    FOREIGN KEY (`requester_id`)
    REFERENCES `prep2getherdb`.`users` (`id`),
  CONSTRAINT `FK3s834qthb3dh584ab8k1w9txj`
    FOREIGN KEY (`expert_profile_id`)
    REFERENCES `prep2getherdb`.`expert_profile` (`id`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci;


-- -----------------------------------------------------
-- Table `prep2getherdb`.`feedback_message`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `prep2getherdb`.`feedback_message` (
  `created_at` DATETIME(6) NOT NULL,
  `feedback_id` BIGINT NOT NULL,
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `sender_id` BIGINT NOT NULL,
  `content` TEXT NOT NULL,
  PRIMARY KEY (`id`),
  INDEX `FKnodylbe3bxj139i2fc2kbu4su` (`feedback_id` ASC) VISIBLE,
  INDEX `FKnxe5udnc3akw0n4wcsuogmj2j` (`sender_id` ASC) VISIBLE,
  CONSTRAINT `FKnodylbe3bxj139i2fc2kbu4su`
    FOREIGN KEY (`feedback_id`)
    REFERENCES `prep2getherdb`.`feedback` (`id`),
  CONSTRAINT `FKnxe5udnc3akw0n4wcsuogmj2j`
    FOREIGN KEY (`sender_id`)
    REFERENCES `prep2getherdb`.`users` (`id`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci;


-- -----------------------------------------------------
-- Table `prep2getherdb`.`oauth_account`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `prep2getherdb`.`oauth_account` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `linked_at` DATETIME(6) NOT NULL,
  `user_id` BIGINT NOT NULL,
  `provider` VARCHAR(20) NOT NULL,
  `provider_id` VARCHAR(100) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE INDEX `UKmx2mn80mbibsar6xiwx4pqyho` (`provider` ASC, `provider_id` ASC) VISIBLE,
  INDEX `FKfo0bd94g5i95ufayqbge39p2f` (`user_id` ASC) VISIBLE,
  CONSTRAINT `FKfo0bd94g5i95ufayqbge39p2f`
    FOREIGN KEY (`user_id`)
    REFERENCES `prep2getherdb`.`users` (`id`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci;


-- -----------------------------------------------------
-- Table `prep2getherdb`.`refresh_token`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `prep2getherdb`.`refresh_token` (
  `created_at` DATETIME(6) NOT NULL,
  `expires_at` DATETIME(6) NOT NULL,
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `token` VARCHAR(500) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE INDEX `UKf95ixxe7pa48ryn1awmh2evt7` (`user_id` ASC) VISIBLE,
  UNIQUE INDEX `UKr4k4edos30bx9neoq81mdvwph` (`token` ASC) VISIBLE,
  CONSTRAINT `FKjtx87i0jvq2svedphegvdwcuy`
    FOREIGN KEY (`user_id`)
    REFERENCES `prep2getherdb`.`users` (`id`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci;


-- -----------------------------------------------------
-- Table `prep2getherdb`.`report`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `prep2getherdb`.`report` (
  `created_at` DATETIME(6) NOT NULL,
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `reporter_id` BIGINT NOT NULL,
  `resolved_at` DATETIME(6) NULL DEFAULT NULL,
  `target_id` BIGINT NOT NULL,
  `detail` TEXT NULL DEFAULT NULL,
  `reason` ENUM('ABUSE', 'ETC', 'INAPPROPRIATE', 'SPAM') NOT NULL,
  `status` ENUM('PENDING', 'RESOLVED') NOT NULL,
  `target_type` ENUM('COMMENT', 'POST', 'STUDY_POST', 'STUDY_POST_COMMENT') NOT NULL,
  PRIMARY KEY (`id`),
  INDEX `FKqbhdxqd3ly7fkhly5nrl2j93k` (`reporter_id` ASC) VISIBLE,
  CONSTRAINT `FKqbhdxqd3ly7fkhly5nrl2j93k`
    FOREIGN KEY (`reporter_id`)
    REFERENCES `prep2getherdb`.`users` (`id`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci;


-- -----------------------------------------------------
-- Table `prep2getherdb`.`study`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `prep2getherdb`.`study` (
  `capacity` INT NOT NULL,
  `deleted` BIT(1) NOT NULL COMMENT 'Soft-delete indicator',
  `recruit_end` DATE NULL DEFAULT NULL,
  `recruit_start` DATE NULL DEFAULT NULL,
  `created_at` DATETIME(6) NOT NULL,
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `leader_id` BIGINT NOT NULL,
  `updated_at` DATETIME(6) NULL DEFAULT NULL,
  `title` VARCHAR(200) NOT NULL,
  `description` TEXT NULL DEFAULT NULL,
  `category` ENUM('CERTIFICATE', 'ETC', 'IT_DEVELOPMENT', 'JOB_PREP', 'LANGUAGE') NOT NULL,
  PRIMARY KEY (`id`),
  INDEX `FKl13b57a8t61n6scmw4lhgud7y` (`leader_id` ASC) VISIBLE,
  CONSTRAINT `FKl13b57a8t61n6scmw4lhgud7y`
    FOREIGN KEY (`leader_id`)
    REFERENCES `prep2getherdb`.`users` (`id`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci;


-- -----------------------------------------------------
-- Table `prep2getherdb`.`study_image`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `prep2getherdb`.`study_image` (
  `image_order` INT NOT NULL,
  `created_at` DATETIME(6) NOT NULL,
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `study_id` BIGINT NOT NULL,
  `image_url` VARCHAR(500) NOT NULL,
  `original_file_name` VARCHAR(255) NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  INDEX `FKdxr654qi6t6ws45n0xsxfnt1t` (`study_id` ASC) VISIBLE,
  CONSTRAINT `FKdxr654qi6t6ws45n0xsxfnt1t`
    FOREIGN KEY (`study_id`)
    REFERENCES `prep2getherdb`.`study` (`id`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci;


-- -----------------------------------------------------
-- Table `prep2getherdb`.`study_member`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `prep2getherdb`.`study_member` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `joined_at` DATETIME(6) NOT NULL,
  `study_id` BIGINT NOT NULL,
  `user_id` BIGINT NOT NULL,
  PRIMARY KEY (`id`),
  INDEX `FKxu4jds4ab0mfyrvdxsu60iut` (`study_id` ASC) VISIBLE,
  INDEX `FKa3rdclacg05je5c2fviwed6i8` (`user_id` ASC) VISIBLE,
  CONSTRAINT `FKa3rdclacg05je5c2fviwed6i8`
    FOREIGN KEY (`user_id`)
    REFERENCES `prep2getherdb`.`users` (`id`),
  CONSTRAINT `FKxu4jds4ab0mfyrvdxsu60iut`
    FOREIGN KEY (`study_id`)
    REFERENCES `prep2getherdb`.`study` (`id`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci;


-- -----------------------------------------------------
-- Table `prep2getherdb`.`study_post`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `prep2getherdb`.`study_post` (
  `deleted` BIT(1) NOT NULL COMMENT 'Soft-delete indicator',
  `created_at` DATETIME(6) NOT NULL,
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `study_id` BIGINT NOT NULL,
  `updated_at` DATETIME(6) NOT NULL,
  `user_id` BIGINT NOT NULL,
  `title` VARCHAR(200) NOT NULL,
  `content` TEXT NOT NULL,
  PRIMARY KEY (`id`),
  INDEX `FK1pc0o2m9yw1ety3l0y7koemhq` (`study_id` ASC) VISIBLE,
  INDEX `FKh7fhcw9bg70nd9wlocrgteolj` (`user_id` ASC) VISIBLE,
  CONSTRAINT `FK1pc0o2m9yw1ety3l0y7koemhq`
    FOREIGN KEY (`study_id`)
    REFERENCES `prep2getherdb`.`study` (`id`),
  CONSTRAINT `FKh7fhcw9bg70nd9wlocrgteolj`
    FOREIGN KEY (`user_id`)
    REFERENCES `prep2getherdb`.`users` (`id`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci;


-- -----------------------------------------------------
-- Table `prep2getherdb`.`study_post_comment`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `prep2getherdb`.`study_post_comment` (
  `deleted` BIT(1) NOT NULL COMMENT 'Soft-delete indicator',
  `created_at` DATETIME(6) NOT NULL,
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `study_post_id` BIGINT NOT NULL,
  `updated_at` DATETIME(6) NOT NULL,
  `user_id` BIGINT NOT NULL,
  `content` TEXT NOT NULL,
  PRIMARY KEY (`id`),
  INDEX `FKric9o4jhwvr940uc40uw5t77e` (`study_post_id` ASC) VISIBLE,
  INDEX `FKlyvep84812ch946ujec3pg8q6` (`user_id` ASC) VISIBLE,
  CONSTRAINT `FKlyvep84812ch946ujec3pg8q6`
    FOREIGN KEY (`user_id`)
    REFERENCES `prep2getherdb`.`users` (`id`),
  CONSTRAINT `FKric9o4jhwvr940uc40uw5t77e`
    FOREIGN KEY (`study_post_id`)
    REFERENCES `prep2getherdb`.`study_post` (`id`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci;


-- -----------------------------------------------------
-- Table `prep2getherdb`.`study_post_image`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `prep2getherdb`.`study_post_image` (
  `image_order` INT NOT NULL,
  `created_at` DATETIME(6) NOT NULL,
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `study_post_id` BIGINT NOT NULL,
  `image_url` VARCHAR(500) NOT NULL,
  `original_file_name` VARCHAR(255) NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  INDEX `FKlvnenwxeulxdyspk6pv1uxcfi` (`study_post_id` ASC) VISIBLE,
  CONSTRAINT `FKlvnenwxeulxdyspk6pv1uxcfi`
    FOREIGN KEY (`study_post_id`)
    REFERENCES `prep2getherdb`.`study_post` (`id`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci;


-- -----------------------------------------------------
-- Table `prep2getherdb`.`subscription`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `prep2getherdb`.`subscription` (
  `expired_at` DATETIME(6) NULL DEFAULT NULL,
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `started_at` DATETIME(6) NOT NULL,
  `user_id` BIGINT NOT NULL,
  `status` ENUM('ACTIVE', 'CANCELLED') NOT NULL,
  PRIMARY KEY (`id`),
  INDEX `FKqwd9pkhbsmapx9poug5wnnpkc` (`user_id` ASC) VISIBLE,
  CONSTRAINT `FKqwd9pkhbsmapx9poug5wnnpkc`
    FOREIGN KEY (`user_id`)
    REFERENCES `prep2getherdb`.`users` (`id`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci;


SET SQL_MODE=@OLD_SQL_MODE;
SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS;
SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS;
