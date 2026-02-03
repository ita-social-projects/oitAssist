package com.itasocialacademy.oitassist.core.rest.controller;

import com.itasocialacademy.oitassist.core.rest.controller.interfaces.RestController;
import com.itasocialacademy.oitassist.core.rest.dto.CreateEntityDTO;
import com.itasocialacademy.oitassist.core.rest.dto.EntityDTO;
import com.itasocialacademy.oitassist.core.rest.dto.UpdateEntityDTO;
import com.itasocialacademy.oitassist.core.rest.service.interfaces.BaseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.modulith.NamedInterface;
import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

@Slf4j
@NamedInterface("AbstractRestControllerImpl")
public abstract class AbstractRestControllerImpl<I extends Serializable, //
    C extends CreateEntityDTO<I>, U extends UpdateEntityDTO<I>, //
    D extends EntityDTO<I>, S extends BaseService<I, C, U, D>>
    implements RestController<I, C, U, D> {
    private final String dtoType;
    private final String createDtoType;
    private final String updateDtoType;
    protected final S service;

    protected AbstractRestControllerImpl(S service) {
        this.service = service;
        Type[] argumentTypes = ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments();
        this.createDtoType = ((Class<?>) argumentTypes[1]).getSimpleName();
        this.updateDtoType = ((Class<?>) argumentTypes[2]).getSimpleName();
        this.dtoType = ((Class<?>) argumentTypes[3]).getSimpleName();
    }

    public ResponseEntity<D> save(C createDto) {
        log.debug("save<{}>(entity={})", createDtoType, createDto);
        return ResponseEntity.ok().body(service.save(createDto));
    }

    public ResponseEntity<D> update(U updateDTO) {
        log.debug("update<{}>(entity={})", updateDtoType, updateDTO);
        return ResponseEntity.ok().body(service.update(updateDTO));
    }

    public ResponseEntity<Void> delete(I id) {
        log.debug("delete<{}>(id={})", dtoType, id);
        service.delete(id);
        return ResponseEntity.ok().build();
    }

    public ResponseEntity<D> getById(I id) {
        log.debug("getById<{}>(id={})", dtoType, id);
        return ResponseEntity.ok().body(service.getById(id));
    }
}
