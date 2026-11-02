package lytheriia.bot.api.dto;

import java.time.Instant;

public record BoostyStatusResponse(int rank, Instant startedAt) {
}