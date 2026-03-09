package com.itasocialacademy.oitassist.core.rest.service;

import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.core.exceptions.NotFoundException;
import com.itasocialacademy.oitassist.core.rest.entity.Entity;
import com.itasocialacademy.oitassist.core.rest.dto.CreateEntityDTO;
import com.itasocialacademy.oitassist.core.rest.dto.EntityDTO;
import com.itasocialacademy.oitassist.core.rest.dto.UpdateEntityDTO;
import com.itasocialacademy.oitassist.core.rest.mapper.GeneralMapper;
import com.itasocialacademy.oitassist.core.rest.repository.EntityRepository;
import com.itasocialacademy.oitassist.core.rest.service.interfaces.BaseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.modulith.NamedInterface;
import org.springframework.transaction.annotation.Transactional;
import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

@Transactional
@Slf4j
@NamedInterface("AbstractServiceImpl")
public abstract class AbstractServiceImpl<I extends Serializable, E extends Entity<I>, //
    C extends CreateEntityDTO<I>, U extends UpdateEntityDTO<I>, D extends EntityDTO<I>, //
    R extends EntityRepository<E, I>, M extends GeneralMapper<E, C, U, D>> //
    implements BaseService<I, C, U, D> {
    private final String entityType;
    private final String createDtoType;
    private final String updateDtoType;
    protected final R repository;
    protected final M mapper;

    protected AbstractServiceImpl(R repository, M mapper) {
        this.repository = repository;
        this.mapper = mapper;
        Type[] argumentTypes = ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments();
        this.entityType = ((Class<?>) argumentTypes[1]).getSimpleName();
        this.createDtoType = ((Class<?>) argumentTypes[2]).getSimpleName();
        this.updateDtoType = ((Class<?>) argumentTypes[3]).getSimpleName();
    }

    @Override
    @Transactional(readOnly = true)
    public D getById(I id) {
        log.debug("getById<{}>(id={})", entityType, id);
        return repository.findById(id).map(mapper::toDTO)
            .orElseThrow(() -> new NotFoundException("Entity " + entityType, ErrorCode.ENTITY_NOT_FOUND));
    }

    @Override
    public D save(C dto) {
        log.debug("save<{}>(dto={})", createDtoType, dto);
        E entity = mapper.toEntity(dto);
        beforeSave(entity, dto);
        return mapper.toDTO(repository.save(entity));
    }

    @Override
    public D update(U dto) {
        log.debug("update<{}>(dto={})", updateDtoType, dto);
        E entity = repository.findById(dto.getId())
            .orElseThrow(() -> new NotFoundException("Entity " + entityType, ErrorCode.ENTITY_NOT_FOUND));
        beforeUpdate(entity, dto);
        mapper.merge(dto, entity);

        return mapper.toDTO(repository.save(entity));
    }

    @Override
    public void delete(I id) {
        log.debug("delete<{}>(id={})", entityType, id);
        repository.findById(id)
            .orElseThrow(() -> new NotFoundException("Entity " + entityType, ErrorCode.ENTITY_NOT_FOUND));
        repository.deleteById(id);
    }

    protected void beforeSave(E entity, C dto) {
    }

    protected void beforeUpdate(E entity, U dto) {
    }
}
