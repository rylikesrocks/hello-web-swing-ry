package com.example.hellowebswing;

import java.util.ArrayList;
import java.util.List;

/**
 * GameModel represents the game state and contains all game logic.
 * Stores information like player health, position, inventory, etc.
 * Handles all state changes and game mechanics.
 */
public class GameModel {
    
    private int playerHealth;
    private int playerMaxHealth;
    private int playerX;
    private int playerY;
    private List<Enemy> enemies;
    
    /**
     * Initialize the game model with default values.
     */
    public GameModel() {
        this.playerMaxHealth = 100;
        this.playerHealth = 100;
        this.playerX = 0;
        this.playerY = 0;
        this.enemies = new ArrayList<>();
        
        // Initialize enemies
        enemies.add(new Enemy(20, 10, 30, 10));
        enemies.add(new Enemy(15, 20, 30, 10));
    }
    
    // Getters
    public int getPlayerHealth() {
        return playerHealth;
    }
    
    public int getPlayerMaxHealth() {
        return playerMaxHealth;
    }
    
    public int getPlayerX() {
        return playerX;
    }
    
    public int getPlayerY() {
        return playerY;
    }
    
    /**
     * Apply damage to the player (game logic).
     */
    public void takeDamage(int damage) {
        this.playerHealth = Math.max(0, Math.min(playerHealth - damage, playerMaxHealth));
    }
    
    /**
     * Heal the player (game logic).
     */
    public void heal(int amount) {
        this.playerHealth = Math.max(0, Math.min(playerHealth + amount, playerMaxHealth));
    }
    
    /**
     * Move the player in a given direction (game logic).
     */
    public void movePlayer(String direction) {
        switch (direction.toLowerCase()) {
            case "up":
                playerY--;
                break;
            case "down":
                playerY++;
                break;
            case "left":
                playerX--;
                break;
            case "right":
                playerX++;
                break;
            default:
                break;
        }
    }
    
    /**
     * Get the list of enemies.
     */
    public List<Enemy> getEnemies() {
        return enemies;
    }
    
    /**
     * Check if an enemy is at the specified position (excluding a specific enemy).
     */
    public boolean isEnemyAt(int x, int y, Enemy exclude) {
        for (Enemy enemy : enemies) {
            if (enemy != exclude && enemy.getX() == x && enemy.getY() == y) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Damage the player from an enemy attack.
     */
    public void damagePlayer(int damage) {
        this.playerHealth = Math.max(0, playerHealth - damage);
    }
    
    /**
     * Update all enemies in the game.
     */
    public void updateEnemies() {
        for (Enemy enemy : enemies) {
            enemy.update(this);
        }
    }
}
