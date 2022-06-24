package com.aljodomo.kontis.telegram;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;

/**
 * @author Aljoscha Domonell
 */
@Component
@Slf4j
public class TelegramUpdateHandler {

    private final MessageHandler messageHandler;

    @Autowired
    public TelegramUpdateHandler(TelegramBot telegramBot, MessageHandler messageHandler) {
        this.messageHandler = messageHandler;
        addListeners(telegramBot);
    }

    private void addListeners(TelegramBot telegramBot) {
        telegramBot.setUpdatesListener(updates -> {
            updates.forEach(this::handleUpdate);
            return UpdatesListener.CONFIRMED_UPDATES_ALL;
        });
    }

    private void handleUpdate(Update update) {
        log.info("Telegram update received");
        String message = update.message().text();
        if (message == null || message.isBlank()) {
            log.info("Update skipped because no message was present");
            return;
        }

        messageHandler.handleMessage(message, ZonedDateTime.now());
    }

}
