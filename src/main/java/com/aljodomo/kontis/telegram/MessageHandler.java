package com.aljodomo.kontis.telegram;

import java.time.ZonedDateTime;

/**
 * @author Aljoscha Domonell
 */
public interface MessageHandler {
    void handleMessage(String update, ZonedDateTime now);
}
