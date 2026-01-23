package com.itasocialacademy.oitassist.user.dao.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CreateUserResponse {
    private String email;
    private String password;
    private String firstName;
    private String lastName;
    private String middleName;
    private String phoneNumber;
}
