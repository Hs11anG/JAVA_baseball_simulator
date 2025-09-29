import data.Pitcher;
import data.BallType;
import data.TrajectoryData;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DatabaseManager {
    private static final String JDBC_URL = "jdbc:mysql://localhost:3306/BASEBALLJAVAGAME?useSSL=false&serverTimezone=UTC";
    private static final String DB_USER = "root"; // Replace with your MySQL username
    private static final String DB_PASSWORD = "0000"; // Replace with your MySQL password

    // Load JDBC Driver
    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("MySQL JDBC Driver not found! Make sure mysql-connector-j-x.x.x.jar is in your classpath.");
            e.printStackTrace();
        }
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(JDBC_URL, DB_USER, DB_PASSWORD);
    }

    public List<Pitcher> getAllPitchers() {
        List<Pitcher> pitchers = new ArrayList<>();
        String sql = "SELECT PID, TID, PNAME, YEAR, LR, STUFF, VELOCITY, PTYPE FROM PITCHER";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                pitchers.add(new Pitcher(
                    rs.getInt("PID"),
                    rs.getInt("TID"),
                    rs.getString("PNAME"),
                    rs.getInt("YEAR"),
                    rs.getString("LR"),
                    rs.getInt("STUFF"),
                    rs.getInt("VELOCITY"),
                    rs.getInt("PTYPE")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching pitchers: " + e.getMessage());
            e.printStackTrace();
        }
        return pitchers;
    }

    public Map<String, TrajectoryData> getPitchDataForPitcher(int pitcherId) {
        Map<String, TrajectoryData> pitcherPitchData = new HashMap<>();
        // Query TRAJECTORY and BALLTYPE tables
        String sql = "SELECT T.PID, T.BID, T.USEP, T.HMOV, T.VMOV, T.REX, T.REY, T.SPEED, B.BNAME " +
                     "FROM TRAJECTORY T JOIN BALLTYPE B ON T.BID = B.BID " +
                     "WHERE T.PID = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, pitcherId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String bname = rs.getString("BNAME");
                    TrajectoryData data = new TrajectoryData(
                        rs.getInt("PID"),
                        rs.getInt("BID"),
                        rs.getDouble("USEP"),
                        rs.getDouble("HMOV"),
                        rs.getDouble("VMOV"),
                        rs.getDouble("REX"),
                        rs.getDouble("REY"),
                        rs.getDouble("SPEED")
                    );
                    pitcherPitchData.put(bname, data);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching pitch data for pitcher " + pitcherId + ": " + e.getMessage());
            e.printStackTrace();
        }
        return pitcherPitchData;
    }
}