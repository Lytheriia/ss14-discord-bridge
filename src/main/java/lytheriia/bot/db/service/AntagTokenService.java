package lytheriia.bot.db.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lytheriia.bot.db.repository.AntagTokensRepository;

@Service
public class AntagTokenService {

    private final AntagTokensRepository tokensRepository;

    public AntagTokenService(AntagTokensRepository tokensRepository) {
        this.tokensRepository = tokensRepository;
    }

    /**
     * @return true if tokens were added, false if the last weekly grant was less than 7 days ago
     */
    @Transactional
    public boolean grantWeekly(long discordId, int amount) {
        requirePositive(amount);
        return tokensRepository.addWeekly(discordId, amount) > 0;
    }

    @Transactional
    public void give(long discordId, int amount) {
        requirePositive(amount);
        tokensRepository.addManual(discordId, amount);
    }

    private static void requirePositive(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Token amount must be positive: " + amount);
        }
    }
}
