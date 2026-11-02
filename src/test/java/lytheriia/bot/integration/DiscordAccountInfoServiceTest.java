package lytheriia.bot.integration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import lytheriia.bot.service.DiscordAccountInfoService;
import lytheriia.bot.support.IntegrationTest;

class DiscordAccountInfoServiceTest extends IntegrationTest {

    @Autowired
    private DiscordAccountInfoService accountInfo;

    private int rows() {
        return jdbc.queryForObject("SELECT count(*) FROM discord_account_info", Integer.class);
    }

    @Test
    void clearingEachFlagLeavesTheOtherOne() {
        accountInfo.setStatus(1, true, true);

        accountInfo.clearBooster(1);
        assertThat(accountInfo.getAllStatuses().get(1L).verified()).isTrue();
        assertThat(accountInfo.getAllStatuses().get(1L).booster()).isFalse();

        accountInfo.clearVerified(1);
        assertThat(accountInfo.getAllStatuses().get(1L).verified()).isFalse();
    }

    @Test
    void clearingUnknownMemberDoesNotCreateRow() {
        accountInfo.clearBooster(1);
        accountInfo.clearVerified(1);

        assertThat(rows()).isZero();
    }
}
