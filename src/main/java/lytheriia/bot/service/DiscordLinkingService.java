package lytheriia.bot.service;

import java.time.Duration;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import lytheriia.bot.db.entity.DiscordLinkEntity;
import lytheriia.bot.db.repository.DiscordLinkRepository;

@Service
public class DiscordLinkingService {

    public enum Result {
        LINKED, ALREADY_LINKED, INVALID_CODE, ACCOUNT_TAKEN
    }

    private static final Duration TTL = Duration.ofMinutes(3);
    private final StringRedisTemplate redis;
    private final DiscordLinkRepository links;
    private final DiscordAccountInfoService info;

    public DiscordLinkingService(StringRedisTemplate redis, DiscordLinkRepository links,
            DiscordAccountInfoService info) {
        this.redis = redis;
        this.links = links;
        this.info = info;
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

        Optional<UUID> userId = consume(code);
        if (userId.isEmpty())
            return Result.INVALID_CODE;

        if (links.existsBySs14UserId(userId.get()))
            return Result.ACCOUNT_TAKEN;

        try {
            links.save(new DiscordLinkEntity(discordId, userId.get()));

            info.createIfMissing(discordId);

            return Result.LINKED;
        } catch (DataIntegrityViolationException e) {
            return Result.ACCOUNT_TAKEN;
        }
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

        String oldCode = redis.opsForValue().getAndSet(userKey, code);
        redis.expire(userKey, TTL);
        if (oldCode != null && !oldCode.equals(code))
            redis.delete("link:code:" + oldCode);
    }

    public Optional<UUID> consume(String code) {
        String userId = redis.opsForValue().getAndDelete("link:code:" + normalize(code));
        if (userId == null)
            return Optional.empty();
        redis.delete("link:user:" + userId);
        return Optional.of(UUID.fromString(userId));
    }
}