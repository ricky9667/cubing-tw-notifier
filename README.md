# Cubing TW Notifier

A notifier bot that checks for updates from [Cubing TW](https://cubing-tw.net/event/) website.

[Join Telegram Channel](https://t.me/cubing_tw_notifier)

## How to run

1. Clone the repository

```bash
git clone git@github.com:ricky9667/cubing-tw-notifier.git
```

2. Add environment variables to `.env` file

```text
SPRING_DATASOURCE_USERNAME=...
SPRING_DATASOURCE_PASSWORD=...
DB_NAME=cubing_events
TELEGRAM_BOT_TOKEN=...
TELEGRAM_CHAT_ID=...
NOTIFICATION_START_ZONE=Asia/Taipei
```

3. Run the application with Docker Compose

```bash
# Run locally
docker compose up -d

# Run in production
docker compose --profile prod up -d
```

4. Run the command below to restart the application and do step 3 to restart 

```bash
# Stop locally
docker compose down -v

# Stop in production
docker compose --profile prod down -v
```
