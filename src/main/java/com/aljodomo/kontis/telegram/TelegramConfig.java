package com.aljodomo.kontis.telegram;

import com.pengrad.telegrambot.TelegramBot;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * @author Aljoscha Domonell
 */
@Configuration
public class TelegramConfig {

    @Bean
    @Profile("!test")
    public TelegramBot telegramBot(TelegramProps telegramProps) {
        return new TelegramBot(telegramProps.getApiKey());
    }

}
