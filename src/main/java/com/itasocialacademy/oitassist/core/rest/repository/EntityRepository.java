package com.itasocialacademy.oitassist.core.rest.repository;

import com.itasocialacademy.oitassist.core.rest.entity.Entity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.modulith.NamedInterface;
import java.io.Serializable;

@NoRepositoryBean
@NamedInterface("EntityRepository")
public interface EntityRepository<E extends Entity<I>, I extends Serializable> extends JpaRepository<E, I> {
}
