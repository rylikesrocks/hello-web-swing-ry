package com.example.hellowebswing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main Spring Boot application for the RPG Dungeon Crawler game.
 * The game is served as a web application accessible via the browser.
 * EnableScheduling allows the game loop to run at a fixed rate.
 */
@SpringBootApplication
@EnableScheduling
public class HelloWebSwingApplication {

    public static void main(String[] args) {
        SpringApplication.run(HelloWebSwingApplication.class, args);
    }
}
