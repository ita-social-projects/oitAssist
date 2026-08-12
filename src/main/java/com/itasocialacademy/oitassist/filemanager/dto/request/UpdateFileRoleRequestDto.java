package com.itasocialacademy.oitassist.filemanager.dto.request;

import com.itasocialacademy.oitassist.filemanager.dao.enums.FileRole;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateFileRoleRequestDto {
    @NotNull(message = "New role must not be null")
    private FileRole newRole;
}
