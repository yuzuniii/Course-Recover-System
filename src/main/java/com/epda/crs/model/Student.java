package com.epda.crs.model;

public class Student {

    private Long id;
    private String studentNumber;
    private String fullName;
    private String programName;
    private String email;
    private int semester;
    private int yearOfStudy;
    private double cgpa;
    private int failedCourseCount;
    private java.util.List<FailedComponent> failedComponents;

    public Student() {
    }

    public Student(Long id, String studentNumber, String fullName, String programName,
                   int semester, int yearOfStudy, double cgpa, int failedCourseCount) {
        this.id               = id;
        this.studentNumber    = studentNumber;
        this.fullName         = fullName;
        this.programName      = programName;
        this.semester         = semester;
        this.yearOfStudy      = yearOfStudy;
        this.cgpa             = cgpa;
        this.failedCourseCount = failedCourseCount;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getStudentNumber() { return studentNumber; }
    public void setStudentNumber(String studentNumber) { this.studentNumber = studentNumber; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getProgramName() { return programName; }
    public void setProgramName(String programName) { this.programName = programName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public int getSemester() { return semester; }
    public void setSemester(int semester) { this.semester = semester; }

    public int getYearOfStudy() { return yearOfStudy; }
    public void setYearOfStudy(int yearOfStudy) { this.yearOfStudy = yearOfStudy; }

    public double getCgpa() { return cgpa; }
    public void setCgpa(double cgpa) { this.cgpa = cgpa; }

    public int getFailedCourseCount() { return failedCourseCount; }
    public void setFailedCourseCount(int failedCourseCount) { this.failedCourseCount = failedCourseCount; }

    public java.util.List<FailedComponent> getFailedComponents() { return failedComponents; }
    public void setFailedComponents(java.util.List<FailedComponent> failedComponents) { this.failedComponents = failedComponents; }
}
