<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c"   uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8"/>
    <title>Academic Report</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 30px; color: #333; }
        h1 { font-size: 1.4em; border-bottom: 2px solid #333; padding-bottom: 6px; }
        h2 { font-size: 1.1em; margin-top: 20px; }
        .info-table { border-collapse: collapse; width: 60%; margin-bottom: 20px; }
        .info-table td { padding: 6px 10px; }
        .info-table td:first-child { font-weight: bold; width: 140px; }
        .results-table { border-collapse: collapse; width: 100%; }
        .results-table th { background-color: #444; color: #fff; padding: 8px 10px; text-align: left; }
        .results-table td { padding: 6px 10px; border-bottom: 1px solid #ddd; }
        .results-table tr:nth-child(even) { background-color: #f9f9f9; }
        .fail { color: red; font-weight: bold; }
        .cgpa-row { margin-top: 16px; font-size: 1.05em; font-weight: bold; }
        .standing-badge { display: inline-block; margin-top: 8px; padding: 4px 12px;
                          border-radius: 4px; font-size: 0.95em; font-weight: 600; }
        .standing-excellent { background: #dcfce7; color: #166534; }
        .standing-good      { background: #dcfce7; color: #166534; }
        .standing-satisfactory { background: #fef9c3; color: #854d0e; }
        .standing-risk      { background: #fee2e2; color: #991b1b; }
        .print-btn { margin-top: 20px; padding: 8px 20px; cursor: pointer; }
        @media print {
            .no-print { display: none; }
            .print-btn { display: none; }
        }
    </style>
</head>
<body>

<%-- Retrieve parameters --%>
<c:set var="studentId" value="${param.studentId}" />
<c:set var="semester"  value="${param.semester}" />
<c:set var="year"      value="${param.year}" />

<h1>Asia Pacific University — Academic Performance Report</h1>

<c:choose>
    <c:when test="${not empty studentId and studentId != '0'}">

        <%-- Load report from session attribute set by ReportBean.generateReport() --%>
        <c:set var="report" value="${sessionScope.currentReport}" />

        <c:choose>
            <c:when test="${report != null and report.studentId == studentId}">

                <h2>Student Information</h2>
                <table class="info-table">
                    <tr>
                        <td>Student Code:</td>
                        <td><c:out value="${report.studentCode}" /></td>
                    </tr>
                    <tr>
                        <td>Student Name:</td>
                        <td><c:out value="${report.studentName}" /></td>
                    </tr>
                    <tr>
                        <td>Programme:</td>
                        <td><c:out value="${report.programme}" /></td>
                    </tr>
                    <tr>
                        <td>Semester:</td>
                        <td><c:out value="${report.semester}" /></td>
                    </tr>
                    <tr>
                        <td>Year of Study:</td>
                        <td><c:out value="${report.yearOfStudy}" /></td>
                    </tr>
                    <tr>
                        <td>CGPA:</td>
                        <td>
                            <fmt:formatNumber value="${report.cgpa}"
                                             minFractionDigits="2" maxFractionDigits="2" />
                        </td>
                    </tr>
                </table>

                <h2>Course Results</h2>
                <c:choose>
                    <c:when test="${not empty report.results}">
                        <table class="results-table">
                            <thead>
                                <tr>
                                    <th>Course Code</th>
                                    <th>Course Name</th>
                                    <th>Credit Hours</th>
                                    <th>Grade</th>
                                    <th>Grade Point</th>
                                    <th>Attempt</th>
                                    <th>Status</th>
                                </tr>
                            </thead>
                            <tbody>
                                <c:forEach var="row" items="${report.results}">
                                    <tr>
                                        <td><c:out value="${row.courseCode}" /></td>
                                        <td><c:out value="${row.courseName}" /></td>
                                        <td><c:out value="${row.creditHours}" /></td>
                                        <td>
                                            <c:choose>
                                                <c:when test="${row.grade == 'F'}">
                                                    <span class="fail"><c:out value="${row.grade}" /></span>
                                                </c:when>
                                                <c:otherwise>
                                                    <c:out value="${row.grade}" />
                                                </c:otherwise>
                                            </c:choose>
                                        </td>
                                        <td>
                                            <fmt:formatNumber value="${row.gradePoint}"
                                                             minFractionDigits="2" maxFractionDigits="2" />
                                        </td>
                                        <td><c:out value="${row.attemptNumber}" /></td>
                                        <td><c:out value="${row.status}" /></td>
                                    </tr>
                                </c:forEach>
                            </tbody>
                        </table>

                        <div class="cgpa-row">
                            Cumulative GPA:
                            <fmt:formatNumber value="${report.cgpa}"
                                             minFractionDigits="2" maxFractionDigits="2" />
                        </div>

                        <%-- Academic standing label --%>
                        <div>
                            <c:choose>
                                <c:when test="${report.cgpa >= 3.5}">
                                    <span class="standing-badge standing-excellent">
                                        &#9733; Excellent Standing
                                    </span>
                                </c:when>
                                <c:when test="${report.cgpa >= 3.0}">
                                    <span class="standing-badge standing-good">
                                        &#10003; Good Standing
                                    </span>
                                </c:when>
                                <c:when test="${report.cgpa >= 2.0}">
                                    <span class="standing-badge standing-satisfactory">
                                        &#8212; Satisfactory Standing
                                    </span>
                                </c:when>
                                <c:otherwise>
                                    <span class="standing-badge standing-risk">
                                        &#9888; At Risk
                                    </span>
                                </c:otherwise>
                            </c:choose>
                        </div>

                    </c:when>
                    <c:otherwise>
                        <p>No course results available for this semester.</p>
                    </c:otherwise>
                </c:choose>

            </c:when>
            <c:otherwise>
                <p>Report not available. Please generate the report from the
                   <a href="${pageContext.request.contextPath}/pages/reports.xhtml">Reports page</a>
                   first.</p>
            </c:otherwise>
        </c:choose>

    </c:when>
    <c:otherwise>
        <p>No student selected. Please go back to the
           <a href="${pageContext.request.contextPath}/pages/reports.xhtml">Reports page</a>.</p>
    </c:otherwise>
</c:choose>

<div class="no-print">
    <button class="print-btn" onclick="window.print()">Print Report</button>
</div>

</body>
</html>
