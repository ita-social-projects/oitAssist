package com.itasocialacademy.oitassist.competition.service;

import com.itasocialacademy.oitassist.competition.api.dto.UpdateTourRequest;
import com.itasocialacademy.oitassist.competition.dao.dto.request.CreateTourRequest;
import com.itasocialacademy.oitassist.competition.dao.dto.response.TourResponse;
import com.itasocialacademy.oitassist.competition.dao.model.Tour;
import com.itasocialacademy.oitassist.competition.dao.repository.TourRepository;
import com.itasocialacademy.oitassist.competition.exceptions.CompetitionHierarchyValidationException;
import com.itasocialacademy.oitassist.competition.exceptions.TourNotFoundException;
import com.itasocialacademy.oitassist.competition.mapper.TourMapper;
import com.itasocialacademy.oitassist.competition.service.interfaces.TourService;
import com.itasocialacademy.oitassist.competition.validation.HierarchyValidator;
import java.util.List;
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
            Tour lastTour = tourRepository.findTopByStageIdOrderBySortPositionDesc(stageId);
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
            .collect(Collectors.toList());
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
    public void delete(Long pathStageId, Long tourId) {
        Tour tour = tourRepository.findById(tourId)
            .orElseThrow(() -> new TourNotFoundException(tourId));

        validator.validateTourEligibility(pathStageId, tour.getStageId());
        validator.validateImmutabilityByStageId(tour.getStageId());

        tourRepository.delete(tour);
    }
}
