package com.itasocialacademy.oitassist.competition.dao.repository;

import com.itasocialacademy.oitassist.competition.dao.model.Competition;
import com.itasocialacademy.oitassist.core.rest.repository.EntityRepository;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface CompetitionRepository extends EntityRepository<Competition, Long> {
    @Query("SELECT DISTINCT c.year FROM Competition c ORDER BY c.year DESC")
    List<Integer> getYears ();
}
