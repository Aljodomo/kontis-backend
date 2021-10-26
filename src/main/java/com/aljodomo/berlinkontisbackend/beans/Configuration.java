package com.aljodomo.berlinkontisbackend.beans;

import com.pengrad.telegrambot.TelegramBot;
import org.springframework.context.annotation.Bean;

/**
 * @author Aljoscha Domonell
 */
@org.springframework.context.annotation.Configuration
public class Configuration {
    private final static String apiToken = "1837251785:AAFOLZx9hdpJQAp3rrfYFZaOYlIH2J4-Zoo";

    @Bean
    public TelegramBot telegramBot() {
        return new TelegramBot(apiToken);
    }

}
