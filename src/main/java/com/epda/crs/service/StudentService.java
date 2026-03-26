package com.epda.crs.service;

import com.epda.crs.dao.StudentDAO;
import com.epda.crs.model.Student;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Optional;

@Stateless
public class StudentService {

    @Inject
    private StudentDAO studentDAO;

    @Inject
    private AuditLogService auditLogService;

    public List<Student> getStudents() {
        return studentDAO.findAll();
    }

    public Optional<Student> getStudentById(Long id) {
        return studentDAO.findById(id);
    }

    public void saveStudent(Student student, String actorUsername) {
        if (student.getId() == null || student.getId() == 0) {
            studentDAO.save(student);
            auditLogService.logAction(actorUsername, "CREATE_STUDENT", "Student", null, "Created new student: " + student.getStudentNumber());
        } else {
            studentDAO.update(student);
            auditLogService.logAction(actorUsername, "UPDATE_STUDENT", "Student", student.getId(), "Updated student details: " + student.getStudentNumber());
        }
    }

    public void deleteStudent(Long id, String actorUsername) {
        // Implementation for student deletion if needed
        // For now, let's assume students are not easily deleted to preserve academic records
        auditLogService.logAction(actorUsername, "DELETE_STUDENT_ATTEMPT", "Student", id, "Attempted to delete student record.");
    }
}
