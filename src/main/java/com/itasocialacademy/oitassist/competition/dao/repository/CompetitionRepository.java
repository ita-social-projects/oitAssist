package com.itasocialacademy.oitassist.competition.dao.repository;

import com.itasocialacademy.oitassist.competition.dao.model.Competition;
import com.itasocialacademy.oitassist.core.rest.repository.EntityRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CompetitionRepository extends EntityRepository<Competition, Long> {
}
