package com.epda.crs.service;

import com.epda.crs.dao.StudentDAO;
import com.epda.crs.dao.FailedComponentDAO;
import com.epda.crs.model.FailedComponent;
import com.epda.crs.model.Student;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Optional;

@Stateless
public class StudentService {

    @Inject
    private StudentDAO studentDAO;

    @Inject
    private FailedComponentDAO failedComponentDAO;

    @EJB
    private AuditLogService auditLogService;

    public List<Student> getStudents() { 
        List<Student> students = studentDAO.findAll();
        for (Student s : students) {
            s.setFailedComponents(failedComponentDAO.findByStudentId(s.getId()));
        }
        return students; 
    }

    public void saveFailedComponent(FailedComponent comp, String actor) {
        if (comp.getComponentId() == 0) {
            failedComponentDAO.save(comp);
            auditLogService.logAction(actor, "ADD_FAILED_COMPONENT", "Student", (long)comp.getResultId(), "Added failed component: " + comp.getComponentName());
        } else {
            failedComponentDAO.update(comp);
            auditLogService.logAction(actor, "UPDATE_FAILED_COMPONENT", "Student", (long) comp.getComponentId(), "Updated failed component: " + comp.getComponentName());
        }
    }

    public void deleteFailedComponent(Long componentId, Integer resultId, String actor) {
        failedComponentDAO.delete(componentId);
        auditLogService.logAction(actor, "DELETE_FAILED_COMPONENT", "Student", (long)resultId, "Deleted failed component ID: " + componentId);
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
        auditLogService.logAction(actorUsername, "DELETE_STUDENT_ATTEMPT", "Student", id, "Attempted to delete student record.");
    }
}
