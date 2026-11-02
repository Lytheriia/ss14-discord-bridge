package lytheriia.bot.config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Validated
@ConfigurationProperties(prefix = "discord")
public record DiscordProperties(
        @NotBlank String token,
        @NotNull Long guildId,
        @NotNull @Valid Roles roles,
        @NotNull Long maintainerId) {

    @Override
    public String toString() {
        return "DiscordProperties[guildId=" + guildId + ", roles=" + roles + ", maintainerId=" + maintainerId + "]";
    }

    public record Roles(@NotNull Long verified, @NotNull Long booster, List<Long> boosty) {

        public Roles {
            boosty = boosty == null ? List.of() : List.copyOf(boosty);
        }

        public Map<Integer, Long> boostyRolesByRank() {
            Map<Integer, Long> rolesByRank = new HashMap<>();

            for (int i = 0; i < boosty.size(); i++) {
                rolesByRank.put(i + 1, boosty.get(i));
            }

            return Map.copyOf(rolesByRank);
        }
    }
}
