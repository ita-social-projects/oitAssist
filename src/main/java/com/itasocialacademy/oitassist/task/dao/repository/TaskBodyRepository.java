package com.itasocialacademy.oitassist.task.dao.repository;

import com.itasocialacademy.oitassist.task.dao.model.TaskBody;
import com.itasocialacademy.oitassist.task.dto.response.TaskResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskBodyRepository extends JpaRepository<TaskBody, Long> {
    Page<TaskResponseDTO> findAllByOwnerId(Long currentUserId, Pageable pageable);
}
