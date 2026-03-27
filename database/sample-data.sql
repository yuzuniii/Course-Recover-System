-- =========================
-- SEED DATA
-- =========================
USE crs_system;

-- --------------------------------------------------------
-- Roles
-- --------------------------------------------------------
INSERT IGNORE INTO roles (role_name)
VALUES ('ADMIN'), ('ACADEMIC_OFFICER');

-- --------------------------------------------------------
-- Users  (passwords are jBCrypt hashes)
-- admin   → Admin@123
-- officer → Officer@123
-- --------------------------------------------------------
INSERT IGNORE INTO users (user_id, username, password, full_name, email, status)
VALUES
(1, 'admin',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
    'Course Administrator', 'admin@crs.local',   'ACTIVE'),
(2, 'officer',
    '$2a$10$8K1p/a0dR1xqM4K3e5v2AeqkBvY8.nMj5g1t2OeP3s7u0yHmKlXQi',
    'Academic Officer',    'officer@crs.local',  'ACTIVE');

INSERT IGNORE INTO user_roles (user_id, role_id)
VALUES (1, 1), (2, 2);

-- --------------------------------------------------------
-- Students  (5 students, mix of Year 1 and Year 2)
--
-- Stored cgpa is a snapshot; live CGPA is recomputed by
-- CGPACalculator from student_course_results at runtime.
--
-- Scenarios:
--   101 Alex Tan    → Scenario 2: NOT ELIGIBLE — low CGPA (1.65 < 2.0)
--   102 Brenda Lim  → Scenario 1: ELIGIBLE      — CGPA 3.43, 0 failed
--   103 Chris Wong  → Scenario 5: Mixed grades   — NOT ELIGIBLE (CGPA 1.68)
--   104 Diana Lee   → Scenario 3: NOT ELIGIBLE   — 4 failed courses (> 3)
--   105 Edward Ng   → Scenario 4: Borderline ELIGIBLE — CGPA exactly 2.0
-- --------------------------------------------------------
INSERT IGNORE INTO students
    (student_id, student_code, name, programme, year_of_study, current_semester, cgpa)
VALUES
(101, '2025A1234', 'Alex Tan',    'Bachelor of Computer Science',       1, 1, 1.65),
(102, '2025A1235', 'Brenda Lim',  'Bachelor of Computer Science',       1, 2, 3.43),
(103, '2025A1236', 'Chris Wong',  'Bachelor of Information Technology', 2, 1, 1.68),
(104, '2025A1237', 'Diana Lee',   'Bachelor of Information Technology', 2, 1, 0.46),
(105, '2025A1238', 'Edward Ng',   'Bachelor of Computer Science',       1, 1, 2.00);

-- --------------------------------------------------------
-- Courses  (5 courses, all 3 credit hours)
-- --------------------------------------------------------
INSERT IGNORE INTO courses (course_id, course_code, course_name, credit_hours)
VALUES
(201, 'CS201', 'Data Structures',      3),
(202, 'CS205', 'Database Systems',     3),
(203, 'CS210', 'Software Engineering', 3),
(204, 'CS301', 'Algorithms',           3),
(205, 'CS302', 'Operating Systems',    3);

-- --------------------------------------------------------
-- Student course results
--
-- Grade → grade_point mapping (must match exactly):
--   A=4.0  A-=3.7  B+=3.3  B=3.0  B-=2.7
--   C+=2.3 C=2.0   C-=1.7  F=0.0
--
-- --- Scenario 2 — Alex Tan (101): NOT ELIGIBLE, low CGPA ---
--   CGPA = (0.0×3 + 3.3×3) / 6 = 9.9 / 6 = 1.65  <  2.0
--   Failed: 1  (≤ 3 but CGPA disqualifies)
--
-- --- Scenario 1 — Brenda Lim (102): ELIGIBLE ---
--   CGPA = (4.0×3 + 3.3×3 + 3.0×3) / 9 = 30.9 / 9 = 3.43
--   Failed: 0
--
-- --- Scenario 5 — Chris Wong (103): Mixed grades, NOT ELIGIBLE ---
--   CGPA = (0.0×3 + 0.0×3 + 3.0×3 + 3.7×3) / 12 = 20.1 / 12 = 1.68
--   Failed: 2  (CGPA < 2.0 disqualifies)
--
-- --- Scenario 3 — Diana Lee (104): NOT ELIGIBLE, > 3 failed ---
--   CGPA = (0.0×3 ×4 + 2.3×3) / 15 = 6.9 / 15 = 0.46
--   Failed: 4  (exceeds maximum of 3)
--
-- --- Scenario 4 — Edward Ng (105): Borderline ELIGIBLE, CGPA = 2.0 ---
--   CGPA = (4.0×3 + 0.0×3 + 2.0×3) / 9 = 18.0 / 9 = 2.0  (exactly)
--   Failed: 1  (≤ 3, CGPA meets minimum)
-- --------------------------------------------------------
INSERT IGNORE INTO student_course_results
    (student_id, course_id, semester, grade, grade_point, marks, attempt_number, status)
VALUES
-- Alex Tan (101) — low CGPA scenario
(101, 201, '2025/1', 'F',   0.0, 38.0, 1, 'FAILED'),
(101, 202, '2025/1', 'B+',  3.3, 74.0, 1, 'PASSED'),

-- Brenda Lim (102) — fully eligible
(102, 201, '2025/1', 'A',   4.0, 90.0, 1, 'PASSED'),
(102, 202, '2025/1', 'B+',  3.3, 76.0, 1, 'PASSED'),
(102, 203, '2025/1', 'B',   3.0, 70.0, 1, 'PASSED'),

-- Chris Wong (103) — mixed grades
(103, 201, '2025/1', 'B',   3.0, 72.0, 1, 'PASSED'),
(103, 202, '2025/1', 'F',   0.0, 35.0, 1, 'FAILED'),
(103, 203, '2025/1', 'F',   0.0, 42.0, 1, 'FAILED'),
(103, 204, '2025/1', 'A-',  3.7, 85.0, 1, 'PASSED'),

-- Diana Lee (104) — 4 failed courses (> 3 limit)
(104, 201, '2025/1', 'F',   0.0, 30.0, 1, 'FAILED'),
(104, 202, '2025/1', 'F',   0.0, 25.0, 1, 'FAILED'),
(104, 203, '2025/1', 'F',   0.0, 38.0, 1, 'FAILED'),
(104, 204, '2025/1', 'F',   0.0, 40.0, 1, 'FAILED'),
(104, 205, '2025/1', 'C+',  2.3, 58.0, 1, 'PASSED'),

-- Edward Ng (105) — borderline CGPA = 2.0 exactly
(105, 201, '2025/1', 'A',   4.0, 91.0, 1, 'PASSED'),
(105, 202, '2025/1', 'F',   0.0, 48.0, 1, 'FAILED'),
(105, 203, '2025/1', 'C',   2.0, 60.0, 1, 'PASSED');

-- --------------------------------------------------------
-- Recovery plans
-- --------------------------------------------------------
INSERT IGNORE INTO recovery_plans
    (plan_id, student_id, course_id, attempt_number, status, recommendation,
     start_date, end_date)
VALUES
(301, 101, 201, 1, 'ACTIVE',
    'Attend weekly revision sessions and resit the final exam.',
    '2026-03-01', '2026-04-15'),
(302, 103, 203, 1, 'ACTIVE',
    'Complete all missing assessment components.',
    '2026-02-15', '2026-04-30'),
(303, 104, 201, 1, 'ACTIVE',
    'Intensive tutoring recommended before any resit attempt.',
    '2026-03-10', '2026-05-01');

-- --------------------------------------------------------
-- Milestones
-- --------------------------------------------------------
INSERT IGNORE INTO recovery_milestones
    (milestone_id, plan_id, title, description, due_date, status)
VALUES
(401, 301, 'Week 1 Review',
     'Review lecture topics 1 to 4.',
     '2026-03-10', 'COMPLETED'),
(402, 301, 'Lecturer Meeting',
     'Meet lecturer for progress feedback.',
     '2026-03-18', 'PENDING'),
(403, 302, 'Assessment Reattempt',
     'Sit for full assessment reattempt.',
     '2026-03-05', 'OVERDUE'),
(404, 303, 'Diagnostic Test',
     'Sit diagnostic test to identify weak areas.',
     '2026-03-20', 'PENDING');

-- --------------------------------------------------------
-- Eligibility records  (pre-checked snapshots)
-- --------------------------------------------------------
INSERT IGNORE INTO eligibility_records
    (student_id, semester, year_of_study, cgpa, failed_count,
     is_eligible, reason, checked_by)
VALUES
(101, 1, 1, 1.65, 1, FALSE,
    'CGPA below minimum requirement of 2.0.', 1),
(102, 2, 1, 3.43, 0, TRUE,
    'Meets all progression requirements', 1),
(103, 1, 2, 1.68, 2, FALSE,
    'CGPA below minimum requirement of 2.0.', 1),
(104, 1, 2, 0.46, 4, FALSE,
    'CGPA below minimum requirement of 2.0. Exceeded maximum of 3 failed courses.', 1),
(105, 1, 1, 2.00, 1, TRUE,
    'Meets all progression requirements', 1);

-- --------------------------------------------------------
-- Failed components (linked via subquery to result_id)
-- --------------------------------------------------------
-- Alex Tan (101) failed CS201 (course_id=201)
INSERT INTO failed_components (result_id, component_name, component_score, pass_required)
SELECT result_id, 'Assignment 1', 32.0, 50.0
FROM student_course_results WHERE student_id=101 AND course_id=201 AND grade='F' LIMIT 1;

INSERT INTO failed_components (result_id, component_name, component_score, pass_required)
SELECT result_id, 'Mid-Term Exam', 28.0, 40.0
FROM student_course_results WHERE student_id=101 AND course_id=201 AND grade='F' LIMIT 1;

INSERT INTO failed_components (result_id, component_name, component_score, pass_required)
SELECT result_id, 'Final Exam', 35.0, 60.0
FROM student_course_results WHERE student_id=101 AND course_id=201 AND grade='F' LIMIT 1;

-- Chris Wong (103) failed course_id=202
INSERT INTO failed_components (result_id, component_name, component_score, pass_required)
SELECT result_id, 'Assignment 1', 15.0, 40.0
FROM student_course_results WHERE student_id=103 AND course_id=202 AND grade='F' LIMIT 1;

INSERT INTO failed_components (result_id, component_name, component_score, pass_required)
SELECT result_id, 'Final Exam', 20.0, 60.0
FROM student_course_results WHERE student_id=103 AND course_id=202 AND grade='F' LIMIT 1;

-- Chris Wong (103) failed course_id=203 (CS210)
INSERT INTO failed_components (result_id, component_name, component_score, pass_required)
SELECT result_id, 'Assignment 2', 18.0, 40.0
FROM student_course_results WHERE student_id=103 AND course_id=203 AND grade='F' LIMIT 1;

INSERT INTO failed_components (result_id, component_name, component_score, pass_required)
SELECT result_id, 'Final Exam', 25.0, 60.0
FROM student_course_results WHERE student_id=103 AND course_id=203 AND grade='F' LIMIT 1;

-- Diana Lee (104) failed course_id=201
INSERT INTO failed_components (result_id, component_name, component_score, pass_required)
SELECT result_id, 'Assignment 1', 10.0, 40.0
FROM student_course_results WHERE student_id=104 AND course_id=201 AND grade='F' LIMIT 1;

INSERT INTO failed_components (result_id, component_name, component_score, pass_required)
SELECT result_id, 'Final Exam', 20.0, 60.0
FROM student_course_results WHERE student_id=104 AND course_id=201 AND grade='F' LIMIT 1;

-- Diana Lee (104) failed course_id=202
INSERT INTO failed_components (result_id, component_name, component_score, pass_required)
SELECT result_id, 'Mid-Term Exam', 12.0, 40.0
FROM student_course_results WHERE student_id=104 AND course_id=202 AND grade='F' LIMIT 1;

INSERT INTO failed_components (result_id, component_name, component_score, pass_required)
SELECT result_id, 'Final Exam', 15.0, 60.0
FROM student_course_results WHERE student_id=104 AND course_id=202 AND grade='F' LIMIT 1;

-- Diana Lee (104) failed course_id=203
INSERT INTO failed_components (result_id, component_name, component_score, pass_required)
SELECT result_id, 'Assignment 1', 20.0, 40.0
FROM student_course_results WHERE student_id=104 AND course_id=203 AND grade='F' LIMIT 1;

INSERT INTO failed_components (result_id, component_name, component_score, pass_required)
SELECT result_id, 'Final Exam', 18.0, 60.0
FROM student_course_results WHERE student_id=104 AND course_id=203 AND grade='F' LIMIT 1;

-- Diana Lee (104) failed course_id=204
INSERT INTO failed_components (result_id, component_name, component_score, pass_required)
SELECT result_id, 'Assignment 2', 22.0, 40.0
FROM student_course_results WHERE student_id=104 AND course_id=204 AND grade='F' LIMIT 1;

INSERT INTO failed_components (result_id, component_name, component_score, pass_required)
SELECT result_id, 'Final Exam', 23.0, 60.0
FROM student_course_results WHERE student_id=104 AND course_id=204 AND grade='F' LIMIT 1;

-- --------------------------------------------------------
-- Imported CSV students and courses
-- --------------------------------------------------------
source database/csv-import-data.sql
