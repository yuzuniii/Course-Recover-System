param(
    [string]$StudentCsv = "C:\Users\user\Downloads\student_information.csv",
    [string]$CourseCsv = "C:\Users\user\Downloads\course_information-APU-LP-0650.csv",
    [string]$MySqlExe = "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe",
    [string]$Database = "crs_system",
    [string]$Username = "root",
    [string]$Password = "admin"
)

$ErrorActionPreference = "Stop"

function Escape-Sql([string]$Value) {
    if ($null -eq $Value) { return "" }
    return $Value.Replace("'", "''")
}

function Convert-Year([string]$Value) {
    switch ($Value.Trim().ToLower()) {
        "freshman" { return 1 }
        "sophomore" { return 2 }
        "junior" { return 3 }
        "senior" { return 4 }
        default { return 1 }
    }
}

function Build-StudentCode([string]$Value) {
    $numericPart = [int]($Value -replace "\D", "")
    return ("2025A{0:D4}" -f $numericPart)
}

function Build-Email([pscustomobject]$Row, [string]$StudentCode) {
    if (-not [string]::IsNullOrWhiteSpace($Row.Email)) {
        return $Row.Email.Trim().ToLower()
    }

    $first = ($Row.FirstName -replace "\s+", "").ToLower()
    $last = ($Row.LastName -replace "\s+", "").ToLower()
    return "$first.$last.$StudentCode@student.crs.local".ToLower()
}

$cgpaCycle = @(2.10, 2.45, 2.80, 3.15, 3.50)
$instructorCycle = @("Dr. Tan", "Dr. Lee", "Prof. Wong", "Prof. Lim", "Dr. Kumar")

$studentRows = Import-Csv $StudentCsv
$courseRows = Import-Csv $CourseCsv
$sqlLines = New-Object System.Collections.Generic.List[string]

$sqlLines.Add("USE $Database;")
$sqlLines.Add("")
$sqlLines.Add("SET @studentEmailExists := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'students' AND column_name = 'email');")
$sqlLines.Add("SET @studentEmailSql := IF(@studentEmailExists = 0, 'ALTER TABLE students ADD COLUMN email VARCHAR(100) NULL AFTER programme', 'SELECT 1');")
$sqlLines.Add("PREPARE stmt FROM @studentEmailSql;")
$sqlLines.Add("EXECUTE stmt;")
$sqlLines.Add("DEALLOCATE PREPARE stmt;")
$sqlLines.Add("")
$sqlLines.Add("SET @courseInstructorExists := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'courses' AND column_name = 'instructor');")
$sqlLines.Add("SET @courseInstructorSql := IF(@courseInstructorExists = 0, 'ALTER TABLE courses ADD COLUMN instructor VARCHAR(100) NULL AFTER credit_hours', 'SELECT 1');")
$sqlLines.Add("PREPARE stmt FROM @courseInstructorSql;")
$sqlLines.Add("EXECUTE stmt;")
$sqlLines.Add("DEALLOCATE PREPARE stmt;")
$sqlLines.Add("")
$sqlLines.Add("SET @courseCodeUniqueExists := (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'courses' AND column_name = 'course_code' AND non_unique = 0);")
$sqlLines.Add("SET @courseCodeUniqueSql := IF(@courseCodeUniqueExists = 0, 'ALTER TABLE courses ADD CONSTRAINT uk_courses_course_code UNIQUE (course_code)', 'SELECT 1');")
$sqlLines.Add("PREPARE stmt FROM @courseCodeUniqueSql;")
$sqlLines.Add("EXECUTE stmt;")
$sqlLines.Add("DEALLOCATE PREPARE stmt;")
$sqlLines.Add("")

$index = 0
foreach ($row in $studentRows) {
    $index++
    $studentCode = Build-StudentCode $row.StudentID
    $fullName = ("{0} {1}" -f $row.FirstName.Trim(), $row.LastName.Trim()).Trim()
    $programme = $row.Major.Trim()
    $email = Build-Email $row $studentCode
    $year = Convert-Year $row.Year
    $semester = if ($index % 2 -eq 1) { 1 } else { 2 }
    $cgpa = $cgpaCycle[($index - 1) % $cgpaCycle.Count].ToString("0.00", [System.Globalization.CultureInfo]::InvariantCulture)
    $sqlLines.Add(
        "INSERT INTO students (student_code, name, programme, email, year_of_study, current_semester, cgpa) " +
        "VALUES ('$(Escape-Sql $studentCode)', '$(Escape-Sql $fullName)', '$(Escape-Sql $programme)', " +
        "'$(Escape-Sql $email)', $year, $semester, $cgpa) " +
        "ON DUPLICATE KEY UPDATE name = VALUES(name), programme = VALUES(programme), email = VALUES(email), " +
        "year_of_study = VALUES(year_of_study), current_semester = VALUES(current_semester), cgpa = VALUES(cgpa);"
    )
}

$sqlLines.Add("")
$index = 0
foreach ($row in $courseRows) {
    $index++
    $courseCode = $row.CourseID.Trim()
    $courseName = $row.CourseName.Trim()
    $creditHours = [int]$row.Credits
    $instructor = if (-not [string]::IsNullOrWhiteSpace($row.Instructor)) {
        $row.Instructor.Trim()
    } else {
        $instructorCycle[($index - 1) % $instructorCycle.Count]
    }

    $sqlLines.Add(
        "INSERT INTO courses (course_code, course_name, credit_hours, instructor) " +
        "VALUES ('$(Escape-Sql $courseCode)', '$(Escape-Sql $courseName)', $creditHours, '$(Escape-Sql $instructor)') " +
        "ON DUPLICATE KEY UPDATE course_name = VALUES(course_name), credit_hours = VALUES(credit_hours), instructor = VALUES(instructor);"
    )
}

$tempSql = Join-Path $env:TEMP "crs_csv_import.sql"
Set-Content -Path $tempSql -Value $sqlLines -Encoding UTF8

Get-Content -Raw $tempSql | & $MySqlExe "-u$Username" "-p$Password" $Database

Write-Output "Imported $($studentRows.Count) students and $($courseRows.Count) courses into $Database."
