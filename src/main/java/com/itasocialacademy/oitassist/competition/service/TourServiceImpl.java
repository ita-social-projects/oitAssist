package com.itasocialacademy.oitassist.competition.service;

import com.itasocialacademy.oitassist.competition.dao.dto.request.CreateTourRequest;
import com.itasocialacademy.oitassist.competition.dao.dto.response.StageResponse;
import com.itasocialacademy.oitassist.competition.dao.dto.response.TourResponse;
import com.itasocialacademy.oitassist.competition.dao.model.Tour;
import com.itasocialacademy.oitassist.competition.dao.repository.TourRepository;
import com.itasocialacademy.oitassist.competition.exceptions.CompetitionHierarchyValidationException;
import com.itasocialacademy.oitassist.competition.mapper.TourMapper;
import com.itasocialacademy.oitassist.competition.service.interfaces.CompetitionService;
import com.itasocialacademy.oitassist.competition.service.interfaces.StageService;
import com.itasocialacademy.oitassist.competition.service.interfaces.TourService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TourServiceImpl implements TourService {
    private final TourRepository tourRepository;
    private final StageService stageService;
    private final CompetitionService competitionService;
    private final TourMapper mapper;

    @Override
    @Transactional
    public TourResponse create(Long stageId, CreateTourRequest request) {
        StageResponse stage = stageService.getById(stageId);

        competitionService.validateHierarchyImmutability(stage.competitionId());

        Tour tour = mapper.toEntity(request);
        tour.setStageId(stageId);

        validateDates(stage, tour);

        if (tourRepository.existsByStageIdAndTitle(stageId, tour.getTitle())) {
            throw new CompetitionHierarchyValidationException("Tour title already exists in this stage.");
        }

        if (tour.getSortPosition() == null) {
            Tour lastTour = tourRepository.findTopByStageIdOrderBySortPositionDesc(stageId);
            tour.setSortPosition(lastTour != null ? (short) (lastTour.getSortPosition() + 1) : 1);
        }

        return mapper.toResponse(tourRepository.save(tour));
    }

    private void validateDates(StageResponse parent, Tour child) {
        if (child.getDateStart().isBefore(parent.dateStart())
            || child.getDateFinish().isAfter(parent.dateFinish())) {
            throw new CompetitionHierarchyValidationException(
                "Tour dates (%s - %s) must be within Stage dates (%s - %s)".formatted(
                    child.getDateStart(), child.getDateFinish(), parent.dateStart(), parent.dateStart()));
        }
    }
}
