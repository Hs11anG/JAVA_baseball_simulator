import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.util.Random;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.List;
import java.util.Collections;
import java.util.ArrayList;

import data.Pitcher;
import data.TrajectoryData;
import data.Point3D;

public class GamePanel extends JPanel {
    private Timer timer;
    private JFrame mainFrame;

    // Physical constants
    private static final double PITCHER_MOUND_DISTANCE_FT = 60.5;
    private static final double HOME_PLATE_FRONT_FT = 1.417;

    // Ball state variables
    private double x_ft, y_ft, z_ft;
    private double vx, vy, vz;
    private double ax, ay;

    // Pre-calculated trajectory
    private List<Point3D> ballTrajectoryPoints;
    private int currentTrajectoryIndex;
    private String preCalculatedPitchResult;

    // Time management
    private long lastFrameTime;

    // Game state
    private String currentHitresult = " ";
    private String pitchType = "none";
    private boolean isPitching = false;
    private boolean ballReachedCatcher = false;
    private boolean isPaused = false;
    private boolean isHittingMode = false;
    private int countdown = 0;
    private String hitResult = null;
    private boolean swingAttempted = false;

    // Ball/strike count
    private int currentStrikes;
    private int currentBalls;
    private final int MAX_STRIKES = 3;
    private final int MAX_BALLS = 4;

    // Play Mode variables
    private boolean isPlayMode;
    private List<Pitcher> playModePitchers;
    private int currentPlayModePitcherIndex;
    private int outs;
    private int hits;
    private final int MAX_OUTS = 5;
    private final int TARGET_HITS = 3;

    // Manual aiming variables
    private double aimX_ft = 0;
    private double aimY_ft = 2.5;
    private Point mousePos;

    // Manual pitching control variables
    private boolean isAimingSequenceActive = false;
    private long aimSequenceStartTime;
    private double aimingCircleRadius_ft;
    private double lockedAimX_ft, lockedAimY_ft;
    private String selectedPitchType = "none";

    // Manual pitching control constants
    private static final double MAX_AIM_RADIUS_FT = 1.5;
    private static final double MIN_AIM_RADIUS_FT = 0.2;
    private static final long AIM_SHRINK_DURATION_MS = 700;

    // Pre-pitch tell feature variables
    private boolean isPrePitchTell = false;
    private int prePitchTellCounter = 0;
    private Point prePitchBallPos;
    // --- MODIFIED: New tell rhythm constants ---
    private static final int PRE_PITCH_TELL_ON_DURATION = 15;  // "On" duration in frames
    private static final int PRE_PITCH_TELL_OFF_DURATION = 15; // "Off" duration in frames
    private static final int PRE_PITCH_TELL_DURATION = PRE_PITCH_TELL_ON_DURATION + PRE_PITCH_TELL_OFF_DURATION; // Total tell time

    // Trajectory start and end points
    private double startX_ft, startY_ft, startZ_ft;
    private double endZ_ft = HOME_PLATE_FRONT_FT;

    // Display and camera parameters
    private final int windowWidth = 1000;
    private final int windowHeight = 700;
    private final int vanishingPointX = windowWidth / 2;
    private final int vanishingPointY = windowHeight / 2;
    private final double cameraY_ft = 2.5;
    private final double cameraZ_ft = -4.0;
    private final double focalLength = 700;

    // Other utilities
    private final Random random = new Random();

    // Strike zone definition
    private final double strikeZoneLeft_ft = -0.78;
    private final double strikeZoneRight_ft = 0.78;
    private final double strikeZoneTop_ft = 3.1;
    private final double strikeZoneBottom_ft = 1.3;

    // Database
    private final Map<String, TrajectoryData> pitchDatabase = new HashMap<>();
    private DatabaseManager dbManager;
    private Pitcher currentPitcher;

    // Renderer
    private BallRenderer ballRenderer;

    public GamePanel(boolean hittingMode, JFrame frame, Pitcher selectedPitcher, boolean isPlayMode) {
        this.isHittingMode = hittingMode;
        this.mainFrame = frame;
        this.currentPitcher = selectedPitcher;
        this.isPlayMode = isPlayMode;
        this.dbManager = new DatabaseManager();
        this.ballRenderer = new BallRenderer(windowWidth, windowHeight);

        if (this.isPlayMode) {
            initializePlayMode();
        } else if (currentPitcher != null) {
            loadPitcherPitchData(currentPitcher.getPid());
        } else {
            System.err.println("Warning: Game mode started without selected pitcher. Loading default pitches.");
            // initialize_AllPitchesDefault();
        }

        setupKeyBindings();

        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                if (!isHittingMode && !isPitching && !isAimingSequenceActive) {
                    mousePos = e.getPoint();
                    updateAimPosition();
                    repaint();
                }
            }
        });

        setFocusable(true);
        timer = new Timer(16, this::actionPerformed);
        timer.start();

        if (isHittingMode || isPlayMode) {
            countdown = 180;
        }
        resetAtBat();
        resetPitch();
    }

    private void loadPitcherPitchData(int pitcherId) {
        pitchDatabase.clear();
        pitchDatabase.putAll(dbManager.getPitchDataForPitcher(pitcherId));
        if (pitchDatabase.isEmpty()) {
            System.err.println("No pitch data found for pitcher PID: " + pitcherId + ". Loading default pitches.");

        }
    }


    private void initializePlayMode() {
        this.outs = 0;
        this.hits = 0;
        this.currentPlayModePitcherIndex = 0;
        List<Pitcher> allPitchers = dbManager.getAllPitchers();
        if (allPitchers.size() < TARGET_HITS) {
            JOptionPane.showMessageDialog(this, "Not enough pitchers in database for Play Mode! Need at least " + TARGET_HITS + " pitchers.", "Error", JOptionPane.ERROR_MESSAGE);
            Main.showStartScreen(mainFrame);
            return;
        }
        Collections.shuffle(allPitchers);
        this.playModePitchers = allPitchers.subList(0, TARGET_HITS);
        this.currentPitcher = playModePitchers.get(currentPlayModePitcherIndex);
        loadPitcherPitchData(currentPitcher.getPid());
    }

    private Point project3D(double objX_ft, double objY_ft, double objZ_ft) {
        double deltaX = objX_ft - 0;
        double deltaY = objY_ft - cameraY_ft;
        double deltaZ = objZ_ft - cameraZ_ft;
        if (deltaZ <= 0.1) return null;
        double projectedX = (deltaX * focalLength) / deltaZ;
        double projectedY = (deltaY * focalLength) / deltaZ;
        int screenX = vanishingPointX + (int) projectedX;
        int screenY = vanishingPointY - (int) projectedY;
        return new Point(screenX, screenY);
    }
    
    private void calculateRealisticTrajectory(String type, double finalTargetX, double finalTargetY) {
        TrajectoryData data = pitchDatabase.get(type);
        if (data == null) {
            data = pitchDatabase.values().stream().findFirst().orElse(new TrajectoryData(0, 1, 33.0, -2.7, -13.5, -2.2, 5.8, 96.8));
        }
        this.pitchType = type;

        startX_ft = data.getRex();
        startY_ft = data.getRey();
        startZ_ft = PITCHER_MOUND_DISTANCE_FT;

        double releaseSpeed_fts = data.getSpeed() * 1.467;
        double pfx_x_ft = data.getHmov() / 12.0;
        double pfx_z_ft = data.getVmov() / 12.0;

        double flightTime = (startZ_ft - endZ_ft) / releaseSpeed_fts;

        ax = (2 * pfx_x_ft) / (flightTime * flightTime);
        ay = (2 * pfx_z_ft) / (flightTime * flightTime);

        double targetX, targetY;
        if (isHittingMode || isPlayMode) { // Chance of being a strike
            if (random.nextDouble() < 0.6) {
                targetX = strikeZoneLeft_ft + random.nextDouble() * (strikeZoneRight_ft - strikeZoneLeft_ft);
                targetY = strikeZoneBottom_ft + random.nextDouble() * (strikeZoneTop_ft - strikeZoneBottom_ft);
            } else {
                double margin = 0.5;
                targetX = (random.nextBoolean() ? 1 : -1) * (strikeZoneRight_ft + random.nextDouble() * margin);
                targetY = (strikeZoneBottom_ft - margin) + random.nextDouble() * (strikeZoneTop_ft - strikeZoneBottom_ft + 2 * margin);
            }
        } else {
            targetX = finalTargetX;
            targetY = finalTargetY;
        }

        vx = ((targetX - startX_ft) / flightTime) - (0.5 * ax * flightTime);
        vy = ((targetY - startY_ft) / flightTime) - (0.5 * ay * flightTime);
        vz = releaseSpeed_fts;

        ballTrajectoryPoints = new ArrayList<>();
        currentTrajectoryIndex = 0;

        double current_x = startX_ft, current_y = startY_ft, current_z = startZ_ft;
        double current_vx = vx, current_vy = vy;

        double simTimeStep = 0.005;
        while (current_z > endZ_ft - 0.1) {
            ballTrajectoryPoints.add(new Point3D(current_x, current_y, current_z));
            current_vx += ax * simTimeStep;
            current_vy += ay * simTimeStep;
            current_x += current_vx * simTimeStep;
            current_y += current_vy * simTimeStep;
            current_z -= vz * simTimeStep;
            if (ballTrajectoryPoints.size() > 2000) break;
        }

        Point3D finalSimulatedPoint;
        if (ballTrajectoryPoints.size() > 1) {
            Point3D prev = ballTrajectoryPoints.get(ballTrajectoryPoints.size() - 2);
            Point3D last = ballTrajectoryPoints.get(ballTrajectoryPoints.size() - 1);
            double frac = (prev.z - endZ_ft) / (prev.z - last.z);
            finalSimulatedPoint = new Point3D(prev.x + (last.x - prev.x) * frac, prev.y + (last.y - prev.y) * frac, endZ_ft);
        } else {
            finalSimulatedPoint = new Point3D(targetX, targetY, endZ_ft);
        }
        
        ballTrajectoryPoints.removeIf(p -> p.z < endZ_ft);
        ballTrajectoryPoints.add(finalSimulatedPoint);

        boolean isStrike = finalSimulatedPoint.x >= strikeZoneLeft_ft && finalSimulatedPoint.x <= strikeZoneRight_ft &&
                           finalSimulatedPoint.y >= strikeZoneBottom_ft && finalSimulatedPoint.y <= strikeZoneTop_ft;
        preCalculatedPitchResult = isStrike ? "Strike" : "Ball";

        x_ft = startX_ft; y_ft = startY_ft; z_ft = startZ_ft;
    }
    
    private void updateAimingSequence() {
        if (!isAimingSequenceActive) return;
        long elapsed = System.currentTimeMillis() - aimSequenceStartTime;
        if (elapsed > AIM_SHRINK_DURATION_MS) {
            aimingCircleRadius_ft = MAX_AIM_RADIUS_FT;
            finalizeAndThrowPitch();
        } else {
            double progress = (double) elapsed / AIM_SHRINK_DURATION_MS;
            aimingCircleRadius_ft = MAX_AIM_RADIUS_FT - (progress * (MAX_AIM_RADIUS_FT - MIN_AIM_RADIUS_FT));
        }
    }

    private void actionPerformed(ActionEvent e) {
        if (isPaused) { lastFrameTime = System.nanoTime(); return; }
        long currentTime = System.nanoTime();
        double frameTime = (currentTime - lastFrameTime) / 1_000_000_000.0;
        lastFrameTime = currentTime;
        frameTime = Math.min(frameTime, 0.05);

        if (isAimingSequenceActive) {
            updateAimingSequence();
        }

        if (isPrePitchTell) {
            prePitchTellCounter++;
            if (prePitchTellCounter > PRE_PITCH_TELL_DURATION) {
                isPrePitchTell = false;
                prePitchTellCounter = 0;
                startPitch(randomPitchType(), 0, 0);
            }
        } else if ((isHittingMode || isPlayMode) && !isPitching && hitResult == null) {
            if (countdown > 0) {
                countdown--;
            } else {
                isPrePitchTell = true;
                TrajectoryData data = pitchDatabase.values().stream().findFirst().orElse(new TrajectoryData(0,0,0,0,0, -2.0, 6.0, 90.0));
                prePitchBallPos = project3D(data.getRex(), data.getRey(), PITCHER_MOUND_DISTANCE_FT);
            }
        }

        if (isPitching && !ballReachedCatcher) {
            int stepsToAdvance = (int) (frameTime / 0.005);
            currentTrajectoryIndex = Math.min(currentTrajectoryIndex + stepsToAdvance, ballTrajectoryPoints.size() - 1);
            Point3D currentPoint = ballTrajectoryPoints.get(currentTrajectoryIndex);
            x_ft = currentPoint.x;
            y_ft = currentPoint.y;
            z_ft = currentPoint.z;

            if (currentTrajectoryIndex >= ballTrajectoryPoints.size() - 1) {
                isPitching = false;
                ballReachedCatcher = true;
                if ((isHittingMode || isPlayMode) && !swingAttempted) {
                    hitResult = preCalculatedPitchResult;
                    if (hitResult.equals("Strike")) {
                        currentStrikes++;
                        if (currentStrikes >= MAX_STRIKES) {
                            hitResult = "Strikeout!";
                            outs++;
                            checkPlayModeGameEnd();
                            resetAtBat();
                        }
                    } else {
                        currentBalls++;
                        if (currentBalls >= MAX_BALLS) {
                            hitResult = "Walk! (Hit)";
                            hits++;
                            resetAtBat();
                            changePlayModePitcher();
                        }
                    }
                }
            }
        }
        repaint();
    }

    private void resetPitch() {
        isPitching = ballReachedCatcher = swingAttempted = false;
        pitchType = "none";
        hitResult = null;
        if (isHittingMode || isPlayMode) { countdown = 180; }
        
        isAimingSequenceActive = false;
        selectedPitchType = "none";
        isPrePitchTell = false;
        prePitchTellCounter = 0;

        TrajectoryData data = pitchDatabase.isEmpty() ? new TrajectoryData(0,0,0,0,0, -2.0, 6.0, 90.0) : pitchDatabase.values().iterator().next();
        x_ft = data.getRex();
        y_ft = data.getRey();
        z_ft = PITCHER_MOUND_DISTANCE_FT;
        lastFrameTime = System.nanoTime();

        if (ballTrajectoryPoints != null) {
            ballTrajectoryPoints.clear();
        }
        currentTrajectoryIndex = 0;
        preCalculatedPitchResult = null;
        repaint();
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setColor(new Color(135, 206, 235));
        g2d.fillRect(0, 0, windowWidth, windowHeight);
        drawField(g2d);
        drawStrikeZone(g2d);
        drawPitcherMound(g2d);

        if (!isHittingMode && !isPitching) {
            if (isAimingSequenceActive) {
                drawAimingCircle(g2d);
            } else {
                drawAimingReticle(g2d);
            }
        }
        
        if (isPrePitchTell) {
            drawPrePitchTell(g2d);
        }
        
        drawUI(g2d);
        if (isPitching || ballReachedCatcher) {
            ballRenderer.drawBall(g2d, x_ft, y_ft, z_ft);
        }
        if (isPaused) {
            g2d.setColor(new Color(0, 0, 0, 150));
            g2d.fillRect(0, 0, windowWidth, windowHeight);
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 48));
            g2d.drawString("Paused", (windowWidth - g2d.getFontMetrics().stringWidth("Paused")) / 2, windowHeight / 2);
        }
    }
    
    // --- MODIFIED: Update flicker logic ---
    private void drawPrePitchTell(Graphics2D g2d) {
        if (prePitchBallPos == null) return;

        // Only draw the white ball during the "on" phase
        if (prePitchTellCounter < PRE_PITCH_TELL_ON_DURATION) {
            g2d.setColor(Color.WHITE);
            int ballSize = 8;
            g2d.fillOval(prePitchBallPos.x - ballSize / 2, prePitchBallPos.y - ballSize / 2, ballSize, ballSize);
        }
        // When the counter exceeds ON_DURATION, enter the "off" phase, no drawing needed here
    }

    private void resetAtBat() {
        currentStrikes = 0;
        currentBalls = 0;
    }

    private String randomPitchType() {
        if (pitchDatabase.isEmpty()) {
            return "4SEAMFAST";
        }
        Object[] types = pitchDatabase.keySet().toArray();
        return (String) types[random.nextInt(types.length)];
    }

    private void startPitch(String type, double targetX, double targetY) {
        if (!pitchDatabase.containsKey(type)) {
            System.err.println("Pitch type " + type + " not in current pitcher's arsenal.");
            return;
        }
        if (!isPitching && !isPaused) {
            this.isPitching = true;
            this.ballReachedCatcher = false;
            this.swingAttempted = false;
            this.hitResult = null;
            calculateRealisticTrajectory(type, targetX, targetY);
        }
    }
    
    private void finalizeAndThrowPitch() {
        isAimingSequenceActive = false;
        double angle = random.nextDouble() * 2 * Math.PI;
        double radius = Math.sqrt(random.nextDouble()) * aimingCircleRadius_ft; 
        double offsetX = Math.cos(angle) * radius;
        double offsetY = Math.sin(angle) * radius;
        double finalTargetX = lockedAimX_ft + offsetX;
        double finalTargetY = lockedAimY_ft + offsetY;
        startPitch(selectedPitchType, finalTargetX, finalTargetY);
        selectedPitchType = "none";
    }

    private void drawField(Graphics2D g2d) {
        Point groundStart = project3D(0, 0, PITCHER_MOUND_DISTANCE_FT);
        Point groundEnd = project3D(0, 0, 0);
        if(groundStart != null && groundEnd != null) {
            g2d.setColor(new Color(188, 143, 143));
            Polygon dirtArea = new Polygon();
            dirtArea.addPoint(groundStart.x - 200, groundStart.y);
            dirtArea.addPoint(groundStart.x + 200, groundStart.y);
            dirtArea.addPoint(windowWidth, groundEnd.y);
            dirtArea.addPoint(0, groundEnd.y);
            g2d.fillPolygon(dirtArea);
        }
    }

    private void drawStrikeZone(Graphics2D g2d) {
        g2d.setStroke(new BasicStroke(3));
        g2d.setColor(new Color(255, 255, 255, 100));
        Point tl = project3D(strikeZoneLeft_ft, strikeZoneTop_ft, endZ_ft);
        Point tr = project3D(strikeZoneRight_ft, strikeZoneTop_ft, endZ_ft);
        Point bl = project3D(strikeZoneLeft_ft, strikeZoneBottom_ft, endZ_ft);
        Point br = project3D(strikeZoneRight_ft, strikeZoneBottom_ft, endZ_ft);
        if (tl != null && tr != null && bl != null && br != null) {
            g2d.drawLine(tl.x, tl.y, tr.x, tr.y);
            g2d.drawLine(tr.x, tr.y, br.x, br.y);
            g2d.drawLine(br.x, br.y, bl.x, bl.y);
            g2d.drawLine(bl.x, bl.y, tl.x, tl.y);
        }
    }

    private void drawPitcherMound(Graphics2D g2d) {
        Point moundCenter = project3D(0, 0, PITCHER_MOUND_DISTANCE_FT);
        if (moundCenter != null) {
            int moundSize = 80;
            g2d.setColor(new Color(160, 82, 45));
            g2d.fillOval(moundCenter.x - moundSize/2, moundCenter.y - moundSize/4, moundSize, moundSize/2);
        }
    }

    private Color getHitResultColor(String result) {
        if (result == null) return Color.RED;
        if (result.contains("Hit")) return Color.CYAN;
        switch (result) {
            case "Perfect": return Color.CYAN;
            case "A bit early": case "A bit late": return Color.GREEN;
            case "Early": case "Late": return Color.ORANGE;
            case "Strike": return Color.YELLOW;
            case "Ball": return Color.MAGENTA;
            case "Miss": return Color.RED;
            case "Strikeout!": return Color.RED;
            case "Walk! (Hit)": return Color.BLUE;
            case "Swing Strike!": return Color.YELLOW;
            default: return Color.RED;
        }
    }

    private void drawUI(Graphics2D g2d) {
        g2d.setColor(new Color(0, 0, 0, 150));
        g2d.fillRoundRect(10, 10, 500, 160, 10, 10);
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 16));
        String modeText = isPlayMode ? "Play Mode" : (isHittingMode ? "Hitting Mode" : "Pitching Mode");
        g2d.drawString(modeText + " - " + (currentPitcher != null ? currentPitcher.getPname() : "N/A"), 20, 30);

        if (isHittingMode || isPlayMode) {
            g2d.setFont(new Font("Arial", Font.PLAIN, 12));
            g2d.drawString("Space=Swing | N=Next Pitch | M=Menu | ESC=Pause", 20, 50);
            if (isPlayMode) {
                g2d.drawString("Hits: " + hits + "/" + TARGET_HITS + " | Outs: " + outs + "/" + MAX_OUTS, 20, 70);
            }
            g2d.setFont(new Font("Arial", Font.BOLD, 20));
            g2d.drawString("S: " + currentStrikes + " B: " + currentBalls, 20, 90);
            
            if (countdown > 0 && !isPrePitchTell) {
                g2d.setFont(new Font("Arial", Font.BOLD, 24));
                g2d.setColor(Color.YELLOW);
                g2d.drawString("Pitch in: " + ((countdown / 60) + 1), windowWidth / 2 - 60, 60);
            } else if (isPrePitchTell) {
                // You can add a text cue here if you want, e.g., an exclamation mark
                // g2d.setFont(new Font("Arial", Font.BOLD, 36));
                // g2d.setColor(Color.RED);
                // g2d.drawString("!", windowWidth / 2 - 8, 60);
            } else if (hitResult != null) {
                g2d.setFont(new Font("Arial", Font.BOLD, 36));
                g2d.setColor(getHitResultColor(hitResult));
                g2d.drawString(hitResult, windowWidth / 2 - g2d.getFontMetrics().stringWidth(hitResult)/2, 80);
            }
            if (isPitching && !pitchType.equals("none")) {
                g2d.setFont(new Font("Arial", Font.BOLD, 24));
                g2d.setColor(Color.WHITE);
                g2d.drawString(pitchType, 20, 140);
            }
        } else {
            g2d.setFont(new Font("Arial", Font.PLAIN, 11));
            String pitchList = "Pitches: ";
            Object[] availablePitches = pitchDatabase.keySet().toArray();
            for(int i = 0; i < availablePitches.length; i++) {
                pitchList += (i + 1) + "=" + availablePitches[i] + " ";
            }
            g2d.drawString(pitchList, 20, 50);
            g2d.setFont(new Font("Arial", Font.PLAIN, 12));
            g2d.drawString("M=Menu | C=Change Pitcher | R=Reset | ESC=Pause", 20, 70);

            String currentPitchSpeed = "---";
            if (!selectedPitchType.equals("none")) {
                currentPitchSpeed = String.format("%.1f mph", pitchDatabase.get(selectedPitchType).getSpeed());
            } else if (!pitchType.equals("none")) {
                currentPitchSpeed = String.format("%.1f mph", pitchDatabase.get(pitchType).getSpeed());
            }
            g2d.drawString("Speed: " + currentPitchSpeed, 20, 90);

            g2d.setFont(new Font("Arial", Font.BOLD, 18));
            if(isPitching) {
                g2d.setColor(Color.GREEN);
                g2d.drawString("Pitching: " + pitchType, 20, 110);
            } else if(ballReachedCatcher) {
                g2d.setColor(Color.ORANGE);
                g2d.drawString("Ball reached catcher. Select a pitch.", 20, 110);
            } else if (isAimingSequenceActive) {
                g2d.setColor(Color.CYAN);
                g2d.drawString("Press SPACE to throw!", 20, 110);
            } else if (!selectedPitchType.equals("none")) {
                g2d.setColor(Color.YELLOW);
                g2d.drawString("Selected: " + selectedPitchType + ". Press SPACE to aim.", 20, 110);
            } else {
                 g2d.setColor(Color.WHITE);
                g2d.drawString("Select Pitch (1-" + availablePitches.length + "), then Aim.", 20, 110);
            }
        }
    }
    
    private void updateAimPosition() {
        if (mousePos == null) return;
        double deltaZ = endZ_ft - cameraZ_ft;
        aimX_ft = (mousePos.x - vanishingPointX) * deltaZ / focalLength;
        aimY_ft = cameraY_ft - (mousePos.y - vanishingPointY) * deltaZ / focalLength;
        
        double worldTargetMinX = -2.5, worldTargetMaxX = 2.5;
        double worldTargetMinY = 0.0, worldTargetMaxY = 5.0;

        aimX_ft = Math.max(worldTargetMinX, Math.min(worldTargetMaxX, aimX_ft));
        aimY_ft = Math.max(worldTargetMinY, Math.min(worldTargetMaxY, aimY_ft));
    }

    private void drawAimingReticle(Graphics2D g2d) {
        Point center = project3D(aimX_ft, aimY_ft, endZ_ft);
        if (center == null) return;
        Point edge = project3D(aimX_ft + MIN_AIM_RADIUS_FT, aimY_ft, endZ_ft);
        if (edge == null) return;
        double screenRadius = center.distance(edge);
        g2d.setColor(new Color(255, 0, 0, 180));
        g2d.setStroke(new BasicStroke(2));
        g2d.draw(new Ellipse2D.Double(center.x - screenRadius, center.y - screenRadius, screenRadius * 2, screenRadius * 2));
    }
    
    private void drawAimingCircle(Graphics2D g2d) {
        Point center = project3D(lockedAimX_ft, lockedAimY_ft, endZ_ft);
        if (center == null) return;
        Point outerEdge = project3D(lockedAimX_ft + aimingCircleRadius_ft, lockedAimY_ft, endZ_ft);
        if (outerEdge == null) return;
        double outerScreenRadius = center.distance(outerEdge);
        g2d.setColor(new Color(255, 255, 255, 180));
        g2d.setStroke(new BasicStroke(3));
        g2d.draw(new Ellipse2D.Double(center.x - outerScreenRadius, center.y - outerScreenRadius, outerScreenRadius * 2, outerScreenRadius * 2));
        Point innerEdge = project3D(lockedAimX_ft + MIN_AIM_RADIUS_FT, lockedAimY_ft, endZ_ft);
        if (innerEdge == null) return;
        double innerScreenRadius = center.distance(innerEdge);
        g2d.setColor(new Color(0, 255, 255, 200));
        g2d.setStroke(new BasicStroke(2));
        g2d.draw(new Ellipse2D.Double(center.x - innerScreenRadius, center.y - innerScreenRadius, innerScreenRadius * 2, innerScreenRadius * 2));
    }

    private void setupKeyBindings() {
        InputMap inputMap = this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = this.getActionMap();

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0, false), "spacebarAction");
        actionMap.put("spacebarAction", new SpacebarAction());

        for (int i = 1; i <= 9; i++) {
            inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_0 + i, 0), "pitchAction" + i);
            actionMap.put("pitchAction" + i, new PitchSelectAction(i));
        }

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_R, 0), "resetAction");
        actionMap.put("resetAction", new ResetAction());

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_M, 0), "menuAction");
        actionMap.put("menuAction", new MenuAction());

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "pauseAction");
        actionMap.put("pauseAction", new PauseAction());

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, 0), "changePitcherAction");
        actionMap.put("changePitcherAction", new ChangePitcherAction());

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_N, 0, false), "nextPitchAction");
        actionMap.put("nextPitchAction", new NextPitchAction());
    }

    private void togglePause() {
        isPaused = !isPaused;
        repaint();
    }

    private abstract class GameAction extends AbstractAction { }

    private class SpacebarAction extends GameAction {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (isHittingMode || isPlayMode) {
                if (isPitching && !swingAttempted) {
                    swingAttempted = true;
                    double swingTimeRatio = (startZ_ft - z_ft) / (startZ_ft - endZ_ft);
                    final double PERFECT_START = 0.91, PERFECT_END = 0.97;
                    final double GOOD_START = 0.86, GOOD_END = 1.0;
                    final double OK_START = 0.82, OK_END = 1.04;
                    boolean isHit = false;

                    if (swingTimeRatio >= PERFECT_START && swingTimeRatio <= PERFECT_END) {
                        hitResult = "Perfect";
                        // Strike/Ball judgment
                        if ("Strike".equals(preCalculatedPitchResult)) { // If it's a strike
                            isHit = calculateHitProbability(0.6);
                        } else { // If it's a ball
                            isHit = calculateHitProbability(0.1); // Judge based on TimeProbability
                        }
                    } else if (swingTimeRatio > PERFECT_END && swingTimeRatio <= GOOD_END) {
                        hitResult = "A bit late";
                        if ("Strike".equals(preCalculatedPitchResult)) { // If it's a strike
                            isHit = calculateHitProbability(0.2);
                        } else { // If it's a ball
                            isHit = calculateHitProbability(0.05); // Judge based on TimeProbability
                        }
                    } else if (swingTimeRatio < PERFECT_START && swingTimeRatio >= GOOD_START) {
                        hitResult = "A bit early";
                        if ("Strike".equals(preCalculatedPitchResult)) { // If it's a strike
                            isHit = calculateHitProbability(0.2);
                        } else { // If it's a ball
                            isHit = calculateHitProbability(0.05); // Judge based on TimeProbability
                        }
                    } else if (swingTimeRatio > GOOD_END && swingTimeRatio <= OK_END) {
                        hitResult = "Late";
                        if ("Strike".equals(preCalculatedPitchResult)) { // If it's a strike
                            isHit = calculateHitProbability(0.1);
                        } else { // If it's a ball
                            isHit = calculateHitProbability(0); // Judge based on TimeProbability
                        }
                    } else if (swingTimeRatio < GOOD_START && swingTimeRatio >= OK_START) {
                        hitResult = "Early";
                        if ("Strike".equals(preCalculatedPitchResult)) { // If it's a strike
                            isHit = calculateHitProbability(0.1);
                        } else { // If it's a ball
                            isHit = calculateHitProbability(0); // Judge based on TimeProbability
                        }
                    } else {
                        hitResult = "Too Early";
                        isHit = false;
                    }

                    if (isHit) {
                        hitResult = "Hit! " + hitResult;
                        hits++;
                        resetAtBat();
                        if (isPlayMode) {
                            changePlayModePitcher();
                        }
                    } else {
                        currentStrikes++;
                        currentHitresult = hitResult ;
                        hitResult = currentHitresult +", Swing Strike!";

                        // Using isHit to calculate if it's an out
                        isHit = calculateHitProbability(0.1);
                        if(isHit){ // isOut
                        currentStrikes = 3;
                        {
                            hitResult = currentHitresult  +", In Play OUT !";
                            if (isPlayMode) {
                                outs++;
                                checkPlayModeGameEnd();
                            }
                            resetAtBat();
                        } 
                        }
                        else if (currentStrikes >= MAX_STRIKES) {
                            hitResult = currentHitresult +", Strikeout!";
                            if (isPlayMode) {
                                outs++;
                                checkPlayModeGameEnd();
                            }
                            resetAtBat();
                        } 
                    }
                }
            } else {
                if (isAimingSequenceActive) {
                    finalizeAndThrowPitch();
                } else if (!isPitching && !selectedPitchType.equals("none")) {
                    isAimingSequenceActive = true;
                    aimSequenceStartTime = System.currentTimeMillis();
                    aimingCircleRadius_ft = MAX_AIM_RADIUS_FT;
                    lockedAimX_ft = aimX_ft;
                    lockedAimY_ft = aimY_ft;
                }
            }
        }
    }

    private boolean calculateHitProbability(double TimeProbability) {
        final int BATTER_POWER = 90;
        final int BATTER_ACCURACY = 90;
        int pitcherStuff = (currentPitcher != null) ? currentPitcher.getStuff() : 50;
        int pitcherVelocity = (currentPitcher != null) ? currentPitcher.getVelocity() : 90;

        double currentPitchSpeed = pitchDatabase.containsKey(pitchType) ? 
                                   pitchDatabase.get(pitchType).getSpeed() : pitcherVelocity;

        double hitProbability = TimeProbability + ((BATTER_POWER - pitcherStuff) + (BATTER_ACCURACY - currentPitchSpeed)) * 0.005;
        hitProbability = Math.max(0.05, Math.min(0.95, hitProbability));
        
        return random.nextDouble() < hitProbability;
    }

    private void changePlayModePitcher() {
        if (!isPlayMode) return;
        currentPlayModePitcherIndex++;
        if (currentPlayModePitcherIndex < playModePitchers.size()) {
            currentPitcher = playModePitchers.get(currentPlayModePitcherIndex);
            loadPitcherPitchData(currentPitcher.getPid());
            resetAtBat();
        } else {
            showGameResult("Victory!");
        }
    }

    private void checkPlayModeGameEnd() {
        if (isPlayMode && outs >= MAX_OUTS) {
            showGameResult("Defeat!");
        }
    }

    private void showGameResult(String message) {
        timer.stop();
        JOptionPane.showMessageDialog(this, message + "\nOuts: " + outs + "/" + MAX_OUTS + "\nHits: " + hits + "/" + TARGET_HITS, "Game Over", JOptionPane.INFORMATION_MESSAGE);
        Main.showStartScreen(mainFrame);
    }

    private class PitchSelectAction extends GameAction {
        private int pitchNumber;
        public PitchSelectAction(int number) { this.pitchNumber = number; }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            if (!isHittingMode && !isPlayMode && !isPitching && !isAimingSequenceActive) {
                String[] availablePitches = pitchDatabase.keySet().toArray(new String[0]);
                if (pitchNumber >= 1 && pitchNumber <= availablePitches.length) {
                    selectedPitchType = availablePitches[pitchNumber - 1];
                    pitchType = "none";
                    ballReachedCatcher = false;
                } else {
                    System.out.println("Invalid pitch selection.");
                }
            }
        }
    }

    private class ResetAction extends GameAction {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (!isHittingMode && !isPlayMode) {
                resetPitch();
            }
        }
    }

    private class MenuAction extends GameAction {
        @Override
        public void actionPerformed(ActionEvent e) {
            timer.stop();
            Main.showStartScreen(mainFrame);
        }
    }

    private class PauseAction extends GameAction {
        @Override
        public void actionPerformed(ActionEvent e) {
            togglePause();
        }
    }

    private class ChangePitcherAction extends GameAction {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (!isHittingMode && !isPlayMode) {
                timer.stop();
                Main.showPitchSelectionScreen(mainFrame, false);
            }
        }
    }

    private class NextPitchAction extends GameAction {
        @Override
        public void actionPerformed(ActionEvent e) {
             if ((isHittingMode || isPlayMode) && hitResult != null ) {
                resetPitch();
                if (isHittingMode || isPlayMode) { countdown = 180; }
            }
        }
    }
}