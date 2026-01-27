package com.itasocialacademy.oitassist.core.rest.dto;

import org.springframework.modulith.NamedInterface;
import java.io.Serializable;

@NamedInterface("UpdateEntityDTO")
public interface UpdateEntityDTO<I extends Serializable> extends EntityDTO<I> {
    I getId();
}
