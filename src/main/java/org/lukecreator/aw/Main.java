package org.lukecreator.aw;

import org.lukecreator.aw.discord.AbilityWarsBot;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Objects;

@SpringBootApplication
public class Main {
    /**
     * If debug is enabled by having the `AW_DEBUG` environment variable set to 1.
     */
    public static final boolean DEBUG = Objects.equals(System.getenv("AW_DEBUG"), "1");

    public static void main(String[] args) {

        try {
            // initializes the SQLite database
            AWDatabase.init();
        } catch (Exception e) {
            System.err.println("Failed to initialize database.\n\n" + e);
        }

        System.out.println("Starting Spring application...");
        SpringApplication.run(Main.class, args);

        System.out.println("Starting Discord bot...");
        new AbilityWarsBot(DEBUG);
    }
}
