package com.itasocialacademy.oitassist.competition.benchmark;

import com.itasocialacademy.oitassist.PostgresIntegrationTest;
import com.itasocialacademy.oitassist.competition.dao.enums.CompetitionStatus;
import com.itasocialacademy.oitassist.competition.dao.enums.StageScope;
import com.itasocialacademy.oitassist.competition.dao.enums.StageStatus;
import com.itasocialacademy.oitassist.competition.dao.model.Competition;
import com.itasocialacademy.oitassist.competition.dao.model.Stage;
import com.itasocialacademy.oitassist.competition.dao.repository.CompetitionRepository;
import com.itasocialacademy.oitassist.competition.dao.repository.StageRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * Manual benchmark comparing {@code findById} (full managed entity) against a
 * constructor projection ({@code findStatusViewById}) over 1000 seeded Stage
 * rows. Not part of the regular test suite — run explicitly (remove
 * {@code @Disabled} temporarily, or run this single test via IDEA) and read the
 * console output to comparison.
 *
 * <p>
 * No assertions on timing: this is a directional decision tool, not a CI gate.
 */
@Disabled("Manual benchmark — not part of CI, run explicitly")
@Tag("benchmark")
@Transactional
class StageQueryBenchmarkIT extends PostgresIntegrationTest {

    private static final int RECORD_COUNT = 1_000;
    private static final int WARMUP_ITERATIONS = 200;
    private static final int MEASURED_ITERATIONS = 1_000;
    private static final int CLEAR_EVERY = 50;
    private static final int BENCHMARK_ROUNDS = 10;
    private static final long RANDOM_SEED = 42L;

    @Autowired
    private CompetitionRepository competitionRepository;
    @Autowired
    private StageRepository stageRepository;
    @PersistenceContext
    private EntityManager entityManager;

    private List<Long> stageIds;

    @BeforeEach
    void seedData() {
        Competition competition = competitionRepository.save(Competition.builder()
            .title("Benchmark Competition " + System.nanoTime())
            .dateStart(ZonedDateTime.now())
            .dateFinish(ZonedDateTime.now().plusDays(365))
            .competitionStatus(CompetitionStatus.DRAFT)
            .createdBy(1L)
            .build());

        List<Stage> stages = new ArrayList<>();
        for (int i = 0; i < RECORD_COUNT; i++) {
            stages.add(Stage.builder()
                .competitionId(competition.getId())
                .title("Stage " + i)
                .description("Lorem ipsum ".repeat(150))
                .dateStart(competition.getDateStart())
                .dateFinish(competition.getDateFinish())
                .sortPosition((short) (i % 30000))
                .scope(StageScope.CITY)
                .status(StageStatus.SCHEDULED)
                .createdBy(1L)
                .build());
        }
        stageIds = stageRepository.saveAll(stages).stream().map(Stage::getId).toList();
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void compareQueriesBenchmark() {
        long findByIdTotalNanos = 0;
        long projectionTotalNanos = 0;

        int findByIdFirstCount = 0;
        int projectionFirstCount = 0;

        System.out.println();
        System.out.println("=== Stage Query Benchmark ===");
        System.out.printf(
            "Records: %d, warmup: %d, measured: %d, rounds: %d%n",
            RECORD_COUNT,
            WARMUP_ITERATIONS,
            MEASURED_ITERATIONS,
            BENCHMARK_ROUNDS);
        System.out.println();

        for (int round = 1; round <= BENCHMARK_ROUNDS; round++) {
            boolean findByIdFirst = round % 2 == 1;

            if (findByIdFirst) {
                findByIdFirstCount++;

                warmupFindById();
                warmupProjection();

                entityManager.clear();

                long findByIdNanos = measureFindById();
                entityManager.clear();

                long projectionNanos = measureProjection();

                findByIdTotalNanos += findByIdNanos;
                projectionTotalNanos += projectionNanos;

                printRound(
                    round,
                    "findById → projection",
                    findByIdNanos,
                    projectionNanos);
            } else {
                projectionFirstCount++;

                warmupProjection();
                warmupFindById();

                entityManager.clear();

                long projectionNanos = measureProjection();
                entityManager.clear();

                long findByIdNanos = measureFindById();

                findByIdTotalNanos += findByIdNanos;
                projectionTotalNanos += projectionNanos;

                printRound(
                    round,
                    "projection → findById",
                    findByIdNanos,
                    projectionNanos);
            }
        }

        printSummary(
            findByIdTotalNanos,
            projectionTotalNanos,
            findByIdFirstCount,
            projectionFirstCount);
    }

    private void warmupFindById() {
        runFindById(WARMUP_ITERATIONS, new Random(RANDOM_SEED));
        entityManager.clear();
    }

    private void warmupProjection() {
        runProjection(WARMUP_ITERATIONS, new Random(RANDOM_SEED));
        entityManager.clear();
    }

    private long measureFindById() {
        return runFindById(
            MEASURED_ITERATIONS,
            new Random(RANDOM_SEED));
    }

    private long measureProjection() {
        return runProjection(
            MEASURED_ITERATIONS,
            new Random(RANDOM_SEED));
    }

    private long runFindById(int iterations, Random random) {
        long start = System.nanoTime();

        for (int i = 0; i < iterations; i++) {
            Long id = stageIds.get(random.nextInt(stageIds.size()));

            stageRepository.findById(id).orElseThrow();

            if (i % CLEAR_EVERY == CLEAR_EVERY - 1) {
                entityManager.clear();
            }
        }

        return System.nanoTime() - start;
    }

    private long runProjection(int iterations, Random random) {
        long start = System.nanoTime();

        for (int i = 0; i < iterations; i++) {
            Long id = stageIds.get(random.nextInt(stageIds.size()));

            stageRepository.findStatusViewById(id).orElseThrow();

            if (i % CLEAR_EVERY == CLEAR_EVERY - 1) {
                entityManager.clear();
            }
        }

        return System.nanoTime() - start;
    }

    private void printRound(
        int round,
        String order,
        long findByIdNanos,
        long projectionNanos) {
        double findByIdAvgMs =
            findByIdNanos / 1_000_000.0 / MEASURED_ITERATIONS;

        double projectionAvgMs =
            projectionNanos / 1_000_000.0 / MEASURED_ITERATIONS;

        double deltaPercent =
            (findByIdAvgMs - projectionAvgMs)
                / findByIdAvgMs
                * 100.0;

        System.out.printf(
            "Round %d | %-23s | findById: %.4f ms | projection: %.4f ms | delta: %+.2f%%%n",
            round,
            order,
            findByIdAvgMs,
            projectionAvgMs,
            deltaPercent);
    }

    private void printSummary(
        long findByIdTotalNanos,
        long projectionTotalNanos,
        int findByIdFirstCount,
        int projectionFirstCount) {
        double findByIdAvgMs =
            findByIdTotalNanos
                / 1_000_000.0
                / MEASURED_ITERATIONS
                / BENCHMARK_ROUNDS;

        double projectionAvgMs =
            projectionTotalNanos
                / 1_000_000.0
                / MEASURED_ITERATIONS
                / BENCHMARK_ROUNDS;

        double deltaPercent =
            (findByIdAvgMs - projectionAvgMs)
                / findByIdAvgMs
                * 100.0;

        System.out.println();
        System.out.println("=== Summary ===");

        System.out.printf(
            "findById first:    %d rounds%n",
            findByIdFirstCount);

        System.out.printf(
            "projection first:  %d rounds%n",
            projectionFirstCount);

        System.out.printf(
            "findById avg:      %.4f ms/call%n",
            findByIdAvgMs);

        System.out.printf(
            "projection avg:    %.4f ms/call%n",
            projectionAvgMs);

        System.out.printf(
            "overall delta:     %+.2f%%%n",
            deltaPercent);
    }
}
