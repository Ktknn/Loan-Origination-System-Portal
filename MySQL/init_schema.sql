-- ============================================================
-- LOS Database Schema
-- ============================================================

CREATE DATABASE IF NOT EXISTS `los`
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE `los`;

-- ============================================================
-- DROP
-- ============================================================
DROP TABLE IF EXISTS `application_assessment`;
DROP TABLE IF EXISTS `otp`;
DROP TABLE IF EXISTS `Loan_Applications`;
DROP TABLE IF EXISTS `Loan_Products`;
DROP TABLE IF EXISTS `users`;
DROP TABLE IF EXISTS `policy`;

-- ============================================================
-- 1. users  (entity: User.java)
-- ============================================================
CREATE TABLE `users` (
    `user_id`       VARCHAR(36)     NOT NULL PRIMARY KEY,
    `full_name`     VARCHAR(255),
    `gender`        ENUM('Nam','Nu'),
    `cccd`          VARCHAR(12)     UNIQUE,
    `email`         VARCHAR(255)    UNIQUE,
    `passwordHash`  VARCHAR(255),
    `phone_number`  VARCHAR(20),
    `birthdate`     DATE,
    `address`       VARCHAR(255),
    `city`          VARCHAR(100),
    `district`      VARCHAR(100),
    `ward`          VARCHAR(100),
    `status`        ENUM('ACTIVE','INACTIVE') DEFAULT 'ACTIVE',
    `created_at`    DATETIME        DEFAULT CURRENT_TIMESTAMP,
    `updated_at`    DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 2. Loan_Products  (entity: LoanProduct.java)
-- ============================================================
CREATE TABLE `Loan_Products` (
    `loan_product_id`   VARCHAR(36)     NOT NULL PRIMARY KEY,
    `name`              VARCHAR(255),
    `amount`            DECIMAL(10, 2),
    `term`              INT,
    `interest_rate`     DECIMAL(10, 2),
    `status`            VARCHAR(20)     DEFAULT 'active',
    `created_at`        DATETIME        DEFAULT CURRENT_TIMESTAMP,
    `updated_at`        DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 3. Loan_Applications  (entity: LoanApplication.java)
-- ============================================================
CREATE TABLE `Loan_Applications` (
    `loan_application_id`       VARCHAR(36)     NOT NULL PRIMARY KEY,
    `user_id`                   VARCHAR(36)     NOT NULL,
    `loan_product_id`           VARCHAR(36),
    `amount`                    DECIMAL(10, 2),
    `term`                      INT,
    `approve_interest_rate`     DECIMAL(10, 2),
    `purpose`                   ENUM(
                                    'MUA_PHUONG_TIEN',
                                    'MUA_SAM_DO_DUNG',
                                    'HOC_TAP',
                                    'CHUA_BENH',
                                    'DU_LICH',
                                    'VAY_TIEU_DUNG_KHAC'
                                ) DEFAULT 'VAY_TIEU_DUNG_KHAC',
    `income_range`              ENUM(
                                    'DUOI_10_TRIEU',
                                    'TU_10_DEN_20_TRIEU',
                                    'TU_20_DEN_30_TRIEU',
                                    'TREN_30_TRIEU'
                                ),
    `occupation`                ENUM(
                                    'CAN_BO_CONG_CHUC',
                                    'HUU_TRI',
                                    'SINH_VIEN',
                                    'KINH_DOANH_TU_DO',
                                    'NHAN_VIEN_CONG_TY',
                                    'KHAC'
                                ) DEFAULT 'KHAC',
    `reference_contact_name_1`  VARCHAR(255),
    `reference_contact_phone_1` VARCHAR(20),
    `reference_contact_name_2`  VARCHAR(255),
    `reference_contact_phone_2` VARCHAR(20),
    `submitted_at`              DATETIME,
    `decision_date`             DATETIME,
    `status`                    ENUM('DRAFT','SUBMITTED','PENDING','APPROVED','REJECTED') DEFAULT 'DRAFT',
    `created_at`                DATETIME        DEFAULT CURRENT_TIMESTAMP,
    `updated_at`                DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_loan_app_user`    FOREIGN KEY (`user_id`)         REFERENCES `users`(`user_id`),
    CONSTRAINT `fk_loan_app_product` FOREIGN KEY (`loan_product_id`) REFERENCES `Loan_Products`(`loan_product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 4. application_assessment  (entity: ApplicationAssessment.java)
-- ============================================================
CREATE TABLE `application_assessment` (
    `assessment_id`         VARCHAR(36)     NOT NULL PRIMARY KEY,
    `loan_application_id`   VARCHAR(36)     NOT NULL UNIQUE,
    `total_score`           INT,
    `income_score`          INT,
    `occupation_score`      INT,
    `purpose_score`         INT,
    `loan_suitability_score` INT,
    `dti`                   DECIMAL(10, 2),
    `status`                ENUM('APPROVED','REJECTED','PENDING') DEFAULT 'PENDING',
    `created_at`            DATETIME        DEFAULT CURRENT_TIMESTAMP,
    `updated_at`            DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_assessment_loan_app` FOREIGN KEY (`loan_application_id`) REFERENCES `Loan_Applications`(`loan_application_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 5. otp  (entity: OtpRecord.java)
-- ============================================================
CREATE TABLE `otp` (
    `otp_id`            VARCHAR(36)     NOT NULL PRIMARY KEY,
    `user_id`           VARCHAR(36),
    `otp`               VARCHAR(6),
    `recipient_email`   VARCHAR(255),
    `sent_at`           DATETIME,
    `expires_at`        DATETIME,
    `verified_at`       DATETIME,
    `status`            ENUM('PENDING','VERIFIED','EXPIRED','REVOKED') DEFAULT 'PENDING',
    `created_at`        DATETIME        DEFAULT CURRENT_TIMESTAMP,
    `updated_at`        DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_otp_user` FOREIGN KEY (`user_id`) REFERENCES `users`(`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 6. policy  (entity: Policy.java)
-- ============================================================
CREATE TABLE `policy` (
    `policy_id`                         VARCHAR(36)     NOT NULL PRIMARY KEY,
    `max_loan_amount`                   DECIMAL(15, 2),
    `min_credit_score`                  INT,
    `required_monthly_income`           DECIMAL(15, 2),
    `dti_threshold`                     DECIMAL(10, 2),
    `active`                            VARCHAR(10)     DEFAULT 'active',
    `created_at`                        DATETIME        DEFAULT CURRENT_TIMESTAMP,
    `updated_at`                        DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
