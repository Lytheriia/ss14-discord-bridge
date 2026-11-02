package lytheriia.bot.properties;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sponsorship")
public record SponsorshipProperties(List<Integer> weeklyTokens) {

    public SponsorshipProperties {
        if (weeklyTokens == null) {
            weeklyTokens = List.of();
        }

        for (Integer tokens : weeklyTokens) {
            if (tokens == null || tokens < 0) {
                throw new IllegalArgumentException(
                        "SPONSOR_WEEKLY_TOKENS must contain only non-negative integers: " + weeklyTokens);
            }
        }
    }

    public int tokensForTier(int tier) {
        int index = tier - 1;

        return index >= 0 && index < weeklyTokens.size() ? weeklyTokens.get(index) : 0;
    }
}
