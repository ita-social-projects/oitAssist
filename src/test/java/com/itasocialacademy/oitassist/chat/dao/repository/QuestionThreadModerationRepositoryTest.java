package com.itasocialacademy.oitassist.chat.dao.repository;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.itasocialacademy.oitassist.chat.dao.enums.QuestionState;
import com.itasocialacademy.oitassist.chat.dao.enums.QuestionStatus;
import com.itasocialacademy.oitassist.chat.dao.enums.QuestionVisibility;
import java.lang.reflect.Method;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

class QuestionThreadModerationRepositoryTest {

    @Test
    void updateVisibility_shouldUseVersionCheckedSingleFieldUpdate()
        throws NoSuchMethodException {

        Method method =
            QuestionThreadRepository.class.getMethod(
                "updateVisibilityIfVersionMatches",
                Long.class,
                QuestionVisibility.class,
                Long.class,
                Instant.class);

        String query = normalizedQuery(method);

        assertTrue(query.contains(
            "SET question.visibility = :visibility"));
        assertTrue(query.contains(
            "question.updatedAt = :updatedAt"));
        assertTrue(query.contains(
            "question.version = question.version + 1"));
        assertTrue(query.contains(
            "question.id = :questionId"));
        assertTrue(query.contains(
            "question.version = :expectedVersion"));

        assertFalse(query.contains(
            "SET question.status"));
        assertFalse(query.contains(
            "SET question.state"));
        assertFalse(query.contains(
            "assignedReviewerId ="));

        assertModifyingContract(method);
    }

    @Test
    void updateStatus_shouldUseVersionCheckedSingleFieldUpdate()
        throws NoSuchMethodException {

        Method method =
            QuestionThreadRepository.class.getMethod(
                "updateStatusIfVersionMatches",
                Long.class,
                QuestionStatus.class,
                Long.class,
                Instant.class);

        String query = normalizedQuery(method);

        assertTrue(query.contains(
            "SET question.status = :status"));
        assertTrue(query.contains(
            "question.updatedAt = :updatedAt"));
        assertTrue(query.contains(
            "question.version = question.version + 1"));
        assertTrue(query.contains(
            "question.version = :expectedVersion"));

        assertFalse(query.contains(
            "SET question.visibility"));
        assertFalse(query.contains(
            "SET question.state"));
        assertFalse(query.contains(
            "assignedReviewerId ="));

        assertModifyingContract(method);
    }

    @Test
    void updateState_shouldUseVersionCheckedSingleFieldUpdate()
        throws NoSuchMethodException {

        Method method =
            QuestionThreadRepository.class.getMethod(
                "updateStateIfVersionMatches",
                Long.class,
                QuestionState.class,
                Long.class,
                Instant.class);

        String query = normalizedQuery(method);

        assertTrue(query.contains(
            "SET question.state = :state"));
        assertTrue(query.contains(
            "question.updatedAt = :updatedAt"));
        assertTrue(query.contains(
            "question.version = question.version + 1"));
        assertTrue(query.contains(
            "question.version = :expectedVersion"));

        assertFalse(query.contains(
            "SET question.visibility"));
        assertFalse(query.contains(
            "SET question.status"));
        assertFalse(query.contains(
            "assignedReviewerId ="));

        assertModifyingContract(method);
    }

    private String normalizedQuery(
        Method method) {

        Query query =
            method.getAnnotation(Query.class);

        assertNotNull(query);

        return query.value()
            .replaceAll("\\s+", " ")
            .trim();
    }

    private void assertModifyingContract(
        Method method) {

        Modifying modifying =
            method.getAnnotation(Modifying.class);

        assertNotNull(modifying);
        assertTrue(modifying.flushAutomatically());
        assertTrue(modifying.clearAutomatically());
    }
}