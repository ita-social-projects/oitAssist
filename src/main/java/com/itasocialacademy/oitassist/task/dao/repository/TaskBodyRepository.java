package com.itasocialacademy.oitassist.task.dao.repository;

import com.itasocialacademy.oitassist.task.dao.model.TaskBody;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskBodyRepository extends JpaRepository<TaskBody, Long> {
}
