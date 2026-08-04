package com.itasocialacademy.oitassist.competition.service;

import com.itasocialacademy.oitassist.competition.dao.enums.ExecutionStatus;
import com.itasocialacademy.oitassist.competition.dao.model.Tour;
import com.itasocialacademy.oitassist.competition.dao.repository.TourRepository;
import com.itasocialacademy.oitassist.competition.dto.request.ChangeTourStatusRequest;
import com.itasocialacademy.oitassist.competition.dto.request.CreateTourRequest;
import com.itasocialacademy.oitassist.competition.dto.request.ReorderToursRequest;
import com.itasocialacademy.oitassist.competition.dto.request.UpdateTourRequest;
import com.itasocialacademy.oitassist.competition.dto.response.TourResponse;
import com.itasocialacademy.oitassist.competition.exceptions.CompetitionHierarchyValidationException;
import com.itasocialacademy.oitassist.competition.exceptions.TourNotFoundException;
import com.itasocialacademy.oitassist.competition.mapper.TourMapper;
import com.itasocialacademy.oitassist.competition.service.interfaces.TourService;
import com.itasocialacademy.oitassist.competition.validation.HierarchyValidator;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TourServiceImpl implements TourService {
    private final TourRepository tourRepository;
    private final TourMapper mapper;
    private final HierarchyValidator validator;

    @Override
    @Transactional
    public TourResponse create(Long stageId, CreateTourRequest request) {
        validator.validateImmutabilityByStageId(stageId);
        validator.validateTourDates(stageId, request.dateStart(), request.dateFinish());

        Tour tour = mapper.toEntity(request);
        tour.setStageId(stageId);

        if (tourRepository.existsByStageIdAndTitle(stageId, tour.getTitle())) {
            throw new CompetitionHierarchyValidationException("Tour title already exists in this stage.");
        }

        if (tour.getSortPosition() == null) {
            Tour lastTour = tourRepository.findTopByStageIdOrderBySortPositionDesc(stageId).orElse(null);
            tour.setSortPosition(lastTour != null ? (short) (lastTour.getSortPosition() + 1) : 1);
        }

        return mapper.toResponse(tourRepository.save(tour));
    }

    @Override
    @Transactional(readOnly = true)
    public TourResponse getById(Long tourId) {
        Tour tour = tourRepository.findById(tourId)
            .orElseThrow(() -> new TourNotFoundException(tourId));
        validator.checkVisibilityAccessByStageId(tour.getStageId());
        return mapper.toResponse(tour);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TourResponse> getAllByStageId(Long stageId) {
        validator.checkVisibilityAccessByStageId(stageId);
        return tourRepository.findAllByStageIdOrderBySortPositionAsc(stageId)
            .stream()
            .map(mapper::toResponse)
            .toList();
    }

    @Override
    @Transactional
    public TourResponse update(Long pathStageId, Long tourId, UpdateTourRequest request) {
        Tour tour = tourRepository.findById(tourId)
            .orElseThrow(() -> new TourNotFoundException(tourId));

        validator.validateTourEligibility(pathStageId, tour.getStageId());
        validator.validateImmutabilityByStageId(tour.getStageId());
        validator.validateTourDates(tour.getStageId(), request.dateStart(), request.dateFinish());

        tour.setTitle(request.title());
        tour.setDescription(request.description());
        tour.setDateStart(request.dateStart());
        tour.setDateFinish(request.dateFinish());
        tour.setLocation(request.location());

        if (request.sortPosition() != null) {
            tour.setSortPosition(request.sortPosition());
        }

        // Check for uniqueness excluding current tour
        tourRepository.findAllByStageIdOrderBySortPositionAsc(tour.getStageId())
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
    public TourResponse changeStatus(Long stageId, Long tourId, ChangeTourStatusRequest request) {
        Tour tour = tourRepository.findById(tourId)
            .orElseThrow(() -> new TourNotFoundException(tourId));

        validator.validateTourEligibility(stageId, tour.getStageId());
        validator.checkIfCompetitionPublishedByStageId(stageId);
        validator.checkIfStageInProgress(stageId, request.status());
        validator.validateTourStatusTransition(tour.getExecutionStatus(), request.status());

        if (request.status() == ExecutionStatus.IN_PROGRESS) {
            if (tour.getExecutionStatus() == ExecutionStatus.SCHEDULED) {
                // Simple start
                validator.validateTourEligibilityToStart(tour);
            } else if (tour.getExecutionStatus() == ExecutionStatus.CLOSED) {
                // Resume after closure
                validator.validateTourEligibilityToResume(tour);
            }
        }
        tour.setExecutionStatus(request.status());

        // place for publishing events (TBD)
        // if (request.status() == ExecutionStatus.FINISHED) {
        // eventPublisher.publishEvent(new TourFinishedEvent(tour.getId()));
        // }

        Tour updatedTour = tourRepository.save(tour);
        return mapper.toResponse(updatedTour);
    }

    @Override
    @Transactional
    public List<TourResponse> reorder(Long stageId, ReorderToursRequest request) {
        // validator.validateImmutabilityByStageId(stageId);
        validator.validateToursNotStartedByStageId(stageId);

        List<Tour> existingTours = tourRepository.findAllByStageIdOrderBySortPositionAsc(stageId);
        Map<Long, Tour> tourById = existingTours.stream()
            .collect(Collectors.toMap(Tour::getId, Function.identity()));

        Set<Long> requestedIds = new LinkedHashSet<>(request.tourIds());
        if (requestedIds.size() != request.tourIds().size()) {
            throw new CompetitionHierarchyValidationException(
                "Reorder request contains duplicate tour IDs.");
        }
        if (!requestedIds.equals(tourById.keySet())) {
            throw new CompetitionHierarchyValidationException(
                "Reorder request must include exactly all tours currently under this stage.");
        }

        short position = 1;
        List<Tour> reordered = new ArrayList<>();
        for (Long tourId : request.tourIds()) {
            Tour tour = tourById.get(tourId);
            tour.setSortPosition(position++);
            reordered.add(tour);
        }

        return tourRepository.saveAll(reordered).stream()
            .map(mapper::toResponse)
            .toList();
    }

    @Override
    @Transactional
    public void delete(Long pathStageId, Long tourId) {
        Tour tour = tourRepository.findById(tourId)
            .orElseThrow(() -> new TourNotFoundException(tourId));

        validator.validateTourEligibility(pathStageId, tour.getStageId());
        validator.validateImmutabilityByStageId(tour.getStageId());

        tourRepository.delete(tour);
    }
}
