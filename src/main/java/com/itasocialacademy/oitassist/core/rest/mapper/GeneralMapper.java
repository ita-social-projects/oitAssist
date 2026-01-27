package com.itasocialacademy.oitassist.core.rest.mapper;

import org.mapstruct.MappingTarget;
import org.springframework.modulith.NamedInterface;

@NamedInterface("GeneralMapper")
public interface GeneralMapper<E, C, U, D> {
    D toDTO(E e);

    E toEntity(C d);

    void merge(U d, @MappingTarget E e);
}
