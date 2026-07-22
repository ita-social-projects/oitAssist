package com.itasocialacademy.oitassist.task.dao.repository;

import com.itasocialacademy.oitassist.task.dao.model.TaskBody;
import com.itasocialacademy.oitassist.task.dao.model.TaskTitleView;
import com.itasocialacademy.oitassist.task.dto.response.TaskResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Collection;
import java.util.List;

@Repository
public interface TaskBodyRepository extends JpaRepository<TaskBody, Long> {
    Page<TaskResponseDTO> findAllByOwnerId(Long currentUserId, Pageable pageable);

    @Query(value = "SELECT t.id AS id, t.title AS title FROM TaskBody t WHERE t.id IN :ids")
    List<TaskTitleView> findTitlesByIds(@Param("ids") Collection<Long> taskIds);
}
