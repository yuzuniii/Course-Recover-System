package com.epda.crs.service;

import com.epda.crs.dao.RecoveryDAO;
import com.epda.crs.enums.MilestoneStatus;
import com.epda.crs.enums.RecoveryStatus;
import com.epda.crs.exception.ValidationException;
import com.epda.crs.model.Milestone;
import com.epda.crs.model.RecoveryPlan;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import java.time.LocalDate;
import java.util.List;

@Stateless
public class RecoveryRuleService {

    @Inject
    private RecoveryDAO recoveryDAO;

    // -----------------------------------------------------------------------
    // Attempt-limit enforcement (CLAUDE.md: maximum 3 attempts per course)
    // -----------------------------------------------------------------------

    /**
     * Returns the next attempt number for this student/course pair.
     * Counts all existing recovery plans, then adds 1.
     */
    public int getNextAttemptNumber(int studentId, int courseId) {
        List<RecoveryPlan> plans = recoveryDAO.findByStudentId(studentId);
        long existing = plans.stream()
                .filter(p -> p.getCourse() != null
                          && p.getCourse().getId().intValue() == courseId)
                .count();
        return (int) existing + 1;
    }

    /**
     * Throws ValidationException if the student has already used all 3 attempts
     * for the given course.
     */
    public void validateAttempt(int studentId, int courseId) {
        int next = getNextAttemptNumber(studentId, courseId);
        if (next > 3) {
            throw new ValidationException(
                "Maximum 3 recovery attempts exceeded for student " + studentId +
                " in course " + courseId);
        }
    }

    /**
     * Returns a human-readable description of what the student must do for
     * the given attempt number.
     *
     * Attempt 1 → Full course recovery
     * Attempt 2 → Failed components only
     * Attempt 3 → All components - final attempt
     */
    public String getAttemptScope(int attemptNo) {
        return switch (attemptNo) {
            case 1 -> "1st Attempt: Standard enrollment";
            case 2 -> "2nd Attempt: Resubmit/resit failed components only";
            case 3 -> "3rd Attempt: Retake all assessment components";
            default -> "No further attempts allowed";
        };
    }

    // -----------------------------------------------------------------------
    // Legacy helpers (used by DashboardService)
    // -----------------------------------------------------------------------

    /** Returns true if attempt count is within the 3-attempt window. */
    public boolean hasExceededAttemptWindow(int attemptNumber) {
        return attemptNumber > 3;
    }

    /**
     * Returns true if the plan is ACTIVE and has not passed its end date.
     * A null end date is treated as "no deadline set" (still active).
     */
    public boolean isPlanActive(RecoveryPlan plan) {
        if (plan.getStatus() != RecoveryStatus.ACTIVE) return false;
        if (plan.getEndDate() == null) return true;
        return !plan.getEndDate().isBefore(LocalDate.now());
    }

    /**
     * Returns true if the milestone is not yet complete and its due date has
     * passed. A null due date is treated as "no deadline" (never overdue).
     */
    public boolean isMilestoneOverdue(Milestone milestone) {
        if (milestone.getStatus() == MilestoneStatus.COMPLETED
                || milestone.getStatus() == MilestoneStatus.DONE) return false;
        if (milestone.getDueDate() == null) return false;
        return milestone.getDueDate().isBefore(LocalDate.now());
    }

    /** Counts overdue milestones in the supplied list. */
    public long countOverdueMilestones(List<Milestone> milestones) {
        return milestones.stream().filter(this::isMilestoneOverdue).count();
    }
}
