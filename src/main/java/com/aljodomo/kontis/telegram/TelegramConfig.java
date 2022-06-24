package com.aljodomo.kontis.telegram;

import com.pengrad.telegrambot.TelegramBot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Aljoscha Domonell
 */
@Configuration
public class TelegramConfig {

    private final TelegramProps telegramProps;

    @Autowired
    public TelegramConfig(TelegramProps telegramProps) {
        this.telegramProps = telegramProps;
    }

    @Bean
    public TelegramBot telegramBot() {
        return new TelegramBot(telegramProps.getApiKey());
    }

}
