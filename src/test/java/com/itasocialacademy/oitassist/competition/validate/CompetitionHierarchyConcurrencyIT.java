package com.itasocialacademy.oitassist.competition.validate;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.itasocialacademy.oitassist.PostgresIntegrationTest;
import com.itasocialacademy.oitassist.competition.dao.enums.CompetitionStatus;
import com.itasocialacademy.oitassist.competition.dao.enums.ExecutionStatus;
import com.itasocialacademy.oitassist.competition.dao.enums.StageScope;
import com.itasocialacademy.oitassist.competition.dao.enums.StageStatus;
import com.itasocialacademy.oitassist.competition.dao.model.Competition;
import com.itasocialacademy.oitassist.competition.dao.model.Stage;
import com.itasocialacademy.oitassist.competition.dao.model.Tour;
import com.itasocialacademy.oitassist.competition.dao.repository.CompetitionRepository;
import com.itasocialacademy.oitassist.competition.dao.repository.StageRepository;
import com.itasocialacademy.oitassist.competition.dao.repository.TourRepository;
import com.itasocialacademy.oitassist.competition.dto.request.ChangeCompetitionStatusRequest;
import com.itasocialacademy.oitassist.competition.service.interfaces.CompetitionService;
import com.itasocialacademy.oitassist.competition.service.interfaces.TourService;
import java.time.ZonedDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.RepeatedTest;
import org.springframework.beans.factory.annotation.Autowired;

class CompetitionHierarchyConcurrencyIT extends PostgresIntegrationTest {

    @Autowired
    private CompetitionRepository competitionRepository;
    @Autowired
    private StageRepository stageRepository;
    @Autowired
    private TourRepository tourRepository;
    @Autowired
    private CompetitionService competitionService;
    @Autowired
    private TourService tourService;

    private ExecutorService executor;

    @AfterEach
    void tearDown() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    /**
     * Reproduces the original race: an admin publishing the competition (DRAFT ->
     * ENROLLMENT) concurrently with another admin deleting the only Tour of its
     * only Stage. Regardless of which transaction wins the row lock, the
     * competition must never end up ENROLLMENT with an empty stage — exactly one of
     * the two operations must succeed, never both, never neither.
     */
    @RepeatedTest(10)
    void concurrentPublishAndDeleteLastTour_neverLeavesEmptyPublishedStage() throws Exception {
        executor = Executors.newFixedThreadPool(2);

        ZonedDateTime start = ZonedDateTime.now().plusDays(1);
        ZonedDateTime finish = start.plusDays(10);

        Competition competition = competitionRepository.save(Competition.builder()
            .title("Race Test " + System.nanoTime())
            .dateStart(start)
            .dateFinish(finish)
            .competitionStatus(CompetitionStatus.DRAFT)
            .createdBy(1L)
            .build());

        Stage stage = stageRepository.save(Stage.builder()
            .competitionId(competition.getId())
            .title("Stage")
            .dateStart(start)
            .dateFinish(finish)
            .sortPosition((short) 1)
            .scope(StageScope.CITY)
            .status(StageStatus.SCHEDULED)
            .createdBy(1L)
            .build());

        Tour tour = tourRepository.save(Tour.builder()
            .stageId(stage.getId())
            .title("Tour")
            .dateStart(start)
            .dateFinish(finish)
            .sortPosition((short) 1)
            .executionStatus(ExecutionStatus.SCHEDULED)
            .location("location")
            .createdBy(1L)
            .build());

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch go = new CountDownLatch(1);

        CompletableFuture<Void> publishTask = CompletableFuture.runAsync(() -> {
            ready.countDown();
            awaitGo(go);
            try {
                ChangeCompetitionStatusRequest request =
                    new ChangeCompetitionStatusRequest(CompetitionStatus.ENROLLMENT, competition.getVersion());
                competitionService.changeStatus(competition.getId(), request);
            } catch (RuntimeException _) {
            }
        }, executor);

        CompletableFuture<Void> deleteTask = CompletableFuture.runAsync(() -> {
            ready.countDown();
            awaitGo(go);
            try {
                tourService.delete(stage.getId(), tour.getId());
            } catch (RuntimeException _) {
            }
        }, executor);

        assertTrue(ready.await(5, TimeUnit.SECONDS), "Both threads should reach the start line");
        go.countDown();

        CompletableFuture.allOf(publishTask, deleteTask).get(10, TimeUnit.SECONDS);

        Competition finalCompetition = competitionRepository.findById(competition.getId()).orElseThrow();
        long remainingTours = tourRepository.countByStageId(stage.getId());

        boolean deletionWon = finalCompetition.getCompetitionStatus() == CompetitionStatus.DRAFT
            && remainingTours == 0;
        boolean publishWon = finalCompetition.getCompetitionStatus() == CompetitionStatus.ENROLLMENT
            && remainingTours == 1;

        assertTrue(deletionWon ^ publishWon,
            () -> "Expected exactly one operation to win. status=" + finalCompetition.getCompetitionStatus()
                + ", remainingTours=" + remainingTours);
    }

    private void awaitGo(CountDownLatch go) {
        try {
            go.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
        }
    }
}