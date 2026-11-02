package lytheriia.bot.integration;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import lytheriia.bot.jobs.AntagTokenGrantJob;
import lytheriia.bot.service.AntagTokenService;
import lytheriia.bot.support.IntegrationTest;

class AntagTokensTest extends IntegrationTest {

    private static final int THREADS = 20;

    @Autowired
    private AntagTokenGrantJob job;

    @Autowired
    private AntagTokenService tokens;

    private void subscribe(long discordId, int rank, Timestamp endedAt) {
        jdbc.update("INSERT INTO boosty_subscription (discord_id, promoted_by_id, rank, ended_at) VALUES (?, 1, ?, ?)",
                discordId, rank, endedAt);
    }

    private Integer balance(long discordId) {
        return jdbc.queryForList("SELECT balance FROM antag_tokens WHERE discord_id = ?", Integer.class, discordId)
                .stream().findFirst().orElse(null);
    }

    private Timestamp lastGrant(long discordId) {
        return jdbc.queryForObject("SELECT last_weekly_grant_at FROM antag_tokens WHERE discord_id = ?",
                Timestamp.class, discordId);
    }

    @Test
    void jobGrantsOnlyForHighestActiveRank() {
        subscribe(1, 2, null);
        subscribe(1, 3, null);
        subscribe(2, 2, null);
        subscribe(3, 1, null);
        subscribe(4, 3, new Timestamp(0));

        job.grantWeeklyTokens();

        assertThat(balance(1)).isEqualTo(3);
        assertThat(balance(2)).isEqualTo(1);
        assertThat(balance(3)).isNull();
        assertThat(balance(4)).isNull();
    }

    @Test
    void jobGrantsAgainOnlyAfterSevenDays() {
        subscribe(1, 3, null);

        job.grantWeeklyTokens();
        job.grantWeeklyTokens();
        assertThat(balance(1)).isEqualTo(3);

        jdbc.update("UPDATE antag_tokens SET last_weekly_grant_at = now() - INTERVAL '8 days' WHERE discord_id = 1");
        job.grantWeeklyTokens();
        assertThat(balance(1)).isEqualTo(6);
    }

    @Test
    void manualGiveKeepsWeeklyGrantDate() {
        tokens.grantWeekly(1, 1);
        Timestamp before = lastGrant(1);

        tokens.give(1, 5);

        assertThat(balance(1)).isEqualTo(6);
        assertThat(lastGrant(1)).isEqualTo(before);
    }

    @Test
    void manualGiveDoesNotBlockFirstWeeklyGrant() {
        tokens.give(1, 5);

        assertThat(tokens.grantWeekly(1, 1)).isTrue();
        assertThat(tokens.grantWeekly(1, 1)).isFalse();
        assertThat(balance(1)).isEqualTo(6);
    }

    @Test
    void rejectsNonPositiveAmounts() {
        assertThatThrownBy(() -> tokens.give(1, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> tokens.grantWeekly(1, -1)).isInstanceOf(IllegalArgumentException.class);
        assertThat(balance(1)).isNull();
    }

    @Test
    void parallelWeeklyGrantsCountOnce() throws Exception {
        AtomicInteger granted = new AtomicInteger();

        runInParallel(() -> {
            if (tokens.grantWeekly(1, 1)) {
                granted.incrementAndGet();
            }
            return null;
        });

        assertThat(granted).hasValue(1);
        assertThat(balance(1)).isEqualTo(1);
    }

    @Test
    void parallelManualGivesAreAllCounted() throws Exception {
        runInParallel(() -> {
            tokens.give(1, 1);
            return null;
        });

        assertThat(balance(1)).isEqualTo(THREADS);
    }

    private void runInParallel(Callable<Void> task) throws Exception {
        List<Callable<Void>> tasks = new ArrayList<>();
        for (int i = 0; i < THREADS; i++) {
            tasks.add(task);
        }

        try (ExecutorService executor = Executors.newFixedThreadPool(THREADS)) {
            for (var future : executor.invokeAll(tasks)) {
                future.get();
            }
        }
    }
}
