package lytheriia.bot.api.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreatePurchaseRequest(
        @NotNull UUID purchaseId,
        @NotBlank @Size(max = 64) String listingId,
        @NotBlank @Size(max = 64) String serverId) {
}
