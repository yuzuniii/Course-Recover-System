# Course Recovery System (CRS)

A Jakarta EE enterprise web application for managing student academic recovery 
plans at higher education institutions. Built as a Year 3 Semester 1 Enterprise 
Programming and Design Architecture (EPDA) assignment at Asia Pacific University.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Platform | Jakarta EE 10 (Payara 7) |
| UI Framework | JSF 4.0 + PrimeFaces 14 |
| Business Tier | @Stateless EJB Session Beans |
| Database | MySQL 8 (crs_system) |
| Data Access | Raw JDBC (no ORM) |
| Password Hashing | jBCrypt |
| Email | JavaMail |
| Build Tool | Maven (WAR packaging) |
| Testing | JUnit 5 |

---

## Project Structure
```
src/main/java/com/epda/crs/
├── bean/        CDI managed beans (one per page)
├── config/      JDBC connection utility
├── dao/         Raw JDBC data access layer
├── dto/         Data transfer objects
├── enums/       Shared enumerations
├── exception/   Custom exceptions
├── filter/      Authentication filter
├── model/       Plain Java domain entities
├── service/     @Stateless EJB business services
└── util/        CGPA calculator and email utility

src/main/webapp/
├── WEB-INF/     web.xml, faces-config.xml, beans.xml
├── pages/       JSF pages and JSP report page
└── css/         Stylesheet

database/
├── schema.sql        Full database schema (15 tables)
└── sample-data.sql   Test data for demo
```

---

## Database Setup

MySQL must be running on localhost:3306.
```bash
mysql -u root -padmin -e "DROP DATABASE crs_system; CREATE DATABASE crs_system;"
mysql -u root -padmin crs_system < database/schema.sql
mysql -u root -padmin crs_system < database/sample-data.sql
```

---

## Build and Run
```bash
# Compile
mvn clean compile

# Run tests
mvn test

# Build WAR
mvn clean package -DskipTests
```

Deploy `target/course-recovery-system.war` to Payara 7 autodeploy directory.

Access at: `http://localhost:8080/course-recovery-system/`

---

## Demo Credentials

| Username | Password | Role |
|---|---|---|
| admin | admin123 | Course Administrator |
| officer | officer123 | Academic Officer |

---

## System Modules

### Module 1 — Authentication & User Management
Handles login, logout, password reset, user account creation, role assignment, 
and account deactivation. Role-based access control enforced via AuthFilter on 
all pages under /pages/*.

### Module 2 — Student & Academic Data
Manages student records, course enrollments, and academic results. Foundation 
data used by eligibility checks and recovery planning.

### Module 3 — Eligibility & Enrolment
Calculates student CGPA using the formula:
```
CGPA = Σ(gradePoint × creditHours) / Σ(creditHours)
```

Eligibility rules:
- CGPA must be >= 2.0
- Failed courses must be <= 3

Provides a human readable reason for every eligibility decision. Automatically 
enrols eligible students upon confirmation.

### Module 4 — Course Recovery Plan
Manages recovery plans with milestone tracking and recommendations. Enforces a 
strict 3-attempt policy:
- Attempt 1 → Full course recovery (all components)
- Attempt 2 → Failed components only (resit or resubmit)
- Attempt 3 → All components again (full resit)

### Module 5 — Academic Reporting
Generates semester and yearly academic performance reports per student. Includes 
CGPA summary, course results, and grade breakdown. Output rendered via JSP/JSTL 
report page.

### Module 6 — Email Notifications
Automated emails triggered by account creation, password reset, recovery plan 
creation, eligibility results, and report generation.

### Module 7 — Dashboard Analytics ⭐ Additional Feature
Real-time system overview showing total students, students under recovery, 
eligible count, not eligible count, and overdue milestones count.

### Module 8 — Audit Log ⭐ Additional Feature
Tracks every mutating system action with timestamp, actor, action type, entity 
type, entity ID, and description. Accessible by Course Administrator only.

---

## Role Permissions

| Feature | Course Administrator | Academic Officer |
|---|---|---|
| Login / Logout | ✅ | ✅ |
| View Students | ✅ | ✅ |
| Eligibility Check | ✅ | ✅ |
| Enrolment | ✅ | ✅ |
| Recovery Plans | ✅ | ✅ |
| Academic Reports | ✅ | ✅ |
| Dashboard | ✅ | ✅ |
| User Management | ✅ | ❌ |
| Audit Log | ✅ | ❌ |

---

## Business Rules

### CGPA Calculation
```
CGPA = Σ(gradePoint × creditHours) / Σ(creditHours)
```

| Grade | Points |
|---|---|
| A | 4.0 |
| A- | 3.7 |
| B+ | 3.3 |
| B | 3.0 |
| B- | 2.7 |
| C+ | 2.3 |
| C | 2.0 |
| C- | 1.7 |
| F | 0.0 |

### Recovery Attempt Policy
- Maximum 3 attempts per course per student
- Attempt 1 → Full course recovery (all components)
- Attempt 2 → Failed components only (resit or resubmit)
- Attempt 3 → All components again (full resit)
- Beyond 3 attempts → ValidationException thrown, plan creation blocked

### Exception Strategy
ValidationException is thrown before any database write to ensure invalid 
operations never reach the persistence layer. All exceptions are caught at 
the bean layer and presented to the user via FacesMessage.

---

## Build Progress

### ✅ Phase 1 — Foundation (Complete)
- All 4 enum classes
- All 7 model classes
- All 3 DTO classes
- AuthenticationException and ValidationException
- DBConnection JDBC utility
- CGPACalculator static utility
- EmailUtil JavaMail wrapper
- web.xml, faces-config.xml, beans.xml configured

### ✅ Phase 2 — Data Access Layer (Complete)
- UserDAO — login, CRUD, account status
- StudentDAO — CRUD, search by code
- CourseDAO — list, search
- ResultDAO — grades, CGPA data, semester filtering
- EligibilityDAO — save and retrieve eligibility records
- EnrollmentDAO — enrol eligible students
- RecoveryDAO — plans, status, recommendations
- MilestoneDAO — milestones, status tracking
- RecoveryRecommendationDAO — recommendations CRUD
- AuditLogDAO — save and retrieve audit entries
- All 15 database tables deployed and seeded

### ✅ Phase 3 — Business Services (Partial)
Person 2 modules complete:
- EligibilityService — CGPA calculation, eligibility check, enrolment 
  trigger, human readable reason messages, empty results guard
- RecoveryRuleService — 3-attempt policy enforcement, attempt scope logic
- RecoveryService — plan management, milestones, recommendations
- ReportService — semester and yearly report generation with filtering

Person 1 modules pending:
- AuditLogService
- AuthService
- UserService
- DashboardService

### ✅ Phase 4 — Managed Beans (Partial)
Person 2 modules complete:
- EligibilityBean — @ViewScoped, FacesMessage feedback, exception handling
- RecoveryBean — @ViewScoped, null guards, exception handling
- MilestoneBean — @RequestScoped
- ReportBean — @ViewScoped, semester and year filtering

Person 1 modules pending:
- LoginBean
- UserBean
- DashboardBean
- AuditLogBean

### ⏳ Phase 5 — JSF Pages (Pending)
- login.xhtml
- dashboard.xhtml
- users.xhtml
- students.xhtml
- eligibility.xhtml
- recovery.xhtml
- reports.xhtml
- report-view.jsp
- audit-log.xhtml

### ⏳ Phase 6 — Integration & Testing (Pending)
- End to end deployment test on Payara 7
- All modules connected and verified
- Sample data demo walkthrough

---

## Key Design Decisions

**Raw JDBC over JPA** — assignment requirement, demonstrates understanding 
of direct database access patterns.

**CDI over JSF ManagedBean** — jakarta.faces.bean was removed entirely in 
JSF 4.0. CDI with @Named and @ViewScoped is the correct replacement for 
Payara 7.

**@ViewScoped for data beans** — prevents stale data between page 
navigations and reduces session memory overhead compared to @SessionScoped.

**EligibilityService split into 3 private methods** — calculateEligibility() 
handles pure computation, persistEligibility() saves the record, 
enrolIfEligible() triggers enrolment. checkEligibility() orchestrates all 
three. Clean separation of concerns at the method level.

**StringBuilder reason messages** — eligibility decisions include a human 
readable explanation covering all failing conditions simultaneously rather 
than stopping at the first failure.

**ValidationException before DB write** — invalid operations are blocked 
before any persistence occurs, maintaining database integrity.

**Proper audit log columns** — each audit field maps to its own database 
column. No serialisation hacks.

**Single AuthFilter registration** — @WebFilter annotation only, no 
duplicate web.xml declaration.

---

## Assignment Marking Targets

| Component | Weight | Target |
|---|---|---|
| Evaluation Report | 20% | Distinction |
| Presentation Tier | 20% | Distinction |
| Business Tier | 20% | Distinction |
| Database Tier | 10% | Distinction |
| Design Documentation | 20% | Distinction |
| Presentation / Viva | 10% | Distinction |
