package com.itasocialacademy.oitassist.security.dao.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Token Request dto")
public class TokenRequest {
    @Schema(description = "User Email", example = "mail@gmail.com")
    private String username;
    @Schema(description = "User Password", example = "password")
    private String password;
}
