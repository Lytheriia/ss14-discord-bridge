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
- **Server presence**: the bot status shows online players, round time and map, refreshed every 30 seconds.
- **Round statistics**: player count snapshots are stored and rendered as charts with `/stats`.
- **Command permissions**: admins can grant specific commands to roles.

</details>

<details>
<summary>Slash commands</summary>

| Command             | Access          | Description                                       |
| ------------------- | --------------- | ------------------------------------------------- |
| `/stats`            | Allowed roles   | Round statistics                                  |
| `/link-setup`       | Administrator   | Post the account linking message to a channel     |
| `/addpermission`    | Administrator   | Allow a role to use a command                     |
| `/removepermission` | Administrator   | Revoke a role's access to a command               |
| `/listpermissions`  | Administrator   | Show which roles can use a command                |
| `/syncdbroles`      | Developer       | Sync Discord roles with the database              |

</details>

<details>
<summary>Internal API</summary>

Every request must carry the `X-Internal-Api-Key` header. If `API_ALLOWED_IPS` is set, only those addresses are accepted.

| Method | Path                  | Description                                                  |
| ------ | --------------------- | ------------------------------------------------------------ |
| `POST` | `/api/link/codes`     | Register a linking code: `204`, `409 ALREADY_LINKED`, `503`  |
| `GET`  | `/api/link/{userId}`  | Link status of an SS14 user                                  |
| `GET`  | `/api/boosty/{user}`  | Boosty sponsor status of an SS14 user                        |
| `GET`  | `/actuator/health`    | Health check (no key required)                               |

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
| `./mvnw test`                | Run tests               |
| `./mvnw package -DskipTests` | Build the jar           |

Database schema changes go through Flyway migrations in `src/main/resources/db/migration`.
Never edit a migration that has already been applied; add a new `V<n>__*.sql` instead.

</details>

<details>
<summary>Configuration</summary>

All settings are read from environment variables (see `.env.example`).

| Variable            | Description                                                              |
| ------------------- | ------------------------------------------------------------------------ |
| `DISCORD_TOKEN`     | Bot token                                                                |
| `DISCORD_GUILD_ID`  | The only guild the bot stays in                                          |
| `INTERNAL_API_KEY`  | Shared secret for the internal API                                       |
| `API_PORT`          | API port                                                                 |
| `API_BIND_HOST`     | `127.0.0.1` if the game server is on the same machine, else `0.0.0.0`    |
| `API_ALLOWED_IPS`   | Comma-separated IPs allowed to call the API; required with `0.0.0.0`     |
| `SS14_SERVER_IP`    | `host:port` of the game server, used for the `/status` presence          |
| `POSTGRES_DB`       | Database name                                                            |
| `POSTGRES_USER`     | Database user                                                            |
| `POSTGRES_PASSWORD` | Database password                                                        |
| `POSTGRES_PORT`     | Postgres port exposed on `127.0.0.1`                                     |
| `POSTGRES_DATA_DIR` | Host directory with the Postgres data (default `~/erida-bot`)            |
| `REDIS_PORT`        | Redis port exposed on `127.0.0.1`                                        |

Role IDs (verified, booster, Boosty tiers) are set in `src/main/resources/application.yaml`.

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
