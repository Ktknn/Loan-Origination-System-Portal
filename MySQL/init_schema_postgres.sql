-- ============================================================
-- LOS Database Schema — PostgreSQL
-- ============================================================

-- ============================================================
-- DROP (đúng thứ tự FK)
-- ============================================================
DROP TABLE IF EXISTS application_assessment CASCADE;
DROP TABLE IF EXISTS otp CASCADE;
DROP TABLE IF EXISTS "Loan_Applications" CASCADE;
DROP TABLE IF EXISTS "Loan_Products" CASCADE;
DROP TABLE IF EXISTS users CASCADE;
DROP TABLE IF EXISTS policy CASCADE;
DROP TABLE IF EXISTS refresh_tokens CASCADE;

-- ============================================================
-- 1. users
-- ============================================================
CREATE TABLE users (
    user_id         VARCHAR(36)     NOT NULL PRIMARY KEY,
    full_name       VARCHAR(255),
    gender          VARCHAR(10)     CHECK (gender IN ('Nam','Nu')),
    cccd            VARCHAR(12)     UNIQUE,
    email           VARCHAR(255)    UNIQUE,
    "passwordHash"  VARCHAR(255),
    phone_number    VARCHAR(20),
    birthdate       DATE,
    address         VARCHAR(255),
    city            VARCHAR(100),
    district        VARCHAR(100),
    ward            VARCHAR(100),
    status          VARCHAR(10)     DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','INACTIVE')),
    created_at      TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- 2. Loan_Products
-- ============================================================
CREATE TABLE "Loan_Products" (
    loan_product_id VARCHAR(36)     NOT NULL PRIMARY KEY,
    name            VARCHAR(255),
    amount          DECIMAL(10, 2),
    term            INT,
    interest_rate   DECIMAL(10, 2),
    status          VARCHAR(20)     DEFAULT 'active',
    created_at      TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- 3. Loan_Applications
-- ============================================================
CREATE TABLE "Loan_Applications" (
    loan_application_id         VARCHAR(36)     NOT NULL PRIMARY KEY,
    user_id                     VARCHAR(36)     NOT NULL,
    loan_product_id             VARCHAR(36),
    amount                      DECIMAL(10, 2),
    term                        INT,
    approve_interest_rate       DECIMAL(10, 2),
    purpose                     VARCHAR(50)     DEFAULT 'VAY_TIEU_DUNG_KHAC',
    income_range                VARCHAR(50),
    occupation                  VARCHAR(50)     DEFAULT 'KHAC',
    reference_contact_name_1    VARCHAR(255),
    reference_contact_phone_1   VARCHAR(20),
    reference_contact_name_2    VARCHAR(255),
    reference_contact_phone_2   VARCHAR(20),
    submitted_at                TIMESTAMP,
    decision_date               TIMESTAMP,
    status                      VARCHAR(20)     DEFAULT 'DRAFT',
    created_at                  TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    updated_at                  TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_loan_app_user    FOREIGN KEY (user_id)         REFERENCES users(user_id),
    CONSTRAINT fk_loan_app_product FOREIGN KEY (loan_product_id) REFERENCES "Loan_Products"(loan_product_id)
);

-- ============================================================
-- 4. application_assessment
-- ============================================================
CREATE TABLE application_assessment (
    assessment_id           VARCHAR(36)     NOT NULL PRIMARY KEY,
    loan_application_id     VARCHAR(36)     NOT NULL UNIQUE,
    total_score             INT,
    income_score            INT,
    occupation_score        INT,
    purpose_score           INT,
    loan_suitability_score  INT,
    dti                     DECIMAL(10, 2),
    status                  VARCHAR(20)     DEFAULT 'PENDING',
    created_at              TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_assessment_loan_app FOREIGN KEY (loan_application_id) REFERENCES "Loan_Applications"(loan_application_id)
);

-- ============================================================
-- 5. otp
-- ============================================================
CREATE TABLE otp (
    otp_id          VARCHAR(36)     NOT NULL PRIMARY KEY,
    user_id         VARCHAR(36),
    otp             VARCHAR(6),
    recipient_email VARCHAR(255),
    sent_at         TIMESTAMP,
    expires_at      TIMESTAMP,
    verified_at     TIMESTAMP,
    status          VARCHAR(20)     DEFAULT 'PENDING',
    created_at      TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_otp_user FOREIGN KEY (user_id) REFERENCES users(user_id)
);

-- ============================================================
-- 6. policy
-- ============================================================
CREATE TABLE policy (
    policy_id                   VARCHAR(36)     NOT NULL PRIMARY KEY,
    policy_name                 VARCHAR(255),
    max_loan_amount             DECIMAL(15, 2),
    min_credit_score            INT,
    required_monthly_income     DECIMAL(15, 2),
    dti_threshold               DECIMAL(10, 2),
    active                      VARCHAR(10)     DEFAULT 'active',
    created_at                  TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    updated_at                  TIMESTAMP       DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- 7. refresh_tokens
-- ============================================================
CREATE TABLE refresh_tokens (
    id          VARCHAR(36)     NOT NULL PRIMARY KEY,
    user_id     VARCHAR(36)     NOT NULL,
    token       VARCHAR(512)    NOT NULL UNIQUE,
    expires_at  TIMESTAMP       NOT NULL,
    created_at  TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_refresh_user FOREIGN KEY (user_id) REFERENCES users(user_id)
);
