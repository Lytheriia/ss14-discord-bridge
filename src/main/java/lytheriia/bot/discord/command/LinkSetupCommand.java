package lytheriia.bot.discord.command;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import lytheriia.bot.discord.DiscordColors;
import lytheriia.bot.service.DiscordLinkingService;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;

@Component
public class LinkSetupCommand extends ListenerAdapter {

        private static final Logger log = LoggerFactory.getLogger(LinkSetupCommand.class);

        private static final String COMMAND = "link-setup";
        private static final String BUTTON_ID = "link:open";
        private static final String MODAL_ID = "link:modal";
        private static final String INPUT_ID = "code";

        private final DiscordLinkingService linkService;

        public LinkSetupCommand(DiscordLinkingService linkService) {
                this.linkService = linkService;
        }

        @Override
        public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
                if (!event.getName().equals(COMMAND))
                        return;

                Member member = event.getMember();
                if (member == null || !member.hasPermission(Permission.ADMINISTRATOR)) {
                        event.reply("Эта команда доступна только администраторам.").setEphemeral(true).queue();
                        return;
                }

                TextChannel channel = event.getOption("channel", o -> o.getAsChannel().asTextChannel());
                if (channel == null) {
                        event.reply("Укажите текстовый канал.").setEphemeral(true).queue();
                        return;
                }

                MessageEmbed embed = new EmbedBuilder()
                                .setTitle("Привязка аккаунта")
                                .setDescription("""
                                                1. Зайдите в лобби игры и найдите кнопку привязки дискорда.
                                                2. Скопируйте выданный код.
                                                3. Нажмите кнопку ниже и вставьте код.

                                                Код действует 3 минуты.""")
                                .setColor(DiscordColors.BASE_EMBED_COLOR)
                                .build();

                channel.sendMessageEmbeds(embed)
                                .setComponents(ActionRow.of(
                                                Button.secondary(BUTTON_ID, "Привязать аккаунт")
                                                                .withEmoji(Emoji.fromUnicode("🔗"))))
                                .queue(
                                                ok -> event.reply("Сообщение отправлено в " + channel.getAsMention())
                                                                .setEphemeral(true).queue(),
                                                err -> event.reply(
                                                                "Ты явно проебался где-то")
                                                                .setEphemeral(true).queue());
        }

        @Override
        public void onButtonInteraction(ButtonInteractionEvent event) {
                if (!event.getComponentId().equals(BUTTON_ID))
                        return;

                TextInput input = TextInput.create(INPUT_ID, "Код привязки", TextInputStyle.SHORT)
                                .setPlaceholder("Вставьте сюда код, полученный в игре")
                                .setRequiredRange(36, 36)
                                .setRequired(true)
                                .build();

                event.replyModal(Modal.create(MODAL_ID, "Привязка к SS14")
                                .addActionRow(input)
                                .build()).queue();
        }

        @Override
        public void onModalInteraction(ModalInteractionEvent event) {
                if (!event.getModalId().equals(MODAL_ID))
                        return;

                ModalMapping mapping = event.getValue(INPUT_ID);
                String code = mapping == null ? "" : mapping.getAsString().trim().toUpperCase();

                event.deferReply(true).queue();

                DiscordLinkingService.Result result;
                try {
                        result = linkService.link(event.getUser().getIdLong(), code);
                } catch (Exception e) {
                        log.error("Failed to link discordId={}", event.getUser().getIdLong(), e);
                        event.getHook().sendMessage("❌ Не удалось привязать аккаунт. Попробуйте ещё раз позже.").queue();
                        return;
                }

                String text = switch (result) {
                        case LINKED -> "✅ Аккаунт успешно привязан.";
                        case ALREADY_LINKED -> "Этот Discord уже привязан к аккаунту SS14.";
                        case ACCOUNT_TAKEN -> "Этот аккаунт SS14 уже привязан к другому Discord.";
                        case INVALID_CODE -> "❌ Код неверный или истёк. Получите новый в игре.";
                };

                event.getHook().sendMessage(text).queue();
        }
}