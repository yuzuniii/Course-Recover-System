package com.epda.crs.model;

import java.io.Serializable;

public class FailedComponent implements Serializable {
    private Long componentId;
    private Integer resultId;
    private String componentName;
    private Double componentScore;
    private Double passRequired;

    public FailedComponent() {}

    public FailedComponent(Long componentId, Integer resultId, String componentName, Double componentScore, Double passRequired) {
        this.componentId = componentId;
        this.resultId = resultId;
        this.componentName = componentName;
        this.componentScore = componentScore;
        this.passRequired = passRequired;
    }

    public Long getComponentId() { return componentId; }
    public void setComponentId(Long componentId) { this.componentId = componentId; }

    public Integer getResultId() { return resultId; }
    public void setResultId(Integer resultId) { this.resultId = resultId; }

    public String getComponentName() { return componentName; }
    public void setComponentName(String componentName) { this.componentName = componentName; }

    public Double getComponentScore() { return componentScore; }
    public void setComponentScore(Double componentScore) { this.componentScore = componentScore; }

    public Double getPassRequired() { return passRequired; }
    public void setPassRequired(Double passRequired) { this.passRequired = passRequired; }
}
