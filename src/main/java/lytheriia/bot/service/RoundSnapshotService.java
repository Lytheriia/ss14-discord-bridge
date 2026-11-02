package lytheriia.bot.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lytheriia.bot.db.repository.RoundSnapshotBucket;
import lytheriia.bot.db.repository.RoundSnapshotRepository;

@Service
public class RoundSnapshotService {

    private final RoundSnapshotRepository roundSnapshotRepository;

    public RoundSnapshotService(RoundSnapshotRepository roundSnapshotRepository) {
        this.roundSnapshotRepository = roundSnapshotRepository;
    }

    @Transactional(readOnly = true)
    public List<RoundSnapshotBucket> findAverages(int days, long bucketSeconds) {
        return roundSnapshotRepository.findAverages(Instant.now().minus(Duration.ofDays(days)), bucketSeconds);
    }

    @Transactional
    public void saveSnapshot(int roundId, int players) {
        roundSnapshotRepository.saveSnapshot(roundId, players);
    }

    @Transactional
    public int deleteOlderThan(int days) {
        return roundSnapshotRepository.deleteOlderThan(Instant.now().minus(Duration.ofDays(days)));
    }
}
