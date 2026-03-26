package com.epda.crs.bean;

import com.epda.crs.dao.StudentDAO;
import com.epda.crs.dto.EligibilityDTO;
import com.epda.crs.model.Student;
import com.epda.crs.service.EligibilityService;
import java.io.Serializable;
import java.util.List;
import jakarta.ejb.EJB;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;

@Named
@ViewScoped
public class StudentBean implements Serializable {
    private final StudentDAO studentDAO = new StudentDAO();

    @EJB
    private EligibilityService eligibilityService;

    public List<Student> getStudents() { return studentDAO.findAll(); }
    public List<EligibilityDTO> getEligibilityResults() { return eligibilityService.getEligibilityBreakdown(); }
}
