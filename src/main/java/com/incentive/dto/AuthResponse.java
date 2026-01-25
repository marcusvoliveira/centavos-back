package com.incentive.dto;

import java.util.List;

public class AuthResponse {

    public String token;
    public UserDTO user;
    public List<UserProjectDTO> projects;

    public AuthResponse(String token, UserDTO user, List<UserProjectDTO> projects) {
        this.token = token;
        this.user = user;
        this.projects = projects;
    }
}
