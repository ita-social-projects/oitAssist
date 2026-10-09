package com.itasocialacademy.oitassist.competitionhistory.service.interfaces;

import com.itasocialacademy.oitassist.competitionhistory.dto.response.CompetitionHistoryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.itasocialacademy.oitassist.core.exceptions.InsufficientPermissionsException;

public interface CompetitionHistoryService {
    /**
     * Retrieves a page of finished competitions in which the user has taken part. A
     * competition appears once even if the user participated in several of its
     * stages. Only the owner of the history or an administrator may access it.
     *
     * @param userId   ID of the user whose history is requested, must not be
     *                 {@code null}
     * @param pageable pagination and sorting information
     * @return a page of past competitions, or an empty page if the user has no
     *         history
     * @throws InsufficientPermissionsException if the current user is neither the
     *                                          owner nor an administrator
     */
    Page<CompetitionHistoryResponse> getHistory(Long userId, Pageable pageable);
}
