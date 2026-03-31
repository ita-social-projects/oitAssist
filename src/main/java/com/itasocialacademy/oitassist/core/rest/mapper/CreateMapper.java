package com.itasocialacademy.oitassist.core.rest.mapper;

import org.springframework.modulith.NamedInterface;

@NamedInterface("CreateMapper")
public interface CreateMapper<E, C> {
    E toEntity(C createDto);
}
