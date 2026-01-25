package com.incentive.dto;

import com.incentive.entity.Role;
import com.incentive.entity.UserProject;

public class UserProjectDTO {

    public Long projectId;
    public String projectName;
    public String projectDescription;
    public Role role;
    public boolean projectActive;

    public UserProjectDTO() {
    }

    public UserProjectDTO(Long projectId, String projectName, String projectDescription, Role role, boolean projectActive) {
        this.projectId = projectId;
        this.projectName = projectName;
        this.projectDescription = projectDescription;
        this.role = role;
        this.projectActive = projectActive;
    }

    public static UserProjectDTO from(UserProject userProject) {
        return new UserProjectDTO(
            userProject.project.id,
            userProject.project.name,
            userProject.project.description,
            userProject.role,
            userProject.project.active
        );
    }
}
