package com.itasocialacademy.oitassist.competition.service;

import com.itasocialacademy.oitassist.competition.api.dto.UpdateTourRequest;
import com.itasocialacademy.oitassist.competition.dao.dto.request.CreateTourRequest;
import com.itasocialacademy.oitassist.competition.dao.dto.response.StageResponse;
import com.itasocialacademy.oitassist.competition.dao.dto.response.TourResponse;
import com.itasocialacademy.oitassist.competition.dao.model.Tour;
import com.itasocialacademy.oitassist.competition.dao.repository.TourRepository;
import com.itasocialacademy.oitassist.competition.exceptions.CompetitionHierarchyValidationException;
import com.itasocialacademy.oitassist.competition.exceptions.TourNotFoundException;
import com.itasocialacademy.oitassist.competition.mapper.TourMapper;
import com.itasocialacademy.oitassist.competition.service.interfaces.CompetitionService;
import com.itasocialacademy.oitassist.competition.service.interfaces.StageService;
import com.itasocialacademy.oitassist.competition.service.interfaces.TourService;
import java.util.List;
import java.util.stream.Collectors;
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

    @Override
    @Transactional(readOnly = true)
    public TourResponse getById(Long tourId) {
        return tourRepository.findById(tourId)
            .map(mapper::toResponse)
            .orElseThrow(() -> new TourNotFoundException(tourId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TourResponse> getAllByStageId(Long stageId) {
        return tourRepository.findAllByStageIdOrderBySortPositionAsc(stageId)
            .stream()
            .map(mapper::toResponse)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public TourResponse update(Long pathStageId, Long tourId, UpdateTourRequest request) {
        Tour tour = tourRepository.findById(tourId)
            .orElseThrow(() -> new TourNotFoundException(tourId));

        Long entityStageId = tour.getStageId();
        validateTourEligibility(pathStageId, entityStageId);

        StageResponse stage = stageService.getById(entityStageId);
        competitionService.validateHierarchyImmutability(stage.competitionId());

        tour.setTitle(request.title());
        tour.setDescription(request.description());
        tour.setDateStart(request.dateStart());
        tour.setDateFinish(request.dateFinish());
        tour.setLocation(request.location());

        if (request.sortPosition() != null) {
            tour.setSortPosition(request.sortPosition());
        }

        validateDates(stage, tour);

        // Check for uniqueness excluding current tour
        tourRepository.findAllByStageIdOrderBySortPositionAsc(entityStageId)
            .stream()
            .filter(existing -> !existing.getId().equals(tourId))
            .forEach(existing -> {
                if (existing.getTitle().equals(tour.getTitle())) {
                    throw new CompetitionHierarchyValidationException("Tour title already exists in this stage.");
                }
                if (existing.getSortPosition().equals(tour.getSortPosition())) {
                    throw new CompetitionHierarchyValidationException(
                        "Sort position " + tour.getSortPosition() + " is already taken.");
                }
            });

        return mapper.toResponse(tourRepository.save(tour));
    }

    @Override
    @Transactional
    public void delete(Long pathStageId, Long tourId) {
        Tour tour = tourRepository.findById(tourId)
            .orElseThrow(() -> new TourNotFoundException(tourId));

        Long entityStageId = tour.getStageId();
        validateTourEligibility(pathStageId, entityStageId);

        StageResponse stage = stageService.getById(entityStageId);
        competitionService.validateHierarchyImmutability(stage.competitionId());

        tourRepository.delete(tour);
    }

    private void validateTourEligibility(Long pathStageId, Long entityStageId) {
        if (!pathStageId.equals(entityStageId)) {
            throw new CompetitionHierarchyValidationException("Tour does not belong to this stage");
        }
    }

    private void validateDates(StageResponse parent, Tour child) {
        if (child.getDateStart().isBefore(parent.dateStart())
            || child.getDateFinish().isAfter(parent.dateFinish())) {
            throw new CompetitionHierarchyValidationException(
                "Tour dates (%s - %s) must be within Stage dates (%s - %s)".formatted(
                    child.getDateStart(), child.getDateFinish(), parent.dateStart(), parent.dateFinish()));
        }
    }
}
