package lytheriia.bot.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lytheriia.bot.db.entity.RoundSnapshotEntity;
import lytheriia.bot.db.repository.RoundSnapshotRepository;

@Service
public class RoundSnapshotService {

    private final RoundSnapshotRepository roundSnapshotsRepository;

    public RoundSnapshotService(RoundSnapshotRepository roundSnapshotsRepository) {
        this.roundSnapshotsRepository = roundSnapshotsRepository;
    }

    @Transactional(readOnly = true)
    public List<RoundSnapshotEntity> findLastDays(int days) {
        return roundSnapshotsRepository.findLastDays(days);
    }

    @Transactional
    public void saveSnapshot(int roundId, int players) {
        roundSnapshotsRepository.saveSnapshot(roundId, players);
    }
}
