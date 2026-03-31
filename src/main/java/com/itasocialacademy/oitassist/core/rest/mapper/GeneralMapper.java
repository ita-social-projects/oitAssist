package com.itasocialacademy.oitassist.core.rest.mapper;

import org.springframework.modulith.NamedInterface;

@NamedInterface("GeneralMapper")
public interface GeneralMapper<E, C, U, D>
    extends DtoMapper<E, D>, CreateMapper<E, C>, UpdateMapper<E, U> {
}
