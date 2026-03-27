package com.epda.crs.bean;

import com.epda.crs.model.FailedComponent;
import com.epda.crs.model.Student;
import com.epda.crs.service.StudentService;
import com.epda.crs.service.EligibilityService;
import com.epda.crs.dto.EligibilityDTO;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
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

    @EJB
    private StudentService studentService;

    @EJB
    private EligibilityService eligibilityService;

    @Inject
    private LoginBean loginBean;

    private List<Student> students;
    private Student selectedStudent;
    
    private FailedComponent selectedComponent;

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

    public void prepareAddComponent(Student student) {
        this.selectedComponent = new FailedComponent();
        // Link to a default result_id for now or fetch the student's actual result_id if available
        // For demonstration purposes, we'll assume result_id 1
        this.selectedComponent.setResultId(1); 
    }

    public void editComponent(FailedComponent comp) {
        this.selectedComponent = comp;
    }

    public void saveComponent() {
        try {
            String actor = loginBean.getCurrentUser() != null ? loginBean.getCurrentUser().getUsername() : "system";
            studentService.saveFailedComponent(selectedComponent, actor);
            
            FacesContext.getCurrentInstance().addMessage(null, 
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", "Failed component saved"));
            
            students = studentService.getStudents(); // Refresh list
            PrimeFaces.current().executeScript("PF('failedComponentDialog').hide()");
            PrimeFaces.current().ajax().update("directoryContent", "globalMessages");
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null, 
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Could not save component: " + e.getMessage()));
        }
    }

    public void deleteComponent(FailedComponent comp) {
        try {
            String actor = loginBean.getCurrentUser() != null ? loginBean.getCurrentUser().getUsername() : "system";
            studentService.deleteFailedComponent((long) comp.getComponentId(), comp.getResultId(), actor);
            
            FacesContext.getCurrentInstance().addMessage(null, 
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Deleted", "Failed component removed"));
            
            students = studentService.getStudents(); // Refresh list
            PrimeFaces.current().ajax().update("directoryContent", "globalMessages");
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null, 
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Could not delete component: " + e.getMessage()));
        }
    }

    public void saveStudent() {
        try {
            String actor = loginBean.getCurrentUser() != null ? loginBean.getCurrentUser().getUsername() : "system";
            studentService.saveStudent(selectedStudent, actor);
            
            FacesContext.getCurrentInstance().addMessage(null, 
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", "Student saved successfully"));
            
            students = studentService.getStudents(); // Refresh list
            PrimeFaces.current().executeScript("PF('studentDialog').hide()");
            PrimeFaces.current().ajax().update("directoryContent", "globalMessages");
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null, 
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Could not save student: " + e.getMessage()));
        }
    }

    public List<Student> getStudents() { return students; }
    public Student getSelectedStudent() { return selectedStudent; }
    public void setSelectedStudent(Student selectedStudent) { this.selectedStudent = selectedStudent; }
    
    public FailedComponent getSelectedComponent() { return selectedComponent; }
    public void setSelectedComponent(FailedComponent selectedComponent) { this.selectedComponent = selectedComponent; }
    
    public List<EligibilityDTO> getEligibilityResults() { return eligibilityService.getEligibilityBreakdown(); }
}
