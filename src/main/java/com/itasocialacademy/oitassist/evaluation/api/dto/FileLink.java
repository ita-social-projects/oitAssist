package com.itasocialacademy.oitassist.evaluation.api.dto;

/**
 * A downloadable submission file exposed to the jury. Carries no
 * participant-identifying data: the download endpoint is referenced by url
 * only.
 */
public record FileLink(
    Long fileId,
    String storedFilename,
    String url) {
}