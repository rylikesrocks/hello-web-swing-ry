package com.example.hellowebswing;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * GameLoopService manages the main game loop running at 60 FPS.
 * Responsible for updating game state at a constant rate.
 * Updates are performed independently of player input.
 */
@Service
public class GameLoopService {
    
    @Autowired
    private GameService gameService;
    
    /**
     * Main game loop that runs at 60 FPS (every 16.67 milliseconds).
     * Updates enemy positions and other game logic.
     */
    @Scheduled(fixedRate = 17) // Approximately 60 FPS
    public void gameLoop() {
        gameService.updateEnemies();
    }
}
