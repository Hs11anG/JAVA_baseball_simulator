import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class BallRenderer {
    private BufferedImage ballImage;
    private final double focalLength;
    private final int vanishingPointX;
    private final int vanishingPointY;
    private final double cameraY_ft;
    private final double cameraZ_ft;

    public BallRenderer(int windowWidth, int windowHeight) {
        this.focalLength = 700; // Consistent with GamePanel
        this.vanishingPointX = windowWidth / 2; // Consistent with GamePanel
        this.vanishingPointY = windowHeight / 2; // Consistent with GamePanel
        this.cameraY_ft = 2.5; // Consistent with GamePanel
        this.cameraZ_ft = -4.0; // Consistent with GamePanel

        try {
            ballImage = ImageIO.read(new File("src/ball.png")); // Ensure ball.png is in the src folder
        } catch (IOException e) {
            System.err.println("Failed to load ball image in BallRenderer: " + e.getMessage());
            // Can set a default error image or flag
        }
    }

    // Method to project 3D coordinates to 2D screen (copied from GamePanel)
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

    // Method to calculate ball size on screen (copied from GamePanel)
    private int calculateBallSize(double objZ_ft) {
        double deltaZ = objZ_ft - cameraZ_ft;
        if (deltaZ <= 0) return 0;
        double visualSize = (0.24 * focalLength) / deltaZ;
        return Math.max(2, (int) visualSize);
    }

    // Method to draw the ball
    public void drawBall(Graphics2D g2d, double x_ft, double y_ft, double z_ft) {
        Point ballPos = project3D(x_ft, y_ft, z_ft);
        if (ballPos == null) return;
        int ballSize = calculateBallSize(z_ft);
        if (ballImage != null) {
            g2d.drawImage(ballImage, ballPos.x - ballSize / 2, ballPos.y - ballSize / 2, ballSize, ballSize, null);
        } else {
            g2d.setColor(Color.WHITE);
            g2d.fillOval(ballPos.x - ballSize / 2, ballPos.y - ballSize / 2, ballSize, ballSize);
        }
    }
}