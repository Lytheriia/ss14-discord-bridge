package lytheriia.bot.discord.listener;

import org.springframework.stereotype.Component;

import lytheriia.bot.config.DiscordProperties;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

@Component

public class AutoLeaveFromServers extends ListenerAdapter {

    private final long allowedGuildId;

    public AutoLeaveFromServers(DiscordProperties props) {
        this.allowedGuildId = props.guildId();
    }

    @Override
    public void onGuildJoin(GuildJoinEvent event) {
        if (event.getGuild().getIdLong() != allowedGuildId) {
            event.getGuild().leave().queue();
        }
    }
}
