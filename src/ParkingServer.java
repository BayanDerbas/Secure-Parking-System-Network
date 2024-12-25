import java.io.*;
import java.net.*;
import java.sql.*;
import org.mindrot.jbcrypt.BCrypt;

public class ParkingServer {
    private static final int PORT = 3000;
    private static final String DB_URL = "jdbc:sqlite:parking_system.db";

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server is listening on port " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("A client has connected from " + clientSocket.getInetAddress());
                new ClientHandler(clientSocket).start();
            }
        } catch (IOException e) {
            System.err.println("Server exception: " + e.getMessage());
        }
    }

    private static boolean registerUser(String fullName, String userType, String phoneNumber, String carPlate, String passwordHash) {
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

    private static boolean loginUser(String fullName, String password) {
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
    private static class ClientHandler extends Thread {
        private Socket clientSocket;
        private PrintWriter out;
        private BufferedReader in;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        public void run() {
            try {
                System.out.println("Client connected: " + clientSocket.getInetAddress());

                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                out = new PrintWriter(clientSocket.getOutputStream(), true);

                String operation;
                while ((operation = in.readLine()) != null) {
                    System.out.println("Operation received: " + operation);

                    if ("register".equals(operation)) {
                        handleRegister();
                    } else if ("login".equals(operation)) {
                        handleLogin();
                    } else if ("view_available_spots".equals(operation)) {
                        viewAvailableParkingSpots();
                    } else if ("reserve_spot".equals(operation)) {
                        handleReserveSpot();
                    } else {
                        out.println("Invalid operation.");
                    }
                }

            } catch (IOException e) {
                System.err.println("Error in client handler: " + e.getMessage());
            } finally {
                try {
                    if (clientSocket != null && !clientSocket.isClosed()) {
                        clientSocket.close();
                        System.out.println("Client socket closed.");
                    }
                } catch (IOException e) {
                    System.err.println("Error closing socket: " + e.getMessage());
                }
            }
        }

        // Handle registration
        private void handleRegister() throws IOException {
            String fullName = in.readLine();
            String userType = in.readLine();
            String phoneNumber = in.readLine();
            String carPlate = in.readLine();
            String password = in.readLine();

            // Hash password before storing it
            String passwordHash = BCrypt.hashpw(password, BCrypt.gensalt());

            // Register the user in the database
            boolean isRegistered = registerUser(fullName, userType, phoneNumber, carPlate, passwordHash);
            out.println(isRegistered ? "User registered successfully!" : "Registration failed!");
        }

        // Handle login
        private void handleLogin() throws IOException {
            String fullName = in.readLine();
            String password = in.readLine();
            boolean isLoggedIn = loginUser(fullName, password);
            out.println(isLoggedIn ? "Login successful!" : "Login failed!");
        }

        // View available parking spots
        private void viewAvailableParkingSpots() throws IOException {
            String availableSpots = getAvailableParkingSpots();
            out.println(availableSpots);
        }

        // Handle reservation
        private void handleReserveSpot() throws IOException {
            int spotNumber = Integer.parseInt(in.readLine());
            String reservedAt = in.readLine();

            if (reserveParkingSpot(spotNumber, reservedAt)) {
                out.println("Reservation successful!");
            } else {
                out.println("Parking spot is already reserved.");
            }
        }

        private static String getAvailableParkingSpots() {
            StringBuilder availableSpots = new StringBuilder();
            String sql = "SELECT spot_number FROM parking_spots WHERE is_reserved = 0";  // 0 تعني المواقف المتاحة

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

        private static boolean reserveParkingSpot(int spotNumber, String reservedAt) {
            // التحقق إذا كان الموقف محجوزًا في الوقت المحدد
            String checkAvailability = "SELECT is_reserved, reserved_at FROM parking_spots WHERE spot_number = ?";
            try (Connection conn = DriverManager.getConnection(DB_URL);
                 PreparedStatement checkStmt = conn.prepareStatement(checkAvailability)) {
                checkStmt.setInt(1, spotNumber);
                ResultSet rs = checkStmt.executeQuery();

                if (rs.next()) {
                    boolean isReserved = rs.getBoolean("is_reserved");
                    String existingReservedAt = rs.getString("reserved_at");

                    // السماح بحجز نفس الموقف في وقت مختلف
                    if (isReserved && existingReservedAt.equals(reservedAt)) {
                        return false; // إذا كان الموقف محجوزًا في الوقت المحدد
                    }
                }

                // الحصول على ID المستخدم بناءً على اسمه
                String userIdQuery = "SELECT id FROM users WHERE full_name = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(userIdQuery)) {
                    pstmt.setString(1, "bayan");  // هنا يجب الحصول على اسم المستخدم المُسجل
                    ResultSet userRs = pstmt.executeQuery();
                    if (userRs.next()) {
                        int userId = userRs.getInt("id");

                        // تحديث الموقف في قاعدة البيانات
                        String updateSql = "UPDATE parking_spots SET is_reserved = 1, reserved_at = ?, reserved_by = ? WHERE spot_number = ? AND is_reserved = 0";
                        try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                            updateStmt.setString(1, reservedAt);
                            updateStmt.setInt(2, userId);
                            updateStmt.setInt(3, spotNumber);

                            int rowsUpdated = updateStmt.executeUpdate();
                            return rowsUpdated > 0;
                        }
                    }
                }
            } catch (SQLException e) {
                System.err.println("Error reserving parking spot: " + e.getMessage());
            }

            return false;
        }
    }
}
