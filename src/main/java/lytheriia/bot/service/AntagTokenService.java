package lytheriia.bot.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import lytheriia.bot.config.BoostyProperties;
import lytheriia.bot.db.entity.AntagTokenBalanceEntity;
import lytheriia.bot.db.entity.AntagTokenPurchaseEntity;
import lytheriia.bot.db.entity.DiscordLinkEntity;
import lytheriia.bot.db.repository.AntagTokenBalanceRepository;
import lytheriia.bot.db.repository.AntagTokenPurchaseRepository;
import lytheriia.bot.db.repository.BoostySubscriptionRepository;
import lytheriia.bot.db.repository.DiscordLinkRepository;

@Service
public class AntagTokenService {

    public enum PurchaseResult {
        CREATED, NOT_LINKED, INSUFFICIENT_TOKENS, ACTIVE_PURCHASE, PURCHASE_ID_CONFLICT
    }

    public enum FinishResult {
        FINISHED, NOT_FOUND, ALREADY_FINISHED
    }

    public record ActivePurchase(UUID id, String listingId, String serverId) {
    }

    /**
     * @param activePurchase null if no purchase is waiting
     * @param nextWeeklyGrantAt null if there was no grant yet or the player has no weekly tokens
     */
    public record TokenStatus(
            int balance,
            ActivePurchase activePurchase,
            int weeklyTokens,
            int weeklyTokenCap,
            Instant nextWeeklyGrantAt) {
    }

    private static final Set<String> PURCHASE_RACE_CONSTRAINTS = Set.of(
            "antag_token_purchase_pkey", "uq_antag_token_purchase_active");

    private static final Duration WEEKLY_GRANT_INTERVAL = Duration.ofDays(7);

    private final AntagTokenBalanceRepository tokensRepository;
    private final AntagTokenPurchaseRepository purchaseRepository;
    private final DiscordLinkRepository linkRepository;
    private final BoostySubscriptionRepository subscriptionRepository;
    private final BoostyProperties properties;
    private final TransactionTemplate transactionTemplate;

    public AntagTokenService(AntagTokenBalanceRepository tokensRepository,
            AntagTokenPurchaseRepository purchaseRepository,
            DiscordLinkRepository linkRepository,
            BoostySubscriptionRepository subscriptionRepository,
            BoostyProperties properties,
            TransactionTemplate transactionTemplate) {
        this.tokensRepository = tokensRepository;
        this.purchaseRepository = purchaseRepository;
        this.linkRepository = linkRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.properties = properties;
        this.transactionTemplate = transactionTemplate;
    }

    /**
     * @return true if tokens were added, false if the last grant was less than 7 days ago or the balance is at the cap
     */
    @Transactional
    public boolean grantWeekly(long discordId, int amount) {
        requirePositive(amount);
        return tokensRepository.addWeekly(discordId, amount, properties.weeklyTokenCap(),
                (int) WEEKLY_GRANT_INTERVAL.toDays()) > 0;
    }

    @Transactional
    public void give(long discordId, int amount) {
        requirePositive(amount);
        tokensRepository.addManual(discordId, amount);
    }

    /**
     * @return empty if the SS14 account is not linked
     */
    @Transactional(readOnly = true)
    public Optional<TokenStatus> findStatus(UUID ss14UserId) {
        return linkRepository.findBySs14UserId(ss14UserId)
                .map(DiscordLinkEntity::getDiscordId)
                .map(this::buildStatus);
    }

    private TokenStatus buildStatus(long discordId) {
        Optional<AntagTokenBalanceEntity> balance = tokensRepository.findById(discordId);
        int weeklyTokens = subscriptionRepository.findTopByDiscordIdAndEndedAtIsNullOrderByRankDesc(discordId)
                .map(subscription -> properties.tokensForRank(subscription.getRank()))
                .orElse(0);

        return new TokenStatus(
                balance.map(AntagTokenBalanceEntity::getBalance).orElse(0),
                purchaseRepository.findPendingByDiscordId(discordId)
                        .map(p -> new ActivePurchase(p.getId(), p.getListingId(), p.getServerId()))
                        .orElse(null),
                weeklyTokens,
                properties.weeklyTokenCap(),
                weeklyTokens == 0 ? null
                        : balance.map(AntagTokenBalanceEntity::getLastWeeklyGrantAt)
                                .map(last -> last.plus(WEEKLY_GRANT_INTERVAL))
                                .orElse(null));
    }

    /**
     * Takes one token and creates a waiting purchase. A retry of a waiting purchase with the same purchaseId changes
     * nothing. A concurrent purchase fails on a constraint, which rolls back the spent token, and is checked again.
     */
    public PurchaseResult purchase(UUID ss14UserId, UUID purchaseId, String listingId, String serverId) {
        try {
            return transactionTemplate.execute(tx -> createPurchase(ss14UserId, purchaseId, listingId, serverId));
        } catch (DataIntegrityViolationException e) {
            if (!(e.getCause() instanceof ConstraintViolationException violation)
                    || !PURCHASE_RACE_CONSTRAINTS.contains(violation.getConstraintName())) {
                throw e;
            }

            // The other transaction has committed, so this run sees its purchase.
            return transactionTemplate.execute(tx -> createPurchase(ss14UserId, purchaseId, listingId, serverId));
        }
    }

    private PurchaseResult createPurchase(UUID ss14UserId, UUID purchaseId, String listingId, String serverId) {
        Optional<Long> discordId = linkRepository.findBySs14UserId(ss14UserId).map(DiscordLinkEntity::getDiscordId);
        if (discordId.isEmpty()) {
            return PurchaseResult.NOT_LINKED;
        }

        Optional<PurchaseResult> blocked = findBlockingResult(discordId.get(), purchaseId, listingId, serverId);
        if (blocked.isPresent()) {
            return blocked.get();
        }

        if (tokensRepository.spendOne(discordId.get()) == 0) {
            // spendOne may have waited for a concurrent purchase that took the last token.
            return findBlockingResult(discordId.get(), purchaseId, listingId, serverId)
                    .orElse(PurchaseResult.INSUFFICIENT_TOKENS);
        }

        purchaseRepository.saveAndFlush(new AntagTokenPurchaseEntity(purchaseId, discordId.get(), listingId, serverId));
        return PurchaseResult.CREATED;
    }

    private Optional<PurchaseResult> findBlockingResult(long discordId, UUID purchaseId, String listingId,
            String serverId) {
        // The purchase can be committed between these two reads, so the second one may find this same purchase.
        return purchaseRepository.findById(purchaseId)
                .or(() -> purchaseRepository.findPendingByDiscordId(discordId))
                .map(found -> {
                    if (!found.getId().equals(purchaseId)) {
                        return PurchaseResult.ACTIVE_PURCHASE;
                    }

                    return found.isSameWaitingPurchase(discordId, listingId, serverId)
                            ? PurchaseResult.CREATED
                            : PurchaseResult.PURCHASE_ID_CONFLICT;
                });
    }

    @Transactional
    public FinishResult markGranted(UUID purchaseId) {
        return finish(purchaseId, AntagTokenPurchaseEntity.Status.GRANTED);
    }

    @Transactional
    public FinishResult refund(UUID purchaseId) {
        return finish(purchaseId, AntagTokenPurchaseEntity.Status.REFUNDED);
    }

    /**
     * @return how many purchases of this server were refunded
     */
    @Transactional
    public int refundPending(String serverId) {
        return refundAll(purchaseRepository.findPendingIds(serverId));
    }

    /**
     * Purchases this old are lost: their server never finished them and did not come back to refund them.
     *
     * @return how many purchases were refunded
     */
    @Transactional
    public int refundExpired(Duration ttl) {
        return refundAll(purchaseRepository.findPendingIdsCreatedBefore(Instant.now().minus(ttl)));
    }

    private int refundAll(List<UUID> purchaseIds) {
        int refunded = 0;
        for (UUID purchaseId : purchaseIds) {
            if (finish(purchaseId, AntagTokenPurchaseEntity.Status.REFUNDED) == FinishResult.FINISHED) {
                refunded++;
            }
        }

        return refunded;
    }

    private FinishResult finish(UUID purchaseId, AntagTokenPurchaseEntity.Status target) {
        Optional<AntagTokenPurchaseEntity> purchase = purchaseRepository.findById(purchaseId);
        if (purchase.isEmpty()) {
            return FinishResult.NOT_FOUND;
        }

        long discordId = purchase.get().getDiscordId();

        if (purchaseRepository.finishIfPending(purchaseId, target.name()) == 0) {
            // A retry of the same transition is fine, the opposite one must not change the balance.
            return purchaseRepository.findById(purchaseId).orElseThrow().getStatus() == target
                    ? FinishResult.FINISHED
                    : FinishResult.ALREADY_FINISHED;
        }

        if (target == AntagTokenPurchaseEntity.Status.REFUNDED) {
            tokensRepository.addManual(discordId, 1);
        }

        return FinishResult.FINISHED;
    }

    private static void requirePositive(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Token amount must be positive: " + amount);
        }
    }
}
