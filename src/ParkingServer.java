import java.io.*;
import java.net.*;
import java.sql.*;

public class ParkingServer {
    private static final int PORT = 3000;
    private static final String DB_URL = "jdbc:sqlite:parking_system.db";

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server is listening on port " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("A client has connected from " + clientSocket.getInetAddress());
                new Thread(new ClientHandler(clientSocket)).start();
            }
        } catch (IOException e) {
            System.err.println("Server exception: " + e.getMessage());
        }
    }

    private static class ClientHandler implements Runnable {
        private Socket clientSocket;
        private PrintWriter out;
        private BufferedReader in;
        private String currentUser; // المستخدم الحالي للجلسة

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

            boolean isRegistered = registerUser(fullName, userType, phoneNumber, carPlate, password);
            out.println(isRegistered ? "User registered successfully!" : "Registration failed!");
        }

        private void handleLogin() throws IOException {
            String fullName = in.readLine();
            String password = in.readLine();
            boolean isLoggedIn = loginUser(fullName, password);
            out.println(isLoggedIn ? "Login successful!" : "Login failed!");

            if (isLoggedIn) {
                this.currentUser = fullName; // حفظ اسم المستخدم الحالي
            }
        }

        private boolean registerUser(String fullName, String userType, String phoneNumber, String carPlate, String password) {
            String sql = "INSERT INTO users (full_name, user_type, phone_number, car_plate, password) VALUES (?, ?, ?, ?, ?)";
            try (Connection conn = DriverManager.getConnection(DB_URL);
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, fullName);
                pstmt.setString(2, userType);
                pstmt.setString(3, phoneNumber);
                pstmt.setString(4, carPlate);
                pstmt.setString(5, password);
                pstmt.executeUpdate();
                return true;
            } catch (SQLException e) {
                System.err.println("Error registering user: " + e.getMessage());
                return false;
            }
        }

        private boolean loginUser(String fullName, String password) {
            String sql = "SELECT password FROM users WHERE full_name = ?";
            try (Connection conn = DriverManager.getConnection(DB_URL);
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, fullName);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    return rs.getString("password").equals(password);
                }
            } catch (SQLException e) {
                System.err.println("Error during login: " + e.getMessage());
            }
            return false;
        }

        private void viewAvailableParkingSpots() throws IOException {
            String availableSpots = getAvailableParkingSpots();
            out.println(availableSpots); // إرسال جميع المواقف
            out.println("END_OF_SPOTS"); // إشارة نهاية
        }

        private void handleReserveSpot() throws IOException {
            String availableSpots = getAvailableParkingSpots();

            out.println(availableSpots);
            out.println("END_OF_SPOTS"); // إشارة نهاية

            if (availableSpots.equals("No parking spots available.")) {
                return; // خروج مبكر إذا لم تكن هناك مواقف
            }

            try {
                int spotNumber = Integer.parseInt(in.readLine());
                String startTime = in.readLine();
                String endTime = in.readLine();

                if (reserveParkingSpot(spotNumber, startTime, endTime)) {
                    out.println("Reservation successful!");
                } else {
                    out.println("The spot is already reserved during the specified time.");
                }
            } catch (NumberFormatException e) {
                out.println("Invalid spot number. Please try again.");
            }
        }

        private String getAvailableParkingSpots() {
            StringBuilder spots = new StringBuilder();
            String sql = """
        SELECT spot_number FROM parking_spots 
        WHERE spot_number NOT IN (
            SELECT parking_spot_id FROM reservations
            WHERE (reserved_at BETWEEN ? AND ?)
            OR (reserved_until BETWEEN ? AND ?)
            OR (? BETWEEN reserved_at AND reserved_until)
            OR (? BETWEEN reserved_at AND reserved_until)
        )
    """;

            try (Connection conn = DriverManager.getConnection(DB_URL);
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                // تمرير التاريخ الحالي لجلب المواقف المتاحة
                String currentTime = getCurrentTime(); // حدد الوقت الحالي
                stmt.setString(1, currentTime); // بداية الوقت
                stmt.setString(2, currentTime); // نهاية الوقت
                stmt.setString(3, currentTime); // بداية الوقت
                stmt.setString(4, currentTime); // نهاية الوقت
                stmt.setString(5, currentTime); // بداية الوقت
                stmt.setString(6, currentTime); // نهاية الوقت

                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    int spotNumber = rs.getInt("spot_number");
                    spots.append(spotNumber).append(". Spot ").append(spotNumber).append("\n");
                }
            } catch (SQLException e) {
                System.err.println("Error fetching parking spots: " + e.getMessage());
            }

            System.out.println("Available spots to send: " + spots.toString()); // طباعة البيانات قبل الإرسال
            return spots.length() > 0 ? spots.toString().trim() : "No parking spots available.";
        }

        private String getCurrentTime() {
            // هنا يمكنك استخدام تاريخ الوقت الحالي بالصيغ المناسبة حسب قاعدة البيانات أو كما ترغب
            return java.time.LocalDateTime.now().toString(); // تعديل على الصيغة إذا لزم الأمر
        }

        private boolean reserveParkingSpot(int spotNumber, String startTime, String endTime) {
            String checkOverlap = """
        SELECT 1 
        FROM reservations 
        WHERE parking_spot_id = (SELECT id FROM parking_spots WHERE spot_number = ?)
          AND ((? BETWEEN reserved_at AND reserved_until) 
           OR (? BETWEEN reserved_at AND reserved_until)
           OR (reserved_at BETWEEN ? AND ?)
           OR (reserved_until BETWEEN ? AND ?));
    """;

            try (Connection conn = DriverManager.getConnection(DB_URL);
                 PreparedStatement checkStmt = conn.prepareStatement(checkOverlap)) {
                checkStmt.setInt(1, spotNumber);
                checkStmt.setString(2, startTime);
                checkStmt.setString(3, endTime);
                checkStmt.setString(4, startTime);
                checkStmt.setString(5, endTime);
                checkStmt.setString(6, startTime);
                checkStmt.setString(7, endTime);

                ResultSet rs = checkStmt.executeQuery();
                if (rs.next()) {
                    return false; // الحجز يتداخل مع حجز موجود
                }

                // إدخال الحجز الجديد
                String insertReservation = """
            INSERT INTO reservations (parking_spot_id, user_id, reserved_at, reserved_until) 
            VALUES ((SELECT id FROM parking_spots WHERE spot_number = ?), ?, ?, ?);
        """;
                try (PreparedStatement insertStmt = conn.prepareStatement(insertReservation)) {
                    insertStmt.setInt(1, spotNumber);
                    insertStmt.setInt(2, getCurrentUserId()); // استخدم معرف المستخدم الحالي
                    insertStmt.setString(3, startTime);
                    insertStmt.setString(4, endTime);
                    return insertStmt.executeUpdate() > 0;
                }
            } catch (SQLException e) {
                System.err.println("Error reserving parking spot: " + e.getMessage());
            }
            return false;
        }

        private int getCurrentUserId() {
            String sql = "SELECT id FROM users WHERE full_name = ?";
            try (Connection conn = DriverManager.getConnection(DB_URL);
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, this.currentUser);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    return rs.getInt("id");
                }
            } catch (SQLException e) {
                System.err.println("Error fetching user ID: " + e.getMessage());
            }
            return -1;
        }

    }
}
