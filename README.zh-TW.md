# 台灣魔術方塊賽事通知機器人 (Cubing TW Notifier)

[![Release](https://img.shields.io/github/v/release/ricky9667/cubing-tw-notifier?display_name=tag&style=for-the-badge)](https://github.com/ricky9667/cubing-tw-notifier/releases) [![GitHub License](https://img.shields.io/github/license/ricky9667/cubing-tw-notifier?style=for-the-badge)](https://github.com/ricky9667/cubing-tw-notifier/blob/update-readme/LICENSE)

[English](README.md) | 繁體中文

這是一個追蹤 [Cubing TW](https://cubing-tw.net/event/) 網站更新並發送通知的機器人。

- [加入 Telegram 頻道](https://t.me/cubing_tw_notifier)：直接在 Telegram 頻道接收通知。
- [設定 Discord 機器人](https://discord.com/oauth2/authorize?client_id=1483280239634288700&permissions=3072&integration_type=0&scope=bot+applications.commands)：將機器人加入伺服器，並在想接收通知的頻道執行 `/subscribe`。
- [官方網站](https://apps.ricky-hu.com/cubing-tw-notifier/)

## 功能

機器人會發送三種通知：

- 新比賽：機器人每小時爬取 Cubing TW 的比賽資訊。發現新比賽時，會將資料存入資料庫並發送通知。
- 報名開放：機器人也會爬取比賽的報名時間，每 5 分鐘檢查是否有報名將在 5 分鐘後開放，並追蹤重新開放的報名。
- 比賽即將開始：若有比賽將在隔天開始，機器人會在上午 8 點發送通知。

## 開發

安裝 Java 17 和 Docker Compose。在專案根目錄建立 `.env`，供本機 PostgreSQL 容器使用：

```dotenv
SPRING_DATASOURCE_USERNAME=cubing
SPRING_DATASOURCE_PASSWORD=local-password
DB_NAME=cubing_events
```

執行 `./gradlew bootRun` 啟動應用程式。Spring Boot 會啟動 `compose.yml` 中的 PostgreSQL 服務並連線。應用程式啟動時會爬取 Cubing TW。沒有機器人憑證時，應用程式不會發送通知。若要連接測試用機器人，請在啟動前設定環境變數 `DISCORD_BOT_TOKEN`，或同時設定 `TELEGRAM_BOT_TOKEN` 和 `TELEGRAM_CHAT_ID`。啟動時爬取網站可能會發送通知，請使用測試用的接收頻道。

執行 `./gradlew test` 跑自動化測試。測試使用記憶體內的 H2 資料庫，不需要 Docker 或機器人憑證。
