package com.example.hellowebswing;

import java.util.Random;

/**
 * Enemy represents an enemy character in the game.
 * Contains enemy attributes: health, damage, position, and movement pattern logic.
 */
public class Enemy {
    
    private int health;
    private int maxHealth;
    private int damage;
    private int x;
    private int y;
    private static final Random random = new Random();
    
    // Game boundaries
    private static final int MAX_X = 40;
    private static final int MAX_Y = 30;
    
    // Movement pattern
    private boolean movingOnXAxis;  // true = moving on X axis, false = moving on Y axis
    private int directionX;         // -1, 0, or 1
    private int directionY;         // -1, 0, or 1
    private int spacesMovedInDirection;
    private int spacesUntilSwitch;  // random interval before switching axes
    
    /**
     * Create an enemy at the specified position.
     */
    public Enemy(int x, int y, int health, int damage) {
        this.x = x;
        this.y = y;
        this.maxHealth = health;
        this.health = health;
        this.damage = damage;
        
        // Initialize movement pattern
        this.movingOnXAxis = random.nextBoolean();
        this.spacesMovedInDirection = 0;
        this.spacesUntilSwitch = random.nextInt(6) + 5; // Random 5-10
        
        // Pick initial direction
        if (movingOnXAxis) {
            directionX = random.nextBoolean() ? 1 : -1;
            directionY = 0;
        } else {
            directionX = 0;
            directionY = random.nextBoolean() ? 1 : -1;
        }
    }
    
    // Getters
    public int getHealth() {
        return health;
    }
    
    public int getMaxHealth() {
        return maxHealth;
    }
    
    public int getDamage() {
        return damage;
    }
    
    public int getX() {
        return x;
    }
    
    public int getY() {
        return y;
    }
    
    /**
     * Apply damage to the enemy.
     */
    public void takeDamage(int damage) {
        this.health = Math.max(0, this.health - damage);
    }
    
    /**
     * Update enemy movement and handle collisions with player.
     * Returns true if the enemy damaged the player, false otherwise.
     */
    public boolean update(GameModel gameModel) {
        boolean damagedPlayer = false;
        
        // Try to move in the current direction
        int newX = x + directionX;
        int newY = y + directionY;
        
        // Check bounds and switch direction if hitting edge
        if (newX < 0 || newX >= MAX_X) {
            directionX *= -1;
            newX = x + directionX;
        }
        if (newY < 0 || newY >= MAX_Y) {
            directionY *= -1;
            newY = y + directionY;
        }
        
        // Check if moving into player
        if (newX == gameModel.getPlayerX() && newY == gameModel.getPlayerY()) {
            gameModel.damagePlayer(damage);
            damagedPlayer = true;
        } else {
            // Move to the new position
            x = newX;
            y = newY;
        }
        
        // Increment spaces moved
        spacesMovedInDirection++;
        
        // Check if it's time to switch axes
        if (spacesMovedInDirection >= spacesUntilSwitch) {
            switchAxes();
        }
        
        return damagedPlayer;
    }
    
    /**
     * Switch the movement axis and pick a new direction and interval.
     */
    private void switchAxes() {
        movingOnXAxis = !movingOnXAxis;
        spacesMovedInDirection = 0;
        spacesUntilSwitch = random.nextInt(6) + 5; // Random 5-10
        
        if (movingOnXAxis) {
            directionX = random.nextBoolean() ? 1 : -1;
            directionY = 0;
        } else {
            directionX = 0;
            directionY = random.nextBoolean() ? 1 : -1;
        }
    }
}
