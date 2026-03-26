package com.epda.crs.model;

public class Course {

    private Long id;
    private String courseCode;
    private String courseName;
    private int creditHours;
    private String instructor;
    private String grade;
    private double gradePoint;

    public Course() {
    }

    public Course(Long id, String courseCode, String courseName,
                  int creditHours, String grade, double gradePoint) {
        this.id          = id;
        this.courseCode  = courseCode;
        this.courseName  = courseName;
        this.creditHours = creditHours;
        this.grade       = grade;
        this.gradePoint  = gradePoint;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCourseCode() { return courseCode; }
    public void setCourseCode(String courseCode) { this.courseCode = courseCode; }

    public String getCourseName() { return courseName; }
    public void setCourseName(String courseName) { this.courseName = courseName; }

    public int getCreditHours() { return creditHours; }
    public void setCreditHours(int creditHours) { this.creditHours = creditHours; }

    public String getInstructor() { return instructor; }
    public void setInstructor(String instructor) { this.instructor = instructor; }

    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }

    public double getGradePoint() { return gradePoint; }
    public void setGradePoint(double gradePoint) { this.gradePoint = gradePoint; }
}
