package lytheriia.bot.discord.command;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;

import lytheriia.bot.db.entity.RoundSnapshotEntity;
import lytheriia.bot.discord.DiscordColors;
import lytheriia.bot.service.RoundSnapshotService;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.api.utils.messages.MessageEditData;

@Component
public class StatsCommand extends ListenerAdapter {

    private static final Logger log = LoggerFactory.getLogger(StatsCommand.class);

    private static final String COMMAND = "stats";

    private static final String BUTTON_PREFIX = "stats:change:";
    private static final String MODAL_PREFIX = "stats:days-modal:";
    private static final String INPUT_ID = "days";

    private static final int DEFAULT_DAYS = 1;
    private static final int MAX_DAYS = 90;
    private static final int TARGET_POINTS = 400;

    private final RoundSnapshotService snapshotService;
    private final TaskExecutor executor;
    private final CommandGuard guard;

    public StatsCommand(RoundSnapshotService snapshotService,
            @Qualifier("applicationTaskExecutor") TaskExecutor executor,
            CommandGuard guard) {
        this.snapshotService = snapshotService;
        this.executor = executor;
        this.guard = guard;
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals(COMMAND))
            return;

        if (!guard.isAllow(event))
            return;

        int days = Math.clamp(
                event.getOption("days", DEFAULT_DAYS, OptionMapping::getAsInt), 1, MAX_DAYS);

        long userId = event.getUser().getIdLong();

        event.deferReply().queue();
        executor.execute(() -> {
            try {
                event.getHook().sendMessage(render(days, userId)).queue();
            } catch (IOException e) {
                log.error("Stats render error", e);
                event.getHook().sendMessage("Не удалось построить график.").queue();
            }
        });
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String componentId = event.getComponentId();

        if (!componentId.startsWith(BUTTON_PREFIX))
            return;

        String payload = componentId.substring(BUTTON_PREFIX.length());
        String[] parts = payload.split(":");

        if (parts.length != 2)
            return;

        long ownerId;
        int currentDays;

        try {
            ownerId = Long.parseLong(parts[0]);
            currentDays = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            log.warn("Invalid stats button id: {}", componentId);
            return;
        }

        if (event.getUser().getIdLong() != ownerId) {
            event.reply(
                    "Этот график может изменять только тот, кто его запросил.").setEphemeral(true).queue();

            return;
        }

        TextInput input = TextInput.create(INPUT_ID, "Количество дней", TextInputStyle.SHORT)
                .setPlaceholder("Число от 1 до " + MAX_DAYS)
                .setValue(String.valueOf(currentDays))
                .setRequiredRange(1, 2)
                .setRequired(true)
                .build();

        Modal modal = Modal.create(
                MODAL_PREFIX + ownerId,
                "Период графика")
                .addActionRow(input)
                .build();

        event.replyModal(modal).queue();
    }

    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        String modalId = event.getModalId();

        if (!modalId.startsWith(MODAL_PREFIX))
            return;

        long ownerId;

        try {
            ownerId = Long.parseLong(
                    modalId.substring(MODAL_PREFIX.length()));
        } catch (NumberFormatException e) {
            log.warn("Invalid stats modal id: {}", modalId);
            return;
        }

        if (event.getUser().getIdLong() != ownerId) {
            event.reply(
                    "Этот график может изменять только тот, кто его запросил.").setEphemeral(true).queue();

            return;
        }

        ModalMapping mapping = event.getValue(INPUT_ID);
        int days = parseDays(mapping == null ? "" : mapping.getAsString());

        if (days < 1 || days > MAX_DAYS) {
            event.reply("Введите целое число от 1 до " + MAX_DAYS + ".")
                    .setEphemeral(true).queue();
            return;
        }

        event.deferEdit().queue();
        executor.execute(() -> {
            try {
                event.getHook()
                        .editOriginal(MessageEditData.fromCreateData(render(days, ownerId)))
                        .queue();

            } catch (IOException e) {
                log.error("Stats render error", e);
                event.getHook().sendMessage("Не удалось построить график.")
                        .setEphemeral(true).queue();
            }
        });
    }

    private static int parseDays(String raw) {
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private MessageCreateData render(int days, long userId) throws IOException {

        Button button = Button.secondary(BUTTON_PREFIX + userId + ":" + days, "Изменить дни")
                .withEmoji(Emoji.fromUnicode("📅"));

        MessageCreateBuilder message = new MessageCreateBuilder()
                .setComponents(ActionRow.of(button));

        List<RoundSnapshotEntity> data = snapshotService.findLastDays(days);

        if (data.isEmpty()) {
            return message.setContent("За последние " + days + " дн. данных нет.").build();
        }

        String fileName = "online_" + days + "d.png";
        byte[] png = renderChart(data, days);

        MessageEmbed embed = new EmbedBuilder()
                .setTitle("Онлайн за " + days + " дн.")
                .setColor(DiscordColors.BASE_EMBED_COLOR)
                .setImage("attachment://" + fileName)
                .build();

        return message
                .setEmbeds(embed)
                .addFiles(FileUpload.fromData(png, fileName))
                .build();
    }

    private byte[] renderChart(List<RoundSnapshotEntity> data, int days) throws IOException {
        long bucketMs = Math.max(30_000L, Duration.ofDays(days).toMillis() / TARGET_POINTS);

        Map<Long, double[]> buckets = new TreeMap<>();
        for (RoundSnapshotEntity s : data) {
            long ms = s.getTimeStamp().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            double[] acc = buckets.computeIfAbsent(ms / bucketMs, k -> new double[2]);
            acc[0] += s.getPlayers();
            acc[1]++;
        }

        TimeSeries series = new TimeSeries("Игроки");
        buckets.forEach((key, acc) -> series.addOrUpdate(new Millisecond(new Date(key * bucketMs)), acc[0] / acc[1]));

        JFreeChart chart = ChartFactory.createTimeSeriesChart(
                null, null, "Игроки", new TimeSeriesCollection(series), false, false, false);

        Color background = new Color(DiscordColors.CHART_BACKGROUND);

        Color grid = new Color(DiscordColors.CHART_GRID);

        Color text = new Color(DiscordColors.CHART_TEXT);

        Font font = new Font("SansSerif", Font.PLAIN, 14);

        chart.setBackgroundPaint(background);

        XYPlot plot = chart.getXYPlot();

        plot.setBackgroundPaint(background);
        plot.setOutlineVisible(false);

        plot.setDomainGridlinePaint(grid);
        plot.setRangeGridlinePaint(grid);

        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(true, false);
        renderer.setSeriesPaint(0, new Color(0x5865F2));
        renderer.setSeriesStroke(0, new BasicStroke(2.2f));
        plot.setRenderer(renderer);

        DateAxis domain = (DateAxis) plot.getDomainAxis();
        domain.setDateFormatOverride(new SimpleDateFormat(days <= 2 ? "HH:mm" : "dd.MM"));
        styleAxis(domain, text, grid, font);

        NumberAxis range = (NumberAxis) plot.getRangeAxis();
        range.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        range.setLowerBound(0);
        styleAxis(range, text, grid, font);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ChartUtils.writeChartAsPNG(out, chart, 1000, 450);
        return out.toByteArray();
    }

    private static void styleAxis(org.jfree.chart.axis.ValueAxis axis, Color text, Color line, Font font) {
        axis.setTickLabelPaint(text);
        axis.setLabelPaint(text);

        axis.setTickLabelFont(font);
        axis.setLabelFont(font);

        axis.setAxisLinePaint(line);
        axis.setTickMarkPaint(line);
    }
}