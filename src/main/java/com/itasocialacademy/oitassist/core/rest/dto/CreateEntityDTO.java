package com.itasocialacademy.oitassist.core.rest.dto;

import org.springframework.modulith.NamedInterface;
import java.io.Serializable;

@NamedInterface("CreateEntityDTO")
public interface CreateEntityDTO<I extends Serializable> extends EntityDTO<I> {
}
