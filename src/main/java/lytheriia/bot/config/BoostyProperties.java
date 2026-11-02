package lytheriia.bot.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "boosty")
public record BoostyProperties(List<Integer> weeklyTokens) {

    public BoostyProperties {
        if (weeklyTokens == null) {
            weeklyTokens = List.of();
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
