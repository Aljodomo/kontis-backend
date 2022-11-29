# kontis-backend

Backend service for the kontis / FreiFahrtenBerlin app.

## Env

| Name | Description |
| ---- | ---- |
| GOOGLE_APPLICATION_CREDENTIALS | Location of the firebase adminsdk json |

## Props

| Name | Description |
| ---- | ---- |
| telegram.api-key | Telegram API key to receive massages |
| gtfs.location | Location of the GTFS files. Can be downloaded from [here](http://vbb.de/vbbgtfs) for Berlin. |

## Profiles

| Name | Description |
| ---- | ---- |
| dev | Uses a in memory database for reports |
| cli | Creates a cli that accepts messages. For fast manuel testing. |
| test | Uses a in memory database for reports and no telegram update listener |

## Usage 

### Run with Spring Boot profiles
```
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```