package com.itasocialacademy.oitassist.usercompetition.dao.repository;

import com.itasocialacademy.oitassist.core.rest.repository.EntityRepository;
import com.itasocialacademy.oitassist.usercompetition.dao.enums.UserCompetitionStatus;
import com.itasocialacademy.oitassist.usercompetition.dao.model.UserCompetition;
import com.itasocialacademy.oitassist.usercompetition.dao.model.UserCompetitionId;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import java.util.List;

@Repository
public interface UserCompetitionRepository extends JpaRepository<UserCompetition, UserCompetitionId>, EntityRepository<UserCompetition, UserCompetitionId> {
    /** Check if entity with user id and status exists. */
    @Query(value = "SELECT EXISTS("
        + "SELECT 1 FROM user_competition uc "
        + "JOIN competitions c ON c.id = uc.competition_id "
        + "WHERE uc.author_id = :userId "
        + "AND c.competition_status::text IN :statuses)",
        nativeQuery = true)
    boolean existsByUserIdAndStatusIn(@Param("userId") Long userId,
        @Param("statuses") List<String> statuses);


    Page<UserCompetition> findAllByAuthorIdAndStatus(Long authorId, UserCompetitionStatus status, Pageable pageable);
}