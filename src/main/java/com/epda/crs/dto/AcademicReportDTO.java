package com.epda.crs.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class AcademicReportDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private int studentId;
    private String studentCode;
    private String studentName;
    private String studentEmail;
    private String programme;
    private int semester;
    private int yearOfStudy;
    private double cgpa;
    private List<CourseResultRow> results = new ArrayList<>();

    public AcademicReportDTO() {
    }

    public AcademicReportDTO(int studentId, String studentCode, String studentName,
                              String programme, int semester, int yearOfStudy,
                              double cgpa, List<CourseResultRow> results) {
        this.studentId   = studentId;
        this.studentCode = studentCode;
        this.studentName = studentName;
        this.programme   = programme;
        this.semester    = semester;
        this.yearOfStudy = yearOfStudy;
        this.cgpa        = cgpa;
        this.results     = results;
    }

    public int getStudentId() { return studentId; }
    public void setStudentId(int studentId) { this.studentId = studentId; }

    public String getStudentCode() { return studentCode; }
    public void setStudentCode(String studentCode) { this.studentCode = studentCode; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getStudentEmail() { return studentEmail; }
    public void setStudentEmail(String studentEmail) { this.studentEmail = studentEmail; }

    public String getProgramme() { return programme; }
    public void setProgramme(String programme) { this.programme = programme; }

    public int getSemester() { return semester; }
    public void setSemester(int semester) { this.semester = semester; }

    public int getYearOfStudy() { return yearOfStudy; }
    public void setYearOfStudy(int yearOfStudy) { this.yearOfStudy = yearOfStudy; }

    public double getCgpa() { return cgpa; }
    public void setCgpa(double cgpa) { this.cgpa = cgpa; }

    public List<CourseResultRow> getResults() { return results; }
    public void setResults(List<CourseResultRow> results) { this.results = results; }

    /** Computed: number of F grades in results. Used by reports.xhtml summary. */
    public long getFailedCount() {
        if (results == null) return 0;
        return results.stream().filter(r -> "F".equals(r.getGrade())).count();
    }

    // One row per student_course_results entry (joined with courses)
    public static class CourseResultRow implements Serializable {

        private static final long serialVersionUID = 1L;

        private String courseCode;
        private String courseName;
        private int creditHours;
        private String grade;
        private double gradePoint;
        private int attemptNumber;
        private String status;

        public CourseResultRow() {
        }

        public CourseResultRow(String courseCode, String courseName, int creditHours,
                               String grade, double gradePoint,
                               int attemptNumber, String status) {
            this.courseCode    = courseCode;
            this.courseName    = courseName;
            this.creditHours   = creditHours;
            this.grade         = grade;
            this.gradePoint    = gradePoint;
            this.attemptNumber = attemptNumber;
            this.status        = status;
        }

        public String getCourseCode() { return courseCode; }
        public void setCourseCode(String courseCode) { this.courseCode = courseCode; }

        public String getCourseName() { return courseName; }
        public void setCourseName(String courseName) { this.courseName = courseName; }

        public int getCreditHours() { return creditHours; }
        public void setCreditHours(int creditHours) { this.creditHours = creditHours; }

        public String getGrade() { return grade; }
        public void setGrade(String grade) { this.grade = grade; }

        public double getGradePoint() { return gradePoint; }
        public void setGradePoint(double gradePoint) { this.gradePoint = gradePoint; }

        public int getAttemptNumber() { return attemptNumber; }
        public void setAttemptNumber(int attemptNumber) { this.attemptNumber = attemptNumber; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }
}
