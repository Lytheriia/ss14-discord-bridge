package lytheriia.bot.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import lytheriia.bot.service.DiscordLinkingService;
import lytheriia.bot.support.IntegrationTest;

/**
 * Pins the contract used by the Nebula client: paths, status codes and JSON fields.
 */
class InternalApiTest extends IntegrationTest {

    private static final String CODE = "22222222-2222-2222-2222-222222222222";
    private static final String USER = "11111111-1111-1111-1111-111111111111";
    private static final String REGISTER_BODY = "{\"code\":\"" + CODE + "\",\"userId\":\"" + USER + "\"}";

    private final HttpClient http = HttpClient.newHttpClient();

    @Autowired
    private DiscordLinkingService linking;

    private HttpResponse<String> get(String path, String key) throws Exception {
        return send(HttpRequest.newBuilder(uri(path)).GET(), key);
    }

    private HttpResponse<String> post(String path, String body) throws Exception {
        return send(HttpRequest.newBuilder(uri(path)).header("Content-Type", "application/json")
                .POST(BodyPublishers.ofString(body)), API_KEY);
    }

    private URI uri(String path) {
        return URI.create("http://127.0.0.1:" + port + path);
    }

    private HttpResponse<String> send(HttpRequest.Builder builder, String key) throws Exception {
        if (key != null) {
            builder.header("X-Internal-Api-Key", key);
        }
        return http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void healthNeedsNoKey() throws Exception {
        assertThat(get("/actuator/health", null).statusCode()).isEqualTo(200);
    }

    @Test
    void apiRejectsMissingAndWrongKey() throws Exception {
        assertThat(get("/api/link/" + USER, null).statusCode()).isEqualTo(401);
        assertThat(get("/api/link/" + USER, "wrong").statusCode()).isEqualTo(401);
    }

    @Test
    void nebulaHealthProbeSucceeds() throws Exception {
        assertThat(get("/api/link/00000000-0000-0000-0000-000000000000", API_KEY).statusCode()).isEqualTo(200);
    }

    @Test
    void linkFlowKeepsContract() throws Exception {
        assertThat(get("/api/link/" + USER, API_KEY).body()).isEqualTo("{\"linked\":false,\"discordId\":null}");
        assertThat(post("/api/link/codes", "{\"code\":\"!\",\"userId\":\"" + USER + "\"}").statusCode()).isEqualTo(400);
        assertThat(post("/api/link/codes", REGISTER_BODY).statusCode()).isEqualTo(204);

        linking.link(500, CODE);

        assertThat(get("/api/link/" + USER, API_KEY).body()).isEqualTo("{\"linked\":true,\"discordId\":\"500\"}");

        HttpResponse<String> again = post("/api/link/codes", REGISTER_BODY);
        assertThat(again.statusCode()).isEqualTo(409);
        assertThat(again.body()).contains("ALREADY_LINKED");
    }

    @Test
    void codeOfAnotherUserIsCollision() throws Exception {
        assertThat(post("/api/link/codes", REGISTER_BODY).statusCode()).isEqualTo(204);

        HttpResponse<String> collision = post("/api/link/codes",
                "{\"code\":\"" + CODE + "\",\"userId\":\"33333333-3333-3333-3333-333333333333\"}");

        assertThat(collision.statusCode()).isEqualTo(409);
        assertThat(collision.body()).contains("CODE_COLLISION");
    }

    @Test
    void tokenPurchaseFlowKeepsContract() throws Exception {
        String purchase = "44444444-4444-4444-4444-444444444444";
        String body = "{\"purchaseId\":\"" + purchase + "\",\"listingId\":\"Traitor\",\"serverId\":\"main\"}";

        assertThat(get("/api/tokens/" + USER, API_KEY).body()).contains("NOT_LINKED");
        assertThat(post("/api/tokens/" + USER + "/purchases", body).statusCode()).isEqualTo(404);

        post("/api/link/codes", REGISTER_BODY);
        linking.link(500, CODE);

        assertThat(get("/api/tokens/" + USER, API_KEY).body()).isEqualTo("{\"balance\":0,\"activePurchase\":null,\"weeklyTokens\":0,\"weeklyTokenCap\":4,\"nextWeeklyGrantAt\":null}");
        assertThat(post("/api/tokens/" + USER + "/purchases", body).body()).contains("INSUFFICIENT_TOKENS");

        jdbc.update("INSERT INTO antag_tokens (discord_id, balance) VALUES (500, 1)");

        assertThat(post("/api/tokens/" + USER + "/purchases", body).statusCode()).isEqualTo(204);
        assertThat(get("/api/tokens/" + USER, API_KEY).body()).isEqualTo("{\"balance\":0,\"activePurchase\":"
                + "{\"id\":\"" + purchase + "\",\"listingId\":\"Traitor\",\"serverId\":\"main\"},\"weeklyTokens\":0,\"weeklyTokenCap\":4,\"nextWeeklyGrantAt\":null}");

        assertThat(post("/api/tokens/purchases/" + purchase + "/refund", "").statusCode()).isEqualTo(204);
        assertThat(get("/api/tokens/" + USER, API_KEY).body()).isEqualTo("{\"balance\":1,\"activePurchase\":null,\"weeklyTokens\":0,\"weeklyTokenCap\":4,\"nextWeeklyGrantAt\":null}");

        assertThat(post("/api/tokens/purchases/" + purchase + "/grant", "").body()).contains("ALREADY_FINISHED");
        assertThat(post("/api/tokens/purchases/55555555-5555-5555-5555-555555555555/refund", "").statusCode())
                .isEqualTo(404);
        assertThat(post("/api/tokens/servers/main/refund-pending", "").body()).isEqualTo("{\"refunded\":0}");
        assertThat(post("/api/tokens/" + USER + "/purchases", "{\"purchaseId\":\"" + purchase + "\"}").statusCode())
                .isEqualTo(400);
    }

    @Test
    void nextWeeklyGrantIsAnIsoTimestamp() throws Exception {
        post("/api/link/codes", REGISTER_BODY);
        linking.link(500, CODE);
        jdbc.update("INSERT INTO boosty_subscription (discord_id, promoted_by_id, rank) VALUES (500, 1, 3)");
        jdbc.update("INSERT INTO antag_tokens (discord_id, balance, last_weekly_grant_at) "
                + "VALUES (500, 0, '2026-01-01T00:00:00Z')");

        assertThat(get("/api/tokens/" + USER, API_KEY).body())
                .contains("\"weeklyTokens\":3,\"weeklyTokenCap\":4,\"nextWeeklyGrantAt\":\"2026-01-08T00:00:00Z\"");
    }

    @Test
    void boostyStatusKeepsContract() throws Exception {
        assertThat(get("/api/boosty/" + USER, API_KEY).statusCode()).isEqualTo(404);

        post("/api/link/codes", REGISTER_BODY);
        linking.link(500, CODE);
        jdbc.update("INSERT INTO boosty_subscription (discord_id, promoted_by_id, rank) VALUES (500, 1, 3)");

        HttpResponse<String> status = get("/api/boosty/" + USER, API_KEY);
        assertThat(status.statusCode()).isEqualTo(200);
        assertThat(status.body()).contains("\"rank\":3").contains("\"startedAt\":");
    }
}
