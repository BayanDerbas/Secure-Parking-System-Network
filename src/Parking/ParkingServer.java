package Parking;
import Utils.AESUtils;

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
                        case "view_reservations":
                            handleViewReservations();
                            break;
                        case "cancel_reservation": // الحالة الجديدة
                            handleCancelReservation();
                            break;
                        default:
                            out.println(AESUtils.encrypt("Invalid operation."));
                            break;
                    }
                }
            } catch (IOException e) {
                System.err.println("Error in client handler: " + e.getMessage());
            } catch (Exception e) {
                throw new RuntimeException(e);
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
            String encryptedPassword = in.readLine();
            double walletBalance = Double.parseDouble(in.readLine());

            boolean isRegistered = registerUser(fullName, userType, phoneNumber, carPlate, encryptedPassword, walletBalance);
            out.println(isRegistered ? "User registered successfully!" : "Registration failed!");
        }
        private boolean registerUser(String fullName, String userType, String phone, String carPlate, String encryptedPassword, double walletBalance) {
            String encryptedCarPlate;
            String encryptedPhone;
            String encryptedUserType;

            try {
                encryptedCarPlate = AESUtils.encrypt(carPlate);
                encryptedPhone = AESUtils.encrypt(phone);
                encryptedUserType = AESUtils.encrypt(userType);

                System.out.println("Encrypted data to be stored:");
                System.out.println("Password: " + encryptedPassword);
                System.out.println("Car Plate: " + encryptedCarPlate);
                System.out.println("Phone: " + encryptedPhone);
                System.out.println("User Type: " + encryptedUserType);
                System.out.println("Wallet Balance: " + walletBalance);
            } catch (Exception e) {
                System.err.println("Error during encryption: " + e.getMessage());
                return false;
            }

            String sql = "INSERT INTO users (full_name, user_type, phone_number, car_plate, password, wallet_balance) VALUES (?, ?, ?, ?, ?, ?)";
            try (Connection conn = DriverManager.getConnection(DB_URL);
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, fullName);
                pstmt.setString(2, encryptedUserType);
                pstmt.setString(3, encryptedPhone);
                pstmt.setString(4, encryptedCarPlate);
                pstmt.setString(5, encryptedPassword);
                pstmt.setDouble(6, walletBalance);
                pstmt.executeUpdate();
                System.out.println("User registered successfully!");
                return true;
            } catch (SQLException e) {
                System.err.println("Error during user registration: " + e.getMessage());
                return false;
            }
        }
        private void handleLogin() throws IOException {
            String fullName = in.readLine();
            String encryptedPassword = in.readLine();

            System.out.println("Received encrypted password: " + encryptedPassword); // Debugging

            // فك تشفير كلمة المرور قبل إرسالها للمقارنة
            String rawPassword;
            try {
                rawPassword = AESUtils.decrypt(encryptedPassword);
            } catch (Exception e) {
                System.err.println("Error decrypting password: " + e.getMessage());
                out.println("Login failed!"); // إرسال خطأ للعميل
                return;
            }

            boolean isLoggedIn = loginUser(fullName, rawPassword); // استخدم الكلمة الأصلية
            out.println(isLoggedIn ? "Login successful!" : "Login failed!");

            if (isLoggedIn) {
                this.currentUser = fullName;
                System.out.println("Current user: " + this.currentUser); // Debugging
            }
        }
        private boolean loginUser(String fullName, String rawPassword) {
            String sql = "SELECT password FROM users WHERE full_name = ?";
            try (Connection conn = DriverManager.getConnection(DB_URL);
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {

                pstmt.setString(1, fullName);
                ResultSet rs = pstmt.executeQuery();

                if (rs.next()) {
                    String storedEncryptedPassword = rs.getString("password");

                    // Encrypt the raw password for comparison
                    String encryptedPassword = AESUtils.encrypt(rawPassword);
                    System.out.println("Stored Encrypted Password: " + storedEncryptedPassword);
                    System.out.println("Re-encrypted Password: " + encryptedPassword);

                    return storedEncryptedPassword.equals(encryptedPassword);
                }
            } catch (SQLException e) {
                System.err.println("Error during login: " + e.getMessage());
            } catch (Exception e) {
                System.err.println("Error encrypting password for comparison: " + e.getMessage());
            }
            return false;
        }
        private void viewAvailableParkingSpots() throws IOException {
            String availableSpots = getAvailableParkingSpots();
            out.println(availableSpots); // إرسال جميع المواقف
            out.println("END_OF_SPOTS"); // إشارة نهاية
        }
        private void handleReserveSpot() throws Exception {
            // عرض المواقف المتاحة
            String availableSpots = getAvailableParkingSpots();
            out.println(availableSpots);
            out.println("END_OF_SPOTS");
            if (availableSpots.equals("No parking spots available.")) {
                return;
            }

            try {
                // استقبال البيانات من العميل
                String encryptedSpotNumber = in.readLine();
                String encryptedStartTime = in.readLine();
                String encryptedEndTime = in.readLine();

                // فك التشفير
                int spotNumber = Integer.parseInt(AESUtils.decrypt(encryptedSpotNumber));
                String startTime = AESUtils.decrypt(encryptedStartTime);
                String endTime = AESUtils.decrypt(encryptedEndTime);

                // حساب الرسوم
                double fee = 10.0; // الرسوم الثابتة
                out.println(fee); // إرسال الرسوم للعميل (بدون تشفير)

                // استقبال تأكيد العميل
                String paymentConfirmation = in.readLine();
                if (paymentConfirmation.equals("cancel_payment")) {
                    System.out.println("Reservation cancelled by client.");
                    return;
                }

                // خصم الرسوم من رصيد العميل
                if (deductUserBalance(fee)) {
                    if (reserveParkingSpot(spotNumber, startTime, endTime)) {
                        out.println(AESUtils.encrypt("Reservation successful!"));
                    } else {
                        out.println(AESUtils.encrypt("The spot is already reserved during the specified time."));
                        refundUserBalance(fee); // استرجاع الرسوم في حالة الفشل
                    }
                } else {
                    out.println(AESUtils.encrypt("Insufficient balance. Reservation failed."));
                }
            } catch (Exception e) {
                System.err.println("Error during reservation: " + e.getMessage());
                out.println(AESUtils.encrypt("An error occurred during reservation. Please try again."));
            }
        }
        private boolean deductUserBalance(double amount) {
            String sql = "UPDATE users SET wallet_balance = wallet_balance - ? WHERE id = ? AND wallet_balance >= ?";
            try (Connection conn = DriverManager.getConnection(DB_URL);
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setDouble(1, amount);
                pstmt.setInt(2, getCurrentUserId());
                pstmt.setDouble(3, amount);

                return pstmt.executeUpdate() > 0;
            } catch (SQLException e) {
                System.err.println("Error deducting user balance: " + e.getMessage());
            }
            return false;
        }
        private void refundUserBalance(double amount) {
            String sql = "UPDATE users SET wallet_balance = wallet_balance + ? WHERE id = ?";
            try (Connection conn = DriverManager.getConnection(DB_URL);
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setDouble(1, amount);
                pstmt.setInt(2, getCurrentUserId());
                pstmt.executeUpdate();
            } catch (SQLException e) {
                System.err.println("Error refunding user balance: " + e.getMessage());
            }
        }
        private String getAvailableParkingSpots() {
            StringBuilder spots = new StringBuilder();
            String sql = """
        SELECT spot_number FROM parking_spots 
        WHERE spot_number NOT IN (
            SELECT parking_spot_id FROM reservations
            WHERE (reserved_at <= ? AND reserved_until >= ?)
        )
    """;

            try (Connection conn = DriverManager.getConnection(DB_URL);
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                String currentTime = getCurrentTime();
                stmt.setString(1, currentTime);
                stmt.setString(2, currentTime);

                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    int spotNumber = rs.getInt("spot_number");
                    spots.append(spotNumber).append(". Spot ").append(spotNumber).append("\n");
                }
            } catch (SQLException e) {
                System.err.println("Error fetching parking spots: " + e.getMessage());
            }

            return spots.length() > 0 ? spots.toString().trim() : "No parking spots available.";
        }
        private String getCurrentTime() {
            // هنا يمكنك استخدام تاريخ الوقت الحالي بالصيغ المناسبة حسب قاعدة البيانات أو كما ترغب
            return java.time.LocalDateTime.now().toString(); // تعديل على الصيغة إذا لزم الأمر
        }
        private boolean reserveParkingSpot(int spotNumber, String startTime, String endTime) {
            String checkOverlap = """
        SELECT COUNT(*) AS overlap_count
        FROM reservations r
        JOIN parking_spots ps ON r.parking_spot_id = ps.id
        WHERE ps.spot_number = ?
        AND (
            (r.reserved_at <= ? AND r.reserved_until > ?) OR
            (r.reserved_at < ? AND r.reserved_until >= ?) OR
            (r.reserved_at >= ? AND r.reserved_until <= ?)
        )
    """;

            String insertReservation = """
        INSERT INTO reservations (parking_spot_id, user_id, reserved_at, reserved_until, fee) 
        VALUES ((SELECT id FROM parking_spots WHERE spot_number = ?), ?, ?, ?, ?);
    """;

            try (Connection conn = DriverManager.getConnection(DB_URL);
                 PreparedStatement checkStmt = conn.prepareStatement(checkOverlap);
                 PreparedStatement insertStmt = conn.prepareStatement(insertReservation)) {

                // إعداد استعلام التحقق من التداخل الزمني
                checkStmt.setInt(1, spotNumber);
                checkStmt.setString(2, endTime);
                checkStmt.setString(3, startTime);
                checkStmt.setString(4, startTime);
                checkStmt.setString(5, endTime);
                checkStmt.setString(6, startTime);
                checkStmt.setString(7, endTime);

                // تنفيذ استعلام التحقق
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next() && rs.getInt("overlap_count") > 0) {
                        System.err.println("Error: The spot is already reserved during the specified time.");
                        return false; // يوجد تداخل زمني
                    }
                }

                // إدخال الحجز الجديد
                insertStmt.setInt(1, spotNumber);
                insertStmt.setInt(2, getCurrentUserId());
                insertStmt.setString(3, startTime);
                insertStmt.setString(4, endTime);
                double fee = 10.0;
                insertStmt.setDouble(5, fee);
                return insertStmt.executeUpdate() > 0;
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
        private void handleViewReservations() throws Exception {
            StringBuilder reservations = new StringBuilder();
            String sql = """
        SELECT ps.spot_number, r.reserved_at, r.reserved_until
        FROM reservations r
        JOIN parking_spots ps ON r.parking_spot_id = ps.id
        WHERE r.user_id = ?
    """;

            try (Connection conn = DriverManager.getConnection(DB_URL);
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setInt(1, getCurrentUserId());
                ResultSet rs = stmt.executeQuery();

                int index = 1; // لتعداد الحجوزات
                while (rs.next()) {
                    int spotNumber = rs.getInt("spot_number");
                    String reservedAt = rs.getString("reserved_at");
                    String reservedUntil = rs.getString("reserved_until");

                    String data = index + ". Spot " + spotNumber + ": from " + reservedAt + " to " + reservedUntil;
                    String encryptedData = AESUtils.encrypt(data); // تشفير البيانات

                    reservations.append(encryptedData).append("\n");
                    index++; // زيادة الرقم
                }
            } catch (Exception e) {
                System.err.println("Error fetching or encrypting reservations: " + e.getMessage());
            }

            if (reservations.length() > 0) {
                out.println(reservations.toString().trim());
            } else {
                out.println(AESUtils.encrypt("No reservations found.")); // تشفير الرسالة
            }
            out.println(AESUtils.encrypt("END_OF_RESERVATIONS")); // إشارة النهاية
        }
        private void handleCancelReservation() throws Exception {
            String encryptedSpotNumber = in.readLine(); // استقبال الرقم المشفر
            int spotNumber;

            try {
                String decryptedSpotNumber = AESUtils.decrypt(encryptedSpotNumber); // فك التشفير
                spotNumber = Integer.parseInt(decryptedSpotNumber);
            } catch (Exception e) {
                out.println(AESUtils.encrypt("Error: Invalid spot number.")); // استجابة مشفرة
                return;
            }

            String getFeeSql = """
        SELECT fee
        FROM reservations
        WHERE parking_spot_id = (SELECT id FROM parking_spots WHERE spot_number = ?)
        AND user_id = ?
    """;

            String deleteSql = """
        DELETE FROM reservations
        WHERE parking_spot_id = (SELECT id FROM parking_spots WHERE spot_number = ?)
        AND user_id = ?
    """;

            try (Connection conn = DriverManager.getConnection(DB_URL);
                 PreparedStatement getFeeStmt = conn.prepareStatement(getFeeSql);
                 PreparedStatement deleteStmt = conn.prepareStatement(deleteSql)) {

                // الحصول على الرسوم من قاعدة البيانات
                getFeeStmt.setInt(1, spotNumber);
                getFeeStmt.setInt(2, getCurrentUserId());
                double fee = 0.0;
                try (ResultSet rs = getFeeStmt.executeQuery()) {
                    if (rs.next()) {
                        fee = rs.getDouble("fee");
                    } else {
                        out.println(AESUtils.encrypt("Error: Reservation not found or already canceled.")); // استجابة مشفرة
                        return;
                    }
                }

                // حذف الحجز
                deleteStmt.setInt(1, spotNumber);
                deleteStmt.setInt(2, getCurrentUserId());
                int rowsAffected = deleteStmt.executeUpdate();

                if (rowsAffected > 0) {
                    // استرداد نصف المبلغ
                    double refundAmount = fee / 2;
                    refundUserBalance(refundAmount);
                    out.println(AESUtils.encrypt("Reservation canceled successfully. Half of the fee (" + refundAmount + ") has been refunded to your balance.")); // استجابة مشفرة
                } else {
                    out.println(AESUtils.encrypt("Error: Reservation not found or already canceled.")); // استجابة مشفرة
                }
            } catch (SQLException e) {
                System.err.println("Error canceling reservation: " + e.getMessage());
                out.println(AESUtils.encrypt("Error: Could not cancel reservation.")); // استجابة مشفرة
            }
        }

    }
}