package com.incentive.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class VerifyEmailRequest {

    @NotBlank(message = "Email é obrigatório")
    @Email(message = "Email deve ser válido")
    public String email;

    @NotBlank(message = "Código de verificação é obrigatório")
    @Size(min = 6, max = 6, message = "Código deve ter 6 dígitos")
    public String code;
}
