package com.itasocialacademy.oitassist.filemanager.api.dto;

/**
 * Carries the entity title hierarchy needed to construct a human-readable
 * storage path. Passed across module boundaries via
 * {@link com.itasocialacademy.oitassist.filemanager.api.FileManagerFacade}.
 *
 * <p>
 * Use the static factory methods to create instances for each entity type. Only
 * the fields relevant to the entity type need to be non-null.
 * </p>
 *
 * @param competitionTitle title of the competition (olympiad hierarchy)
 * @param stageTitle       title of the stage (olympiad hierarchy)
 * @param tourTitle        title of the tour (olympiad hierarchy)
 * @param taskTitle        title of the task body (pool or hierarchy)
 * @param newsTitle        title of the news article
 * @param userEmail        email of the submitting user (submissions only)
 */
public record StorageContext(
    String competitionTitle,
    String stageTitle,
    String tourTitle,
    String taskTitle,
    String newsTitle,
    String userEmail) {
    /**
     * Context for uploading task pool files ({@code tasks/{taskTitle}/{role}/}).
     */
    public static StorageContext forTask(String taskTitle) {
        return new StorageContext(null, null, null, taskTitle, null, null);
    }

    /**
     * Context for uploading news files ({@code news/{newsTitle}/}).
     */
    public static StorageContext forNews(String newsTitle) {
        return new StorageContext(null, null, null, null, newsTitle, null);
    }

    /**
     * Context for uploading submission files
     * ({@code olympiads/{comp}/{stage}/{tour}/{task}/submissions/{email}/}).
     */
    public static StorageContext forSubmission(
        String competitionTitle, String stageTitle,
        String tourTitle, String taskTitle, String userEmail) {
        return new StorageContext(
            competitionTitle, stageTitle, tourTitle, taskTitle, null, userEmail);
    }

    /**
     * Context for task assignment references (symlinks / .url files). Same as
     * submission context but without a user email.
     */
    public static StorageContext forAssignment(
        String competitionTitle, String stageTitle,
        String tourTitle, String taskTitle) {
        return new StorageContext(
            competitionTitle, stageTitle, tourTitle, taskTitle, null, null);
    }
}
