package com.epda.crs.model;

public class FailedComponent {

    private int componentId;
    private int resultId;
    private String componentName;
    private double componentScore;
    private double passRequired;

    // Inferred from componentName since there is no component_type column
    public String getComponentType() {
        if (componentName == null) return "Other";
        String lower = componentName.toLowerCase();
        if (lower.contains("final")) return "Final Exam";
        if (lower.contains("mid")) return "Mid-Term";
        if (lower.contains("assignment")) return "Assignment";
        if (lower.contains("quiz")) return "Quiz";
        if (lower.contains("project")) return "Project";
        return "Other";
    }

    public double getMarksDifference() {
        return componentScore - passRequired;
    }

    // -----------------------------------------------------------------------
    // Getters and setters
    // -----------------------------------------------------------------------

    public int getComponentId() { return componentId; }
    public void setComponentId(int componentId) { this.componentId = componentId; }

    public int getResultId() { return resultId; }
    public void setResultId(int resultId) { this.resultId = resultId; }

    public String getComponentName() { return componentName; }
    public void setComponentName(String componentName) { this.componentName = componentName; }

    public double getComponentScore() { return componentScore; }
    public void setComponentScore(double componentScore) { this.componentScore = componentScore; }

    public double getPassRequired() { return passRequired; }
    public void setPassRequired(double passRequired) { this.passRequired = passRequired; }
}
