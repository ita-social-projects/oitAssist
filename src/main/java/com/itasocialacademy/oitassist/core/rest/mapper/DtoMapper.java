package com.itasocialacademy.oitassist.core.rest.mapper;

import org.springframework.modulith.NamedInterface;

@NamedInterface("DtoMapper")
public interface DtoMapper<E, D> {
    D toDto(E entity);
}
