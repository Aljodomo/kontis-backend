package com.aljodomo.kontis.beans;

import com.pengrad.telegrambot.TelegramBot;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

/**
 * @author Aljoscha Domonell
 */
@org.springframework.context.annotation.Configuration
public class Configuration {

    @Value("${telegram.bot.api-key}")
    private String apiKey;

    @Bean
    public TelegramBot telegramBot() {
        return new TelegramBot(apiKey);
    }

}
