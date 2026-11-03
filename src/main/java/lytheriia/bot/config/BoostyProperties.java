package lytheriia.bot.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "boosty")
public record BoostyProperties(List<Integer> weeklyTokens, Integer weeklyTokenCap, Integer purchaseTtlMinutes) {

    private static final int DEFAULT_WEEKLY_TOKEN_CAP = 4;
    private static final int DEFAULT_PURCHASE_TTL_MINUTES = 60;

    public BoostyProperties {
        if (weeklyTokens == null) {
            weeklyTokens = List.of();
        }

        if (weeklyTokenCap == null) {
            weeklyTokenCap = DEFAULT_WEEKLY_TOKEN_CAP;
        }

        if (weeklyTokenCap < 0) {
            throw new IllegalArgumentException("BOOSTY_WEEKLY_TOKEN_CAP must not be negative: " + weeklyTokenCap);
        }

        if (purchaseTtlMinutes == null) {
            purchaseTtlMinutes = DEFAULT_PURCHASE_TTL_MINUTES;
        }

        if (purchaseTtlMinutes <= 0) {
            throw new IllegalArgumentException("BOOSTY_PURCHASE_TTL_MINUTES must be positive: " + purchaseTtlMinutes);
        }

        for (Integer tokens : weeklyTokens) {
            if (tokens == null || tokens < 0) {
                throw new IllegalArgumentException(
                        "BOOSTY_WEEKLY_TOKENS must contain only non-negative integers: " + weeklyTokens);
            }
        }
    }

    public int tokensForRank(int rank) {
        int index = rank - 1;

        return index >= 0 && index < weeklyTokens.size() ? weeklyTokens.get(index) : 0;
    }
}
