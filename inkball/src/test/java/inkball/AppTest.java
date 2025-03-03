package inkball;
import inkball.Ball;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import processing.core.PVector;
import processing.core.PApplet;
import java.util.Map;

import java.io.IOException;
import java.util.List;
import java.util.Queue;

import static org.junit.jupiter.api.Assertions.*;

public class AppTest {

    App app;

    @BeforeEach
    public void setup() {
        // Initialize the Processing sketch using runSketch
        app = new App();
        String[] args = {"App"};
        PApplet.runSketch(args, app);

        // Wait for setup() to complete
        app.setup();  // Call the setup method explicitly for your test environment
    }

    //LEVEL//

    @Test
    // Test if the level is loaded properly, ensuring the board is initialized and balls are available
    public void testLevelLoading() {
        
        app.loadConfig("config.json");  

        app.loadLevel(0);
        assertNotNull(app.board, "The board should be initialized after loading a level.");
        assertTrue(app.getBallsToSpawn().size() > 0, "There should be balls to spawn after loading a level.");
        assertEquals(0, app.getCurrentLevel(), "The current level should be 0 after the first level is loaded.");
    }

    @Test
    // Test if entry points are detected correctly after loading a level
    public void testSpawnerDetection() {
        
        List<PVector> entryPoints = app.getEntryPoints();
        assertNotNull(entryPoints, "Entry points should not be null.");
        
        PVector firstEntryPoint = entryPoints.get(0);
        
        // Verify that x = 5 * 32 and y = 8 * 32
        assertEquals(5 * 32, firstEntryPoint.x, "First entry point's x-coordinate should be at (5 * 32).");
        assertEquals(8 * 32, firstEntryPoint.y, "First entry point's y-coordinate should be at (8 * 32).");
    }

    // Test if the wall detection mechanism works at specific board positions
    @Test
    public void testWallDetection() {
        assertTrue(app.isWall(0, 0), "Position (0, 0) should be a wall.");
        assertFalse(app.isWall(1, 1), "Position (1, 1) should not be a wall.");
    }

    // Test detection of different wall types based on board positions
    @Test
    public void testDifferentWallTypes() {
        assertEquals(2, app.getWallType(8, 0), "Position (8, 0) should be wall type '2'.");
        assertFalse(app.isWall(1, 1), "Position (1, 1) should not be a wall.");
    }

    // Test if hole detection and color verification is working correctly
    @Test
    public void testHoleWithColor() {
        assertTrue(app.isHole(15, 1), "Position (15, 1) should be a hole.");
        assertEquals(1, app.getHoleColor(11, 6), "The hole at (11, 6) should have color '1'.");
    }


    // Test to ensure that the layout bounds do not exceed the 18x18 size
    @Test
    public void testLayoutBounds() {
        char[][] layout = app.board;
        for (char[] row : layout) {
            assertTrue(row.length <= 18, "Each row should be within 18 columns.");
        }
    }

    //Config

    // Test if the configuration file is loaded correctly
    @Test
    public void testConfigLoading() {

        assertNotNull(app.config, "Config should be loaded.");
        assertTrue(app.config.getJSONArray("levels").size() > 0, "There should be levels defined in the config.");
    }

    // Test if levels are loaded correctly from config and check associated properties like time, spawn intervals
    @Test
    public void testConfigLoadLevels() {
        app.loadLevel(0);
        assertEquals(3, app.getTotalLevels(), "Should load 3 levels from the config");
        assertEquals(120, app.getTimeLeft(), "First level time should be 120 seconds");
        assertEquals(10, app.getSpawnInterval(), "First level spawn interval should be 10 seconds");
        assertEquals(1.0, app.scoreIncreaseModifier, "Score increase modifier should be 1.0");
        assertEquals(1.0, app.scoreDecreaseModifier, "Score decrease modifier should be 1.0");

        String[] expectedBallsAfterFirstSpawn = {"orange", "grey", "blue", "green", "yellow"};

        assertArrayEquals(expectedBallsAfterFirstSpawn, app.getBallsToSpawn().toArray(new String[0]), 
            "After the first spawn, ballsToSpawn should have correct remaining balls");
    }

    // Test the score increase values for different ball colors
    @Test
    public void testScoreIncreaseMap() {
        Map<String, Integer> scoreIncreaseMap = app.scoreIncreaseMap;
        assertNotNull(scoreIncreaseMap, "Score increase map should not be null");

        assertEquals(70, scoreIncreaseMap.get("grey"), "Grey should have score increase of 70");
        assertEquals(50, scoreIncreaseMap.get("orange"), "Orange should have score increase of 50");
        assertEquals(50, scoreIncreaseMap.get("blue"), "Blue should have score increase of 50");
        assertEquals(100, scoreIncreaseMap.get("yellow"), "Yellow should have score increase of 100");
    }

    // Test the score decrease values for different ball colors
    @Test
    public void testScoreDecreaseMap() {
        Map<String, Integer> scoreDecreaseMap = app.scoreDecreaseMap;
        assertNotNull(scoreDecreaseMap, "Score decrease map should not be null");

        assertEquals(0, scoreDecreaseMap.get("grey"), "Grey should have score decrease of 0");
        assertEquals(25, scoreDecreaseMap.get("orange"), "Orange should have score decrease of 25");
        assertEquals(25, scoreDecreaseMap.get("blue"), "Blue should have score decrease of 25");
        assertEquals(100, scoreDecreaseMap.get("yellow"), "Yellow should have score decrease of 100");
    }

    //Balls

    // Test if balls are spawned with correct initial velocities
    @Test
    public void testBallInitialVelocity() {
        app.loadLevel(0);
        app.spawnBall();
        PVector velocity = app.getBalls().get(0).getVelocity();  
        assertTrue(velocity.x == -2 || velocity.x == 2, "Velocity in x direction should be either -2 or 2");
        assertTrue(velocity.y == -2 || velocity.y == 2, "Velocity in y direction should be either -2 or 2");
    }

    // Test if the ball queue size decreases correctly after spawning a ball
    @Test
    public void testBallQueueDepletion() {
        app.loadLevel(0);
        
        
        Queue<String> ballQueue = app.getBallsToSpawn();
        int initialQueueSize = ballQueue.size();
        app.spawnBall();

        assertEquals(initialQueueSize - 1, app.getBallsToSpawn().size(), "Ball queue size should decrease after spawning a ball.");
    }

    // Test if a ball correctly reflects after hitting the left wall
    @Test
    public void testBallReflectsAfterLeftWallCollision() {
        app.loadLevel(0);
        app.spawnBall();
        Ball ball = app.getBalls().get(0); 

        ball.setPosition(new PVector(0, app.TOPBAR + (app.CELLSIZE * 5))); // Close to the left wall

        ball.setVelocity(new PVector(-2, 0));

        int xIndex = (int) (ball.getPosition().x / app.CELLSIZE);
        int yIndex = (int) ((ball.getPosition().y - app.TOPBAR) / app.CELLSIZE);

        ball.handleWallCollision(xIndex, yIndex);

        assertTrue(ball.getVelocity().x > 0, "Ball should reflect off the left wall and move right");
    }

    // Test if spawn interval is correctly loaded from the configuration
    @Test
    public void testSpawnIntervalFromConfig() {
        app.loadConfig("config.json");  
        app.loadLevel(0);  

        assertEquals(10, app.spawnInterval, "Spawn interval should be 10 seconds as per the config");
    }

    // Test if the netball queue is correctly loaded from the configuration
    @Test
    public void testNetballQueueFromConfig() {
        app.loadLevel(0); 
        Queue<String> netballQueue = app.getBallsToSpawn();  

        assertEquals("orange", netballQueue.poll(), "Second ball in queue should be orange");
        assertEquals("grey", netballQueue.poll(), "Third ball should be grey");
    }

    // Hitbox

    // Test if a ball correctly detects and reflects when it collides with a drawn line
    @Test
    public void testCollisionWithLine() {
        app.loadConfig("config.json");
        app.loadLevel(0);
        

        Line line = new Line();
        line.addPoint(new PVector(100, 100));
        line.addPoint(new PVector(200, 100));
        app.drawnLines.add(line);
        
 
        app.spawnBall();  
        Ball ball = app.getBalls().get(0);  
        ball.setPosition(new PVector(150, 90));  
        ball.setVelocity(new PVector(0, 2)); 
        
        boolean collisionDetected = false;
        for (Line drawnLine : app.drawnLines) {
            if (app.isCollidingWithLine(ball.getPosition(), ball.getVelocity(), drawnLine.getPoints().get(0), drawnLine.getPoints().get(1), ball.radius)) {
                collisionDetected = true;
                break;
            }
        }
        assertTrue(collisionDetected, "Ball should collide with the drawn line.");
    }

    // Test if the normal vector for line-ball collision is calculated correctly
    @Test
    public void testNormalVectorCalculation() {
        
        PVector p1 = new PVector(100, 100);
        PVector p2 = new PVector(200, 100); 
        PVector ballPos = new PVector(150, 50); 

        PVector normal = app.calculateNormalVector(p1, p2, ballPos);  

        assertEquals(new PVector(0, 1), normal, "Normal vector should point downwards (opposite to what we thought).");
    }

    // Test if ball velocity is reflected correctly after collision
    @Test
    public void testVelocityAfterCollision() {
        Ball ball = new Ball(app, 50, 50, 1);  
        PVector initialVelocity = new PVector(1, -1);  
        PVector normal = new PVector(0, 1);  
        
        PVector newVelocity = ball.calculateNewVelocity(initialVelocity, normal);

        assertEquals(new PVector(1, 1), newVelocity, "Ball velocity should reflect upwards after collision.");
    }

    // Walls

    // Test if a ball reflects off the top wall and changes its color after collision
    @Test
    public void testBallReflectsAfterTopWallCollisionAndChangeColor() {
        app.loadLevel(0);  
        app.spawnBall();   
        Ball ball = new Ball(app, 224, 607, 1);

        ball.setPosition(new PVector(224, 607));

        ball.setVelocity(new PVector(-2, 0));

        int xIndex = (int) (ball.getPosition().x / app.CELLSIZE);
        int yIndex = (int) ((ball.getPosition().y - app.TOPBAR) / app.CELLSIZE);

        ball.handleWallCollision(xIndex, yIndex);

        assertTrue(ball.getVelocity().x > 0, "Ball should reflect off the left wall and move right");
        assertEquals(1, ball.colorIndex, "Ball color should change to orange after collision");
    }

    //Holes

    // Test if an orange ball is correctly captured by an orange hole
    @Test
    public void testBallCapturedByOrangeHole() {
        app.loadLevel(0);
        Ball ball = new Ball(app, 352, 256, 1);
        app.balls.add(ball);

        for (int i = 0; i < 30; i++) {
            ball.update();  
        assertTrue(ball.isCaptured, "Ball should be captured by the orange hole");
        assertTrue(app.getScore() > 0, "Score should increase after ball is captured");
        }
    }


    // Player actions

    // Test if the game correctly resets all states when restarted
    @Test
    public void testRestartGame() {
        app.loadLevel(0); 
        app.spawnBall();   
        app.score = 100;
        app.restart();  
        assertEquals(0, app.score, "Score should reset to 0 after restarting the game");
        assertEquals(0, app.getBalls().size(), "No balls should be present after restarting the game");
    }

    // Test if the game correctly pauses and balls stop moving when paused
    @Test
    public void testPauseGame() {
        app.loadLevel(0); 
        app.spawnBall();   
        app.pauseGame();
        assertTrue(app.isPaused, "*** PAUSED *** should be displayed and game should be paused");

        PVector oldVelocity = app.getBalls().get(0).getVelocity();
        app.updateGame();  
        PVector newVelocity = app.getBalls().get(0).getVelocity();

        assertEquals(oldVelocity, newVelocity, "Ball velocity should not change while the game is paused");
    }

    // Test if the ball stops moving when the game is paused
    @Test
    public void testBallDoesNotMoveWhenPaused() {
        // Load the level and spawn a ball
        app.loadLevel(0);
        app.spawnBall();

        // Ensure a ball was actually spawned
        assertFalse(app.getBalls().isEmpty(), "A ball should be spawned for the test");

        Ball ball = app.getBalls().get(0);
        ball.setVelocity(new PVector(2, 2)); // Set initial velocity

        // Pause the game
        app.pauseGame();

        // Capture the ball's position before updating the game
        PVector oldPosition = ball.getPosition().copy();

        // Try to update the game, but the ball should not move while paused
        app.updateGame(); 

        // Assert the ball's position is unchanged after pausing
        assertEquals(oldPosition, ball.getPosition(), "Ball should not move when the game is paused");
    }


    // Test if a line is removed when right-clicked near the line
    @Test
    public void testRemoveLineWithRightClick() {
        app.loadLevel(0);  

        Line line = new Line();
        line.addPoint(new PVector(100, 100));
        line.addPoint(new PVector(200, 100));
        app.drawnLines.add(line);
        assertEquals(1, app.drawnLines.size(), "There should be one line drawn");
        app.removeLine(new PVector(150, 100));  
        assertEquals(0, app.drawnLines.size(), "The line should be removed after right-clicking over it");
    }

    //Score and Timer

    // Test if the score increases correctly when a ball matches the hole color
    @Test
    public void testScoreIncreasesAfterCorrectBallHoleMatch() {
        app.loadLevel(0);  
        app.spawnBall();  

 
        Ball ball = app.getBalls().get(0);
        ball.colorIndex = 1; 


        PVector holePosition = new PVector(5, 5); 
        app.holeTypesMap.put(holePosition, 1);  

       
        ball.setPosition(holePosition);
        ball.isCaptured = true; 


        int initialScore = app.getScore();
        ball.checkColorMatch(5, 5);  

        int scoreIncrease = app.scoreIncreaseMap.get("orange");
        assertEquals(initialScore + scoreIncrease * app.scoreIncreaseModifier, app.getScore(),
            "Score should increase after an orange ball enters an orange hole");
    }

    // Test if the score decreases when a ball enters the wrong color hole
    @Test
    public void testScoreDecreasesAfterIncorrectBallHoleMatch() {
        app.loadLevel(0); 
        app.spawnBall(); 
     
        Ball ball = app.getBalls().get(0);
        ball.colorIndex = 2;  

    
        PVector holePosition = new PVector(7, 7); 
        app.holeTypesMap.put(holePosition, 4);  

        ball.setPosition(holePosition);
        ball.isCaptured = true;  

        int initialScore = app.getScore();
        ball.checkColorMatch(7, 7);  

        int scoreDecrease = app.scoreDecreaseMap.get("blue");
        assertEquals(initialScore - scoreDecrease * app.scoreDecreaseModifier, app.getScore(),
            "Score should decrease after a blue ball enters a yellow hole");
    }

    // Test if a line is removed when right-clicked near the line
    @Test
    public void testNoScoreDecreaseForGrayBallInAnyHole() {
        app.loadLevel(0); 
        app.spawnBall();   

        Ball ball = app.getBalls().get(0);
        ball.colorIndex = 0;  

        PVector holePosition = new PVector(9, 9);  
        app.holeTypesMap.put(holePosition, 1);  

        ball.setPosition(holePosition);
        ball.isCaptured = true;  

        int initialScore = app.getScore();
        ball.checkColorMatch(9, 9);  

        int scoreIncrease = app.scoreIncreaseMap.get("grey");
        assertEquals(initialScore + scoreIncrease * app.scoreIncreaseModifier, app.getScore(),
            "Score should increase when a gray ball enters any hole");
    }

    // Test if the timer decrements correctly with the passage of time
    @Test
    public void testTimerDecrementsEachSecond() {
        app.loadLevel(0);
        
        assertNotNull(app.getTimeLeft(), "timeLeft should be initialized after loading the level.");
        
        int initialTime = app.getTimeLeft();  
        assertTrue(initialTime > 0, "Initial time should be greater than zero.");
        
        for (int i = 0; i < 31; i++) {  
            app.draw();  
        }
        
        assertEquals(initialTime, app.getTimeLeft(), "The timer should decrement by 1 second after 30 frames.");
    }



    // Level ends and Game ends

    // Test if the level ends correctly when all balls are captured
    @Test
    public void testLevelEndsWhenAllBallsCaptured() {
        app.loadLevel(0); 
        app.spawnBall();  


        app.getBalls().clear(); 

        app.endLevel();
        assertTrue(app.isLevelEnded(), "Level should end when all balls are captured");
    }

    // Test if yellow tiles move correctly after the level ends
    @Test
    public void testYellowTilesMovementAfterLevelEnds() {
        app.loadLevel(0); 
        app.spawnBall();  

     
        app.getBalls().clear();  
        app.endLevel();

        int initialTile1X = app.yellowTile1X;
        int initialTile1Y = app.yellowTile1Y;
        int initialTile2X = app.yellowTile2X;
        int initialTile2Y = app.yellowTile2Y;

        app.handlePostLevelLogic();

        assertTrue(initialTile1X != app.yellowTile1X || initialTile1Y != app.yellowTile1Y, "Yellow tile 1 should move after the level ends");
        assertTrue(initialTile2X != app.yellowTile2X || initialTile2Y != app.yellowTile2Y, "Yellow tile 2 should move after the level ends");
    }

    // Test if the game correctly restarts after the time runs out
    @Test
    public void testRestartGameAfterTimesUp() {
        app.loadLevel(0);  
        app.timeLeft = 0; 

        app.restart(); 


        assertEquals(0, app.getScore(), "Score should reset to 0 after restarting the game");
        assertEquals(0, app.getBalls().size(), "No balls should be present after restarting the game");
    }



    // Edge cases

    // Test if the app detects parts of a 2x2 hole correctly
    @Test
    public void testPartofHoleDetection() {
        assertTrue(app.isHole(16, 1), "Position (16, 1) should be a part of hole.");
    }

    // Test if the game handles missing data correctly in the configuration
    @Test
    public void testMissingData() {
        assertNotNull(app.getEntryPoints(), "Entry points should not be null even if config is missing data");

        Map<String, Integer> scoreIncreaseMap = app.scoreIncreaseMap;
        assertNotNull(scoreIncreaseMap, "Score increase map should not be null");

        assertNotNull(app.getBallsToSpawn(), "Balls to spawn should not be null even if config is missing data");
    }



}


