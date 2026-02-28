package com.incentive.dto;

import jakarta.validation.constraints.NotBlank;

public class CompleteRegistrationRequest {

    @NotBlank(message = "Token é obrigatório")
    public String token;

    public String nome;

    @NotBlank(message = "Email é obrigatório")
    public String email;

    @NotBlank(message = "Senha é obrigatória")
    public String senha;

    public String telefone;
    public String endereco;
    public String bairro;
    public String cidade;
    public String uf;
    public String cep;
}
