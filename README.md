# kontis-backend

Backend service for the kontis / FreiFahrtenBerlin app.

### Env

- GOOGLE_APPLICATION_CREDENTIALS
  - Location of the firebase adminsdk json

### Props

- telegram.api-key
  - Telegram API key to receive massages
- gtfs.location
  - Location of the GTFS files
  - Can be downloaded from (http://vbb.de/vbbgtfs) for Berlin
- gtfs.filter.agencyWhitelist
  - List of agency ids that are used for massage processing
- gtfs.filter.routeShortNameRegEx
  - RegEx to filter routes that are used for massage processing

### Profiles

- dev
  - Uses a in memory database for reports
- test
  - in memory database for reports
  - No telegram update listener
