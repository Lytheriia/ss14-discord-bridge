package lytheriia.bot.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import lytheriia.bot.service.AntagTokenService;
import lytheriia.bot.service.AntagTokenService.FinishResult;
import lytheriia.bot.service.AntagTokenService.PurchaseResult;
import lytheriia.bot.support.IntegrationTest;

class AntagTokenPurchaseTest extends IntegrationTest {

    private static final long DISCORD_ID = 500;
    private static final UUID USER = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OTHER_USER = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID PURCHASE = UUID.fromString("44444444-4444-4444-4444-444444444444");

    @Autowired
    private AntagTokenService tokens;

    @BeforeEach
    void linkUser() {
        jdbc.update("INSERT INTO discord_linked_accounts (discord_id, ss14_user_id) VALUES (?, ?)", DISCORD_ID, USER);
    }

    private int balance() {
        return tokens.findStatus(USER).orElseThrow().balance();
    }

    private String status(UUID purchaseId) {
        return jdbc.queryForObject("SELECT status FROM antag_token_purchase WHERE id = ?", String.class, purchaseId);
    }

    @Test
    void purchaseTakesOneTokenAndBecomesActive() {
        tokens.give(DISCORD_ID, 2);

        assertThat(tokens.purchase(USER, PURCHASE, "Traitor", "main")).isEqualTo(PurchaseResult.CREATED);

        assertThat(balance()).isEqualTo(1);
        assertThat(tokens.findStatus(USER).orElseThrow().activePurchase()).isNotNull();
    }

    @Test
    void unlinkedUserCannotBuy() {
        assertThat(tokens.purchase(OTHER_USER, PURCHASE, "Traitor", "main")).isEqualTo(PurchaseResult.NOT_LINKED);
        assertThat(tokens.findStatus(OTHER_USER)).isEmpty();
    }

    @Test
    void emptyBalanceCannotBuy() {
        assertThat(tokens.purchase(USER, PURCHASE, "Traitor", "main")).isEqualTo(PurchaseResult.INSUFFICIENT_TOKENS);
        assertThat(tokens.findStatus(USER).orElseThrow().activePurchase()).isNull();
    }

    @Test
    void retryWithSamePurchaseIdDoesNotSpendAgain() {
        tokens.give(DISCORD_ID, 2);

        tokens.purchase(USER, PURCHASE, "Traitor", "main");

        assertThat(tokens.purchase(USER, PURCHASE, "Traitor", "main")).isEqualTo(PurchaseResult.CREATED);
        assertThat(balance()).isEqualTo(1);
    }

    @Test
    void secondActivePurchaseIsRejected() {
        tokens.give(DISCORD_ID, 2);
        tokens.purchase(USER, PURCHASE, "Traitor", "main");

        assertThat(tokens.purchase(USER, UUID.randomUUID(), "Thief", "main")).isEqualTo(PurchaseResult.ACTIVE_PURCHASE);
        assertThat(balance()).isEqualTo(1);
    }

    private List<PurchaseResult> runInParallel(int count, Callable<PurchaseResult> task) throws Exception {
        List<PurchaseResult> results = new ArrayList<>();

        try (ExecutorService executor = Executors.newFixedThreadPool(count)) {
            for (var future : executor.invokeAll(Collections.nCopies(count, task))) {
                results.add(future.get());
            }
        }

        return results;
    }

    @Test
    void parallelPurchasesCreateOnlyOneAndKeepOtherTokens() throws Exception {
        tokens.give(DISCORD_ID, 5);

        List<PurchaseResult> results = runInParallel(20,
                () -> tokens.purchase(USER, UUID.randomUUID(), "Traitor", "main"));

        assertThat(results).containsOnly(PurchaseResult.CREATED, PurchaseResult.ACTIVE_PURCHASE);
        assertThat(results).filteredOn(PurchaseResult.CREATED::equals).hasSize(1);
        assertThat(balance()).isEqualTo(4);
    }

    @Test
    void parallelPurchasesForLastTokenAreActivePurchaseNotInsufficient() throws Exception {
        tokens.give(DISCORD_ID, 1);

        List<PurchaseResult> results = runInParallel(20,
                () -> tokens.purchase(USER, UUID.randomUUID(), "Traitor", "main"));

        assertThat(results).containsOnly(PurchaseResult.CREATED, PurchaseResult.ACTIVE_PURCHASE);
        assertThat(results).filteredOn(PurchaseResult.CREATED::equals).hasSize(1);
        assertThat(balance()).isZero();
    }

    @Test
    void parallelRetriesOfOnePurchaseAllSucceedAndSpendOnce() throws Exception {
        tokens.give(DISCORD_ID, 5);

        List<PurchaseResult> results = runInParallel(20, () -> tokens.purchase(USER, PURCHASE, "Traitor", "main"));

        assertThat(results).containsOnly(PurchaseResult.CREATED);
        assertThat(balance()).isEqualTo(4);
    }

    @Test
    void parallelGrantAndRefundFinishOnceAndKeepBalanceConsistent() throws Exception {
        tokens.give(DISCORD_ID, 1);
        tokens.purchase(USER, PURCHASE, "Traitor", "main");
        CountDownLatch start = new CountDownLatch(1);

        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            var grant = executor.submit(() -> {
                start.await();
                return tokens.markGranted(PURCHASE);
            });
            var refund = executor.submit(() -> {
                start.await();
                return tokens.refund(PURCHASE);
            });
            start.countDown();

            assertThat(List.of(grant.get(), refund.get()))
                    .containsExactlyInAnyOrder(FinishResult.FINISHED, FinishResult.ALREADY_FINISHED);
        }

        assertThat(balance()).isEqualTo(status(PURCHASE).equals("REFUNDED") ? 1 : 0);
    }

    @Test
    void retryOfFinishedPurchaseIsConflictAndSpendsNothing() {
        tokens.give(DISCORD_ID, 1);
        tokens.purchase(USER, PURCHASE, "Traitor", "main");
        tokens.refund(PURCHASE);

        assertThat(tokens.purchase(USER, PURCHASE, "Traitor", "main")).isEqualTo(PurchaseResult.PURCHASE_ID_CONFLICT);
        assertThat(balance()).isEqualTo(1);
        assertThat(status(PURCHASE)).isEqualTo("REFUNDED");
    }

    @Test
    void samePurchaseIdWithOtherListingIsConflict() {
        tokens.give(DISCORD_ID, 2);
        tokens.purchase(USER, PURCHASE, "Traitor", "main");

        assertThat(tokens.purchase(USER, PURCHASE, "Thief", "main")).isEqualTo(PurchaseResult.PURCHASE_ID_CONFLICT);
        assertThat(balance()).isEqualTo(1);
    }

    @Test
    void purchaseOfOtherPlayerIdIsConflict() {
        jdbc.update("INSERT INTO discord_linked_accounts (discord_id, ss14_user_id) VALUES (?, ?)", 501L, OTHER_USER);
        tokens.give(DISCORD_ID, 1);
        tokens.give(501, 1);
        tokens.purchase(USER, PURCHASE, "Traitor", "main");

        assertThat(tokens.purchase(OTHER_USER, PURCHASE, "Traitor", "main"))
                .isEqualTo(PurchaseResult.PURCHASE_ID_CONFLICT);
    }

    @Test
    void refundOvertakingLatePurchaseIsCleanedUpByExpiry() {
        tokens.give(DISCORD_ID, 1);

        assertThat(tokens.refund(PURCHASE)).isEqualTo(FinishResult.NOT_FOUND);
        tokens.purchase(USER, PURCHASE, "Traitor", "main");
        assertThat(balance()).isZero();

        assertThat(tokens.refundExpired(Duration.ofHours(24))).isZero();
        jdbc.update("UPDATE antag_token_purchase SET created_at = now() - INTERVAL '25 hours' WHERE id = ?", PURCHASE);

        assertThat(tokens.refundExpired(Duration.ofHours(24))).isEqualTo(1);
        assertThat(balance()).isEqualTo(1);
        assertThat(status(PURCHASE)).isEqualTo("REFUNDED");
    }

    @Test
    void expiryKeepsFinishedPurchases() {
        tokens.give(DISCORD_ID, 1);
        tokens.purchase(USER, PURCHASE, "Traitor", "main");
        tokens.markGranted(PURCHASE);
        jdbc.update("UPDATE antag_token_purchase SET created_at = now() - INTERVAL '25 hours' WHERE id = ?", PURCHASE);

        assertThat(tokens.refundExpired(Duration.ofHours(24))).isZero();
        assertThat(status(PURCHASE)).isEqualTo("GRANTED");
    }

    @Test
    void refundReturnsTokenOnlyOnce() {
        tokens.give(DISCORD_ID, 1);
        tokens.purchase(USER, PURCHASE, "Traitor", "main");

        assertThat(tokens.refund(PURCHASE)).isEqualTo(FinishResult.FINISHED);
        assertThat(tokens.refund(PURCHASE)).isEqualTo(FinishResult.FINISHED);

        assertThat(balance()).isEqualTo(1);
        assertThat(status(PURCHASE)).isEqualTo("REFUNDED");
    }

    @Test
    void grantedPurchaseCannotBeRefunded() {
        tokens.give(DISCORD_ID, 1);
        tokens.purchase(USER, PURCHASE, "Traitor", "main");

        assertThat(tokens.markGranted(PURCHASE)).isEqualTo(FinishResult.FINISHED);
        assertThat(tokens.markGranted(PURCHASE)).isEqualTo(FinishResult.FINISHED);
        assertThat(tokens.refund(PURCHASE)).isEqualTo(FinishResult.ALREADY_FINISHED);

        assertThat(balance()).isZero();
        assertThat(status(PURCHASE)).isEqualTo("GRANTED");
    }

    @Test
    void refundedPurchaseAllowsNewOne() {
        tokens.give(DISCORD_ID, 1);
        tokens.purchase(USER, PURCHASE, "Traitor", "main");
        tokens.refund(PURCHASE);

        assertThat(tokens.purchase(USER, UUID.randomUUID(), "Thief", "main")).isEqualTo(PurchaseResult.CREATED);
    }

    @Test
    void statusShowsWeeklyGrantOfHighestRank() {
        jdbc.update("INSERT INTO boosty_subscription (discord_id, promoted_by_id, rank) VALUES (?, 1, 2), (?, 1, 3)",
                DISCORD_ID, DISCORD_ID);

        AntagTokenService.TokenStatus before = tokens.findStatus(USER).orElseThrow();
        assertThat(before.weeklyTokens()).isEqualTo(3);
        assertThat(before.weeklyTokenCap()).isEqualTo(4);
        assertThat(before.nextWeeklyGrantAt()).isNull();

        tokens.grantWeekly(DISCORD_ID, 3);

        Instant next = tokens.findStatus(USER).orElseThrow().nextWeeklyGrantAt();
        assertThat(next).isBetween(Instant.now().plus(Duration.ofDays(6)), Instant.now().plus(Duration.ofDays(8)));
    }

    @Test
    void statusWithoutSubscriptionHasNoWeeklyTokens() {
        assertThat(tokens.findStatus(USER).orElseThrow().weeklyTokens()).isZero();
    }

    @Test
    void statusHidesNextGrantAfterSubscriptionEnded() {
        tokens.grantWeekly(DISCORD_ID, 1);

        assertThat(tokens.findStatus(USER).orElseThrow().nextWeeklyGrantAt()).isNull();
    }

    @Test
    void unknownPurchaseIsNotFound() {
        assertThat(tokens.refund(PURCHASE)).isEqualTo(FinishResult.NOT_FOUND);
    }

    @Test
    void refundPendingTouchesOnlyThisServer() {
        jdbc.update("INSERT INTO discord_linked_accounts (discord_id, ss14_user_id) VALUES (?, ?)", 501L, OTHER_USER);
        tokens.give(DISCORD_ID, 1);
        tokens.give(501, 1);
        tokens.purchase(USER, PURCHASE, "Traitor", "main");
        UUID otherPurchase = UUID.randomUUID();
        tokens.purchase(OTHER_USER, otherPurchase, "Traitor", "second");

        assertThat(tokens.refundPending("main")).isEqualTo(1);
        assertThat(tokens.refundPending("main")).isZero();

        assertThat(balance()).isEqualTo(1);
        assertThat(status(otherPurchase)).isEqualTo("PENDING");
    }
}
