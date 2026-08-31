package com.itasocialacademy.oitassist.competition.service.interfaces;

import com.itasocialacademy.oitassist.competition.dto.request.ChangeTourStatusRequest;
import com.itasocialacademy.oitassist.competition.dto.request.ReorderToursRequest;
import com.itasocialacademy.oitassist.competition.dto.request.UpdateTourRequest;
import com.itasocialacademy.oitassist.competition.dto.request.CreateTourRequest;
import com.itasocialacademy.oitassist.competition.dto.response.TourResponse;
import java.util.List;

public interface TourService {
    /**
     * Creates a new Tour within a specific Stage.
     * <p>
     * Business Rules Validated:
     * </p>
     * <ul>
     * <li>Parent stage's competition must not be in an immutable state (e.g.,
     * ARCHIVED or locked by active participations).</li>
     * <li>Tour dates must fall completely within the parent stage's dates.</li>
     * <li>Tour title must be unique within the stage.</li>
     * <li>Sort position must be unique. If not provided, it is
     * auto-incremented.</li>
     * </ul>
     *
     * @param stageId the ID of the parent stage
     * @param request the DTO containing new tour details
     * @return the created tour mapped to a response DTO
     */
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
     * <p>
     * Business Rules Validated:
     * </p>
     * <ul>
     * <li>The tour must actually belong to the stage specified in the URL
     * path.</li>
     * <li>Parent stage's competition must not be in an immutable state.</li>
     * <li>Updated dates must remain within the parent stage's boundaries.</li>
     * <li>Updated title and sort position must not conflict with other existing
     * tours in the stage.</li>
     * <li>The request's {@code version} must match the tour's current version, or
     * the update is rejected as a stale-version conflict.</li>
     * </ul>
     *
     * @param pathStageId the stage ID from the request path
     * @param tourId      the ID of the tour to update
     * @param request     the DTO containing updated tour details and the expected
     *                    version
     * @return the updated tour response DTO
     */
    TourResponse update(Long pathStageId, Long tourId, UpdateTourRequest request);

    /**
     * Changes a Tour's execution status manually.
     * <p>
     * Business Rules Validated:
     * </p>
     * <ul>
     * <li>The tour must actually belong to the stage specified in the URL
     * path.</li>
     * <li>The parent competition must be PUBLISHED.</li>
     * <li>The parent stage must be IN_PROGRESS (unless the target status is
     * CANCELLED).</li>
     * <li>The requested transition must be valid for the tour's current execution
     * status.</li>
     * <li>Starting a tour requires the previous tour (by sort position) to be
     * FINISHED; resuming a CLOSED tour requires its finish date not to be in the
     * past.</li>
     * <li>The request's {@code version} must match the tour's current version, or
     * the change is rejected as a stale-version conflict.</li>
     * </ul>
     *
     * @param stageId the stage ID from the request path
     * @param tourId  the ID of the tour whose status is being changed
     * @param request the DTO containing the target status and the expected version
     * @return the updated tour response DTO
     */
    TourResponse changeStatus(Long stageId, Long tourId, ChangeTourStatusRequest request);

    /**
     * Reassigns the sort position of every Tour within a Stage according to the
     * given order.
     * <p>
     * Business Rules Validated:
     * </p>
     * <ul>
     * <li>Parent stage's competition must not be in an immutable state (not
     * FINISHED|ARCHIVED)</li>
     * <li>No tour in the stage may have left SCHEDULED status yet.</li>
     * <li>The request must list exactly the IDs of all tours currently under the
     * stage, with no duplicates or omissions.</li>
     * </ul>
     *
     * @param stageId the ID of the stage whose tours are being reordered
     * @param request the DTO containing the desired tour order
     * @return the reordered tours, mapped to response DTOs, in their new order
     */
    List<TourResponse> reorder(Long stageId, ReorderToursRequest request);

    /**
     * Deletes a specific Tour.
     * <p>
     * Business Rules Validated:
     * </p>
     * <ul>
     * <li>The tour must actually belong to the stage specified in the URL
     * path.</li>
     * <li>Parent stage's competition must not be in an immutable state.</li>
     * </ul>
     *
     * @param pathStageId the stage ID from the request path
     * @param tourId      the ID of the tour to delete
     */
    void delete(Long pathStageId, Long tourId);
}
