package lytheriia.bot.discord.commands;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import lytheriia.bot.db.service.AntagTokenService;
import lytheriia.bot.discord.interfaces.IBaseCommand;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

@Component
public class TokensCommand implements IBaseCommand {

    private static final Logger log = LoggerFactory.getLogger(TokensCommand.class);

    private static final String COMMAND = "tokens";
    private static final String SUBCOMMAND_GIVE = "give";
    private static final String OPTION_USER = "user";
    private static final String OPTION_AMOUNT = "amount";
    private static final int MAX_AMOUNT = 1000;

    private final AntagTokenService tokenService;

    public TokensCommand(AntagTokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    public SlashCommandData data() {
        return Commands.slash(COMMAND, "Антаг-токены")
                .setContexts(InteractionContextType.GUILD)
                // Access is checked by CommandGuard, so roles from /addpermission can see the command too.
                .setDefaultPermissions(DefaultMemberPermissions.ENABLED)
                .addSubcommands(new SubcommandData(SUBCOMMAND_GIVE, "Выдать токены участнику")
                        .addOptions(
                                new OptionData(OptionType.USER, OPTION_USER, "Кому выдать", true),
                                new OptionData(OptionType.INTEGER, OPTION_AMOUNT, "Количество токенов", true)
                                        .setRequiredRange(1, MAX_AMOUNT)));
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        User target = event.getOption(OPTION_USER, OptionMapping::getAsUser);
        int amount = event.getOption(OPTION_AMOUNT, OptionMapping::getAsInt);

        if (target.isBot()) {
            event.reply("Ботам нельзя выдавать токены.").setEphemeral(true).queue();
            return;
        }

        tokenService.give(target.getIdLong(), amount);
        log.info("Tokens given: amount={}, targetId={}, adminId={}", amount, target.getIdLong(),
                event.getUser().getIdLong());

        event.reply("Участнику " + target.getAsMention() + " выдано токенов: " + amount + ".")
                .setEphemeral(true)
                .queue();
    }
}
