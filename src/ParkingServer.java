import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.concurrent.*;
import org.mindrot.jbcrypt.BCrypt;

public class ParkingServer {
    private static final int PORT = 3000;
    private static final String DB_URL = "jdbc:sqlite:parking_system.db";
    private static final int MAX_THREADS = 100; // تحديد الحد الأقصى للخيوط

    public static void main(String[] args) {
        ExecutorService threadPool = Executors.newFixedThreadPool(MAX_THREADS); // ThreadPool ثابت

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server is listening on port " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("A client has connected from " + clientSocket.getInetAddress());

                // تشغيل معالج العميل في خيط ضمن الـ ThreadPool
                threadPool.execute(new ClientHandler(clientSocket));
            }
        } catch (IOException e) {
            System.err.println("Server exception: " + e.getMessage());
        } finally {
            threadPool.shutdown();
        }
    }

    private static class ClientHandler implements Runnable {
        private Socket clientSocket;
        private PrintWriter out;
        private BufferedReader in;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                out = new PrintWriter(clientSocket.getOutputStream(), true);

                String operation;
                while ((operation = in.readLine()) != null) {
                    System.out.println("Operation received: " + operation);

                    switch (operation) {
                        case "register":
                            handleRegister();
                            break;
                        case "login":
                            handleLogin();
                            break;
                        case "view_available_spots":
                            viewAvailableParkingSpots();
                            break;
                        case "reserve_spot":
                            handleReserveSpot();
                            break;
                        default:
                            out.println("Invalid operation.");
                            break;
                    }
                }
            } catch (IOException e) {
                System.err.println("Error in client handler: " + e.getMessage());
            } finally {
                closeResources();
            }
        }

        private void closeResources() {
            try {
                if (clientSocket != null && !clientSocket.isClosed()) {
                    clientSocket.close();
                }
                if (in != null) in.close();
                if (out != null) out.close();
                System.out.println("Client resources closed.");
            } catch (IOException e) {
                System.err.println("Error closing resources: " + e.getMessage());
            }
        }

        private void handleRegister() throws IOException {
            String fullName = in.readLine();
            String userType = in.readLine();
            String phoneNumber = in.readLine();
            String carPlate = in.readLine();
            String password = in.readLine();

            String passwordHash = BCrypt.hashpw(password, BCrypt.gensalt());
            boolean isRegistered = registerUser(fullName, userType, phoneNumber, carPlate, passwordHash);
            out.println(isRegistered ? "User registered successfully!" : "Registration failed!");
        }

        private void handleLogin() throws IOException {
            String fullName = in.readLine();
            String password = in.readLine();
            boolean isLoggedIn = loginUser(fullName, password);
            out.println(isLoggedIn ? "Login successful!" : "Login failed!");
        }

        private void viewAvailableParkingSpots() throws IOException {
            String availableSpots = getAvailableParkingSpots();
            out.println(availableSpots);
        }

        private void handleReserveSpot() throws IOException {
            int spotNumber = Integer.parseInt(in.readLine());
            String reservedAt = in.readLine();

            if (reserveParkingSpot(spotNumber, reservedAt)) {
                out.println("Reservation successful!");
            } else {
                out.println("Parking spot is already reserved.");
            }
        }

        private boolean registerUser(String fullName, String userType, String phoneNumber, String carPlate, String passwordHash) {
            String sql = "INSERT INTO users (full_name, user_type, phone_number, car_plate, password_hash) VALUES (?, ?, ?, ?, ?)";
            try (Connection conn = DriverManager.getConnection(DB_URL);
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, fullName);
                pstmt.setString(2, userType);
                pstmt.setString(3, phoneNumber);
                pstmt.setString(4, carPlate);
                pstmt.setString(5, passwordHash);
                pstmt.executeUpdate();
                return true;
            } catch (SQLException e) {
                System.err.println("Error registering user: " + e.getMessage());
                return false;
            }
        }

        private boolean loginUser(String fullName, String password) {
            String sql = "SELECT password_hash FROM users WHERE full_name = ?";
            try (Connection conn = DriverManager.getConnection(DB_URL);
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, fullName);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    return BCrypt.checkpw(password, rs.getString("password_hash"));
                }
            } catch (SQLException e) {
                System.err.println("Error during login: " + e.getMessage());
            }
            return false;
        }

        private String getAvailableParkingSpots() {
            StringBuilder availableSpots = new StringBuilder();
            String sql = "SELECT spot_number FROM parking_spots WHERE is_reserved = 0";

            try (Connection conn = DriverManager.getConnection(DB_URL);
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    int spotNumber = rs.getInt("spot_number");
                    availableSpots.append(spotNumber).append(". Spot ").append(spotNumber).append("\n");
                }
            } catch (SQLException e) {
                System.err.println("Error fetching available spots: " + e.getMessage());
            }

            return availableSpots.length() > 0 ? availableSpots.toString() : "No available spots.";
        }

        private boolean reserveParkingSpot(int spotNumber, String reservedAt) {
            String checkAvailability = "SELECT is_reserved FROM parking_spots WHERE spot_number = ?";
            try (Connection conn = DriverManager.getConnection(DB_URL);
                 PreparedStatement checkStmt = conn.prepareStatement(checkAvailability)) {
                checkStmt.setInt(1, spotNumber);
                ResultSet rs = checkStmt.executeQuery();
                if (rs.next() && rs.getBoolean("is_reserved")) {
                    return false;
                }

                String updateSql = "UPDATE parking_spots SET is_reserved = 1, reserved_at = ? WHERE spot_number = ?";
                try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                    updateStmt.setString(1, reservedAt);
                    updateStmt.setInt(2, spotNumber);
                    return updateStmt.executeUpdate() > 0;
                }
            } catch (SQLException e) {
                System.err.println("Error reserving parking spot: " + e.getMessage());
            }
            return false;
        }
    }
}