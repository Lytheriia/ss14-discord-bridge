package lytheriia.bot.config;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lytheriia.bot.discord.command.CommandDispatcher;
import lytheriia.bot.service.CommandPermissionService;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;

@Configuration
public class JdaConfig {

        private static final Logger log = LoggerFactory.getLogger(JdaConfig.class);

        private static OptionData commandsOption() {
                OptionData option = new OptionData(OptionType.STRING, "command", "Команда", true);
                CommandPermissionService.MANAGED.forEach(c -> option.addChoice(c, c));
                return option;
        }

        @Bean(destroyMethod = "shutdown")
        public JDA jda(DiscordProperties props,
                        List<ListenerAdapter> listeners, CommandDispatcher commandDispatcher)
                        throws InterruptedException {

                JDA jda = JDABuilder.createLight(props.getToken())
                                .setStatus(OnlineStatus.DO_NOT_DISTURB)
                                .setActivity(Activity.customStatus("Запуск..."))
                                .enableIntents(GatewayIntent.GUILD_MEMBERS)
                                .setMemberCachePolicy(MemberCachePolicy.ALL)
                                .setChunkingFilter(ChunkingFilter.ALL)
                                .addEventListeners(listeners.toArray())
                                .build()
                                .awaitReady();

                long guildId = props.getGuildId();

                jda.getGuilds().stream()
                                .filter(g -> g.getIdLong() != guildId)
                                .forEach(g -> g.leave().queue());

                Guild guild = jda.getGuildById(guildId);
                if (guild == null) {
                        throw new IllegalStateException("Main guild not found : " + guildId);
                }

                jda.updateCommands().queue();

                commandDispatcher.register(guild);

                // TODO REBUILD THAT SHIT
                jda.updateCommands().addCommands(
                                Commands.slash("stats", "Статистика раундов")
                                                .setDefaultPermissions(DefaultMemberPermissions.ENABLED),

                                Commands.slash("link-setup", "Отправить сообщение с кнопкой привязки")
                                                .setContexts(InteractionContextType.GUILD)
                                                .setDefaultPermissions(DefaultMemberPermissions
                                                                .enabledFor(Permission.ADMINISTRATOR))
                                                .addOptions(new OptionData(OptionType.CHANNEL, "channel",
                                                                "Канал для сообщения", true)
                                                                .setChannelTypes(ChannelType.TEXT)),

                                Commands.slash("addpermission", "Разрешить роли использовать команду")
                                                .setDefaultPermissions(DefaultMemberPermissions
                                                                .enabledFor(Permission.ADMINISTRATOR))
                                                .addOptions(new OptionData(OptionType.ROLE, "role", "Роль", true),
                                                                commandsOption()),

                                Commands.slash("removepermission", "Убрать доступ роли к команде")
                                                .setDefaultPermissions(DefaultMemberPermissions
                                                                .enabledFor(Permission.ADMINISTRATOR))
                                                .addOptions(new OptionData(OptionType.ROLE, "role", "Роль", true),
                                                                commandsOption()),

                                Commands.slash("listpermissions", "Кто может использовать команду")
                                                .setDefaultPermissions(DefaultMemberPermissions
                                                                .enabledFor(Permission.ADMINISTRATOR))
                                                .addOptions(commandsOption()),
                                Commands.slash("syncdbroles", "Синхронизация дискорд сервера с базой данных")
                                                .setDefaultPermissions(DefaultMemberPermissions
                                                                .enabledFor(Permission.ADMINISTRATOR)))
                                .queue(
                                                ok -> log.info("Registered {} guild commands", ok.size()),
                                                err -> log.error("Failed to register commands", err));

                return jda;
        }
}