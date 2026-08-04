package com.itasocialacademy.oitassist.chat.dao.dto.response;

/**
 * Result of an idempotent responder grant.
 *
 * @param created   {@code true} when a new assignment was inserted;
 *                  {@code false} when it already existed
 * @param responder current responder assignment representation
 */
public record TaskAssignmentForumResponderGrantResult(
    boolean created,
    TaskAssignmentForumResponderDTO responder) {
}