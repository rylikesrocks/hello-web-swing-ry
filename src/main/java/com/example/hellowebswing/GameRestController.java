package com.example.hellowebswing;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * GameRestController handles HTTP requests (player input) for the game.
 * Coordinates between user input and the model, returning the updated state.
 * Only concerned with handling input - game logic is in GameModel.
 */
@RestController
@RequestMapping("/api/game")
@CrossOrigin(origins = "*")
public class GameRestController {
    
    @Autowired
    private GameService gameService;
    
    /**
     * Get the current game state (for the view to render).
     */
    @GetMapping("/state")
    public GameModel getGameState() {
        return gameService.getGameState();
    }
    
    /**
     * Get the list of enemies.
     */
    @GetMapping("/enemies")
    public List<Enemy> getEnemies() {
        return gameService.getEnemies();
    }
    
    /**
     * Handle player movement input.
     * Player input is processed by the controller and delegated to the model.
     * Enemy movement is handled by the game loop independently.
     */
    @PostMapping("/move")
    public GameModel movePlayer(@RequestParam String direction) {
        gameService.getGameState().movePlayer(direction);
        return gameService.getGameState();
    }
    
    /**
     * Handle damage input.
     * Player input (or game events) trigger damage in the model.
     */
    @PostMapping("/damage")
    public GameModel damagePlayer(@RequestParam int amount) {
        gameService.getGameState().takeDamage(amount);
        return gameService.getGameState();
    }
    
    /**
     * Handle heal input.
     * Player input (or game events) trigger healing in the model.
     */
    @PostMapping("/heal")
    public GameModel healPlayer(@RequestParam int amount) {
        gameService.getGameState().heal(amount);
        return gameService.getGameState();
    }
    
    /**
     * Reset the game.
     */
    @PostMapping("/reset")
    public GameModel resetGame() {
        gameService.resetGame();
        return gameService.getGameState();
    }
}
