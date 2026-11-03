package lytheriia.bot.api.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * @param weeklyTokens tokens the current Boosty subscription gives per week, 0 without one
 * @param weeklyTokenCap balance where weekly grants stop
 * @param nextWeeklyGrantAt when the next weekly grant is allowed, null if there was none yet
 */
public record TokenStatusResponse(
        int balance,
        ActivePurchase activePurchase,
        int weeklyTokens,
        int weeklyTokenCap,
        Instant nextWeeklyGrantAt) {

    public record ActivePurchase(UUID id, String listingId, String serverId) {
    }
}
