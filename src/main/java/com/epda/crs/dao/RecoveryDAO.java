package com.epda.crs.dao;

import com.epda.crs.config.DBConnection;
import com.epda.crs.enums.RecoveryStatus;
import com.epda.crs.model.Course;
import com.epda.crs.model.RecoveryPlan;
import com.epda.crs.model.Student;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import jakarta.ejb.Stateless;

@Stateless
public class RecoveryDAO {

    private static final String BASE_SELECT =
        "SELECT p.plan_id, p.attempt_number, p.status, p.recommendation, " +
        "       p.start_date, p.end_date, p.created_at, " +
        "       s.student_id, s.student_code, s.name, s.programme, s.year_of_study, " +
        "       s.current_semester, s.cgpa, " +
        "       c.course_id, c.course_code, c.course_name, c.credit_hours " +
        "FROM   recovery_plans p " +
        "JOIN   students s ON p.student_id = s.student_id " +
        "JOIN   courses  c ON p.course_id  = c.course_id ";

    private RecoveryPlan mapRow(ResultSet rs) throws SQLException {
        Student student = new Student();
        student.setId((long) rs.getInt("student_id"));
        student.setStudentNumber(rs.getString("student_code"));
        student.setFullName(rs.getString("name"));
        student.setProgramName(rs.getString("programme"));
        student.setYearOfStudy(rs.getInt("year_of_study"));
        student.setSemester(rs.getInt("current_semester"));
        student.setCgpa(rs.getDouble("cgpa"));
        student.setFailedCourseCount(0);

        Course course = new Course();
        course.setId((long) rs.getInt("course_id"));
        course.setCourseCode(rs.getString("course_code"));
        course.setCourseName(rs.getString("course_name"));
        course.setCreditHours(rs.getInt("credit_hours"));
        course.setGrade("");
        course.setGradePoint(0.0);

        RecoveryStatus status;
        try {
            status = RecoveryStatus.valueOf(rs.getString("status"));
        } catch (Exception e) {
            status = RecoveryStatus.ACTIVE;
        }

        RecoveryPlan plan = new RecoveryPlan();
        plan.setId((long) rs.getInt("plan_id"));
        plan.setAttemptNumber(rs.getInt("attempt_number"));
        plan.setStudent(student);
        plan.setCourse(course);
        plan.setStatus(status);
        plan.setRecommendation(rs.getString("recommendation"));
        java.sql.Date startDate = rs.getDate("start_date");
        if (startDate != null) plan.setStartDate(startDate.toLocalDate());
        java.sql.Date endDate = rs.getDate("end_date");
        if (endDate != null) plan.setEndDate(endDate.toLocalDate());
        return plan;
    }

    public List<RecoveryPlan> findAll() {
        List<RecoveryPlan> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(BASE_SELECT + "ORDER BY p.plan_id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            throw new RuntimeException("RecoveryDAO.findAll failed", e);
        }
        return list;
    }

    public List<RecoveryPlan> findByStudentId(int studentId) {
        String sql = BASE_SELECT + "WHERE p.student_id = ? ORDER BY p.plan_id";
        List<RecoveryPlan> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("RecoveryDAO.findByStudentId failed", e);
        }
        return list;
    }

    public Optional<RecoveryPlan> findById(Long id) {
        String sql = BASE_SELECT + "WHERE p.plan_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id.intValue());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("RecoveryDAO.findById failed", e);
        }
        return Optional.empty();
    }

    public void save(RecoveryPlan plan) {
        String sql = "INSERT INTO recovery_plans (student_id, course_id, attempt_number, status, recommendation, start_date, end_date) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, plan.getStudent().getId().intValue());
            ps.setInt(2, plan.getCourse().getId().intValue());
            ps.setInt(3, plan.getAttemptNumber());
            ps.setString(4, plan.getStatus() != null ? plan.getStatus().name() : RecoveryStatus.ACTIVE.name());
            ps.setString(5, plan.getRecommendation());
            ps.setDate(6, plan.getStartDate() != null ? java.sql.Date.valueOf(plan.getStartDate()) : null);
            ps.setDate(7, plan.getEndDate()   != null ? java.sql.Date.valueOf(plan.getEndDate())   : null);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) plan.setId(keys.getLong(1));
            }
        } catch (SQLException e) {
            throw new RuntimeException("RecoveryDAO.save failed", e);
        }
    }

    public void update(RecoveryPlan plan) {
        String sql = "UPDATE recovery_plans SET attempt_number = ?, status = ?, " +
                     "recommendation = ?, start_date = ?, end_date = ? WHERE plan_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, plan.getAttemptNumber());
            ps.setString(2, plan.getStatus() != null ? plan.getStatus().name() : RecoveryStatus.ACTIVE.name());
            ps.setString(3, plan.getRecommendation());
            ps.setDate(4, plan.getStartDate() != null ? java.sql.Date.valueOf(plan.getStartDate()) : null);
            ps.setDate(5, plan.getEndDate()   != null ? java.sql.Date.valueOf(plan.getEndDate())   : null);
            ps.setInt(6, plan.getId().intValue());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("RecoveryDAO.update failed", e);
        }
    }

    public long countActivePlans() {
        String sql = "SELECT COUNT(*) FROM recovery_plans WHERE status = 'ACTIVE'";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getLong(1);
        } catch (SQLException e) {
            throw new RuntimeException("RecoveryDAO.countActivePlans failed", e);
        }
        return 0;
    }

    public List<RecoveryPlan> findCompletedPlans() {
        String sql = BASE_SELECT + "WHERE p.status = 'COMPLETED'";
        List<RecoveryPlan> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            throw new RuntimeException("RecoveryDAO.findCompletedPlans failed", e);
        }
        return list;
    }

    public void updateStatus(Long planId, RecoveryStatus status) {
        String sql = "UPDATE recovery_plans SET status = ? WHERE plan_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setInt(2, planId.intValue());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("RecoveryDAO.updateStatus failed", e);
        }
    }
}
