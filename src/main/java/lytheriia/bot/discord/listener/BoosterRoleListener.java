package lytheriia.bot.discord.listener;

import org.springframework.stereotype.Component;

import lytheriia.bot.config.DiscordProperties;
import lytheriia.bot.service.DiscordAccountInfoService;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleAddEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

@Component
public class BoosterRoleListener extends ListenerAdapter {

    private long boosterRoleId;

    private final DiscordAccountInfoService accountService;

    public BoosterRoleListener(DiscordAccountInfoService accountService, DiscordProperties props) {
        this.accountService = accountService;

        this.boosterRoleId = props.getRoles().getBooster();
    }

    @Override
    public void onGuildMemberRoleAdd(GuildMemberRoleAddEvent event) {
        boolean boosterRoleAdded = event.getRoles().stream()
                .anyMatch(role -> role.getIdLong() == boosterRoleId);

        if (!boosterRoleAdded) {
            return;
        }

        accountService.setBooster(event.getUser().getIdLong(), true);
    }

    @Override
    public void onGuildMemberRoleRemove(GuildMemberRoleRemoveEvent event) {
        boolean boosterRole = event.getRoles().stream().anyMatch(role -> role.getIdLong() == boosterRoleId);

        if (!boosterRole) {
            return;
        }

        accountService.setBooster(event.getUser().getIdLong(), false);
    }

}
