<div align="center">
  <img src="assets/icon.png" alt="Cubing TW Notifier logo" width="128">
  <h1>Cubing TW Notifier</h1>
  <p>
    <a href="https://github.com/ricky9667/cubing-tw-notifier/releases"><img src="https://img.shields.io/github/v/release/ricky9667/cubing-tw-notifier?display_name=tag&amp;style=for-the-badge" alt="Release"></a>
    <a href="https://github.com/ricky9667/cubing-tw-notifier/blob/update-readme/LICENSE"><img src="https://img.shields.io/github/license/ricky9667/cubing-tw-notifier?style=for-the-badge" alt="GitHub License"></a>
  </p>
  <p>English | <a href="README.zh-TW.md">繁體中文</a></p>
</div>

## About

A notifier bot that checks for updates from [Cubing TW](https://cubing-tw.net/event/) website.

- [Join Telegram Channel](https://t.me/cubing_tw_notifier): Get notifications directly from a Telegram Channel.
- [Setup Discord Bot](https://discord.com/oauth2/authorize?client_id=1483280239634288700&permissions=3072&integration_type=0&scope=bot+applications.commands): Add the bot to your server, and run `/subscribe` in the channel you want to receive notifications.
- [Official Website](https://apps.ricky-hu.com/cubing-tw-notifier/)

## Features

The notifier bot sends 3 kinds of messages:

- New competition: The bot crawls events from Cubing TW every hour. When a new event is discovered, the bot will save it to its database and send a "New Competition Announced" message.
- Registration open: The bot also crawls the registration time of the events. It checks every 5 minutes whether there is a registration going to open 5 minutes later. It also checks for reopened registrations.
- Competition arrival: The bot notifies if there is a competition starting tomorrow at 8 AM.

## Development

Install Java 17 and Docker with Compose. From the repository root, create a `.env` file for the local PostgreSQL container:

```dotenv
SPRING_DATASOURCE_USERNAME=cubing
SPRING_DATASOURCE_PASSWORD=local-password
DB_NAME=cubing_events
```

Start the app with `./gradlew bootRun`. Spring Boot starts the PostgreSQL service in `compose.yml` and connects to it. The app crawls Cubing TW on startup. Without bot credentials, it does not send notifications. To connect test bots, export `DISCORD_BOT_TOKEN` and/or both `TELEGRAM_BOT_TOKEN` and `TELEGRAM_CHAT_ID` before starting the app. A startup crawl may send alerts, so use test destinations.

Run the automated tests with `./gradlew test`. They use an in-memory H2 database and need neither Docker nor bot credentials.

For a release, tag the commit `vX.Y.Z` and push the tag. The release workflow passes that tag to Gradle, which embeds `X.Y.Z` in the JAR. Publish a GitHub Release separately to add it to the releases page. Local builds show `dev` unless you pass `-PreleaseVersion=vX.Y.Z`.
