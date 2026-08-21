package com.itasocialacademy.oitassist.competition.service.interfaces;

import com.itasocialacademy.oitassist.competition.dto.request.ChangeStageStatusRequest;
import com.itasocialacademy.oitassist.competition.dto.request.CreateStageRequest;
import com.itasocialacademy.oitassist.competition.dto.request.UpdateStageRequest;
import com.itasocialacademy.oitassist.competition.dto.response.StageResponse;
import java.util.List;

public interface StageService {
    /**
     * Creates a new Stage within a specific Competition.
     * <p>
     * Business Rules Validated:
     * </p>
     * <ul>
     * <li>Parent competition must not be in an immutable state (e.g.,
     * ARCHIVED).</li>
     * <li>Stage dates must fall completely within the parent competition's
     * dates.</li>
     * <li>Stage title must be unique within the competition.</li>
     * <li>Sort position must be unique. If not provided, it is
     * auto-incremented.</li>
     * </ul>
     *
     * @param competitionId the ID of the parent competition
     * @param request       the DTO containing new stage details
     * @return the created stage mapped to a response DTO
     */
    StageResponse create(Long competitionId, CreateStageRequest request);

    /**
     * Retrieves a specific Stage by its unique ID.
     *
     * @param stageId the ID of the stage to retrieve
     * @return the stage response DTO
     */
    StageResponse getById(Long stageId);

    /**
     * Retrieves all Stages belonging to a specific Competition, ordered by their sort position.
     *
     * @param competitionId the ID of the parent competition
     * @return a list of stage response DTOs, ordered ascending by sortPosition
     */
    List<StageResponse> getAllByCompetitionId(Long competitionId);

    /**
     * Updates an existing Stage's details.
     * <p>
     * Business Rules Validated:
     * </p>
     * <ul>
     * <li>The stage must actually belong to the competition specified in the URL
     * path.</li>
     * <li>Parent competition must not be in an immutable state.</li>
     * <li>Updated dates must remain within the parent competition's
     * boundaries.</li>
     * <li>Updated title and sort position must not conflict with other existing
     * stages.</li>
     * </ul>
     *
     * @param compId  the competition ID from the request path
     * @param stageId the ID of the stage to update
     * @param request the DTO containing updated stage details
     * @return the updated stage response DTO
     */
    StageResponse update(Long compId, Long stageId, UpdateStageRequest request);

    /**
     * Changes a Stage's status manually.
     * <p>
     * Business Rules Validated:
     * </p>
     * <ul>
     * <li>The stage must actually belong to the competition specified in the URL
     * path.</li>
     * <li>The parent competition must be PUBLISHED.</li>
     * <li>The requested transition must be valid for the stage's current
     * status.</li>
     * <li>Starting a stage requires the previous stage (by sort position) to be
     * FINISHED; finishing a stage requires all its tours to be completed.</li>
     * <li>The request's {@code version} must match the stage's current version,
     * or the change is rejected as a stale-version conflict.</li>
     * </ul>
     *
     * @param compId  the competition ID from the request path
     * @param stageId the ID of the stage whose status is being changed
     * @param request the DTO containing the target status and the expected version
     * @return the updated stage response DTO
     */
    StageResponse changeStatus(Long compId, Long stageId, ChangeStageStatusRequest request);

    /**
     * Deletes a specific Stage.
     * <p>
     * Business Rules Validated:
     * </p>
     * <ul>
     * <li>The stage must actually belong to the competition specified in the URL
     * path.</li>
     * <li>Parent competition must not be in an immutable state.</li>
     * </ul>
     *
     * @param compId  the competition ID from the request path
     * @param stageId the ID of the stage to delete
     */
    void delete(Long compId, Long stageId);
}
