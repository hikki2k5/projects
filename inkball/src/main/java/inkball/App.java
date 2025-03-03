// App.java
package inkball;

import processing.core.PApplet;
import processing.core.PVector;
import processing.core.PImage;
import processing.data.JSONObject; // For reading JSON configs
import processing.data.JSONArray; // For reading JSON arrays
import processing.event.MouseEvent;
import java.util.*;


/**
 * Interface for objects that can be drawn.
 */

interface Drawable {
    /**
     * Draws the object on the screen.
     */
    void draw();
}


/**
 * Represents a line drawn by the user in the game.
 * A Line object is a collection of points (PVector) that form a sequence.
 */

class Line implements Drawable {
    // List of points that form the line.
    List<PVector> points = new ArrayList<>();

    /**
     * Adds a point to the line.
     * @param point the PVector point to be added to the line.
     */
    public void addPoint(PVector point) {
        points.add(point);
    }

    /**
     * Gets all points that form this line.
     * @return a List of PVectors representing the points of the line.
     */
    public List<PVector> getPoints() {
        return points;
    }

    /**
     * Checks if a given position (point) is close to any segment of this line.
     * @param position the PVector position to check.
     * @return true if the position is within a certain distance to the line, false otherwise.
     */
    public boolean contains(PVector position) {
        for (int i = 1; i < points.size(); i++) {
            PVector p1 = points.get(i - 1);
            PVector p2 = points.get(i);

            if (distToSegment(position, p1, p2) < App.LINE_THICKNESS) {
                return true;
            }
        }
        return false;
    

    }

    


    /**
     * Calculates the shortest distance from point p to the line segment between points v and w.
     * This uses projection math to find the closest point on the segment and then calculates the distance.
     * 
     * @param p the point for which to calculate the distance.
     * @param v the start of the line segment.
     * @param w the end of the line segment.
     * @return the shortest distance from p to the line segment.
     */
    private float distToSegment(PVector p, PVector v, PVector w) {
        float l2 = PVector.dist(v, w) * PVector.dist(v, w);  
        if (l2 == 0.0) return PVector.dist(p, v);  
        float t = PVector.sub(p, v).dot(PVector.sub(w, v)) / l2;
        t = Math.max(0, Math.min(1, t));
        PVector projection = PVector.add(v, PVector.mult(PVector.sub(w, v), t));
        return PVector.dist(p, projection);
    }

    /**
     * Draws the line on the screen.
     * Uses the stroke and line methods from Processing to render the line segments.
     */
    @Override
    public void draw() {
        if (points.size() > 1) {
            for (int i = 1; i < points.size(); i++) {
                PVector p1 = points.get(i - 1);
                PVector p2 = points.get(i);
                App.instance.stroke(0);   // Set stroke color to black
                App.instance.strokeWeight(App.LINE_THICKNESS); // Set line thickness
                App.instance.line(p1.x, p1.y, p2.x, p2.y); // Draw the line
            }
        }
    }
}

/**
 * Represents a ball in the Inkball game. Each ball has a position, velocity, color, and can 
 * interact with walls, acceleration zones, and holes on the game board.
 */

class Ball implements Drawable {
    PApplet parent; // Reference to the Processing applet
    App app; // Reference to the main game application
    PVector position;  // Ball's current position
    PVector velocity;  // Ball's velocity vector
    int colorIndex;    // Color of the ball (mapped to images)
    float radius = 12; // Default radius of the ball
    boolean isCaptured = false; // Status indicating if the ball is captured by a hole
    float accelerationFactor = 1.0f; // Multiplier for the ball's speed (1.0 = normal speed)
    long accelerationStartTime = 0;  // Time when acceleration started
    long accelerationDuration = 2000; // Duration for which the ball accelerates (in ms)
    public float scale = 1.0f;
    public boolean inGame = true;
    public float vx, vy;
    public boolean captured = false;
    public float distanceToHole = Float.MAX_VALUE; // Variable to store distance to the nearest hole

    /**
     * Creates a new Ball object with a given position and color index.
     * Initializes the ball with random velocity.
     * 
     * @param app        Reference to the main App object.
     * @param x          Initial x-coordinate of the ball.
     * @param y          Initial y-coordinate of the ball.
     * @param colorIndex Index representing the color of the ball.
     */

    public Ball(App app, float x, float y, int colorIndex) {
        this.parent = app;
        this.app = app;
        this.position = new PVector(x, y); // Initialize position
        this.colorIndex = colorIndex; // Assign the ball's color

        // Set random initial velocity for the ball (-2 or 2 for both x and y)
        float[] possibleSpeeds = {-2, 2};
        float vx = possibleSpeeds[(int) (Math.random() * 2)];  // Using Math.random()
        float vy = possibleSpeeds[(int) (Math.random() * 2)];  // Using Math.random()
        this.velocity = new PVector(vx, vy);
        
    }

    /**
     * Gets the current position of the ball.
     * 
     * @return The position vector of the ball.
     */
    public PVector getPosition() {
        return this.position;
    }

    /**
     * Gets the current velocity of the ball.
     * 
     * @return The velocity vector of the ball.
     */
    public PVector getVelocity() {
        return this.velocity;
    }

    /**
     * Sets the velocity of the ball.
     * 
     * @param newVelocity The new velocity vector to be set.
     */
    public void setVelocity(PVector newVelocity) {
        this.velocity = newVelocity;
    }

    /**
     * Sets the position of the ball.
     * 
     * @param position The new position vector to be set.
     */
    public void setPosition(PVector position) {
        this.position = position;
    }


    /**
     * Updates the ball's position and handles interactions such as acceleration zones,
     * collisions with walls, boundaries, and holes.
     */
    public void update() {
        if (!isCaptured) {
            // Check for acceleration time
            if (parent.millis() - accelerationStartTime > accelerationDuration) {
                accelerationFactor = 1.0f; // Reset to normal speed after duration ends
            }

            // Move the ball by applying its velocity and acceleration factor
            position.add(PVector.mult(velocity, accelerationFactor));

            int xIndex = (int) (position.x / app.CELLSIZE);
            int yIndex = (int) ((position.y - app.TOPBAR) / app.CELLSIZE);

            if (xIndex >= 0 && xIndex < app.BOARD_WIDTH && yIndex >= 0 && yIndex < app.BOARD_HEIGHT) {
                char cell = app.board[yIndex][xIndex];

                // Calculate distanceToHole with all holes and get the nearest hole
                distanceToHole = Float.MAX_VALUE;  // Reset to maximum value
                PVector closestHolePosition = null;

                for (PVector holePosition : app.holeTypesMap.keySet()) {
                    // Retrieve the hole type, or skip if it's not a valid hole type
                    Integer holeType = app.holeTypesMap.get(holePosition);
                    if (holeType == null || holeType < 0 || holeType > 4) {
                        continue; // Skip invalid or unknown hole types
                    }

                    float holeCenterX = (holePosition.x + 1) * app.CELLSIZE;
                    float holeCenterY = app.TOPBAR + (holePosition.y + 1) * app.CELLSIZE;
                    PVector holeCenter = new PVector(holeCenterX, holeCenterY);

                    float currentDistanceToHole = PVector.dist(position, holeCenter);

                    // Update closest hole if this one is closer
                    if (currentDistanceToHole < distanceToHole) {
                        distanceToHole = currentDistanceToHole;
                        closestHolePosition = holePosition;
                    }
                }

                // Call attractToHole if there is a nearest hole and within the attraction zone
                if (closestHolePosition != null && distanceToHole <= 32) {
                    if (attractToHole((int) closestHolePosition.x, (int) closestHolePosition.y)) {
                        // If the ball is attracted into the hole, stop further updates for this ball
                        return;
                    }
                }

                // Handle acceleration zones ('A') with directions ('U' or 'D')
                if (cell == 'A') {
                    if (app.board[yIndex][xIndex + 1] == 'U') {
                        applyAcceleration(new PVector(0, -1)); // Up acceleration
                    } else if (app.board[yIndex][xIndex + 1] == 'D') {
                        applyAcceleration(new PVector(0, 1)); // Down acceleration
                    }
                }

                // Handle colored wall collisions ('1', '2', '3', '4')
                if (cell >= '1' && cell <= '4') {
                    handleWallCollision(xIndex, yIndex);  // Change color and reflect on colored wall collision
                }
                // Handle gray wall collisions ('X')
                else if (cell == 'X') {
                    handleWallCollision(xIndex, yIndex);  // Reflect on gray wall collision without color change
                }
            }

            // Handle boundary collisions
            handleBoundaryCollisions();
        }
    }

    
    /**
     * Attracts the ball to a hole if it is within a certain range. As the ball approaches
     * the hole, it is gradually pulled toward the center of the hole and shrinks in size.
     * If the ball gets close enough to the hole, it is captured, and the game's score is updated
     * based on the ball's color and the hole's color.
     * 
     * @param xIndex The x-coordinate index of the hole on the game board.
     * @param yIndex The y-coordinate index of the hole on the game board.
     * @return true if the ball is captured by the hole, false otherwise.
     */
    public boolean attractToHole(int xIndex, int yIndex) {
        // Get the center position of the hole
        float holeCenterX = (xIndex + 1) * app.CELLSIZE;
        float holeCenterY = app.TOPBAR + (yIndex + 1) * app.CELLSIZE;
        PVector holeCenter = new PVector(holeCenterX, holeCenterY);

        // If the ball is within the attraction range (32 units)
        if (distanceToHole <= 32) {
            // Calculate the attraction force and the direction to the hole
            PVector directionToHole = PVector.sub(holeCenter, position).normalize();
            float forceAttraction = PApplet.map(distanceToHole, 32, 0, 0.01f, 0.1f);  // Increase attraction force

            // Cập nhật vận tốc bóng dựa trên lực hút
            velocity.add(directionToHole.mult(forceAttraction));

            // Shrink the ball as it approaches the hole
            this.scale = PApplet.map(distanceToHole, 32, 0, 1.0f, 0.0f);   // Gradually shrink the ball

            // If the ball is very close to the center of the hole
            if (distanceToHole < 5) {
                this.isCaptured = true;  // Mark the ball as captured
                this.inGame = false;
                this.vx = 0;
                this.vy = 0;

                // Check if the ball's color matches the hole's color
                checkColorMatch(xIndex, yIndex);
                return true;  // Return true if the ball is captured by the hole
            }
        } else {
            this.scale = 1.0f;  // If outside the attraction range, reset the scale
        }
        return false;  // Return false if no interaction with the hole occurs
    }







    /**
     * Checks if the ball's color matches the hole's color and adjusts the score accordingly.
     * 
     * @param xIndex The x-coordinate index of the hole.
     * @param yIndex The y-coordinate index of the hole.
     */
    public void checkColorMatch(int xIndex, int yIndex) {
        PVector holePosition = new PVector(xIndex, yIndex); // Get the hole's position
        Integer holeColorIndex = app.holeTypesMap.get(holePosition); // Get the colour of the hole

        if (holeColorIndex == null) {
            holeColorIndex = 0;  // Default is grey
        }

        String holeColor = getColorName(holeColorIndex);
        String ballColor = getColorName(colorIndex);

        isCaptured = true;

        int scoreIncrease = app.scoreIncreaseMap.getOrDefault(ballColor, 0);
        int scoreDecrease = app.scoreDecreaseMap.getOrDefault(ballColor, 0);

        
        if (ballColor.equals("grey") || ballColor.equals(holeColor) || holeColor.equals("grey")) {
            /**
            Score is increased in terms of the config file if ball's colour is grey or 
            ball's colour matches with hole's colour or hole's colour is grey  
            */ 
            app.score += scoreIncrease * app.scoreIncreaseModifier;
        } else {
            // Else, the score is decreased
            app.score -= scoreDecrease * app.scoreDecreaseModifier;
            app.ballsToSpawn.add(ballColor.toLowerCase());
        }
    }

    /**
     * Returns the color name corresponding to the given index.
     * If the index is not recognized, the method defaults to returning "grey".
     * 
     * @param index The index of the color.
     * @return The name of the color as a String.
     */
    public String getColorName(int index) {
        switch (index) {
            case 1: return "orange";
            case 2: return "blue";
            case 3: return "green";
            case 4: return "yellow";
            default: return "grey"; // Default color if the index is not matched
        }
    }

    /**
     * Handles collisions with walls, potentially changing the ball's direction and color.
     * 
     * @param xIndex The x-coordinate index of the wall.
     * @param yIndex The y-coordinate index of the wall.
     */
    public void handleWallCollision(int xIndex, int yIndex) {
        // Define wall bounds
        PVector wallPos = new PVector(xIndex * app.CELLSIZE, app.TOPBAR + yIndex * app.CELLSIZE);
        float wallLeft = wallPos.x;
        float wallRight = wallPos.x + app.CELLSIZE;
        float wallTop = wallPos.y;
        float wallBottom = wallPos.y + app.CELLSIZE;

        // Calculate distances to the walls
        float distToLeft = position.x - wallLeft;
        float distToRight = wallRight - position.x;
        float distToTop = position.y - wallTop;
        float distToBottom = wallBottom - position.y;

        // Find the closest distance to the wall
        float minDistToWall = parent.min(parent.min(distToLeft, distToRight), parent.min(distToTop, distToBottom));

        // Check for corner (two wall) collision first
        boolean isTwoWallCorner = false;

        // Check if the current position is at the corner of two walls
        if ((xIndex - 1 >= 0 && app.isWall(xIndex - 1, yIndex)) && (yIndex - 1 >= 0 && app.isWall(xIndex, yIndex - 1))) {
            // This is a corner where two walls meet
            isTwoWallCorner = true;
        } else if ((xIndex + 1 < app.BOARD_WIDTH && app.isWall(xIndex + 1, yIndex)) && (yIndex + 1 < app.BOARD_HEIGHT && app.isWall(xIndex, yIndex + 1))) {
            // Another corner check for adjacent walls
            isTwoWallCorner = true;
        }

        // If it's a corner collision, handle it like colliding with two walls
        if (isTwoWallCorner) {
            // Reflect velocity based on collision with two walls
            // Use the normal reflection logic you applied in line segment collisions

            // Calculate normals for the two walls and reflect the ball
            PVector normalX = new PVector(1, 0);  // Normal vector for horizontal wall
            PVector normalY = new PVector(0, 1);  // Normal vector for vertical wall

            // Reflect the velocity along both normals
            velocity = calculateNewVelocity(velocity, normalX);
            velocity = calculateNewVelocity(velocity, normalY);

            return;  // Stop further processing, as we have handled the corner collision
        }

        // If not a corner, proceed with single wall collision detection
        boolean hitVerticalWall = false;
        boolean hitHorizontalWall = false;

        // Determine which wall the ball hit
        if (minDistToWall == distToLeft || minDistToWall == distToRight) {
            hitVerticalWall = true;
        }
        if (minDistToWall == distToTop || minDistToWall == distToBottom) {
            hitHorizontalWall = true;
        }

        // Handle corner collisions
        if (hitVerticalWall && hitHorizontalWall) {
            // Reflect velocity based on the closest direction
            if (Math.abs(velocity.x) > Math.abs(velocity.y)) {
                velocity.x *= -1;
            } else {
                velocity.y *= -1;
            }
        }
        // Handle vertical wall collision
        else if (hitVerticalWall) {
            velocity.x *= -1;  // Reflect horizontally
        }
        // Handle horizontal wall collision
        else if (hitHorizontalWall) {
            velocity.y *= -1;  // Reflect vertically
        }

        // If the wall is colored (1-4), change the ball color
        char cell = app.board[yIndex][xIndex];
        if (cell >= '1' && cell <= '4') { 
            int wallColorIndex = Character.getNumericValue(cell);
            this.colorIndex = wallColorIndex;  // Change ball color to match wall
        }
    }


    /**
     * Checks if the ball collides with a given line segment and reflects its velocity if it does.
     * 
     * @param p1 The starting point of the line segment.
     * @param p2 The ending point of the line segment.
     * @return true if the ball collides with the line segment, false otherwise.
     */
    public boolean collideWithLineSegment(float[] p1, float[] p2) {
        float collisionBuffer = 0.1f;

        // Future position based on current velocity
        float futureX = position.x + velocity.x;
        float futureY = position.y + velocity.y;

        // Calculate distances between the ball and the line segment
        float distanceP1ToBall = PVector.dist(new PVector(p1[0], p1[1]), new PVector(futureX, futureY));
        float distanceP2ToBall = PVector.dist(new PVector(p2[0], p2[1]), new PVector(futureX, futureY));
        float distanceP1ToP2 = PVector.dist(new PVector(p1[0], p1[1]), new PVector(p2[0], p2[1]));

        // Check if the ball collides with the line segment
        if (distanceP1ToBall + distanceP2ToBall <= distanceP1ToP2 + radius - collisionBuffer) {
            // Calculate the direction vectors
            float dx = p2[0] - p1[0];
            float dy = p2[1] - p1[1];

            // Normal vectors for the line segment
            PVector n1 = new PVector(dy, -dx).normalize();
            PVector n2 = new PVector(-dy, dx).normalize();

            // Calculate mid-point of the line segment
            float midX = (p1[0] + p2[0]) / 2;
            float midY = (p1[1] + p2[1]) / 2;

            // Calculate the points along the normals
            PVector pointN1 = new PVector(midX + n1.x, midY + n1.y);
            PVector pointN2 = new PVector(midX + n2.x, midY + n2.y);

            // Choose the correct normal based on the ball's position
            float distanceToN1 = PVector.dist(pointN1, position);
            float distanceToN2 = PVector.dist(pointN2, position);
            PVector chosenNormal = (distanceToN1 < distanceToN2) ? n1 : n2;

            // Reflect the ball's velocity using the chosen normal
            float dotProduct = velocity.dot(chosenNormal);
            velocity.x -= 2 * dotProduct * chosenNormal.x;
            velocity.y -= 2 * dotProduct * chosenNormal.y;

            return true;
        }
        return false;
    }


    /**
     * Reflects the ball's velocity when it hits a surface.
     * 
     * @param velocity The current velocity of the ball.
     * @param normal The normal vector of the surface.
     * @return The new velocity after reflection.
     */
    public PVector calculateNewVelocity(PVector velocity, PVector normal) {
        float dotProduct = velocity.dot(normal);
        return PVector.sub(velocity, PVector.mult(normal, 2 * dotProduct));
    }

    /**
     * Handles collisions with the game boundary (edges of the screen).
     * Reverses the ball's velocity when it hits the boundary.
     */
    public void handleBoundaryCollisions() {
        if (position.x - radius < 0) {
            position.x = radius;
            velocity.x *= -1; // Reflect horizontally
        }
        if (position.x + radius > app.WIDTH) {
            position.x = app.WIDTH - radius;
            velocity.x *= -1; // Reflect horizontally
        }
        if (position.y - radius < app.TOPBAR) {
            position.y = app.TOPBAR + radius;
            velocity.y *= -1; // Reflect vertically
        }
        if (position.y + radius > app.HEIGHT) {
            position.y = app.HEIGHT - radius;
            velocity.y *= -1; // Reflect vertically
        }
    }

    /**
     * Applies acceleration to the ball, changing its speed and direction.
     * 
     * @param direction The direction in which to apply acceleration.
     */

    public void applyAcceleration(PVector direction) {
        // Set the acceleration factor and modify velocity direction
        accelerationFactor = 1.5f; // Speed boost
        accelerationStartTime = parent.millis(); // Start timer
        velocity.set(direction.mult(velocity.mag())); // Update velocity in the new direction
    }


    /**
     * Draws the ball onto the screen if it is not captured.
     */
    @Override
    public void draw() {
        if (!isCaptured) {
            float adjustedRadius = radius * scale;  // Use scale to adjust ball's size
            App.instance.image(App.instance.ballImages[colorIndex], 
                position.x - adjustedRadius, 
                position.y - adjustedRadius, 
                adjustedRadius * 2, 
                adjustedRadius * 2);  // Draw scaled ball
        }
    }

}



/**
 * Main application class for the Inkball game. Handles game initialization, 
 * rendering, and updating the game board, balls, and interaction logic.
 */
public class App extends PApplet {

    public static App instance;  // Singleton instance of the App
    public static final int CELLSIZE = 32;  // Size of each cell on the board
    public static final int TOPBAR = 64;    // Height of the top bar area
    public static int WIDTH = CELLSIZE * 18;  // Total width of the game window
    public static int HEIGHT = TOPBAR + CELLSIZE * 18;  // Total height of the game window
    public static final int BOARD_WIDTH = WIDTH / CELLSIZE;  // Width of the game board in cells
    public static final int BOARD_HEIGHT = (HEIGHT - TOPBAR) / CELLSIZE;  // Height of the game board in cells
    public static char[][] board;  // 2D array representing the game board layout
    public static final int FPS = 30;  // Frames per second for the game loop
    public static Random random = new Random();  // Random number generator for game events
    

    // Variables for tracking yellow tile positions and movements
    public int yellowTile1X, yellowTile1Y;  // Position for the first yellow tile
    public int yellowTile2X, yellowTile2Y;  // Position for the second yellow tile
    public int remainingTimeBonus = 0;      // Tracks remaining time bonus
    public long lastYellowTileMoveTime = 0; // Timer for yellow tile movement

    //Declare variables for level ends/ game ends section
    boolean isPaused = false;  // Track if the game is paused
    public boolean levelEnded = false; // To track if the level has ended
    public boolean postLevelInProgress = false; // To track if the level has ended
    public boolean gameEnded = false;  // To track if the game has ended
    public int currentLevel = 0;       // Track current level

    
    // Declare variables for ball spawner
    public int spawnerX;  // X-coordinate of the ball spawner
    public int spawnerY;  // Y-coordinate of the ball spawner
    public Queue<String> ballsToSpawn;  // Queue to manage balls to spawn
    public List<PVector> entrypoints = new ArrayList<>();  // Entry points for spawning balls
    public int currentEntryPointIndex = 0;  // Used for cycling through entry points
    public int lastSpawnTime = 0; // Timer for spawning balls


    // Declare image variables
    PImage wallImage;  // Image for wall tiles
    PImage[] holeImages;  // Array of images for holes
    PImage[] ballImages;  // Array of images for balls
    PImage tileImage;  // Image for background tile
    PImage entrypointImage;  // Image for entry points
    PImage[] wallImages;  // Array of images for walls
    PImage upAccelerationImage;  // Image for upward acceleration tiles
    PImage downAccelerationImage;  // Image for downward acceleration tiles

    // Declare variables for drawing lines
    List<Ball> balls = new ArrayList<>(); // List of balls currently in play
    List<Line> drawnLines = new ArrayList<>(); // List of lines drawn by the user
    public Line currentLine; // The current line being drawn by the user
    public static final float LINE_THICKNESS = 10;  // Thickness of drawn lines

    // Declare variables for config section
    public int totalLevels;  // Total number of levels in the game
    public int score = 0; // The player's inititalised score
    public int timeLeft; // Time left in the current level (in seconds)
    public String configPath; // Path to the configuration file (JSON format)
    public JSONObject config;  // To store the configuration data
    public int spawnInterval;  // To store the spawn interval for each level
    public float spawnIntervalLeft; // Change from int to float
    public float lastFrameTime; // The time of the previous frame (used for timing calculations)

    
    //Declare variables for score modification when a ball collides with a hole
    public boolean isCollidingWithHole = false;  // Tracks if the ball is colliding with a hole
    public Map<String, Integer> scoreIncreaseMap = new HashMap<>();  // Map to store score increases by ball color
    public Map<String, Integer> scoreDecreaseMap = new HashMap<>();  // Map to store score decreases by ball color
    public Map<PVector, Integer> holeTypesMap = new HashMap<>();  // Maps hole positions to their types
    public float scoreIncreaseModifier;  // Modifier for score increases
    public float scoreDecreaseModifier;  // Modifier for score decreases
    
    /**
     * Constructor for the App class. Initializes the path to the configuration file.
     */
    public App() {
        this.configPath = "config.json"; // JSON config file path
        this.ballsToSpawn = new LinkedList<>(); // Initialize ballsToSpawn here
    }

    /**
     * Processing setup method for defining the window size.
     */
    @Override
    public void settings() {
        size(WIDTH, HEIGHT);  // Set the size of the game window
    }

    /**
     * Setup method for initializing the game elements. 
     * Loads configuration, images, and level data.
     */
    @Override
    public void setup() {
        instance = this;
        frameRate(FPS);  // Set frame rate to 30 frames per second
        lastFrameTime = millis() / 1000.0f; // Initialize last frame time

        board = new char[BOARD_HEIGHT][BOARD_WIDTH];  // Create the game board

        // Load configuration and score rules
        loadConfig(configPath);
        loadScoreRules();

        // Set spawner location to the middle of the board
        spawnerX = BOARD_WIDTH / 2 * CELLSIZE;
        spawnerY = TOPBAR + (BOARD_HEIGHT / 2) * CELLSIZE;

        // Load images for walls, entry points, and background tile
        wallImage = loadImage("src/main/resources/inkball/wall0.png");
        entrypointImage = loadImage("src/main/resources/inkball/entrypoint.png");
        tileImage = loadImage("src/main/resources/inkball/tile.png");

        // Load images for holes
        holeImages = new PImage[5];
        for (int i = 0; i < 5; i++) {
            holeImages[i] = loadImage("src/main/resources/inkball/hole" + i + ".png");
        }

        // Load images for walls
        wallImages = new PImage[5];
        for (int i = 0; i < 5; i++) {
            wallImages[i] = loadImage("src/main/resources/inkball/wall" + i + ".png");
        }

        // Load images for balls
        ballImages = new PImage[5];
        for (int i = 0; i < 5; i++) {
            ballImages[i] = loadImage("src/main/resources/inkball/ball" + i + ".png");
        }

        // Initialize the yellow tiles
        yellowTile1X = 0;
        yellowTile1Y = 0;
        yellowTile2X = BOARD_WIDTH - 1;
        yellowTile2Y = BOARD_HEIGHT - 1;
        totalLevels = config.getJSONArray("levels").size(); // Get the number of levels

        // Load images for acceleration zones
        upAccelerationImage = loadImage("src/main/resources/inkball/up_acceleration.png");
        downAccelerationImage = loadImage("src/main/resources/inkball/down_acceleration.png");

        // Load the first level layout from config
        loadLevel(0);    
    }

    /**
     * Loads the specified level from the configuration file.
     * @param levelIndex The index of the level to load.
     */
    public void loadLevel(int levelIndex) {
        if (levelIndex >= totalLevels) return; // Ensure we don't exceed the number of levels
        
        levelEnded = false; // Reset level end flag
        gameEnded = false;  // Reset game end flag

        JSONObject level = config.getJSONArray("levels").getJSONObject(levelIndex);  // Get level config
        drawnLines.clear();  // Clear all drawn lines when starting a new level
        timeLeft = level.getInt("time");  // Set level time
        spawnInterval = level.getInt("spawn_interval");  // Set spawn interval
        spawnIntervalLeft = spawnInterval;  // Initialize countdown for ball spawning
        scoreIncreaseModifier = (float) level.getDouble("score_increase_from_hole_capture_modifier");  // Set score modifiers
        scoreDecreaseModifier = (float) level.getDouble("score_decrease_from_wrong_hole_modifier");

        loadLayout(level.getString("layout"));  // Load the level layout
        ballsToSpawn.clear();  // Clear the ball spawn queue

        // Add the specified balls to spawn for the level
        JSONArray ballColors = level.getJSONArray("balls");
        for (int i = 0; i < ballColors.size(); i++) {
            ballsToSpawn.add(ballColors.getString(i).toLowerCase());
        }

        spawnBall();  // Spawn the first ball immediately
    }



    /**
     * Loads the game configuration from a JSON file.
     * @param path The file path to the configuration file.
     */
    public void loadConfig(String path) {
        // Load the config.json file
        config = loadJSONObject(path);
    }

    /**
     * Loads scoring rules from the configuration.
     * Populates maps for score increases and decreases based on ball color.
     */
    public void loadScoreRules() {
        JSONObject scoreIncrease = config.getJSONObject("score_increase_from_hole_capture");
        JSONObject scoreDecrease = config.getJSONObject("score_decrease_from_wrong_hole");

        for (Object keyObj : scoreIncrease.keys()) {
            String color = (String) keyObj;
            scoreIncreaseMap.put(color.toLowerCase(), scoreIncrease.getInt(color));
        }
        for (Object keyObj : scoreDecrease.keys()) {
            String color = (String) keyObj;
            scoreDecreaseMap.put(color.toLowerCase(), scoreDecrease.getInt(color));
        }
    }

    

    /**
     * Loads the game board layout from a specified file.
     * Each character in the file represents a different game object (e.g., walls, holes, balls).
     * @param layoutFile The file path of the layout file.
     */
    public void loadLayout(String layoutFile) {
        String[] lines = loadStrings(layoutFile);
        for (int y = 0; y < lines.length && y < BOARD_HEIGHT; y++) {
            String line = lines[y];

            for (int x = 0; x < line.length() && x < BOARD_WIDTH; x++) {
                board[y][x] = line.charAt(x); // Assign characters to the board

                switch (line.charAt(x)) {
                    case 'B': // Balls with color index
                        // Ensure there's a color index after 'B' (look ahead)
                        if (x + 1 < line.length()) {
                            char nextChar = line.charAt(x + 1);
                            if (Character.isDigit(nextChar)) {
                                int ballColorIndex = Character.getNumericValue(nextChar);
                                if (ballColorIndex >= 0 && ballColorIndex < ballImages.length) {
                                    // Add the ball to the game board at its position
                                    balls.add(new Ball(this, x * CELLSIZE, TOPBAR + y * CELLSIZE, ballColorIndex)); // Add ball to the game
                                }

                                x++; // Skip the color number after B (so that it's not processed again)
                            }
                        }
                        break;

                    case 'S': // Entry points
                        entrypoints.add(new PVector(x * CELLSIZE, TOPBAR + y * CELLSIZE)); // Add entry points to the game
                        break;

                    case 'H': // Holes
                        if (x + 1 < line.length() && y + 1 < lines.length) { 
                            char nextChar = line.charAt(x + 1); // Look at the character after 'H'
                            if (nextChar >= '0' && nextChar <= '4') { 
                                int holeType = nextChar - '0'; // Get the hole index

                                // Save hole position and type
                                holeTypesMap.put(new PVector(x, y), holeType);

                                // Mark the 2x2 area for the hole
                                board[y][x] = 'H';         // Top-left
                                board[y][x + 1] = 'H';     // Top-right
                                board[y + 1][x] = 'H';     // Bottom-left
                                board[y + 1][x + 1] = 'H'; // Bottom-right

                                x++; // Skip the next character (part of the hole)
                            }
                        
                        }
                        break;

                    case 'A': // Acceleration detection
                        if (x + 1 < line.length()) {
                            char nextChar = line.charAt(x + 1);
                            if (nextChar == 'U') {
                                board[y][x] = 'A'; // Up acceleration
                                board[y][x + 1] = 'U';
                                x++; // Skip the next character
                            } else if (nextChar == 'D') {
                                board[y][x] = 'A'; // Down acceleration
                                board[y][x + 1] = 'D';
                                x++; // Skip the next character
                            }
                        }
                        break;
                    }
            }
        }
    }

    /**
     * Converts a string color name to its corresponding index.
     * @param color The color name as a string.
     * @return The index representing the color, or 0 (grey) if unknown.
     */
    public int getColorIndex(String color) {
        switch (color.toLowerCase()) {
            case "orange":
                return 1;
            case "blue":
                return 2;
            case "green":
                return 3;
            case "yellow":
                return 4;
            case "grey":
                return 0;
            default:
                return 0; // Default to grey if unknown color
        }
    }

    /**
     * Draws the game board, including tiles, walls, entry points, holes, and accelerators.
     */
    public void drawBoard() {
        // Draw the background tiles
        for (int y = 0; y < BOARD_HEIGHT; y++) {
            for (int x = 0; x < BOARD_WIDTH; x++) {
                image(tileImage, x * CELLSIZE, TOPBAR + y * CELLSIZE); 
            }
        }

        // Draw board elements (walls, holes, entry points, accelerators)
        for (int y = 0; y < BOARD_HEIGHT; y++) {
            for (int x = 0; x < BOARD_WIDTH; x++) {
                char cell = board[y][x];

                switch (cell) {
                    case 'X': // Grey walls
                        image(wallImages[0], x * CELLSIZE, TOPBAR + y * CELLSIZE);
                        break;
                    case '1': // Orange walls
                        image(wallImages[1], x * CELLSIZE, TOPBAR + y * CELLSIZE);
                        break;
                    case '2': // Blue walls
                        image(wallImages[2], x * CELLSIZE, TOPBAR + y * CELLSIZE);
                        break;
                    case '3': // Green walls
                        image(wallImages[3], x * CELLSIZE, TOPBAR + y * CELLSIZE);
                        break;
                    case '4': // Yellow walls
                        image(wallImages[4], x * CELLSIZE, TOPBAR + y * CELLSIZE);
                        break;

                    case 'S': // Entry points
                        image(entrypointImage, x * CELLSIZE, TOPBAR + y * CELLSIZE);
                        break;
                    
                    case 'H':  // Holes
                        PVector pos = new PVector(x, y);
                        if (holeTypesMap.containsKey(pos)) {
                            int holeType = holeTypesMap.get(pos); // Fetch the hole type
                            if (holeType >= 0 && holeType < holeImages.length) {
                                image(holeImages[holeType], x * CELLSIZE, TOPBAR + y * CELLSIZE, CELLSIZE * 2, CELLSIZE * 2); // Draw the hole (2x2 size)
                            }

                            x++;  // Skip the next cell, as the hole covers two columns
                        }
                        break;
                    
                    case 'A': // Acceleration tiles
                        if (board[y][x + 1] == 'U') {
                            image(upAccelerationImage, x * CELLSIZE, TOPBAR + y * CELLSIZE, CELLSIZE, CELLSIZE);
                        } else if (board[y][x + 1] == 'D') {
                            image(downAccelerationImage, x * CELLSIZE, TOPBAR + y * CELLSIZE, CELLSIZE, CELLSIZE);
                        }

                        x++; // Skip the next character 'U' or 'D'
                        break;
                }
            }
        }
    }

    /**
     * Updates the list of balls by removing those that have been captured.
     * Also updates and draws the remaining balls.
     */
    public void updateBalls() {
        // Remove balls that have been captured
        balls.removeIf(ball -> {
            if (ball.isCaptured) {
                return true; // Remove this ball from the list
            }
            return false;
        });

        // Update and draw the remaining balls
        for (Ball ball : balls) {
            ball.update();  // Update the ball's state
            ball.draw();    // Draw the ball
        }
    }

    /**
     * Spawns a new ball at a random entry point, using the next color in the spawn queue.
     */
    public void spawnBall() {
        if (!ballsToSpawn.isEmpty() && !entrypoints.isEmpty()) {
            String color = ballsToSpawn.poll();  // Get the next ball color from the queue
            int colorIndex = getColorIndex(color);

            // Randomly select an entry point from the list of entry points
            int randomEntryPointIndex = random.nextInt(entrypoints.size());
            PVector randomEntryPoint = entrypoints.get(randomEntryPointIndex);

            // Adjust the ball position to the center of the entry point
            float ballX = randomEntryPoint.x + CELLSIZE / 2;
            float ballY = randomEntryPoint.y + CELLSIZE / 2;

            // Create and add the new ball to the game
            Ball newBall = new Ball(this, ballX, ballY, colorIndex);
            balls.add(newBall);
        }
    }


    /**
     * Draws the next upcoming balls in the game.
     * Displays up to 5 balls from the queue with a background.
     */
    public void drawNextBalls() {
        // Define the background size and position for the netball
        int ballSize = 32; // Size of each ball to be displayed
        int backgroundWidth = ballSize * 5 + 30;
        int backgroundHeight = ballSize + 20;
        int offsetX = 10;  // X position for the background
        int offsetY = 10;  // Y position for the background

        // Draw a background for the ball queue
        fill(0);  // Set background color to black
        rect(offsetX - 10, offsetY - 10, backgroundWidth, backgroundHeight); // Draw the rectangle

        // Display the next balls
        Iterator<String> queueIterator = ballsToSpawn.iterator();
        int displayCount = 5;  // Limit the display to 5 balls

        for (int i = 0; i < displayCount && queueIterator.hasNext(); i++) {
            String color = queueIterator.next();
            int colorIndex = getColorIndex(color);
            image(ballImages[colorIndex], offsetX + i * (ballSize + 5), offsetY, ballSize, ballSize); // Display ball
        }
    }

    /**
     * Handles time management and spawning logic for the game.
     * Decrements the time left for the current level and handles ball spawning.
     */
    public void handleTimeAndSpawning() {
        // Decrease the time left for the current level every second
        if (frameCount % FPS == 0 && timeLeft > 0) {
            timeLeft--;  // Decrease time
        }

        // Check if time has run out
        if (timeLeft <= 0) {
            timeLeft = 0;
            noLoop();  // Stop the game loop
            fill(0);  // Set text color to black
            textSize(20);  // Set text size
            textAlign(LEFT, TOP);  // Align text to top left
            text("=== TIME'S UP ===", 250, 15);  // Display the message "TIME'S UP"
        } 
    }  


    /**
     * Draws the countdown timer until the next ball is spawned.
     */
    public void drawSpawnIntervalCountdown() {
        fill(0);  // Set text color to black
        textSize(24);  // Set text size
        textAlign(LEFT, TOP);  // Align text to top left
        text(nf(spawnIntervalLeft, 1, 1), 200, 10);  // Display time left with 1 decimal point
    
        // Update the spawn interval countdown
        float timePassedSinceLastSpawn = (millis() - lastSpawnTime) / 1000.0f;  // Time passed in seconds
        spawnIntervalLeft = spawnInterval - timePassedSinceLastSpawn;  // Update the spawn countdown

        // Spawn a new ball if the countdown reaches 0
        if (spawnIntervalLeft <= 0) {
            spawnBall();  // Spawn a new ball
            lastSpawnTime = millis();  // Reset the spawn timer
            spawnIntervalLeft = spawnInterval;  // Reset the spawn interval countdown
        }
    }

    /**
     * Displays the current score and the remaining time.
     */
    public void displayScoreAndTime() {
        fill(0);  // Set text color to black
        textSize(24);  // Set text size
        textAlign(RIGHT, TOP);  // Align text to the top right
        text("Score: " + (int) score, WIDTH - 10, 10);  // Display the score
        text("Time: " + timeLeft, WIDTH - 10, 40);  // Display the remaining time
    }


    /**
     * Main game draw loop. Handles rendering of the game board, balls, 
     * time, score, and collisions. Also manages the end of levels and the game.
     */
    @Override
    public void draw() {
        background(200);  // Clear the background
        drawBoard();      // Draw the game board

        // If the game is not paused, update balls and handle spawning
        if (!isPaused) {
            updateBalls();  // Update ball positions
            handleTimeAndSpawning();  // Handle spawning and time
        }

        // Always draw the balls, even if the game is paused
        for (Ball ball : balls) {
            ball.draw();  // Draw each ball
        }

        // Display the score and time
        displayScoreAndTime();
        drawNextBalls();  // Draw the upcoming balls in the queue
        
        // Display the spawn interval countdown if there are balls left to spawn
        if (!ballsToSpawn.isEmpty()) {
            drawSpawnIntervalCountdown();
        }

        // Draw the lines
        stroke(0);  // Set stroke color to black
        strokeWeight(LINE_THICKNESS);  // Set line thickness
        for (Line line : drawnLines) {
            List<PVector> points = line.getPoints();
            for (int i = 1; i < points.size(); i++) {
                PVector p1 = points.get(i - 1);
                PVector p2 = points.get(i);
                line(p1.x, p1.y, p2.x, p2.y);  // Draw each segment of the line
            }
        }

        // If a new line is being drawn, display it
        if (currentLine != null) {
            stroke(0);
            strokeWeight(LINE_THICKNESS);
            List<PVector> points = currentLine.getPoints();
            for (int i = 1; i < points.size(); i++) {
                PVector p1 = points.get(i - 1);
                PVector p2 = points.get(i);
                line(p1.x, p1.y, p2.x, p2.y);
            }
        }

        // Display a "PAUSED" message if the game is paused
        if (isPaused) {
            fill(0);
            textSize(20);
            textAlign(LEFT, TOP);
            text("*** PAUSED ***", 250, 15);  // Display the paused message
        }


        // Check for ball collisions with lines if the game is not paused
        if (!isPaused) {
            checkBallLineCollisions();
        }

        // Handle the end of the level or game
        if (levelEnded && postLevelInProgress) {
            handlePostLevelLogic();  // Handle post-level activities
        } else if (ballsToSpawn.isEmpty() && balls.isEmpty() && !postLevelInProgress) {
            endLevel();  // End the level when there are no more balls
        }

        // Display the end game message if the game has ended
        if (gameEnded) {
            displayEndGameMessage();  // Show the "ENDED" message
        }

        // Draw the yellow tiles if the level has ended
        if (levelEnded) {
            drawYellowTiles();
        }
    }

    /**
     * Handles the collision checks between the balls and the drawn lines.
     * If a collision is detected, it reflects the ball and removes the line.
     */
    public void checkBallLineCollisions() {
        for (Ball ball : balls) {
            // Loop through all the drawn lines
            for (int i = drawnLines.size() - 1; i >= 0; i--) {
                Line line = drawnLines.get(i);
                List<PVector> points = line.getPoints();

                // Check for collision between the ball and each line segment
                for (int j = 1; j < points.size(); j++) {
                    PVector p1 = points.get(j - 1);
                    PVector p2 = points.get(j);

                    // Convert PVector points to float arrays
                    float[] p1Array = new float[]{p1.x, p1.y};
                    float[] p2Array = new float[]{p2.x, p2.y};

                    // Call the collision method from the Ball class
                    if (ball.collideWithLineSegment(p1Array, p2Array)) {
                        drawnLines.remove(i); // Remove the line segment after a collision
                        break;
                    }
                }
            }
        }

    }

    /**
     * Draws the yellow tiles at their positions after a level ends.
     */
    public void drawYellowTiles() {
        if (levelEnded) {  // Only draw if the level has ended
            // Draw yellow tiles
            image(wallImages[4], yellowTile1X * CELLSIZE, TOPBAR + yellowTile1Y * CELLSIZE, CELLSIZE, CELLSIZE);
            image(wallImages[4], yellowTile2X * CELLSIZE, TOPBAR + yellowTile2Y * CELLSIZE, CELLSIZE, CELLSIZE);
        }
    }

    
    /**
     * Adds the remaining time to the score during post-level activities.
     */
    public void addRemainingTimeToScore() {
        float timeIncrementRate = 0.067f * 1000;  // Every 0.067 seconds adds 1 to the score
        if (timeLeft > 0 && millis() - lastSpawnTime > timeIncrementRate) {
            score++;  // Increment score by 1
            timeLeft--;  // Decrement the remaining time
            lastSpawnTime = millis();  // Update the time for the next increment
        } else if (timeLeft == 0 && currentLevel == totalLevels - 1) {
            gameEnded = true;  // End the game if this is the final level
        }
    }

    /**
     * Moves the yellow tiles around the board in a clockwise direction.
     * This happens as part of post-level activities.
     */
    public void moveYellowTiles() {
        int movementRate = 67;  // Move 1 tile every 0.067 seconds
        if (millis() - lastFrameTime > movementRate) {
            // Restore previous yellow tile positions back to grey
            board[yellowTile1Y][yellowTile1X] = 'X';  // Tile 1
            board[yellowTile2Y][yellowTile2X] = 'X';  // Tile 2

            // Move yellow tile 1 in a clockwise pattern
            if (yellowTile1Y == 0 && yellowTile1X < BOARD_WIDTH - 1) yellowTile1X++;
            else if (yellowTile1X == BOARD_WIDTH - 1 && yellowTile1Y < BOARD_HEIGHT - 1) yellowTile1Y++;
            else if (yellowTile1Y == BOARD_HEIGHT - 1 && yellowTile1X > 0) yellowTile1X--;
            else if (yellowTile1X == 0 && yellowTile1Y > 0) yellowTile1Y--;

            // Move yellow tile 2 in the opposite direction (counterclockwise)
            if (yellowTile2Y == BOARD_HEIGHT - 1 && yellowTile2X > 0) yellowTile2X--;
            else if (yellowTile2X == 0 && yellowTile2Y > 0) yellowTile2Y--;
            else if (yellowTile2Y == 0 && yellowTile2X < BOARD_WIDTH - 1) yellowTile2X++;
            else if (yellowTile2X == BOARD_WIDTH - 1 && yellowTile2Y < BOARD_HEIGHT - 1) yellowTile2Y++;

            // Set the new positions to yellow ('4')
            board[yellowTile1Y][yellowTile1X] = '4';
            board[yellowTile2Y][yellowTile2X] = '4';

            lastFrameTime = millis();  // Update the last frame time for the next move
        }
    }

    /**
     * Ends the current level and initiates the post-level logic.
     */
    public void endLevel() {
        levelEnded = true;
        postLevelInProgress = true;  // Activate postLevel progress
        lastSpawnTime = millis();   // Record the current time to manage post-level timing
    }
    

    /**
     * Handles post-level logic such as adding time bonuses, moving yellow tiles, 
     * and preparing for the next level or ending the game.
     */
    public void handlePostLevelLogic() {
        addRemainingTimeToScore();  // Add remaining time to the score
        moveYellowTiles();  // Move yellow tiles as part of post-level action

        // Check if the level's post-level activities are completed (i.e., time is up)
        if (timeLeft <= 0) {
            postLevelInProgress = false;  // Mark post-level as completed

            // Move to the next level if available, otherwise end the game
            if (currentLevel < totalLevels - 1) {
                currentLevel++;  // Advance to the next level
                loadLevel(currentLevel);  // Load the next level
                levelEnded = false;  // Reset the level-ended flag
            } else {
                gameEnded = true;  // Mark the game as ended if it's the last level
            }
        }
    }


    /**
     * Displays the end-game message.
     * Shows "=== ENDED ===" when the game is finished.
     */
    public void displayEndGameMessage() {
        fill(0);  // Set text color to black
        textSize(20);  // Set the font size
        textAlign(LEFT, TOP);  // Align text to the top-left
        text("=== ENDED ===", 250, 15);  // Display the end game message
    }

    /**
     * Resets the current level to its initial state, clearing the board, score, and other elements.
     */
    public void resetLevel() {
        // Load the first level from the configuration file
        JSONObject level = config.getJSONArray("levels").getJSONObject(0);  // Load the first level

        // Reset time and score
        timeLeft = level.getInt("time");  // Reset the time for the level
        score = 0;  // Reset the score
        balls.clear();  // Clear all existing balls
        drawnLines.clear();  // Clear all drawn lines
        ballsToSpawn.clear();  // Clear the ball spawn queue
        levelEnded = false;  // Reset the level end status
        gameEnded = false;  // Reset the game end status

        // Reset the position of the yellow tiles
        yellowTile1X = 0;
        yellowTile1Y = 0;
        yellowTile2X = BOARD_WIDTH - 1;
        yellowTile2Y = BOARD_HEIGHT - 1;

        // Reset remaining time bonus
        remainingTimeBonus = 0;  // Reset time bonus

        // Load ball colors from the configuration
        JSONArray ballColors = level.getJSONArray("balls");
        for (int i = 0; i < ballColors.size(); i++) {
            ballsToSpawn.add(ballColors.getString(i).toLowerCase());
        }

        // Reset spawnInterval for level 1
        spawnInterval = level.getInt("spawn_interval");  // Reset spawn interval to level 1 value
        spawnIntervalLeft = spawnInterval;  // Reinitialize the countdown for ball spawning

        loadLayout(level.getString("layout"));  // Load the layout for the level
        spawnBall();  // Spawn the first ball for the level

        loop();  // Resume the game loop

        // Reset timers for tile movement and ball spawning
        lastSpawnTime = millis();
        lastYellowTileMoveTime = millis();
    }

    
    /**
     * Handles key press events in the game.
     * - 'r' key: Resets the current level.
     * - Spacebar: Toggles pause/unpause of the game.
     */
    @Override
    public void keyPressed() {
        if (key == 'r') {
            resetLevel();  // Reset the current level
        }
        if (key == ' ') {
            togglePause();  // Pause or unpause the game
        }
    }


    /**
     * Toggles the pause state of the game.
     */
    public void togglePause() {
        isPaused = !isPaused;  // Invert the pause status
    }


    /**
     * Handles mouse press events for drawing and removing lines.
     * - Left click: Starts drawing a new line.
    * - Right click: Removes a line near the clicked position.
    */
    @Override
    public void mousePressed(MouseEvent e) {
        if (e.getButton() == LEFT) {
            currentLine = new Line();  // Start a new line
        } else if (e.getButton() == RIGHT) {
            // Remove a line if a point is near the clicked position
            PVector clickPos = new PVector(e.getX(), e.getY());
            drawnLines.removeIf(line -> line.getPoints().stream().anyMatch(p -> PVector.dist(p, clickPos) < LINE_THICKNESS));
        }
    }

    /**
     * Handles mouse drag events to add points to the current line being drawn.
     */
    @Override
    public void mouseDragged(MouseEvent e) {
        if (currentLine != null) {
            currentLine.addPoint(new PVector(e.getX(), e.getY()));  // Add a point to the line
        }
    }

    /**
     * Handles mouse release events to finalize the drawing of a line.
     */
    @Override
    public void mouseReleased(MouseEvent e) {
        if (currentLine != null) {
            drawnLines.add(currentLine);  // Add the completed line to the list
            currentLine = null;  // Clear the current line
        }
    }



    /**
     * Calculates the normal vector of the line segment for collision reflection.
     * @param p1 The starting point of the line segment.
     * @param p2 The ending point of the line segment.
     * @param ballPos The position of the ball.
     * @return The normal vector used for reflecting the ball.
     */
    public PVector calculateNormalVector(PVector p1, PVector p2, PVector ballPos) {
        PVector lineVector = PVector.sub(p2, p1);
        PVector normal = new PVector(-lineVector.y, lineVector.x).normalize();
        PVector midpoint = PVector.add(p1, p2).div(2);
        // Ensure the normal vector points in the correct direction
        if (PVector.dist(midpoint, PVector.add(ballPos, normal)) > PVector.dist(midpoint, ballPos)) {
            normal.mult(-1);
        }
        return normal;
    }


    /**
     * Checks whether a ball is colliding with a line segment.
     * The collision is detected based on the future position of the ball and the proximity of the ball to the line segment.
     *
     * @param ballPosition  The current position of the ball (PVector).
     * @param ballVelocity  The current velocity of the ball (PVector).
     * @param lineStart     The starting point of the line segment (PVector).
     * @param lineEnd       The ending point of the line segment (PVector).
     * @param ballRadius    The radius of the ball (float).
     * @return true if the ball is colliding with the line segment, false otherwise.
     */
    public boolean isCollidingWithLine(PVector ballPosition, PVector ballVelocity, PVector lineStart, PVector lineEnd, float ballRadius) {
        // Future position based on current velocity
        PVector futureBallPosition = PVector.add(ballPosition, ballVelocity);

        // Calculate distances between the ball and the line segment
        float distanceP1ToBall = PVector.dist(lineStart, futureBallPosition);
        float distanceP2ToBall = PVector.dist(lineEnd, futureBallPosition);
        float distanceP1ToP2 = PVector.dist(lineStart, lineEnd);

        // Check if the ball collides with the line segment
        return distanceP1ToBall + distanceP2ToBall <= distanceP1ToP2 + ballRadius;
    }



    /**
     * Retrieves the list of current balls in play.
     * @return A list of Ball objects.
     */
    public List<Ball> getBalls() {
        return balls;
    }

    /**
     * Retrieves a copy of the queue of balls to be spawned.
     * @return A copy of the queue of balls waiting to be spawned.
     */
    public Queue<String> getBallsToSpawn() {
        return new LinkedList<>(ballsToSpawn);  // Return a copy to prevent modification of the original queue
    }

    /**
     * Retrieves the current level number.
     * @return The current level index.
     */
    public int getCurrentLevel() {
        return currentLevel;
    }

    /**
     * Retrieves the list of entry points where balls can spawn.
     * @return A list of PVector objects representing entry points.
     */
    public List<PVector> getEntryPoints() {
        return entrypoints;
    }

    /**
     * Checks if the given coordinates represent a wall.
     * @param x The x-coordinate in the board.
     * @param y The y-coordinate in the board.
     * @return True if the coordinates represent a wall, otherwise false.
     */
    public boolean isWall(int x, int y) {
        return board[y][x] == 'X' || (board[y][x] >= '1' && board[y][x] <= '4');
    }

    /**
     * Retrieves the type of wall at the given coordinates.
     * @param x The x-coordinate in the board.
     * @param y The y-coordinate in the board.
     * @return An integer representing the wall type.
     */
    public int getWallType(int x, int y) {
        return Character.getNumericValue(board[y][x]);
    }

    /**
     * Checks if the given coordinates represent a hole.
     * @param x The x-coordinate in the board.
     * @param y The y-coordinate in the board.
     * @return True if the coordinates represent a hole, otherwise false.
     */
    public boolean isHole(int x, int y) {
        return board[y][x] == 'H';
    }

    /**
     * Retrieves the color of the hole at the given coordinates.
     * @param x The x-coordinate in the board.
     * @param y The y-coordinate in the board.
     * @return An integer representing the color of the hole, or 0 (default grey) if unknown.
     */
    public int getHoleColor(int x, int y) {
        return holeTypesMap.getOrDefault(new PVector(x, y), 0);
    }

    /**
     * Checks if there is a ball at the given coordinates.
     * @param x The x-coordinate in the board.
     * @param y The y-coordinate in the board.
     * @return True if there is a ball at the coordinates, otherwise false.
     */
    public boolean isBall(int x, int y) {
        if (x < 0 || x >= 18 || y < 0 || y >= 18) {
            return false; // Out of bounds check
        }
        return board[y][x] == 'B';
    }

    /**
     * Retrieves the color of the ball at the given coordinates.
     * @param x The x-coordinate in the board.
     * @param y The y-coordinate in the board.
     * @return The color index of the ball, or -1 if no ball is present.
     */
    public int getBallColor(int x, int y) {
        if (board[y][x] == 'B') {
            return Character.getNumericValue(board[y][x + 1]);
        }
        return -1; // No ball present
    }

    /**
     * Retrieves the time left for the current level.
     * @return The remaining time for the level.
     */
    public int getTimeLeft() {
        return timeLeft;
    }

    /**
     * Retrieves the current score.
     * @return The current score.
     */
    public int getScore() {
        return score;
    }

    /**
     * Retrieves the map that defines the score increase values for each ball color.
     * @return A map of color names to score increase values.
     */
    public Map<String, Integer> getScoreIncreaseMap() {
        return scoreIncreaseMap;
    }

    /**
     * Retrieves the map that defines the score decrease values for each ball color.
     * @return A map of color names to score decrease values.
     */
    public Map<String, Integer> getScoreDecreaseMap() {
        return scoreDecreaseMap;
    }

    /**
     * Retrieves the total number of levels in the game.
     * @return The total number of levels.
     */
    public int getTotalLevels() {
        return totalLevels;
    }

    /**
     * Retrieves the spawn interval for the current level.
     * @return The spawn interval in milliseconds.
     */
    public int getSpawnInterval() {
        return spawnInterval;
    }

    /**
     * Checks if the current level has ended.
     * @return True if the level has ended, otherwise false.
     */
    public boolean isLevelEnded() {
        return levelEnded;
    }

    /**
     * Checks if the game has ended.
     * @return True if the game has ended, otherwise false.
     */
    public boolean isGameEnded() {
        return gameEnded;
    }

    /**
     * Restarts the current level, resetting the balls, score, and game state.
     */
    public void restart() {
        loadLevel(currentLevel);  // Reload the current level
        score = 0;  // Reset score
        balls.clear();  // Clear all current balls
    }

    /**
     * Updates the game state, including moving the balls if the game is not paused.
     */
    public void updateGame() {
        if (!isPaused) {
            // Update the balls and any other game elements
            for (Ball ball : balls) {
                ball.update();
            }
        }
    }

    /**
     * Removes a line near the specified position from the drawn lines.
     * @param position The position to check for nearby lines.
     */
    public void removeLine(PVector position) {
        // Remove the line if it contains a point close to the given position
        drawnLines.removeIf(line -> line.contains(position));
    }

    /**
     * Pauses the game, setting the paused state to true.
     */
    public void pauseGame() {
        isPaused = true;  // Set the game to a paused state
    }

    /**
     * The main entry point of the application. Starts the Processing PApplet.
     * @param args The command line arguments.
     */
    public static void main(String[] args) {
        PApplet.main("inkball.App");
    }

}
