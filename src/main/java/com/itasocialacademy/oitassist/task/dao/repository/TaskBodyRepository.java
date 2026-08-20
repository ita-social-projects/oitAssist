package com.itasocialacademy.oitassist.task.dao.repository;

import com.itasocialacademy.oitassist.task.dao.model.TaskBody;
import com.itasocialacademy.oitassist.task.dao.model.TaskTitleView;
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
    @Query("""
        SELECT t
        FROM TaskBody t
        JOIN t.owners o
        WHERE o.id.ownerId = :ownerId AND LOWER(t.title) LIKE LOWER(CONCAT('%', :search, '%')) ESCAPE '\\'
        """)
    Page<TaskBody> findAllByOwnerId(@Param("ownerId") Long ownerId, @Param("search") String search, Pageable pageable);

    @Query(value = "SELECT t.id AS id, t.title AS title FROM TaskBody t WHERE t.id IN :ids")
    List<TaskTitleView> findTitlesByIds(@Param("ids") Collection<Long> taskIds);

    @Query("""
        SELECT t
        FROM TaskBody t
        WHERE LOWER(t.title) LIKE LOWER(CONCAT('%', :search, '%')) ESCAPE '\\'
        """)
    Page<TaskBody> findAllByTitleLike(String search, Pageable pageable);
}
