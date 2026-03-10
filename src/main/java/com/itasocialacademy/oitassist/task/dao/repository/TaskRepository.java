package com.itasocialacademy.oitassist.task.dao.repository;

import com.itasocialacademy.oitassist.core.rest.repository.EntityRepository;
import com.itasocialacademy.oitassist.task.dao.model.Task;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskRepository extends EntityRepository<Task, Long> {
}
