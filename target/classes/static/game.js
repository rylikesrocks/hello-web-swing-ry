// Game configuration
const GAME_API_URL = '/api/game';
const TILE_SIZE = 40;
const DUNGEON_WIDTH = 20;
const DUNGEON_HEIGHT = 15;
const TARGET_FPS = 60;
const FRAME_TIME = 1000 / TARGET_FPS; // ~16.67ms per frame
const INPUT_TICK_RATE = 10; // Process one buffered input every 10 frames (6 inputs/sec max)
const ENEMY_FETCH_RATE = 5; // Fetch enemies every 5 frames (~12 fps enemy update)

// Game state
let gameState = null;
let enemies = [];
let doors = [];
let canvas = null;
let ctx = null;
let lastFrameTime = 0;
let isPlayerDead = false;  // Track if player is currently dead

// Damage indicator state
let lastDamageTime = 0;
let previousInvulnerabilityTime = 0;  // Track previous invulnerability time to detect damage
let damageFlashDuration = 300;  // Flash for 300ms
let invulnerabilityFlashDuration = 3000;  // Show invulnerability for 3 seconds

// Enemy position interpolation
let previousEnemies = [];
let lastEnemyFetchTime = 0;
let enemyInterpolationFactor = 0; // 0 to 1, how far between previous and current positions

// Input buffering
let pressedKeys = new Set();
let inputTickCounter = 0;
let enemyTickCounter = 0;

// Sword mechanics
let lastDirection = 'right';  // Default sword direction
let swordSwingActive = false;  // Whether sword is currently swinging
let swordSwingStartTime = 0;  // When the swing started
const SWORD_SWING_DURATION = 300;  // Swing animation duration in ms
const SWORD_DAMAGE = 15;  // Damage dealt by sword (matches backend)
let lastAttackTime = 0;  // Track when the last attack was sent to prevent spam
const ATTACK_THROTTLE = 150;  // Minimum milliseconds between attack requests (must be < SWORD_SWING_DURATION)

// Request throttling
let pendingEnemyFetch = null;  // Track pending enemy/door fetch to avoid duplicate requests
let pendingMoveFetch = null;    // Track pending move request
let lastSuccessfulFetchTime = 0;

// Initialize the game when the page loads
window.addEventListener('DOMContentLoaded', () => {
    initializeGame();
    setupEventListeners();
    gameLoop();
});

/**
 * Initialize the game canvas and start the game loop
 */
function initializeGame() {
    canvas = document.getElementById('gameCanvas');
    ctx = canvas.getContext('2d');
    
    // Fetch initial game state, enemies, and doors
    Promise.all([
        fetch(`${GAME_API_URL}/state`).then(r => r.json()),
        fetch(`${GAME_API_URL}/enemies`).then(r => r.json()),
        fetch(`${GAME_API_URL}/doors`).then(r => r.json())
    ])
    .then(([state, enemyList, doorList]) => {
        gameState = state;
        enemies = enemyList;
        doors = doorList;
        lastDamageTime = Date.now();
        previousInvulnerabilityTime = 0;
        updateUI();
    })
    .catch(error => console.error('Error initializing game:', error));
    
    // Start the game loop
    gameLoop(0);
}

/**
 * Set up button and keyboard event listeners
 */
function setupEventListeners() {
    // Reset button (from controls area)
    document.getElementById('resetBtn').addEventListener('click', () => {
        resetGameState();
    });
    
    // Reset button from death screen
    document.getElementById('resetFromDeathBtn').addEventListener('click', () => {
        resetGameState();
    });
    
    // Keyboard movement - buffer key presses instead of sending requests immediately
    document.addEventListener('keydown', handleKeyDown);
    document.addEventListener('keyup', handleKeyUp);
}

/**
 * Reset the game state and hide death screen
 */
function resetGameState() {
    fetch(`${GAME_API_URL}/reset`, { method: 'POST' })
        .then(response => response.json())
        .then(data => {
            gameState = data;
            lastDamageTime = Date.now();
            previousInvulnerabilityTime = 0;
            isPlayerDead = false;
            hideDeathScreen();
            updateUI();
        })
        .catch(error => console.error('Error:', error));
}

/**
 * Show the death screen overlay
 */
function showDeathScreen() {
    const deathScreen = document.getElementById('deathScreen');
    deathScreen.classList.remove('hidden');
}

/**
 * Hide the death screen overlay
 */
function hideDeathScreen() {
    const deathScreen = document.getElementById('deathScreen');
    deathScreen.classList.add('hidden');
}

/**
 * Handle keyboard key down - buffer the key press
 * Disabled when player is dead
 */
function handleKeyDown(event) {
    if (isPlayerDead) {
        return;  // Don't allow input when dead
    }
    
    let direction = null;
    
    switch(event.key) {
        case 'ArrowUp':
        case 'w':
        case 'W':
            direction = 'up';
            event.preventDefault();
            break;
        case 'ArrowDown':
        case 's':
        case 'S':
            direction = 'down';
            event.preventDefault();
            break;
        case 'ArrowLeft':
        case 'a':
        case 'A':
            direction = 'left';
            event.preventDefault();
            break;
        case 'ArrowRight':
        case 'd':
        case 'D':
            direction = 'right';
            event.preventDefault();
            break;
        case ' ':
            // Space bar triggers sword attack
            attackWithSword();
            event.preventDefault();
            return;
        default:
            return;
    }
    
    if (direction) {
        pressedKeys.add(direction);
        lastDirection = direction;  // Track the last direction pressed
    }
}

/**
 * Handle keyboard key up - stop tracking this key
 */
function handleKeyUp(event) {
    let direction = null;
    
    switch(event.key) {
        case 'ArrowUp':
        case 'w':
        case 'W':
            direction = 'up';
            event.preventDefault();
            break;
        case 'ArrowDown':
        case 's':
        case 'S':
            direction = 'down';
            event.preventDefault();
            break;
        case 'ArrowLeft':
        case 'a':
        case 'A':
            direction = 'left';
            event.preventDefault();
            break;
        case 'ArrowRight':
        case 'd':
        case 'D':
            direction = 'right';
            event.preventDefault();
            break;
        default:
            return;
    }
    
    if (direction) {
        pressedKeys.delete(direction);
    }
}

/**
 * Send a move request to the server and fetch updated game state.
 * Uses deduplication to avoid duplicate in-flight requests.
 * Disabled when player is dead.
 */
function movePlayer(direction) {
    // Don't allow movement when dead
    if (isPlayerDead) {
        return;
    }
    
    // If a move request is already pending, don't send another
    if (pendingMoveFetch) {
        return;
    }
    
    pendingMoveFetch = fetch(`${GAME_API_URL}/move?direction=${direction}`, { method: 'POST' })
        .then(response => response.json())
        .then(data => {
            gameState = data;
            updateUI();
            pendingMoveFetch = null;
        })
        .catch(error => {
            console.error('Error:', error);
            pendingMoveFetch = null;
        });
}

/**
 * Send a sword attack request to the server in the last pressed direction.
 * Throttled to prevent spam - maximum one attack every ATTACK_THROTTLE milliseconds.
 * Disabled when player is dead.
 */
function attackWithSword() {
    // Don't allow attacks when dead
    if (isPlayerDead) {
        return;
    }
    
    const currentTime = Date.now();
    
    // Throttle attacks to prevent spamming
    if (currentTime - lastAttackTime < ATTACK_THROTTLE) {
        return;
    }
    
    // Only allow one swing animation at a time
    if (swordSwingActive) {
        return;
    }
    
    swordSwingActive = true;
    swordSwingStartTime = currentTime;
    lastAttackTime = currentTime;
    
    fetch(`${GAME_API_URL}/attack?direction=${lastDirection}`, { method: 'POST' })
        .then(response => response.json())
        .then(data => {
            gameState = data;
            updateUI();
        })
        .catch(error => console.error('Error attacking:', error));
}

/**
 * Fetch the latest game state and enemies from the server
 */
function fetchGameState() {
    Promise.all([
        fetch(`${GAME_API_URL}/state`).then(r => r.json()),
        fetch(`${GAME_API_URL}/enemies`).then(r => r.json())
    ])
    .then(([state, enemyList]) => {
        gameState = state;
        enemies = enemyList;
        updateUI();
    })
    .catch(error => console.error('Error fetching game state:', error));
}

/**
 * Update the UI elements (health bar, etc.)
 */
function updateUI() {
    if (!gameState) return;
    
    // Check if player has died
    if (gameState.playerHealth <= 0 && !isPlayerDead) {
        isPlayerDead = true;
        showDeathScreen();
    }
    
    // Check if player just took damage by detecting invulnerability time increasing
    const currentTime = Date.now();
    const currentInvulnTime = gameState.invulnerabilityTimeRemaining || 0;
    
    if (currentInvulnTime > previousInvulnerabilityTime && previousInvulnerabilityTime === 0) {
        // Invulnerability just started (player took damage)
        lastDamageTime = currentTime;
    }
    previousInvulnerabilityTime = currentInvulnTime;
    
    // Update health bar
    const healthPercent = (gameState.playerHealth / gameState.playerMaxHealth) * 100;
    document.getElementById('healthFill').style.width = healthPercent + '%';
    document.getElementById('healthText').textContent = 
        `${gameState.playerHealth}/${gameState.playerMaxHealth}`;
    
    // Change health bar color based on health
    const healthFill = document.getElementById('healthFill');
    if (healthPercent > 50) {
        healthFill.style.background = 'linear-gradient(to right, #4CAF50, #8BC34A)';
    } else if (healthPercent > 25) {
        healthFill.style.background = 'linear-gradient(to right, #FFC107, #FF9800)';
    } else {
        healthFill.style.background = 'linear-gradient(to right, #F44336, #E91E63)';
    }
}

/**
 * Main game loop - runs at 60 FPS with buffered input processing and periodic enemy fetching.
 * Processes one buffered input every INPUT_TICK_RATE frames to prevent request spam.
 * Fetches enemy positions every ENEMY_FETCH_RATE frames to show their movement.
 */
function gameLoop(currentTime) {
    // Calculate delta time
    if (lastFrameTime === 0) {
        lastFrameTime = currentTime;
    }
    
    const deltaTime = currentTime - lastFrameTime;
    lastFrameTime = currentTime;
    
    // Process buffered input at controlled rate
    inputTickCounter++;
    if (inputTickCounter >= INPUT_TICK_RATE && pressedKeys.size > 0) {
        inputTickCounter = 0;
        
        // Get the first direction from the buffer and send it
        const direction = pressedKeys.values().next().value;
        movePlayer(direction);
    }
    
    // Fetch enemies and doors periodically to show their movement and room state
    // Uses deduplication to avoid sending multiple requests if one is still pending
    enemyTickCounter++;
    if (enemyTickCounter >= ENEMY_FETCH_RATE) {
        enemyTickCounter = 0;
        
        // Only fetch if no fetch is currently pending
        if (!pendingEnemyFetch) {
            lastEnemyFetchTime = currentTime;
            enemyInterpolationFactor = 0;
            
            // Save previous enemy positions for interpolation
            previousEnemies = enemies.map(e => ({x: e.x, y: e.y}));
            
            // Fetch enemies and doors to update their positions
            // Use deduplication - if a fetch is already in flight, don't send another
            pendingEnemyFetch = Promise.all([
                fetch(`${GAME_API_URL}/enemies`).then(r => r.json()),
                fetch(`${GAME_API_URL}/doors`).then(r => r.json())
            ])
            .then(([enemyList, doorList]) => {
                enemies = enemyList;
                doors = doorList;
                lastSuccessfulFetchTime = currentTime;
                pendingEnemyFetch = null;
            })
            .catch(error => {
                console.error('Error fetching enemies/doors:', error);
                pendingEnemyFetch = null;
            });
        }
    } else {
        // Update interpolation factor between fetches
        const timeSinceLastFetch = currentTime - lastEnemyFetchTime;
        const fetchInterval = (1000 / 60) * ENEMY_FETCH_RATE;  // Time between fetches in ms
        enemyInterpolationFactor = Math.min(1, timeSinceLastFetch / fetchInterval);
    }
    
    // Render every frame
    render();
    
    // Schedule next frame
    requestAnimationFrame(gameLoop);
}

/**
 * Render the game scene
 */
function render() {
    if (!gameState || !ctx) return;
    
    // Check if sword swing animation is complete
    if (swordSwingActive) {
        const elapsedTime = Date.now() - swordSwingStartTime;
        if (elapsedTime > SWORD_SWING_DURATION) {
            swordSwingActive = false;
        }
    }
    
    // Clear the canvas
    ctx.fillStyle = '#1a1a1a';
    ctx.fillRect(0, 0, canvas.width, canvas.height);
    
    // Draw dungeon background
    drawDungeon();
    
    // Draw doors
    drawDoors();
    
    // Draw enemies
    drawEnemies();
    
    // Draw player
    drawPlayer();
    
    // Draw sword
    drawSword();
    
    // Draw grid (optional, for visualization)
    drawGrid();
}

/**
 * Draw the dungeon background
 */
function drawDungeon() {
    ctx.fillStyle = '#8D8C86';
    ctx.fillRect(0, 0, canvas.width, canvas.height);
    
    // Draw dungeon border
    ctx.strokeStyle = '#3a3a3a';
    ctx.lineWidth = 2;
    ctx.strokeRect(0, 0, canvas.width, canvas.height);
}

/**
 * Draw all doors in the current room
 */
function drawDoors() {
    if (!doors || doors.length === 0) return;
    
    for (let door of doors) {
        // Draw first tile of door
        const doorx1 = door.x1 * TILE_SIZE;
        const doory1 = door.y1 * TILE_SIZE;
        
        // Draw door with distinct color (cyan/bright blue)
        ctx.fillStyle = 'rgba(0, 200, 255, 0.6)';
        ctx.fillRect(doorx1, doory1, TILE_SIZE, TILE_SIZE);
        
        // Draw door border
        ctx.strokeStyle = '#00D4FF';
        ctx.lineWidth = 2;
        ctx.strokeRect(doorx1, doory1, TILE_SIZE, TILE_SIZE);
        
        // Draw second tile of door if different positions
        if (door.x1 !== door.x2 || door.y1 !== door.y2) {
            const doorx2 = door.x2 * TILE_SIZE;
            const doory2 = door.y2 * TILE_SIZE;
            
            ctx.fillStyle = 'rgba(0, 200, 255, 0.6)';
            ctx.fillRect(doorx2, doory2, TILE_SIZE, TILE_SIZE);
            
            ctx.strokeStyle = '#00D4FF';
            ctx.lineWidth = 2;
            ctx.strokeRect(doorx2, doory2, TILE_SIZE, TILE_SIZE);
        }
    }
}

/**
 * Draw the player character with damage/invulnerability effects
 */
function drawPlayer() {
    const playerScreenX = gameState.playerX * TILE_SIZE;
    const playerScreenY = gameState.playerY * TILE_SIZE;
    const currentTime = Date.now();
    const invulnTime = gameState.invulnerabilityTimeRemaining || 0;
    
    // Determine player color based on damage/invulnerability state
    let playerColor = '#4CAF50';  // Default green
    let borderColor = '#8BC34A';
    
    if (invulnTime > 0) {
        // Flash between red and green based on time since damage
        const timeSinceDamage = currentTime - lastDamageTime;
        
        // Flash red for first 300ms
        if (timeSinceDamage < damageFlashDuration) {
            playerColor = '#FF5252';  // Bright red
            borderColor = '#FF1744';
        } else {
            // Then flash green for the remaining invulnerability time
            const flashCycle = (timeSinceDamage - damageFlashDuration) % 200;  // 200ms flash cycle
            if (flashCycle < 100) {
                playerColor = '#4CAF50';  // Green
                borderColor = '#8BC34A';
            } else {
                playerColor = '#81C784';  // Lighter green
                borderColor = '#66BB6A';
            }
        }
    }
    
    // Draw player as a colored square
    ctx.fillStyle = playerColor;
    ctx.fillRect(playerScreenX, playerScreenY, TILE_SIZE, TILE_SIZE);
    
    // Draw player border
    ctx.strokeStyle = borderColor;
    ctx.lineWidth = 2;
    ctx.strokeRect(playerScreenX, playerScreenY, TILE_SIZE, TILE_SIZE);
}

/**
 * Draw all enemies with smooth interpolation between server updates
 */
function drawEnemies() {
    const currentTime = Date.now();
    const invulnTime = gameState.invulnerabilityTimeRemaining || 0;
    
    for (let i = 0; i < enemies.length; i++) {
        let enemy = enemies[i];
        let enemyScreenX = enemy.x * TILE_SIZE;
        let enemyScreenY = enemy.y * TILE_SIZE;
        
        // Apply interpolation if we have previous positions
        if (previousEnemies.length > i && previousEnemies[i] && enemyInterpolationFactor < 1) {
            const prevX = previousEnemies[i].x;
            const prevY = previousEnemies[i].y;
            
            // Linear interpolation between previous and current position
            enemyScreenX = (prevX + (enemy.x - prevX) * enemyInterpolationFactor) * TILE_SIZE;
            enemyScreenY = (prevY + (enemy.y - prevY) * enemyInterpolationFactor) * TILE_SIZE;
        }
        
        // Draw enemy as a red square
        let enemyColor = '#F44336';  // Default red
        let enemyBorder = '#FF9800';
        
        // Flash the square with invulnerability indicator
        if (invulnTime > 0) {
            const timeSinceDamage = currentTime - lastDamageTime;
            // Flash background color for the first 500ms to show invulnerability frames activated
            if (timeSinceDamage < 500) {
                const flashCycle = timeSinceDamage % 100;  // 100ms flash cycle
                if (flashCycle < 50) {
                    enemyColor = '#FFD700';  // Gold
                    enemyBorder = '#FFC107';
                } else {
                    enemyColor = '#F44336';  // Red
                    enemyBorder = '#FF9800';
                }
            }
        }
        
        ctx.fillStyle = enemyColor;
        ctx.fillRect(enemyScreenX, enemyScreenY, TILE_SIZE, TILE_SIZE);
        
        // Draw enemy border
        ctx.strokeStyle = enemyBorder;
        ctx.lineWidth = 2;
        ctx.strokeRect(enemyScreenX, enemyScreenY, TILE_SIZE, TILE_SIZE);
    }
}

/**
 * Draw the sword sprite next to the player
 * The sword swings in the direction the player last pressed
 * During a swing, it extends away from the player
 */
function drawSword() {
    if (!gameState) return;
    
    const playerScreenX = gameState.playerX * TILE_SIZE;
    const playerScreenY = gameState.playerY * TILE_SIZE;
    const swordSize = TILE_SIZE * 0.8;
    const swordCenter = TILE_SIZE / 2;
    
    // Calculate sword animation progress (0 to 1)
    let swingProgress = 0;
    if (swordSwingActive) {
        const elapsedTime = Date.now() - swordSwingStartTime;
        swingProgress = Math.min(1, elapsedTime / SWORD_SWING_DURATION);
    }
    
    // Calculate sword angle and distance based on direction and swing state
    let angle = 0;
    let distance = TILE_SIZE * 0.5;  // Default offset from player
    let baseAngle = 0;
    
    // Set base angle based on direction
    switch(lastDirection) {
        case 'right':
            baseAngle = 0;
            break;
        case 'left':
            baseAngle = Math.PI;
            break;
        case 'up':
            baseAngle = -Math.PI / 2;
            break;
        case 'down':
            baseAngle = Math.PI / 2;
            break;
    }
    
    // During swing, the sword rotates outward and back
    // Create an arc motion: start at -45 degrees, swing to +45 degrees, then back
    const swingArc = Math.PI / 4;  // 45 degrees in radians
    if (swingProgress < 0.5) {
        // First half: swing out
        angle = baseAngle - swingArc + (swingProgress * 2) * (swingArc * 2);
        distance = TILE_SIZE * 0.5 + (swingProgress * 2) * 0.2 * TILE_SIZE;
    } else {
        // Second half: swing back
        angle = baseAngle + swingArc - ((swingProgress - 0.5) * 2) * (swingArc * 2);
        distance = TILE_SIZE * 0.7 - ((swingProgress - 0.5) * 2) * 0.2 * TILE_SIZE;
    }
    
    if (!swordSwingActive) {
        // Not swinging - just show sword to the side at ready position
        angle = baseAngle;
        distance = TILE_SIZE * 0.5;
    }
    
    // Calculate sword position
    const swordX = playerScreenX + swordCenter + Math.cos(angle) * distance;
    const swordY = playerScreenY + swordCenter + Math.sin(angle) * distance;
    
    // Save canvas state for rotation
    ctx.save();
    ctx.translate(swordX, swordY);
    ctx.rotate(angle);
    
    // Draw sword blade (yellow/gold color)
    ctx.fillStyle = '#FFD700';
    ctx.shadowColor = 'rgba(255, 215, 0, 0.5)';
    ctx.shadowBlur = 10;
    ctx.fillRect(-TILE_SIZE * 0.15, -swordSize * 0.5, TILE_SIZE * 0.3, swordSize);
    
    // Draw sword border
    ctx.strokeStyle = '#FFA500';
    ctx.lineWidth = 2;
    ctx.strokeRect(-TILE_SIZE * 0.15, -swordSize * 0.5, TILE_SIZE * 0.3, swordSize);
    
    // Restore canvas state
    ctx.restore();
}

/**
 * Draw grid for visualization (optional)
 */
function drawGrid() {
    ctx.strokeStyle = 'rgba(0, 0, 0, 0.3)';
    ctx.lineWidth = 0.5;
    
    for (let i = 0; i <= DUNGEON_WIDTH; i++) {
        const x = i * TILE_SIZE;
        ctx.beginPath();
        ctx.moveTo(x, 0);
        ctx.lineTo(x, canvas.height);
        ctx.stroke();
    }
    
    for (let i = 0; i <= DUNGEON_HEIGHT; i++) {
        const y = i * TILE_SIZE;
        ctx.beginPath();
        ctx.moveTo(0, y);
        ctx.lineTo(canvas.width, y);
        ctx.stroke();
    }
}
