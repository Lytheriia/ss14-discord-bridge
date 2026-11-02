package lytheriia.bot.api.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record RegisterCodeRequest(
        @NotNull @Pattern(regexp = "^[A-Za-z0-9-]{8,36}$") String code,
        @NotNull UUID userId) {
}
