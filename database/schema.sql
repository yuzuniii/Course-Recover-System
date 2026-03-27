-- =========================
-- DATABASE
-- =========================
CREATE DATABASE IF NOT EXISTS crs_system;
USE crs_system;

-- =========================
-- ROLES & USERS
-- =========================
CREATE TABLE roles (
    role_id   INT AUTO_INCREMENT PRIMARY KEY,
    role_name VARCHAR(50) UNIQUE NOT NULL
);

CREATE TABLE users (
    user_id    INT AUTO_INCREMENT PRIMARY KEY,
    username   VARCHAR(50)  UNIQUE NOT NULL,
    password   VARCHAR(255) NOT NULL,
    full_name  VARCHAR(150),
    email      VARCHAR(100),
    status     VARCHAR(20)  DEFAULT 'ACTIVE',
    last_login TIMESTAMP    NULL DEFAULT NULL,  -- NEW COLUMN ADDED HERE
    created_at TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE user_roles (
    user_id INT,
    role_id INT,
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES users(user_id)   ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES roles(role_id)   ON DELETE CASCADE
);

-- =========================
-- STUDENTS & PROGRAMMES
-- =========================
CREATE TABLE students (
    student_id       INT AUTO_INCREMENT PRIMARY KEY,
    student_code     VARCHAR(50) UNIQUE,
    name             VARCHAR(100),
    programme        VARCHAR(100),
    email            VARCHAR(100),
    year_of_study    INT,
    current_semester INT          DEFAULT 1,
    cgpa             DECIMAL(3,2) DEFAULT 0.00,
    created_at       TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

-- =========================
-- COURSES
-- =========================
CREATE TABLE courses (
    course_id    INT AUTO_INCREMENT PRIMARY KEY,
    course_code  VARCHAR(20) UNIQUE,
    course_name  VARCHAR(100),
    credit_hours INT,
    instructor   VARCHAR(100)
);

-- =========================
-- STUDENT COURSE RESULTS
-- =========================
CREATE TABLE student_course_results (
    result_id      INT AUTO_INCREMENT PRIMARY KEY,
    student_id     INT,
    course_id      INT,
    semester       VARCHAR(20),
    grade          VARCHAR(5),
    grade_point    DECIMAL(3,2),
    marks          DECIMAL(5,2),
    attempt_number INT,
    status         VARCHAR(20),
    FOREIGN KEY (student_id) REFERENCES students(student_id),
    FOREIGN KEY (course_id)  REFERENCES courses(course_id)
);

-- =========================
-- FAILED COMPONENTS
-- =========================
CREATE TABLE failed_components (
    component_id    INT AUTO_INCREMENT PRIMARY KEY,
    result_id       INT,
    component_name  VARCHAR(100),
    component_score DECIMAL(5,2),
    pass_required   DECIMAL(5,2),
    FOREIGN KEY (result_id) REFERENCES student_course_results(result_id) ON DELETE CASCADE
);

-- =========================
-- ENROLLMENT
-- =========================
CREATE TABLE enrollments (
    enrollment_id INT AUTO_INCREMENT PRIMARY KEY,
    student_id    INT,
    course_id     INT,
    status        VARCHAR(20) DEFAULT 'PENDING',
    enrolled_at   TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (student_id) REFERENCES students(student_id),
    FOREIGN KEY (course_id)  REFERENCES courses(course_id)
);

-- =========================
-- RECOVERY PLAN
-- =========================
CREATE TABLE recovery_plans (
    plan_id        INT AUTO_INCREMENT PRIMARY KEY,
    student_id     INT,
    course_id      INT,
    attempt_number INT,
    status         VARCHAR(20) DEFAULT 'ACTIVE',
    recommendation TEXT,
    start_date     DATE,
    end_date       DATE,
    created_at     TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (student_id) REFERENCES students(student_id),
    FOREIGN KEY (course_id)  REFERENCES courses(course_id)
);

-- =========================
-- MILESTONES
-- =========================
CREATE TABLE recovery_milestones (
    milestone_id INT AUTO_INCREMENT PRIMARY KEY,
    plan_id      INT,
    title        VARCHAR(150),
    description  TEXT,
    due_date     DATE,
    status       VARCHAR(20) DEFAULT 'PENDING',
    created_at   TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (plan_id) REFERENCES recovery_plans(plan_id) ON DELETE CASCADE
);

-- =========================
-- PROGRESS TRACKING
-- =========================
CREATE TABLE recovery_progress (
    progress_id    INT AUTO_INCREMENT PRIMARY KEY,
    milestone_id   INT,
    progress_note  TEXT,
    updated_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (milestone_id) REFERENCES recovery_milestones(milestone_id) ON DELETE CASCADE
);

-- =========================
-- RECOVERY RECOMMENDATIONS
-- =========================
CREATE TABLE recovery_recommendations (
    rec_id         INT AUTO_INCREMENT PRIMARY KEY,
    plan_id        INT          NOT NULL,
    recommendation TEXT         NOT NULL,
    added_by       INT,
    added_at       TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (plan_id)  REFERENCES recovery_plans(plan_id) ON DELETE CASCADE,
    FOREIGN KEY (added_by) REFERENCES users(user_id)
);

-- =========================
-- ELIGIBILITY RECORDS
-- =========================
CREATE TABLE eligibility_records (
    eligibility_id INT AUTO_INCREMENT PRIMARY KEY,
    student_id     INT NOT NULL,
    semester       INT NOT NULL,
    year_of_study  INT NOT NULL,
    cgpa           DECIMAL(4,2),
    failed_count   INT,
    is_eligible    BOOLEAN DEFAULT FALSE,
    reason         VARCHAR(255),
    checked_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    checked_by     INT,
    FOREIGN KEY (student_id) REFERENCES students(student_id),
    FOREIGN KEY (checked_by) REFERENCES users(user_id)
);

-- =========================
-- AUDIT LOG
-- =========================
CREATE TABLE audit_logs (
    log_id      INT AUTO_INCREMENT PRIMARY KEY,
    user_id     INT,
    action      VARCHAR(100),
    entity_type VARCHAR(50),
    entity_id   INT,
    description TEXT,
    logged_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id)
);

-- =========================
-- EMAIL NOTIFICATIONS
-- =========================
CREATE TABLE email_notifications (
    email_id  INT AUTO_INCREMENT PRIMARY KEY,
    recipient VARCHAR(100),
    subject   VARCHAR(255),
    message   TEXT,
    status    VARCHAR(20) DEFAULT 'SENT',
    sent_at   TIMESTAMP   DEFAULT CURRENT_TIMESTAMP
);

-- =========================
-- RECOVERY PLAN COMPONENTS
-- =========================
CREATE TABLE IF NOT EXISTS recovery_plan_components (
    rpc_id       INT AUTO_INCREMENT PRIMARY KEY,
    plan_id      INT NOT NULL,
    component_id INT NOT NULL,
    status       VARCHAR(20) DEFAULT 'PENDING',
    new_marks    DECIMAL(5,2),
    updated_at   TIMESTAMP,
    FOREIGN KEY (plan_id)      REFERENCES recovery_plans(plan_id)      ON DELETE CASCADE,
    FOREIGN KEY (component_id) REFERENCES failed_components(component_id) ON DELETE CASCADE
);
