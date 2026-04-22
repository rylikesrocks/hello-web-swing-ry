package com.example.hellowebswing;

import org.springframework.stereotype.Service;
import java.util.List;

/**
 * GameService manages the game state presentation.
 * Only concerned with providing access to the model for the controller and rendering.
 * Does not contain game logic - that belongs in GameModel.
 */
@Service
public class GameService {
    
    private GameModel gameModel;
    
    public GameService() {
        this.gameModel = new GameModel();
    }
    
    /**
     * Get the current game state for presentation.
     */
    public GameModel getGameState() {
        return gameModel;
    }
    
    /**
     * Get the list of enemies for rendering.
     */
    public List<Enemy> getEnemies() {
        return gameModel.getEnemies();
    }
    
    /**
     * Update all enemies in the game.
     */
    public void updateEnemies() {
        gameModel.updateEnemies();
    }
    
    /**
     * Reset the game to its initial state.
     */
    public void resetGame() {
        this.gameModel = new GameModel();
    }
}
