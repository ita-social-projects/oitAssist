package com.itasocialacademy.oitassist.participation.service;

import com.itasocialacademy.oitassist.competition.api.CompetitionFacade;
import com.itasocialacademy.oitassist.competition.api.dto.StageDetail;
import com.itasocialacademy.oitassist.competition.exceptions.CompetitionHierarchyValidationException;
import com.itasocialacademy.oitassist.competition.exceptions.CompetitionNotFoundException;
import com.itasocialacademy.oitassist.competition.exceptions.StageNotFoundException;
import com.itasocialacademy.oitassist.participation.dao.dto.response.ParticipationListItemResponse;
import com.itasocialacademy.oitassist.participation.dao.model.Participation;
import com.itasocialacademy.oitassist.participation.dao.repository.ParticipationRepository;
import com.itasocialacademy.oitassist.participation.dao.specification.ParticipationSpecification;
import com.itasocialacademy.oitassist.participation.mapper.UserEnrollmentAssembler;
import com.itasocialacademy.oitassist.participation.mapper.interfaces.UserSummaryMapper;
import com.itasocialacademy.oitassist.participation.service.interfaces.ParticipationService;
import com.itasocialacademy.oitassist.user.api.interfaces.UserFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ParticipationServiceImpl implements ParticipationService {
    private final ParticipationRepository repository;
    private final CompetitionFacade competitionFacade;
    private final UserFacade userFacade;
    private final UserEnrollmentAssembler enrollmentAssembler;
    private final UserSummaryMapper userSummaryMapper;

    @Override
    public Page<ParticipationListItemResponse> getParticipationList(
        Long competitionId,
        Long stageId,
        String search,
        Pageable pageable) {
        validateCompetitionAndStageInfo(competitionId, stageId);
        List<Long> candidateUserIds = repository.findAll(
            ParticipationSpecification.hasCompetitionAndStage(competitionId, stageId))
            .stream()
            .map(Participation::getUserId)
            .distinct()
            .toList();
        if (candidateUserIds.isEmpty()) {
            return Page.empty(pageable);
        }
        Optional<List<Long>> matchingUserIds = userFacade.findUserIdsBySearchWithinIds(search, candidateUserIds);
        List<Long> filterIds = matchingUserIds.orElse(candidateUserIds);
        if (matchingUserIds.isPresent() && matchingUserIds.get().isEmpty()) {
            return Page.empty(pageable);
        }

        Page<Participation> participations = repository.findAll(
            ParticipationSpecification.hasCompetitionAndStage(competitionId, stageId)
                .and(ParticipationSpecification.userIdIn(filterIds)),
            pageable);
        List<ParticipationListItemResponse> responses = enrollmentAssembler.enrichWithUser(
            participations.toList(), Participation::getUserId,
            (participation, user) -> new ParticipationListItemResponse(
                participation.getId(),
                participation.getUserId(),
                userSummaryMapper.toUserSummary(user)));

        return new PageImpl<>(responses, pageable, participations.getTotalElements());
    }

    private void validateCompetitionAndStageInfo(Long competitionId, Long stageId) {
        competitionFacade.findCompetitionById(competitionId)
            .orElseThrow(() -> new CompetitionNotFoundException(competitionId));
        StageDetail stageDetail = competitionFacade.findStageById(stageId)
            .orElseThrow(() -> new StageNotFoundException(stageId));
        validateHierarchy(competitionId, stageDetail);
    }

    private void validateHierarchy(Long competitionId, StageDetail stageDetail) {
        if (!stageDetail.competitionId().equals(competitionId)) {
            throw new CompetitionHierarchyValidationException("Specified stage does not belong to this competition");
        }
    }

    @Override
    public boolean competitionHasParticipants(Long competitionId) {
        return repository.existsByCompetitionId(competitionId);
    }

    @Override
    public boolean stageHasParticipants(Long stageId) {
        return repository.existsByStageId(stageId);
    }

    @Override
    public boolean isUserParticipant(Long userId, Long competitionId, Long stageId) {
        return repository.existsByUserIdAndCompetitionIdAndStageId(userId, competitionId, stageId);
    }

    @Override
    public boolean isUserAStageParticipant(Long userId, Long stageId) {
        return repository.existsByUserIdAndStageId(userId, stageId);
    }
}
