package com.itasocialacademy.oitassist.core.rest.service.interfaces;

import com.itasocialacademy.oitassist.core.rest.dto.CreateEntityDTO;
import com.itasocialacademy.oitassist.core.rest.dto.EntityDTO;
import com.itasocialacademy.oitassist.core.rest.dto.UpdateEntityDTO;
import org.springframework.modulith.NamedInterface;
import java.io.Serializable;

@NamedInterface("BaseService")
public interface BaseService<I extends Serializable, C extends CreateEntityDTO<I>, //
    U extends UpdateEntityDTO<I>, R extends EntityDTO<I>> {
    R save(C entity);

    R update(U entity);

    void delete(I entityId);

    R getById(I id);
}
