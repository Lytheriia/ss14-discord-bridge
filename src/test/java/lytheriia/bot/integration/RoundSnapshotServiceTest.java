package lytheriia.bot.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import lytheriia.bot.db.repository.RoundSnapshotBucket;
import lytheriia.bot.jobs.RoundSnapshotCleanupJob;
import lytheriia.bot.service.RoundSnapshotService;
import lytheriia.bot.support.IntegrationTest;

class RoundSnapshotServiceTest extends IntegrationTest {

    @Autowired
    private RoundSnapshotService snapshots;

    @Autowired
    private RoundSnapshotCleanupJob cleanupJob;

    private void snapshotAt(String age, int players) {
        jdbc.update("INSERT INTO round_snapshots (time_stamp, round_id, players) VALUES (now() - CAST(? AS interval), 1, ?)",
                age, players);
    }

    private int rows() {
        return jdbc.queryForObject("SELECT count(*) FROM round_snapshots", Integer.class);
    }

    @Test
    void averagesAreGroupedInBucketsWithinPeriod() {
        jdbc.update("INSERT INTO round_snapshots (time_stamp, round_id, players) VALUES "
                + "(date_trunc('day', now()) - INTERVAL '1 hour', 1, 10), "
                + "(date_trunc('day', now()) - INTERVAL '1 hour' + INTERVAL '1 minute', 1, 20)");
        snapshotAt("10 days", 99);

        List<RoundSnapshotBucket> buckets = snapshots.findAverages(5, 3600);

        assertThat(buckets).hasSize(1);
        assertThat(buckets.getFirst().getPlayers()).isEqualTo(15.0);
        assertThat(buckets.getFirst().getBucket()).isNotNull();
    }

    @Test
    void savedSnapshotIsReturned() {
        snapshots.saveSnapshot(9, 42);

        assertThat(snapshots.findAverages(1, 30)).singleElement()
                .extracting(RoundSnapshotBucket::getPlayers).isEqualTo(42.0);
    }

    @Test
    void cleanupDeletesOnlyRowsOlderThanRetention() {
        snapshotAt("91 days", 1);
        snapshotAt("89 days", 2);

        cleanupJob.deleteOldSnapshots();

        assertThat(rows()).isEqualTo(1);
    }
}
