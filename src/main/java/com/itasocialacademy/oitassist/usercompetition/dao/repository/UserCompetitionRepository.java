package com.itasocialacademy.oitassist.usercompetition.dao.repository;

import com.itasocialacademy.oitassist.competition.dao.enums.CompetitionStatus;
import com.itasocialacademy.oitassist.usercompetition.dao.model.UserCompetition;
import com.itasocialacademy.oitassist.usercompetition.dao.model.UserCompetitionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.modulith.NamedInterface;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@NamedInterface("UserCompetitionRepository")
public interface UserCompetitionRepository extends JpaRepository<UserCompetition, UserCompetitionId> {
    /** Check if entity with user id and status exists*/
    @Query(value = "SELECT EXISTS(" +
            "SELECT 1 FROM user_competition uc " +
            "JOIN competitions c ON c.id = uc.competition_id " +
            "WHERE uc.author_id = :userId " +
            "AND c.competition_status::text IN :statuses)",
            nativeQuery = true)
    boolean existsByUserIdAndStatusIn(@Param("userId") Long userId,
                                      @Param("statuses") List<String> statuses);
}