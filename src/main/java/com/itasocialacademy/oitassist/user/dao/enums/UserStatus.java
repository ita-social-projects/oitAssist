package com.itasocialacademy.oitassist.user.dao.enums;

import org.springframework.modulith.NamedInterface;

@NamedInterface("UserStatus")
public enum UserStatus {
    PENDING, ACTIVE, INACTIVE, BLOCKED, DELETED
}
