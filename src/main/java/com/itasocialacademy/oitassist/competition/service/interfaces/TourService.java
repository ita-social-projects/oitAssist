package com.itasocialacademy.oitassist.competition.service.interfaces;

import com.itasocialacademy.oitassist.competition.api.dto.UpdateTourRequest;
import com.itasocialacademy.oitassist.competition.dao.dto.request.CreateTourRequest;
import com.itasocialacademy.oitassist.competition.dao.dto.response.TourResponse;
import java.util.List;

public interface TourService {
    TourResponse create(Long stageId, CreateTourRequest request);

    /**
     * Retrieves a specific Tour by its unique ID.
     *
     * @param tourId the ID of the tour to retrieve
     * @return the tour response DTO
     */
    TourResponse getById(Long tourId);

    /**
     * Retrieves all Tours belonging to a specific Stage, ordered by their sort
     * position.
     *
     * @param stageId the ID of the parent stage
     * @return a list of tour response DTOs
     */
    List<TourResponse> getAllByStageId(Long stageId);

    /**
     * Updates an existing Tour's details.
     *
     * @param pathStageId the stage ID from the request path
     * @param tourId      the ID of the tour to update
     * @param request     the DTO containing updated tour details
     * @return the updated tour response DTO
     */
    TourResponse update(Long pathStageId, Long tourId, UpdateTourRequest request);

    /**
     * Deletes a specific Tour.
     *
     * @param pathStageId the stage ID from the request path
     * @param tourId      the ID of the tour to delete
     */
    void delete(Long pathStageId, Long tourId);
}
