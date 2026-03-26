package com.epda.crs.bean;

import com.epda.crs.model.Student;
import com.epda.crs.service.StudentService;
import com.epda.crs.service.EligibilityService;
import com.epda.crs.dto.EligibilityDTO;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.primefaces.PrimeFaces;
import java.io.Serializable;
import java.util.List;

@Named
@ViewScoped
public class StudentBean implements Serializable {

    @Inject
    private StudentService studentService;

    @Inject
    private EligibilityService eligibilityService;

    @Inject
    private LoginBean loginBean;

    private List<Student> students;
    private Student selectedStudent;

    @PostConstruct
    public void init() {
        students = studentService.getStudents();
    }

    public void prepareNewStudent() {
        this.selectedStudent = new Student();
    }

    public void prepareEditStudent(Student student) {
        this.selectedStudent = student;
    }

    public void saveStudent() {
        try {
            String actor = loginBean.getCurrentUser() != null ? loginBean.getCurrentUser().getUsername() : "system";
            studentService.saveStudent(selectedStudent, actor);
            
            FacesContext.getCurrentInstance().addMessage(null, 
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", "Student saved successfully"));
            
            students = studentService.getStudents(); // Refresh list
            PrimeFaces.current().executeScript("PF('studentDialog').hide()");
            PrimeFaces.current().ajax().update("form:dt-students", "globalMessages");
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null, 
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Could not save student: " + e.getMessage()));
        }
    }

    public List<Student> getStudents() { return students; }
    public Student getSelectedStudent() { return selectedStudent; }
    public void setSelectedStudent(Student selectedStudent) { this.selectedStudent = selectedStudent; }
    
    public List<EligibilityDTO> getEligibilityResults() { return eligibilityService.getEligibilityBreakdown(); }
}
