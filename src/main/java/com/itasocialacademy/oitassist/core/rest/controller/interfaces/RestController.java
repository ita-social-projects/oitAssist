package com.itasocialacademy.oitassist.core.rest.controller.interfaces;

import com.itasocialacademy.oitassist.core.rest.dto.CreateEntityDTO;
import com.itasocialacademy.oitassist.core.rest.dto.EntityDTO;
import com.itasocialacademy.oitassist.core.rest.dto.UpdateEntityDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.modulith.NamedInterface;
import org.springframework.web.bind.annotation.*;
import java.io.Serializable;

/*
 * Annotation @RequestMapping must be used in the implementation class so that each
 * controllers has their own path. Because of this, annotation @RequestMapping on this
 * interface is overridden and never used
 */
@RequestMapping(value = "/api")
@NamedInterface("RestController")
public interface RestController<I extends Serializable, C extends CreateEntityDTO<I>, //
    U extends UpdateEntityDTO<I>, R extends EntityDTO<I>> {
    ResponseEntity<R> save(@RequestBody C dto);

    ResponseEntity<R> update(@RequestBody U dto);

    ResponseEntity<Void> delete(@PathVariable I id);

    ResponseEntity<R> getById(@PathVariable I id);
}
