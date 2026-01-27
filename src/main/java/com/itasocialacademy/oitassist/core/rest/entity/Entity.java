package com.itasocialacademy.oitassist.core.rest.entity;

import java.io.Serializable;

public interface Entity<I extends Serializable> extends Serializable {
    I getId();

    void setId(I id);
}
