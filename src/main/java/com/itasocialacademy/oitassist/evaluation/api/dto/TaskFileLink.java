package com.itasocialacademy.oitassist.evaluation.api.dto;

/**
 * A downloadable task file exposed to the jury with its role: PROBLEM (task
 * statement), REFERENCE (supporting materials), SOLUTION (reference solution).
 */
public record TaskFileLink(
    Long fileId,
    String storedFilename,
    String url,
    String role) {
}