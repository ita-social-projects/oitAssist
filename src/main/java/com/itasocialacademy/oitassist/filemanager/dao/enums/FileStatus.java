package com.itasocialacademy.oitassist.filemanager.dao.enums;

import org.springframework.modulith.NamedInterface;

@NamedInterface("FileStatus")
public enum FileStatus {
    TEMPORARY,
    ATTACHED,
    FAILED,
    SOFT_DELETED,
    HARD_DELETED
}
