package com.incentive.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class InviteUserRequest {

    @NotBlank(message = "Nome é obrigatório")
    public String name;

    @NotBlank(message = "Email é obrigatório")
    @Email(message = "Email deve ser válido")
    public String email;

    public String role = "MODERATOR";

    public Long projectId;
}
