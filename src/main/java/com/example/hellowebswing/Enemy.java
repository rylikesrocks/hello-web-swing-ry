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
    private double x;  // Now uses decimal positions for smooth movement
    private double y;
    private static final Random random = new Random();
    
    // Game boundaries
    private static final int MAX_X = 20;
    private static final int MAX_Y = 15;
    
    // Movement pattern - one of: UP, DOWN, LEFT, RIGHT
    public enum Direction {
        UP(0, -1),
        DOWN(0, 1),
        LEFT(-1, 0),
        RIGHT(1, 0);
        
        public final int dx;
        public final int dy;
        
        Direction(int dx, int dy) {
            this.dx = dx;
            this.dy = dy;
        }
    }
    
    private Direction currentDirection;
    private int spacesMovedInDirection;
    private int spacesUntilSwitch;  // random interval (5-12) before switching direction
    private static final double MOVEMENT_SPEED = 0.1;  // Move 0.1 tiles per tick for smooth gliding
    
    // Knockback mechanics
    private int knockbackDistance = 0;  // Distance remaining to knockback
    private Direction knockbackDirection = null;  // Direction of knockback
    private static final int KNOCKBACK_DISTANCE = 2;  // Knockback 2 squares when hit
    
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
        this.currentDirection = Direction.values()[random.nextInt(Direction.values().length)];
        this.spacesMovedInDirection = 0;
        this.spacesUntilSwitch = random.nextInt(8) + 5; // Random 5-12
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
        return (int) Math.round(x);
    }
    
    public int getY() {
        return (int) Math.round(y);
    }
    
    public double getXRaw() {
        return x;
    }
    
    public double getYRaw() {
        return y;
    }
    
    /**
     * Apply damage to the enemy and trigger knockback.
     * Enemy moves back 2 squares in the opposite direction from the attacker.
     */
    public void takeDamage(int damage, Direction attackDirection) {
        this.health = Math.max(0, this.health - damage);
        // Apply knockback in opposite direction
        if (attackDirection == Direction.LEFT) {
            this.knockbackDirection = Direction.RIGHT;
        } else if (attackDirection == Direction.RIGHT) {
            this.knockbackDirection = Direction.LEFT;
        } else if (attackDirection == Direction.UP) {
            this.knockbackDirection = Direction.DOWN;
        } else if (attackDirection == Direction.DOWN) {
            this.knockbackDirection = Direction.UP;
        }
        this.knockbackDistance = KNOCKBACK_DISTANCE;
    }
    
    /**
     * Apply damage to the enemy without knockback (used for old takeDamage calls).
     */
    public void takeDamage(int damage) {
        this.health = Math.max(0, this.health - damage);
    }
    
    /**
     * Update enemy movement and handle collisions with player.
     * Returns true if the enemy damaged the player, false otherwise.
     * Enemies move smoothly at MOVEMENT_SPEED per tick for continuous gliding.
     */
    public boolean update(GameModel gameModel) {
        // This method is kept for backward compatibility with GameLoopService
        return updateWithPlayerPosition(gameModel.getPlayerX(), gameModel.getPlayerY(), null);
    }
    
    /**
     * Update enemy movement with specific player position and room.
     * Returns true if the enemy damaged the player, false otherwise.
     * Moves smoothly using decimal positions with proper boundary handling.
     * Enemies move in a direction for a random interval (5-12 tiles), then pick a new random direction.
     */
    public boolean updateWithPlayerPosition(int playerX, int playerY, Room room) {
        boolean damagedPlayer = false;
        
        // Handle knockback if active
        if (knockbackDistance > 0 && knockbackDirection != null) {
            double newX = x + (knockbackDirection.dx * MOVEMENT_SPEED);
            double newY = y + (knockbackDirection.dy * MOVEMENT_SPEED);

            double maxX = (room != null) ? Room.ROOM_WIDTH : MAX_X;
            double maxY = (room != null) ? Room.ROOM_HEIGHT : MAX_Y;

            newX = Math.max(0, Math.min(newX, maxX - 0.01));
            newY = Math.max(0, Math.min(newY, maxY - 0.01));

            x = newX;
            y = newY;
            knockbackDistance -= MOVEMENT_SPEED;
            return false;
        }

        // Calculate new position based on current direction (fractional movement)
        double newX = x + (currentDirection.dx * MOVEMENT_SPEED);
        double newY = y + (currentDirection.dy * MOVEMENT_SPEED);
        
        // Get boundaries (use room if provided, otherwise use class constants)
        double maxX = (room != null) ? Room.ROOM_WIDTH : MAX_X;
        double maxY = (room != null) ? Room.ROOM_HEIGHT : MAX_Y;
        
        // Check bounds and bounce off edges - keep in bounds but allow counter to continue
        boolean hitBoundary = false;
        if (newX < 0) {
            newX = 0;
            // Only bounce if actually moving left
            if (currentDirection == Direction.LEFT) {
                currentDirection = Direction.RIGHT;
                hitBoundary = true;
            }
        } else if (newX >= maxX) {
            newX = maxX - 0.01;  // Stay just inside the boundary
            // Only bounce if actually moving right
            if (currentDirection == Direction.RIGHT) {
                currentDirection = Direction.LEFT;
                hitBoundary = true;
            } else if (newX <= maxX) {
                newX = maxX + 0.01; // Stay just inside the boundary
            }
        }
        
        if (newY < 0) {
            newY = 0;
            // Only bounce if actually moving up
            if (currentDirection == Direction.UP) {
                currentDirection = Direction.DOWN;
                hitBoundary = true;
            }
        } else if (newY >= maxY) {
            newY = maxY - 0.01;  // Stay just inside the boundary
            // Only bounce if actually moving down
            if (currentDirection == Direction.DOWN) {
                currentDirection = Direction.UP;
                hitBoundary = true;
            }
        }
        
        // Check if current position is close to player (within 0.5 tiles)
        int currentTileX = (int) Math.round(newX);
        int currentTileY = (int) Math.round(newY);
        
        if (currentTileX == playerX && currentTileY == playerY) {
            damagedPlayer = true;
        } else {
            // Move to the new position
            x = newX;
            y = newY;
            
            // Only increment if not hitting a boundary - this way we don't break the counter on bounces
            if (!hitBoundary) {
                spacesMovedInDirection += MOVEMENT_SPEED;
            }
            
            // Check if it's time to switch to a random direction (after moving the specified number of tiles)
            if (spacesMovedInDirection >= spacesUntilSwitch) {
                chooseNewDirection();
            }
        }
        
        return damagedPlayer;
    }
    
    /**
     * Choose a new random direction and set the interval for the next switch.
     */
    private void chooseNewDirection() {
        currentDirection = Direction.values()[random.nextInt(Direction.values().length)];
        spacesMovedInDirection = 0;
        spacesUntilSwitch = random.nextInt(8) + 5; // Random 5-12
    }
}
