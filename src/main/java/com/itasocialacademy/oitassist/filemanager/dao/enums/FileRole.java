package com.itasocialacademy.oitassist.filemanager.dao.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.modulith.NamedInterface;

@NamedInterface("FileRole")
@Getter
@RequiredArgsConstructor
public enum FileRole {
    GENERIC("Generic"),
    PROBLEM("Problem"),
    REFERENCE("Reference"),
    SOLUTION("Solution");

    private final String roleName;
}