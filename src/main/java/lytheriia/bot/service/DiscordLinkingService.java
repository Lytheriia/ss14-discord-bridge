package lytheriia.bot.service;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import lytheriia.bot.db.entity.DiscordLinkEntity;
import lytheriia.bot.db.repository.DiscordLinkRepository;

@Service
public class DiscordLinkingService {

    public enum Result {
        LINKED, ALREADY_LINKED, INVALID_CODE, ACCOUNT_TAKEN
    }

    private static final Logger log = LoggerFactory.getLogger(DiscordLinkingService.class);

    private static final Duration TTL = Duration.ofMinutes(3);
    private final StringRedisTemplate redis;
    private final DiscordLinkRepository links;
    private final DiscordAccountInfoService info;
    private final TransactionTemplate transactionTemplate;

    public DiscordLinkingService(StringRedisTemplate redis, DiscordLinkRepository links,
            DiscordAccountInfoService info, TransactionTemplate transactionTemplate) {
        this.redis = redis;
        this.links = links;
        this.info = info;
        this.transactionTemplate = transactionTemplate;
    }

    private static String normalize(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
    }

    public boolean isLinked(UUID userId) {
        return links.existsBySs14UserId(userId);
    }

    public Optional<DiscordLinkEntity> findLink(UUID userId) {
        return links.findBySs14UserId(userId);
    }

    public Result link(long discordId, String code) {

        code = normalize(code);

        if (links.existsByDiscordId(discordId))
            return Result.ALREADY_LINKED;

        Optional<UUID> found = findUserByCode(code);
        if (found.isEmpty())
            return Result.INVALID_CODE;

        UUID userId = found.get();

        if (links.existsBySs14UserId(userId)) {
            deleteCode(code, userId);
            return Result.ACCOUNT_TAKEN;
        }

        try {
            // The code stays in Redis until the commit, so a database failure does not burn it.
            // A concurrent use of the same code is stopped by the unique ss14_user_id.
            transactionTemplate.executeWithoutResult(status -> {
                links.save(new DiscordLinkEntity(discordId, userId));
                info.createIfMissing(discordId);
            });
        } catch (DataIntegrityViolationException e) {
            return Result.ACCOUNT_TAKEN;
        }

        deleteCode(code, userId);
        return Result.LINKED;
    }

    public void register(String code, UUID userId) {
        code = normalize(code);

        String codeKey = "link:code:" + code;
        String userKey = "link:user:" + userId;
        String id = userId.toString();

        Boolean created = redis.opsForValue().setIfAbsent(codeKey, id, TTL);
        if (!Boolean.TRUE.equals(created)) {
            if (!id.equals(redis.opsForValue().get(codeKey)))
                throw new IllegalStateException("Code collision");
            redis.expire(codeKey, TTL);
        }

        String oldCode = redis.opsForValue().setGet(userKey, code, TTL);
        if (oldCode != null && !oldCode.equals(code))
            redis.delete("link:code:" + oldCode);
    }

    private Optional<UUID> findUserByCode(String code) {
        return Optional.ofNullable(redis.opsForValue().get("link:code:" + code)).map(UUID::fromString);
    }

    private void deleteCode(String code, UUID userId) {
        try {
            redis.delete(List.of("link:code:" + code, "link:user:" + userId));
        } catch (DataAccessException e) {
            // Already linked at this point; the keys expire by TTL and a reused code gets ACCOUNT_TAKEN.
            log.warn("Failed to delete linking code of userId={}", userId, e);
        }
    }
}
