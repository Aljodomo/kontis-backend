package com.aljodomo.kontis.utils;

import com.aljodomo.kontis.telegram.MessageHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.Scanner;

@Slf4j
@Profile("dev")
@Service
public class CLIAdapter {

    @Autowired
    public CLIAdapter(MessageHandler messageHandler) {
        new Thread(() -> {
            log.info("Starting CLI adapter thread");

            var scanner = new Scanner(System.in);
            String line = "";
            while(!line.equals("exit")) {
                try {
                    System.out.println("Enter message (type 'exit' to close) > ");
                    line = scanner.nextLine();
                    System.out.println("Input : " + line);
                    messageHandler.handleMessage(line, ZonedDateTime.now());
                } catch (Exception e) {
                    log.error("Error", e);
                }
            }
        }).start();
    }

}
