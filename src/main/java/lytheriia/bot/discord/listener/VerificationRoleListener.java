package lytheriia.bot.discord.listener;

import org.springframework.stereotype.Component;

import lytheriia.bot.config.DiscordProperties;
import lytheriia.bot.service.DiscordAccountInfoService;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleAddEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

@Component
public class VerificationRoleListener extends ListenerAdapter {

    private long verifiedRoleId;

    private final DiscordAccountInfoService accountService;

    public VerificationRoleListener(DiscordAccountInfoService accountService, DiscordProperties props) {
        this.accountService = accountService;

        this.verifiedRoleId = props.getRoles().getVerified();
    }

    @Override
    public void onGuildMemberRoleAdd(GuildMemberRoleAddEvent event) {
        boolean verifiedRoleAdded = event.getRoles().stream()
                .anyMatch(role -> role.getIdLong() == verifiedRoleId);

        if (!verifiedRoleAdded) {
            return;
        }

        accountService.setVerified(event.getUser().getIdLong(), true);
    }

    @Override
    public void onGuildMemberRoleRemove(GuildMemberRoleRemoveEvent event) {
        boolean verifiedRole = event.getRoles().stream().anyMatch(role -> role.getIdLong() == verifiedRoleId);

        if (!verifiedRole) {
            return;
        }

        accountService.setVerified(event.getUser().getIdLong(), false);
    }
}