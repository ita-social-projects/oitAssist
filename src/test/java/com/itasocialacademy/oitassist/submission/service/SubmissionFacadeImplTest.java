package com.itasocialacademy.oitassist.submission.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.itasocialacademy.oitassist.submission.api.dto.SubmissionDetail;
import com.itasocialacademy.oitassist.submission.dao.model.Submission;
import com.itasocialacademy.oitassist.submission.dao.repository.SubmissionRepository;
import com.itasocialacademy.oitassist.submission.mapper.SubmissionMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SubmissionFacadeImplTest {
    private static final Long USER_ID = 5L;
    private static final Long FIRST_ASSIGNMENT_ID = 10L;
    private static final Long SECOND_ASSIGNMENT_ID = 11L;

    @Mock
    private SubmissionRepository submissionRepository;

    @Mock
    private SubmissionMapper submissionMapper;

    @InjectMocks
    private SubmissionFacadeImpl submissionFacade;

    @Test
    void getUserSubmissionsByTaskAssignmentIds_ShouldReturnMappedDetails_WhenSubmissionsExist() {
        Submission first = submission(1L, FIRST_ASSIGNMENT_ID);
        Submission second = submission(2L, SECOND_ASSIGNMENT_ID);
        SubmissionDetail firstDetail = detail(1L, FIRST_ASSIGNMENT_ID);
        SubmissionDetail secondDetail = detail(2L, SECOND_ASSIGNMENT_ID);
        List<Long> taskAssignmentIds = List.of(FIRST_ASSIGNMENT_ID, SECOND_ASSIGNMENT_ID);

        when(submissionRepository.findAllBySubmittedByAndTaskAssignmentIdIn(USER_ID, taskAssignmentIds))
            .thenReturn(List.of(first, second));
        when(submissionMapper.toDetail(first)).thenReturn(firstDetail);
        when(submissionMapper.toDetail(second)).thenReturn(secondDetail);

        List<SubmissionDetail> result =
            submissionFacade.getUserSubmissionsByTaskAssignmentIds(USER_ID, taskAssignmentIds);

        assertEquals(List.of(firstDetail, secondDetail), result);
    }

    @Test
    void getUserSubmissionsByTaskAssignmentIds_ShouldReturnEmptyList_WhenTaskAssignmentIdsAreEmpty() {
        List<SubmissionDetail> result = submissionFacade.getUserSubmissionsByTaskAssignmentIds(USER_ID, List.of());

        assertTrue(result.isEmpty());
        verifyNoInteractions(submissionRepository, submissionMapper);
    }

    @Test
    void getUserSubmissionsByTaskAssignmentIds_ShouldThrowNullPointerException_WhenUserIdIsNull() {
        List<Long> taskAssignmentIds = List.of(FIRST_ASSIGNMENT_ID);

        assertThrows(NullPointerException.class,
            () -> submissionFacade.getUserSubmissionsByTaskAssignmentIds(null, taskAssignmentIds));
        verifyNoInteractions(submissionRepository);
    }

    @Test
    void getUserSubmissionsByTaskAssignmentIds_ShouldThrowNullPointerException_WhenTaskAssignmentIdsAreNull() {
        assertThrows(NullPointerException.class,
            () -> submissionFacade.getUserSubmissionsByTaskAssignmentIds(USER_ID, null));
        verifyNoInteractions(submissionRepository);
    }

    private static Submission submission(Long id, Long taskAssignmentId) {
        return Submission.builder()
            .id(id)
            .submittedBy(USER_ID)
            .taskAssignmentId(taskAssignmentId)
            .build();
    }

    private static SubmissionDetail detail(Long id, Long taskAssignmentId) {
        return SubmissionDetail.builder()
            .id(id)
            .submittedBy(USER_ID)
            .taskAssignmentId(taskAssignmentId)
            .build();
    }
}