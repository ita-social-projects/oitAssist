package com.itasocialacademy.oitassist.core.rest.mapper;

import org.mapstruct.MappingTarget;
import org.springframework.modulith.NamedInterface;

@NamedInterface("UpdateMapper")
public interface UpdateMapper<E, U> {
    void merge(U updateDto, @MappingTarget E entity);
}
