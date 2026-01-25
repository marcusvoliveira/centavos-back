package com.incentive.dto;

public class AuthResponse {

    public String token;
    public UserDTO user;

    public AuthResponse(String token, UserDTO user) {
        this.token = token;
        this.user = user;
    }
}
