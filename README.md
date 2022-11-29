# kontis-backend

Backend service for the kontis / FreiFahrtenBerlin app.

### Env

- GOOGLE_APPLICATION_CREDENTIALS
  - Location of the firebase adminsdk json

## Props

| Name | Description |
| ---- | ---- |
| telegram.api-key | Telegram API key to receive massages |
| gtfs.location | Location of the GTFS files. Can be downloaded from [here](http://vbb.de/vbbgtfs) for Berlin. |

## Profiles

### Profiles

- dev
  - Uses a in memory database for reports
- test
  - in memory database for reports
  - No telegram update listener
