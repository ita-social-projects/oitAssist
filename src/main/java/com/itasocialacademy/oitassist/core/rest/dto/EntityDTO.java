package com.itasocialacademy.oitassist.core.rest.dto;

import org.springframework.modulith.NamedInterface;
import java.io.Serializable;

@NamedInterface("EntityDTO")
public interface EntityDTO<I extends Serializable> extends Serializable {
}
