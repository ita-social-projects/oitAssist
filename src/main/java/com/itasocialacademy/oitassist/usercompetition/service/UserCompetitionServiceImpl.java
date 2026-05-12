package com.itasocialacademy.oitassist.usercompetition.service;

import com.itasocialacademy.oitassist.competition.dao.enums.CompetitionStatus;
import com.itasocialacademy.oitassist.usercompetition.dao.repository.UserCompetitionRepository;
import com.itasocialacademy.oitassist.usercompetition.service.interfaces.UserCompetitionService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserCompetitionServiceImpl implements UserCompetitionService {

    private final UserCompetitionRepository repository;

    public UserCompetitionServiceImpl(UserCompetitionRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean hasActiveCompetitions(Long userId, List<CompetitionStatus> statuses) {
        List<String> statusStrings = statuses.stream()
                .map(Enum::name)
                .toList();

        return repository.existsByUserIdAndStatusIn(userId, statusStrings);
    }
}
