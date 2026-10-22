package com.itasocialacademy.oitassist.competition.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import java.time.ZonedDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

class CompetitionLockTimeoutIT extends PostgresIntegrationTest {

    @PersistenceContext
    private EntityManager entityManager;
    @Autowired
    private JpaTransactionManager transactionManager;
    @Autowired
    private CompetitionRepository competitionRepository;
    @Autowired
    private StageRepository stageRepository;
    @Autowired
    private TourRepository tourRepository;
    @Autowired
    private CompetitionService competitionService;

    private ExecutorService executor;

    @AfterEach
    void tearDown() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    @Test
    void lockHeldLongerThanTimeout_shouldSurfaceAsPessimisticLockingFailure() throws Exception {
        executor = Executors.newFixedThreadPool(1);

        Competition competition = competitionRepository.save(Competition.builder()
            .title("Timeout Test")
            .dateStart(ZonedDateTime.now().plusDays(1))
            .dateFinish(ZonedDateTime.now().plusDays(10))
            .competitionStatus(CompetitionStatus.DRAFT)
            .createdBy(1L)
            .build());

        Stage stage = stageRepository.save(Stage.builder()
            .competitionId(competition.getId())
            .title("Stage")
            .dateStart(competition.getDateStart())
            .dateFinish(competition.getDateFinish())
            .sortPosition((short) 1)
            .scope(StageScope.CITY)
            .status(StageStatus.SCHEDULED)
            .createdBy(1L)
            .build());

        tourRepository.save(Tour.builder()
            .stageId(stage.getId())
            .title("Tour")
            .dateStart(competition.getDateStart())
            .dateFinish(competition.getDateFinish())
            .sortPosition((short) 1)
            .executionStatus(ExecutionStatus.SCHEDULED)
            .location("location")
            .createdBy(1L)
            .build());

        TransactionTemplate holderTx = new TransactionTemplate(transactionManager);
        CountDownLatch lockAcquired = new CountDownLatch(1);
        CountDownLatch releaseHolder = new CountDownLatch(1);

        Future<?> holderFuture = executor.submit(() -> holderTx.executeWithoutResult(_ -> {
            entityManager.find(Competition.class, competition.getId(), LockModeType.PESSIMISTIC_WRITE);

            lockAcquired.countDown();
            try {
                releaseHolder.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        }));

        assertTrue(lockAcquired.await(5, TimeUnit.SECONDS), "Holder thread should acquire the lock");

        ChangeCompetitionStatusRequest request =
            new ChangeCompetitionStatusRequest(CompetitionStatus.ENROLLMENT, competition.getVersion());

        Long id = competition.getId();
        assertNotNull(id);

        assertThrows(PessimisticLockingFailureException.class,
            () -> competitionService.changeStatus(id, request));

        releaseHolder.countDown();
        holderFuture.get(5, TimeUnit.SECONDS);
    }
}
