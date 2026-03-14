package com.incentive.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RegisterRequest {

    @NotBlank(message = "Nome é obrigatório")
    @Size(min = 3, max = 100, message = "Nome deve ter entre 3 e 100 caracteres")
    public String name;

    @NotBlank(message = "Email é obrigatório")
    @Email(message = "Email deve ser válido")
    public String email;

    @NotBlank(message = "Senha é obrigatória")
    @Size(min = 8, message = "Senha deve ter no mínimo 8 caracteres")
    public String password;

    @Size(max = 20)
    public String phone;

    public String projectHash;
}
