package lytheriia.bot.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import lytheriia.bot.service.DiscordAccountInfoService;
import lytheriia.bot.service.DiscordLinkingService;
import lytheriia.bot.service.DiscordLinkingService.Result;
import lytheriia.bot.support.IntegrationTest;

class DiscordLinkingServiceTest extends IntegrationTest {

    private static final String CODE = "22222222-2222-2222-2222-222222222222";
    private static final String CODE_KEY = "link:code:" + CODE;
    private static final UUID USER = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final String USER_KEY = "link:user:" + USER;

    @Autowired
    private DiscordLinkingService linking;

    @MockitoSpyBean
    private DiscordAccountInfoService accountInfo;

    private int linkCount() {
        return jdbc.queryForObject("SELECT count(*) FROM discord_linked_accounts", Integer.class);
    }

    @Test
    void registerGivesBothKeysTtl() {
        linking.register(CODE, USER);

        assertThat(redis.getExpire(CODE_KEY)).isPositive();
        assertThat(redis.getExpire(USER_KEY)).isPositive();
    }

    @Test
    void newCodeReplacesOldCodeOfSameUser() {
        String newCode = "33333333-3333-3333-3333-333333333333";

        linking.register(CODE, USER);
        linking.register(newCode, USER);

        assertThat(redis.hasKey(CODE_KEY)).isFalse();
        assertThat(redis.hasKey("link:code:" + newCode)).isTrue();
    }

    @Test
    void successfulLinkDeletesCode() {
        linking.register(CODE, USER);

        assertThat(linking.link(500, CODE)).isEqualTo(Result.LINKED);

        assertThat(linking.isLinked(USER)).isTrue();
        assertThat(redis.hasKey(CODE_KEY)).isFalse();
        assertThat(redis.hasKey(USER_KEY)).isFalse();
    }

    @Test
    void databaseFailureRollsBackLinkAndKeepsCode() {
        linking.register(CODE, USER);
        doThrow(new IllegalStateException("database is down")).when(accountInfo).createIfMissing(anyLong());

        assertThatThrownBy(() -> linking.link(500, CODE)).isInstanceOf(IllegalStateException.class);

        assertThat(linkCount()).isZero();
        assertThat(redis.hasKey(CODE_KEY)).isTrue();
    }

    @Test
    void unknownCodeIsInvalid() {
        assertThat(linking.link(500, CODE)).isEqualTo(Result.INVALID_CODE);
    }

    @Test
    void accountOfAnotherDiscordIsTakenAndCodeIsDeleted() {
        linking.register(CODE, USER);
        linking.link(500, CODE);
        redis.opsForValue().set(CODE_KEY, USER.toString());

        assertThat(linking.link(501, CODE)).isEqualTo(Result.ACCOUNT_TAKEN);
        assertThat(redis.hasKey(CODE_KEY)).isFalse();
    }
}
