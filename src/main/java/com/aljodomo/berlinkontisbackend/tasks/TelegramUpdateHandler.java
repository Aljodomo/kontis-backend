package com.aljodomo.berlinkontisbackend.tasks;

import com.aljodomo.berlinkontisbackend.db.InMemoryDB;
import com.aljodomo.berlinkontisbackend.domain.Report;
import com.aljodomo.berlinkontisbackend.domain.Stop;
import com.aljodomo.berlinkontisbackend.tagger.StopMatcher;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;

/**
 * @author Aljoscha Domonell
 */
@Component
public class TelegramUpdateHandler {

    final StopMatcher stopMatcher;
    final InMemoryDB inMemoryDB;


    @Autowired
    public TelegramUpdateHandler(TelegramBot telegramBot, StopMatcher stopMatcher, InMemoryDB inMemoryDB) {
        this.inMemoryDB = inMemoryDB;
        telegramBot.setUpdatesListener(updates -> {
            updates.forEach(this::handleUpdate);
            return UpdatesListener.CONFIRMED_UPDATES_ALL;
        });
        this.stopMatcher = stopMatcher;
    }

    private void handleUpdate(Update update) {
        System.out.println("Telegram update received");
        Optional<Stop> s = this.stopMatcher.match(update.message().text());
        if(s.isPresent()) {
            System.out.println("Report created: " + s.get());
            this.inMemoryDB.addReport(new Report(s.get(), Instant.now()));
        }
    }

}
