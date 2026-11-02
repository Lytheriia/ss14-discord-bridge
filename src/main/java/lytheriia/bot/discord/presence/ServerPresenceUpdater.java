package lytheriia.bot.discord.presence;

import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lytheriia.bot.service.RoundSnapshotService;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.OnlineStatus;

@Component
public class ServerPresenceUpdater {

    private static final Logger log = LoggerFactory.getLogger(ServerPresenceUpdater.class);

    private final JDA jda;
    private final ServerStatusClient statusClient;
    private final RoundSnapshotService snapshots;

    private volatile ServerStatus lastStatus;
    private String lastText;

    private final Boolean IsDisabled = false;

    public ServerPresenceUpdater(JDA jda, ServerStatusClient statusClient,
            RoundSnapshotService snapshots) {
        this.jda = jda;
        this.statusClient = statusClient;
        this.snapshots = snapshots;
    }

    @Scheduled(fixedRate = 30_000)
    public void updateStatus() {
        try {
            if (IsDisabled) {
                setPresence(OnlineStatus.DO_NOT_DISTURB, "🛠️ Тех работы");
                return;
            }

            Optional<ServerStatus> result = statusClient.fetch();
            lastStatus = result.orElse(null);

            if (result.isEmpty()) {
                setPresence(OnlineStatus.DO_NOT_DISTURB, "🔴 Сервер оффлайн");
                return;
            }

            ServerStatus s = result.get();
            int players = orDefault(s.players(), 0);
            int maxPlayers = orDefault(s.softMaxPlayers(), 120);
            int roundId = orDefault(s.roundId(), 0);
            int runLevel = orDefault(s.runLevel(), 1);
            String map = s.map() != null ? s.map() : "?";

            String playersText = players + "/" + maxPlayers;
            String duration = formatRoundDuration(s.roundStartTime(), runLevel);

            String text = "👤" + playersText + " 🕒" + duration + " #" + roundId;

            if (text.codePointCount(0, text.length()) + map.length() + 3 <= 128) {
                text = "👤" + playersText + " 🕒" + duration + " 🗺️" + map;
            }

            if (Boolean.TRUE.equals(s.panicBunker())) {
                text += " 🛡️";
            }

            setPresence(OnlineStatus.ONLINE, text);

        } catch (Exception e) {
            log.error("Presence update error", e);
            jda.getPresence().setPresence(OnlineStatus.IDLE, Activity.playing("⚠ Ошибка обновления"));
        }
    }

    @Scheduled(fixedRate = 30_000)
    public void snapshotRound() {
        ServerStatus s = lastStatus;
        if (s == null)
            return;

        try {
            snapshots.saveSnapshot(
                    orDefault(s.roundId(), 0),
                    orDefault(s.players(), 0));
        } catch (Exception e) {
            log.error("Database round snapshot error", e);
        }
    }

    private void setPresence(OnlineStatus status, String text) {
        String key = status + "|" + text;
        if (key.equals(lastText))
            return;
        lastText = key;
        jda.getPresence().setPresence(status, Activity.customStatus(text));
    }

    private static int orDefault(Integer value, int def) {
        return value != null ? value : def;
    }

    static String formatRoundDuration(String startTime, int runLevel) {
        if (runLevel == 0 || startTime == null || startTime.isBlank()) {
            return "0ч 0м";
        }
        try {
            Duration d = Duration.between(OffsetDateTime.parse(startTime).toInstant(), Instant.now());
            return d.toHours() + "ч " + d.toMinutesPart() + "м";
        } catch (DateTimeParseException e) {
            log.error("Time parse error: {} | {}", startTime, e.getMessage());
            return "???";
        }
    }
}