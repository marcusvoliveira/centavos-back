package com.incentive.dto;

import com.incentive.entity.Role;
import com.incentive.entity.User;

public class UserDTO {

    public Long id;
    public String name;
    public String email;
    public String cpf;
    public String phone;
    public Role role;
    public boolean emailVerified;

    public static UserDTO from(User user) {
        UserDTO dto = new UserDTO();
        dto.id = user.id;
        dto.name = user.name;
        dto.email = user.email;
        dto.cpf = user.cpf;
        dto.phone = user.phone;
        dto.role = user.role;
        dto.emailVerified = user.emailVerified;
        return dto;
    }
}
