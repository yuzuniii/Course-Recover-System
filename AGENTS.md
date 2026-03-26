# AGENTS.md

This file provides guidance to Codex when working with this repository.

## Project Overview

**Course Recovery System (CRS)** — A Jakarta EE enterprise web application
for managing student academic recovery plans at higher education institutions.
Built as a Year 3 Semester 1 Enterprise Programming and Design Architecture
(EPDA) assignment at Asia Pacific University.

- **Java:** JDK 21
- **Platform:** Jakarta EE 10 (Payara 7)
- **UI:** JSF 4.0 + PrimeFaces 14 (jakarta classifier) + 1x JSP with JSTL
- **Business tier:** @Stateless EJB Session Beans
- **Database:** MySQL 8 — database name: crs_system
- **JDBC:** Raw JDBC only — NO JPA, NO Hibernate, NO ORM of any kind
- **Build:** Maven WAR packaging
- **Base package:** com.epda.crs

## CRITICAL: Import Rules

This project uses Jakarta EE 10 on Payara 7. ALL imports MUST use
jakarta.* namespace. NEVER use javax.* under any circumstances —
it will break deployment on Payara 7.

| Correct (jakarta.*)                    | Wrong (javax.*)                    |
|----------------------------------------|------------------------------------|
| jakarta.ejb.Stateless                  | javax.ejb.Stateless                |
| jakarta.ejb.EJB                        | javax.ejb.EJB                      |
| jakarta.faces.view.ViewScoped          | javax.faces.view.ViewScoped        |
| jakarta.enterprise.context.SessionScoped | javax.enterprise.context.SessionScoped |
| jakarta.enterprise.context.RequestScoped | javax.enterprise.context.RequestScoped |
| jakarta.inject.Named                   | javax.inject.Named                 |
| jakarta.servlet.Filter                 | javax.servlet.Filter               |
| jakarta.servlet.annotation.WebFilter   | javax.servlet.annotation.WebFilter |
| jakarta.annotation.PostConstruct       | javax.annotation.PostConstruct     |
| jakarta.mail.Session                   | javax.mail.Session                 |

## Commands
```bash
mvn clean compile                  # Compile only
mvn test                           # Run all unit tests
mvn test -Dtest=ClassName          # Run single test class
mvn clean package                  # Build WAR
mvn clean package -DskipTests      # Build WAR skipping tests
```

Database setup (MySQL must be running on localhost:3306):
```bash
mysql -u root -padmin -e "DROP DATABASE crs_system; CREATE DATABASE crs_system;"
mysql -u root -padmin crs_system < database/schema.sql
mysql -u root -padmin crs_system < database/sample-data.sql
```

## Architecture — Strict Layer Rules
```
JSF Page (*.xhtml) or JSP (*.jsp)
        ↓
Managed Bean (bean/) — @Named + CDI scope
        ↓
EJB Service (service/) — @Stateless — business logic ONLY, no SQL
        ↓
DAO (dao/) — raw JDBC ONLY, no business logic
        ↓
DBConnection.java — single JDBC connection utility
        ↓
MySQL — crs_system database
```

### Layer responsibilities — enforce strictly

- **Beans** — JSF interaction only. Inject services via @EJB. Never call
  DAOs directly. Never write SQL. Never write business logic. Wrap every
  service call in try/catch and display errors via FacesMessage.
- **Services** — Business rules only. Annotated @Stateless. Call DAOs for
  data. Call AuditLogService at the end of every mutating operation.
- **DAOs** — SQL only. Use DBConnection.getConnection() inside
  try-with-resources. Return model objects or lists. No business logic.
- **Models** — Plain Java POJOs. No annotations. No logic. Fields must
  match the database table columns exactly.
- **DTOs** — Data transfer only. Used to pass computed results between
  layers (e.g. EligibilityDTO carries cgpa + reason + isEligible).
- **Utils** — Stateless helper classes. No DB access. Pure logic only.

## Bean Scopes

| Bean | Scope | Reason |
|---|---|---|
| LoginBean | @SessionScoped | Holds logged in user for entire session |
| UserBean | @SessionScoped | User management persists across navigations |
| StudentBean | @SessionScoped | Student list persists across navigations |
| EligibilityBean | @ViewScoped | Data scoped to eligibility page only |
| RecoveryBean | @ViewScoped | Data scoped to recovery page only |
| MilestoneBean | @RequestScoped | Short lived, reloads per request |
| ReportBean | @ViewScoped | Data scoped to report page only |
| DashboardBean | @SessionScoped | Analytics visible across session |
| AuditLogBean | @SessionScoped | Log persists across navigations |

All beans use @Named (jakarta.inject.Named) — never @ManagedBean.
jakarta.faces.bean.* was removed in JSF 4.0 and must never be used.

## Database

- Host: localhost
- Port: 3306
- Database: crs_system
- Username: root
- Password: admin

### Tables (15 total)

- roles
- users
- programmes
- students
- courses
- student_results
- failed_components
- eligibility_records
- enrollments
- recovery_plans
- milestones
- recovery_recommendations
- audit_logs
- email_notifications
- programmes

### JDBC Rules

- ALL DAOs must use raw JDBC via DBConnection.getConnection()
- Use PreparedStatement for every query — NEVER concatenate SQL strings
- Always close connections using try-with-resources
- Never use JPA, Hibernate, or any ORM
- Return model objects from DAOs, never ResultSet directly

## Key Business Rules — Implement Exactly As Stated

### Eligibility (EligibilityService)

CGPA formula:
    CGPA = Σ(gradePoint × creditHours) / Σ(creditHours)

Rules:
    ELIGIBLE     → CGPA >= 2.0 AND failedCourseCount <= 3
    NOT ELIGIBLE → CGPA < 2.0 OR failedCourseCount > 3

Reason messages (StringBuilder — show ALL failing conditions):
    CGPA < 2.0       → append "CGPA below minimum requirement of 2.0. "
    failedCount > 3  → append "Exceeded maximum of 3 failed courses. "
    eligible         → "Meets all progression requirements"

Empty results guard:
    If no results found → cgpa=0.0, failedCourses=0, eligible=false,
    reason="No academic results found for this semester"

checkEligibility() is split into three private methods:
    calculateEligibility() — pure computation, returns EligibilityDTO
    persistEligibility()   — saves to eligibility_records
    enrolIfEligible()      — calls EnrollmentDAO only if eligible=true

On check:
    1. calculateEligibility() → EligibilityDTO
    2. persistEligibility(dto)
    3. enrolIfEligible(dto)
    4. AuditLogService.log()
    5. Return EligibilityDTO

### Recovery Attempt Policy (RecoveryRuleService)

- Maximum 3 attempts per course per student — hard limit
- Attempt 1 → full course recovery (all components)
- Attempt 2 → failed components only (resit or resubmit)
- Attempt 3 → all components again (full resit)
- If attemptNo > 3 → throw ValidationException, block plan creation

createPlan() in RecoveryService MUST call:
    1. RecoveryRuleService.validateAttempt() first
    2. RecoveryRuleService.getNextAttemptNumber()
    3. Only then proceed to save

### Role-Based Access (AuthFilter)

- ADMIN → access to all pages
- ACADEMIC_OFFICER → blocked from:
    - /pages/users.xhtml
    - /pages/audit-log.xhtml
- All pages under /pages/* require a valid HTTP session
- No session → redirect to /pages/login.xhtml
- Wrong role → redirect to /pages/dashboard.xhtml
- AuthFilter registered via @WebFilter("/pages/*") annotation ONLY
  No duplicate registration in web.xml

### Audit Logging

Every mutating service method (create, update, delete, status change)
must call AuditLogService at the end:

    auditLogService.log(userId, "ACTION_NAME", "ENTITY_TYPE", entityId,
                        "Human readable description");

AuditLogService must be injected into all other services via @EJB.

### Password Hashing

- Use jBCrypt (org.mindrot:jbcrypt:0.4) for all password operations
- Hash on create/reset: BCrypt.hashpw(plainText, BCrypt.gensalt())
- Verify on login: BCrypt.checkpw(plainText, storedHash)
- Never store or compare plain text passwords anywhere

### Email Notifications

EmailUtil called by services only — never from beans:
- User account created → welcome email
- Password reset → reset email
- Recovery plan created → plan details email to student
- Eligibility result determined → result email to student
- Academic report generated → notification email to student

### Exception Handling

- ValidationException thrown before any DB write
- All service calls in beans wrapped in try/catch
- ValidationException → FacesMessage SEVERITY_ERROR with e.getMessage()
- Exception → FacesMessage SEVERITY_ERROR "An unexpected error occurred"
- Exceptions never bubble up to the JSF layer

## Current Build Status

### Complete
- All 4 enum classes
- All 7 model classes + RecoveryRecommendation model
- All 3 DTO classes
- AuthenticationException, ValidationException
- DBConnection JDBC utility
- CGPACalculator static utility
- EmailUtil JavaMail wrapper
- All 10 DAOs with real JDBC
- EligibilityService, RecoveryRuleService, 
  RecoveryService, ReportService
- EligibilityBean, RecoveryBean, MilestoneBean, ReportBean
- web.xml, faces-config.xml, beans.xml

### Pending
- AuditLogService, AuthService, UserService, DashboardService
- LoginBean, UserBean, DashboardBean, AuditLogBean
- All JSF pages and report-view.jsp

## File Structure
```
src/main/java/com/epda/crs/
├── bean/
│   ├── LoginBean.java              @Named @SessionScoped
│   ├── UserBean.java               @Named @SessionScoped
│   ├── StudentBean.java            @Named @SessionScoped
│   ├── EligibilityBean.java        @Named @ViewScoped
│   ├── RecoveryBean.java           @Named @ViewScoped
│   ├── MilestoneBean.java          @Named @RequestScoped
│   ├── ReportBean.java             @Named @ViewScoped
│   ├── DashboardBean.java          @Named @SessionScoped
│   └── AuditLogBean.java           @Named @SessionScoped
├── config/
│   └── DBConnection.java
├── dao/
│   ├── UserDAO.java
│   ├── StudentDAO.java
│   ├── CourseDAO.java
│   ├── ResultDAO.java
│   ├── EligibilityDAO.java
│   ├── EnrollmentDAO.java
│   ├── RecoveryDAO.java
│   ├── MilestoneDAO.java
│   ├── RecoveryRecommendationDAO.java
│   └── AuditLogDAO.java
├── dto/
│   ├── EligibilityDTO.java
│   ├── AcademicReportDTO.java
│   └── DashboardAnalyticsDTO.java
├── enums/
│   ├── UserRole.java               ADMIN, ACADEMIC_OFFICER
│   ├── AccountStatus.java          ACTIVE, INACTIVE
│   ├── RecoveryStatus.java         ACTIVE, COMPLETED, FAILED
│   └── MilestoneStatus.java        PENDING, DONE, COMPLETED, OVERDUE
├── exception/
│   ├── AuthenticationException.java
│   └── ValidationException.java
├── filter/
│   └── AuthFilter.java             @WebFilter("/pages/*") only
├── model/
│   ├── User.java
│   ├── Student.java
│   ├── Course.java
│   ├── RecoveryPlan.java
│   ├── Milestone.java
│   ├── AuditLog.java
│   └── RecoveryRecommendation.java
├── service/
│   ├── AuthService.java            @Stateless
│   ├── UserService.java            @Stateless
│   ├── EligibilityService.java     @Stateless — complete
│   ├── RecoveryService.java        @Stateless — complete
│   ├── RecoveryRuleService.java    @Stateless — complete
│   ├── ReportService.java          @Stateless — complete
│   ├── DashboardService.java       @Stateless
│   └── AuditLogService.java        @Stateless
└── util/
    ├── CGPACalculator.java         Pure static logic, no DB access
    └── EmailUtil.java              JavaMail wrapper

src/main/webapp/
├── WEB-INF/
│   ├── web.xml                     Jakarta EE 10 namespace, 30min timeout
│   ├── faces-config.xml            JSF 4.0 namespace, navigation rules
│   └── beans.xml                   bean-discovery-mode="annotated"
├── pages/
│   ├── login.xhtml
│   ├── dashboard.xhtml
│   ├── users.xhtml                 ADMIN only
│   ├── students.xhtml
│   ├── eligibility.xhtml
│   ├── recovery.xhtml
│   ├── reports.xhtml
│   ├── report-view.jsp             JSP + JSTL — required by assignment
│   └── audit-log.xhtml             ADMIN only
└── css/
    └── styles.css

database/
├── schema.sql                      Full schema — 15 tables
└── sample-data.sql                 Test data for demo
```

## pom.xml Dependencies
```xml
<!-- Jakarta EE 10 — PROVIDED by Payara 7 -->
<dependency>
    <groupId>jakarta.platform</groupId>
    <artifactId>jakarta.jakartaee-api</artifactId>
    <version>10.0.0</version>
    <scope>provided</scope>
</dependency>

<!-- PrimeFaces 14 for Jakarta EE -->
<dependency>
    <groupId>org.primefaces</groupId>
    <artifactId>primefaces</artifactId>
    <version>14.0.0</version>
    <classifier>jakarta</classifier>
</dependency>

<!-- MySQL JDBC Driver -->
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <version>8.3.0</version>
</dependency>

<!-- jBCrypt for password hashing -->
<dependency>
    <groupId>org.mindrot</groupId>
    <artifactId>jbcrypt</artifactId>
    <version>0.4</version>
</dependency>

<!-- JavaMail — PROVIDED by Payara 7 -->
<dependency>
    <groupId>jakarta.mail</groupId>
    <artifactId>jakarta.mail-api</artifactId>
    <version>2.1.0</version>
    <scope>provided</scope>
</dependency>

<!-- JUnit 5 -->
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
    <version>5.10.0</version>
    <scope>test</scope>
</dependency>
```

## Maven Compiler Config
```xml
<properties>
    <maven.compiler.source>21</maven.compiler.source>
    <maven.compiler.target>21</maven.compiler.target>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
</properties>
```

## Demo Credentials

| Username | Password   | Role                  |
|----------|------------|-----------------------|
| admin    | admin123   | ADMIN                 |
| officer  | officer123 | ACADEMIC_OFFICER      |

## Tests

- EligibilityServiceTest — CGPA threshold and failed course count rules
- CGPACalculatorTest — weighted CGPA formula validation

Both tests must pass before any WAR is built.

## Assignment Marking Weights

- Evaluation Report (Part 1 only): 20%
- Presentation Tier (JSF + JSP pages): 20%
- Business Tier (EJB services): 20%
- Database Tier (MySQL + JDBC DAOs): 10%
- Design Documentation: 20%
- Presentation / Viva: 10%

For distinction band: all 3 tiers fully implemented + minimum 2 additional
features documented with evidence. Chosen extras: Dashboard Analytics
and Audit Log System.

## Code Quality Rules

- Every DAO method uses PreparedStatement — no string concatenation in SQL
- Every service method that mutates data calls AuditLogService at the end
- Every bean uses @EJB injection — never instantiates services with new
- Every password operation uses BCrypt — never plain text
- Every connection is closed via try-with-resources — never manual close
- No business logic in beans — no SQL in services — no logic in DAOs
- JSF pages use PrimeFaces components for all form inputs and data tables
- report-view.jsp must use JSTL c:forEach for table rendering
- Beans must implement Serializable when using @ViewScoped