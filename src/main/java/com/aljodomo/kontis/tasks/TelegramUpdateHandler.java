package com.aljodomo.kontis.tasks;

import com.aljodomo.kontis.db.InMemoryDB;
import com.aljodomo.kontis.domain.Report;
import com.aljodomo.kontis.domain.SLD;
import com.aljodomo.kontis.nlp.SLDBuilder;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;

/**
 * @author Aljoscha Domonell
 */
@Component
@Slf4j
public class TelegramUpdateHandler {

    private final SLDBuilder sldBuilder;
    private final InMemoryDB inMemoryDB;

    @Autowired
    public TelegramUpdateHandler(TelegramBot telegramBot, SLDBuilder sldBuilder, InMemoryDB inMemoryDB) {
        this.inMemoryDB = inMemoryDB;
        telegramBot.setUpdatesListener(updates -> {
            updates.forEach(this::handleUpdate);
            return UpdatesListener.CONFIRMED_UPDATES_ALL;
        });
        this.sldBuilder = sldBuilder;
    }

    private void handleUpdate(Update update) {
        log.info("Telegram update received");
        String message = update.message().text();
        if (message == null || message.isBlank()) {
            log.info("Update skipped because no message was present");
            return;
        }
        Optional<SLD> s = sldBuilder.from(update.message().text());
        if (s.isPresent()) {
            Report report = new Report(message, s.get(), Instant.now());
            log.info("Report created: {}", report);
            inMemoryDB.addReport(report);
        }
    }

}
