package com.itasocialacademy.oitassist.participation.service;

import com.itasocialacademy.oitassist.PostgresIntegrationTest;
import com.itasocialacademy.oitassist.competition.dao.enums.CompetitionStatus;
import com.itasocialacademy.oitassist.competition.dao.enums.StageScope;
import com.itasocialacademy.oitassist.competition.dao.enums.StageStatus;
import com.itasocialacademy.oitassist.competition.dao.model.Competition;
import com.itasocialacademy.oitassist.competition.dao.model.Stage;
import com.itasocialacademy.oitassist.competition.dao.repository.CompetitionRepository;
import com.itasocialacademy.oitassist.competition.dao.repository.StageRepository;
import com.itasocialacademy.oitassist.participation.dao.enums.RequestStatus;
import com.itasocialacademy.oitassist.participation.dao.model.Application;
import com.itasocialacademy.oitassist.participation.dao.repository.ApplicationRepository;
import com.itasocialacademy.oitassist.participation.exceptions.UnableToProcessApplicationException;
import com.itasocialacademy.oitassist.participation.service.interfaces.ApplicationService;
import com.itasocialacademy.oitassist.security.api.interfaces.SecurityFacade;
import com.itasocialacademy.oitassist.user.dao.enums.Role;
import com.itasocialacademy.oitassist.user.dao.enums.UserStatus;
import com.itasocialacademy.oitassist.user.dao.model.User;
import com.itasocialacademy.oitassist.user.dao.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

public class ApplicationOptimisticLockingIT extends PostgresIntegrationTest {
    @Autowired
    private ApplicationService applicationService;
    @Autowired
    private ApplicationRepository applicationRepository;
    @Autowired
    private CompetitionRepository competitionRepository;
    @Autowired
    private StageRepository stageRepository;
    @Autowired
    private UserRepository userRepository;
    @MockitoBean
    private SecurityFacade securityFacade;

    @AfterEach
    void cleanUp() {
        applicationRepository.deleteAll();
        stageRepository.deleteAll();
        competitionRepository.deleteAll();
    }

    @Test
    void staleVersionSave_shouldThrowOptimisticLockingFailure() {
        Long id = createPendingApplication();

        Application first = applicationRepository.findById(id).orElseThrow();
        Application second = applicationRepository.findById(id).orElseThrow();

        first.setStatus(RequestStatus.CANCELLED);
        applicationRepository.saveAndFlush(first);

        second.setStatus(RequestStatus.CANCELLED);
        assertThrows(ObjectOptimisticLockingFailureException.class,
            () -> applicationRepository.saveAndFlush(second));
    }

    @Test
    void concurrentCancel_shouldOnlyLetOneRequestSucceed() throws InterruptedException {
        Long applicationId = createPendingApplication();

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startSignal = new CountDownLatch(1);
        List<Throwable> errors = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger successCount = new AtomicInteger();

        Runnable cancelAttempt = () -> {
            try {
                startSignal.await();
                applicationService.cancelRequest(applicationId);
                successCount.incrementAndGet();
            } catch (Throwable e) {
                errors.add(e);
            }
        };

        executor.submit(cancelAttempt);
        executor.submit(cancelAttempt);
        startSignal.countDown();
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        assertEquals(1, successCount.get());
        assertEquals(1, errors.size());
        assertInstanceOf(UnableToProcessApplicationException.class, errors.getFirst());

        Application finalState = applicationRepository.findById(applicationId).orElseThrow();
        assertEquals(RequestStatus.CANCELLED, finalState.getStatus());
    }

    private Long createPendingApplication() {
        String randomEmail = UUID.randomUUID() + "@mail.com";

        User user = userRepository.save(User.builder()
            .email(randomEmail).password("pass")
            .firstName("Test").surname("Tester").createdAt(Instant.now())
            .role(Role.USER).userStatus(UserStatus.ACTIVE)
            .build());
        Competition competition = competitionRepository.save(Competition.builder()
            .title("Test Competition")
            .dateStart(ZonedDateTime.now().plusDays(1))
            .dateFinish(ZonedDateTime.now().plusDays(10))
            .competitionStatus(CompetitionStatus.ENROLLMENT)
            .createdBy(user.getId())
            .build());

        Stage stage = stageRepository.save(Stage.builder()
            .competitionId(competition.getId())
            .title("Test Stage")
            .dateStart(competition.getDateStart())
            .dateFinish(competition.getDateFinish())
            .sortPosition((short) 1)
            .scope(StageScope.CITY)
            .status(StageStatus.SCHEDULED)
            .createdBy(user.getId())
            .build());

        Application application = new Application();
        application.setCompetitionId(competition.getId());
        application.setStageId(stage.getId());
        application.setIssuedBy(user.getId());
        application.setStatus(RequestStatus.PENDING);

        when(securityFacade.getCurrentUserId()).thenReturn(Optional.of(user.getId()));

        Application saved = applicationRepository.save(application);
        return saved.getId();
    }
}
