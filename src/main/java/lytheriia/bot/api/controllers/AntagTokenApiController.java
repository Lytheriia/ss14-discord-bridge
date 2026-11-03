package lytheriia.bot.api.controllers;

import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lytheriia.bot.api.dto.CreatePurchaseRequest;
import lytheriia.bot.api.dto.RefundPendingResponse;
import lytheriia.bot.api.dto.TokenStatusResponse;
import lytheriia.bot.service.AntagTokenService;

@RestController
@RequestMapping("/api/tokens")
public class AntagTokenApiController {

    private final AntagTokenService tokenService;

    public AntagTokenApiController(AntagTokenService tokenService) {
        this.tokenService = tokenService;
    }

    @GetMapping("/{userId}")
    public ResponseEntity<?> status(@PathVariable UUID userId) {
        return tokenService.findStatus(userId)
                .<ResponseEntity<?>>map(status -> ResponseEntity.ok(new TokenStatusResponse(
                        status.balance(),
                        toResponse(status.activePurchase()),
                        status.weeklyTokens(),
                        status.weeklyTokenCap(),
                        status.nextWeeklyGrantAt())))
                .orElseGet(() -> error(HttpStatus.NOT_FOUND, "NOT_LINKED"));
    }

    @PostMapping("/{userId}/purchases")
    public ResponseEntity<?> purchase(@PathVariable UUID userId, @Valid @RequestBody CreatePurchaseRequest req) {
        return switch (tokenService.purchase(userId, req.purchaseId(), req.listingId(), req.serverId())) {
            case CREATED -> ResponseEntity.noContent().build();
            case NOT_LINKED -> error(HttpStatus.NOT_FOUND, "NOT_LINKED");
            case INSUFFICIENT_TOKENS -> error(HttpStatus.CONFLICT, "INSUFFICIENT_TOKENS");
            case ACTIVE_PURCHASE -> error(HttpStatus.CONFLICT, "ACTIVE_PURCHASE");
            case PURCHASE_ID_CONFLICT -> error(HttpStatus.CONFLICT, "PURCHASE_ID_CONFLICT");
        };
    }

    @PostMapping("/purchases/{purchaseId}/grant")
    public ResponseEntity<?> grant(@PathVariable UUID purchaseId) {
        return finishResponse(tokenService.markGranted(purchaseId));
    }

    @PostMapping("/purchases/{purchaseId}/refund")
    public ResponseEntity<?> refund(@PathVariable UUID purchaseId) {
        return finishResponse(tokenService.refund(purchaseId));
    }

    @PostMapping("/servers/{serverId}/refund-pending")
    public RefundPendingResponse refundPending(@PathVariable String serverId) {
        return new RefundPendingResponse(tokenService.refundPending(serverId));
    }

    private static TokenStatusResponse.ActivePurchase toResponse(AntagTokenService.ActivePurchase purchase) {
        return purchase == null ? null
                : new TokenStatusResponse.ActivePurchase(purchase.id(), purchase.listingId(), purchase.serverId());
    }

    private static ResponseEntity<?> finishResponse(AntagTokenService.FinishResult result) {
        return switch (result) {
            case FINISHED -> ResponseEntity.noContent().build();
            case NOT_FOUND -> error(HttpStatus.NOT_FOUND, "PURCHASE_NOT_FOUND");
            case ALREADY_FINISHED -> error(HttpStatus.CONFLICT, "ALREADY_FINISHED");
        };
    }

    private static ResponseEntity<Map<String, String>> error(HttpStatus status, String code) {
        return ResponseEntity.status(status).body(Map.of("error", code));
    }
}
