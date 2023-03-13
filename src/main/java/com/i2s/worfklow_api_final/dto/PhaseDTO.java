package com.i2s.worfklow_api_final.dto;

import com.i2s.worfklow_api_final.model.Phase;
import com.i2s.worfklow_api_final.model.Project;
import com.i2s.worfklow_api_final.model.Step;

import java.util.List;

public class PhaseDTO {
    private long id;
    private String phaseName;
    private String description;
    private List<Step> steps;
    private Project project;
    public PhaseDTO() {
    }

    public PhaseDTO(Phase phase){
        this.id= phase.getId();
        this.phaseName = phase.getPhaseName();
        this.description = phase.getDescription();
        this.steps = phase.getSteps();
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public String getPhaseName() {
        return phaseName;
    }

    public void setPhaseName(String phaseName) {
        this.phaseName = phaseName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<Step> getSteps() {
        return steps;
    }

    public void setSteps(List<Step> steps) {
        this.steps = steps;
    }

    @Override
    public String toString() {
        return "PhaseDTO [id=" + id + ", phaseName=" + phaseName + ", description=" + description + "]";
    }
}