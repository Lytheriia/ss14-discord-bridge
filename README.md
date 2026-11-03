<div align="center">
    <h1>✦ SS14 Discord Bridge ✦</h1>
    <h3></h3>
</div>

<div align="center">

![Last commit](https://img.shields.io/github/last-commit/Lytheriia/ss14-discord-bridge?&style=for-the-badge&color=60a5fa&logo=git&logoColor=DBEAFE&labelColor=172033)
![Java 21](https://img.shields.io/badge/java-21-60a5fa?style=for-the-badge&logo=openjdk&logoColor=DBEAFE&labelColor=172033)
![Spring Boot 4](https://img.shields.io/badge/spring%20boot-4-60a5fa?style=for-the-badge&logo=springboot&logoColor=DBEAFE&labelColor=172033)
![Repository size](https://img.shields.io/github/repo-size/Lytheriia/ss14-discord-bridge?color=93c5fd&label=SIZE&logo=files&style=for-the-badge&logoColor=DBEAFE&labelColor=172033)

</div>

<div align="center">
    <h2>• overview •</h2>
    <h3></h3>
</div>

<div align="center">
  Discord bot and internal API that connects <a href="https://spacestation14.com/">Space Station 14</a>
  game servers with their Discord communities.
  <br>
  Built with <a href="https://github.com/discord-jda/JDA">JDA</a>, Spring Boot, PostgreSQL and Redis.
</div>
<br>

<details>
<summary>Features</summary>

- **Account linking**: players get a one-time code in the game lobby and link their Discord account
  through a button and modal.
- **Role sync**: verified, server booster and Boosty roles are tracked in the database and exposed to the game server.
- **Boosty reconciliation**: an hourly job compares active Boosty subscriptions with Discord roles
  and fixes any differences, including changes made while the bot was offline.
- **Antag tokens**: an hourly job gives active Boosty subscribers tokens for their highest rank, at most once in 7 days,
  up to `BOOSTY_WEEKLY_TOKEN_CAP`; at the cap the week is not used up.
  Allowed roles can also give tokens with `/tokens give`.
  Game servers spend tokens through the internal API; a purchase that is not granted returns its token.
  An hourly job refunds purchases still waiting after `BOOSTY_PURCHASE_TTL_MINUTES`.
- **Server presence**: the bot status shows online players, round time and map, refreshed every 30 seconds.
- **Round statistics**: player count snapshots are stored and rendered as charts with `/stats`.
  A daily job can delete old snapshots if `STATS_RETENTION_DAYS` is set.
- **Command permissions**: admins can grant specific commands to roles.

</details>

<details>
<summary>Slash commands</summary>

| Command             | Access          | Description                                       |
| ------------------- | --------------- | ------------------------------------------------- |
| `/stats`            | Allowed roles   | Round statistics                                  |
| `/tokens give`      | Allowed roles   | Give antag tokens to a member                     |
| `/link-setup`       | Administrator   | Post the account linking message to a channel     |
| `/addpermission`    | Administrator   | Allow a role to use a command                     |
| `/removepermission` | Administrator   | Revoke a role's access to a command               |
| `/listpermissions`  | Administrator   | Show which roles can use a command                |
| `/syncdbroles`      | Maintainer      | Sync Discord roles with the database              |

</details>

<details>
<summary>Internal API</summary>

Every request must carry the `X-Internal-Api-Key` header (otherwise `401`).
If `API_ALLOWED_IPS` is set, only those addresses are accepted (otherwise `403`).

| Method | Path                                            | Description                                                                                                                |
| ------ | ----------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------- |
| `POST` | `/api/link/codes`                               | Register a linking code: `204`, `400`, `409 ALREADY_LINKED` / `CODE_COLLISION`, `503 STORAGE_UNAVAILABLE`                  |
| `GET`  | `/api/link/{userId}`                            | Link status of an SS14 user                                                                                                |
| `GET`  | `/api/boosty/{user}`                            | Boosty status of an SS14 user: `200`, `404` without a subscription                                                         |
| `GET`  | `/api/tokens/{userId}`                          | Token balance and active purchase: `200`, `404 NOT_LINKED`                                                                 |
| `POST` | `/api/tokens/{userId}/purchases`                | Buy with one token: `204`, `400`, `404 NOT_LINKED`, `409 INSUFFICIENT_TOKENS` / `ACTIVE_PURCHASE` / `PURCHASE_ID_CONFLICT` |
| `POST` | `/api/tokens/purchases/{id}/grant`              | Mark a purchase as granted: `204`, `404`, `409 ALREADY_FINISHED`                                                           |
| `POST` | `/api/tokens/purchases/{id}/refund`             | Return the token of a purchase: `204`, `404`, `409 ALREADY_FINISHED`                                                       |
| `POST` | `/api/tokens/servers/{serverId}/refund-pending` | Refund all waiting purchases of a server: `200`                                                                            |
| `GET`  | `/actuator/health`                              | Health check (no key required)                                                                                             |

</details>

<div align="center">
    <h2>• building •</h2>
    <h3></h3>
</div>

<details>
<summary>Build dependencies</summary>

- Git
- JDK 21
- Docker with Compose

Maven is not required: the repository ships the Maven wrapper (`./mvnw`).

</details>

<details>
<summary>Quick start (Docker)</summary>

1. Clone this repository
2. Copy `.env.example` to `.env` and fill in the values
3. Run `docker compose up -d --build`
4. Check `http://localhost:<API_PORT>/actuator/health`

</details>

<details>
<summary>Local development</summary>

1. Start the databases: `docker compose up -d postgres redis`
2. Run the bot with the variables from `.env`:
   - **VS Code**: open the folder, install the recommended extensions and press `F5` (**Bot (local)**).
   - **Terminal**: `set -a; . ./.env; set +a; SPRING_DATA_REDIS_HOST=localhost ./mvnw spring-boot:run`

| Command                      | Action                  |
| ---------------------------- | ----------------------- |
| `./mvnw -q compile`          | Compile                 |
| `./mvnw test`                | Run tests; needs Docker |
| `./mvnw package -DskipTests` | Build the jar           |

Database schema changes go through Flyway migrations in `src/main/resources/db/migration`.
Never edit a migration that has already been applied; add a new `V<n>__*.sql` instead.

</details>

<details>
<summary>Configuration</summary>

All settings are read from environment variables (see `.env.example`).

| Variable                      | Description                                                           |
| ----------------------------- | --------------------------------------------------------------------- |
| `DISCORD_TOKEN`               | Bot token                                                             |
| `DISCORD_GUILD_ID`            | The only guild the bot stays in                                       |
| `DISCORD_MAINTAINER_ID`       | Discord user ID allowed to run maintainer-only commands               |
| `DISCORD_VERIFIED_ROLE_ID`    | ID of the role given to verified members                              |
| `DISCORD_BOOSTER_ROLE_ID`     | ID of the server booster role                                         |
| `DISCORD_BOOSTY_ROLES`        | Boosty rank role IDs, comma-separated, lowest rank first; append only |
| `BOOSTY_WEEKLY_TOKENS`        | Weekly tokens per Boosty rank, comma-separated, lowest rank first     |
| `BOOSTY_WEEKLY_TOKEN_CAP`     | Balance where weekly grants stop, default 4                           |
| `BOOSTY_PURCHASE_TTL_MINUTES` | Minutes until a waiting purchase is refunded, default 60              |
| `STATS_RETENTION_DAYS`        | Days to keep round snapshots, at least 90; empty keeps them forever   |
| `INTERNAL_API_KEY`            | Shared secret for the internal API                                    |
| `API_PORT`                    | API port                                                              |
| `API_BIND_HOST`               | `127.0.0.1` if the game server is on the same machine, else `0.0.0.0` |
| `API_ALLOWED_IPS`             | Comma-separated IPs allowed to call the API; required with `0.0.0.0`  |
| `SS14_SERVER_IP`              | `host:port` of the game server, used for the `/status` presence       |
| `POSTGRES_DB`                 | Database name                                                         |
| `POSTGRES_USER`               | Database user                                                         |
| `POSTGRES_PASSWORD`           | Database password                                                     |
| `POSTGRES_PORT`               | Postgres port exposed on `127.0.0.1`                                  |
| `POSTGRES_DATA_DIR`           | Required. Host directory with the Postgres data                       |
| `REDIS_PORT`                  | Redis port exposed on `127.0.0.1`                                     |

</details>

<details>
<summary>Moving the database from ~/erida-bot</summary>

`POSTGRES_DATA_DIR` no longer has a default, so Compose stops with an error until it is set.
If your data is still in `~/erida-bot`, back it up and move it:

1. Stop the services: `docker compose stop backend postgres`
2. Create the parent directory: `sudo mkdir -p /srv/ss14-discord-bridge`
3. Move the data: `sudo mv ~/erida-bot /srv/ss14-discord-bridge/postgres`
4. Set `POSTGRES_DATA_DIR=/srv/ss14-discord-bridge/postgres` in `.env`
5. Start the services: `docker compose up -d`

Use `sudo rsync -a` instead of `mv` when moving to another disk, so the file owner is kept.
Check that the existing data is there before deleting any old copy.

</details>

<div align="center">
    <h2>• license •</h2>
    <h3></h3>
</div>

<div align="center">
  All code in this repository is released under the <a href="/LICENSE">MIT</a> license.
</div>
<br>

<div align="center">

![License](https://img.shields.io/badge/license-MIT-60a5fa?style=for-the-badge&labelColor=172033)

</div>
