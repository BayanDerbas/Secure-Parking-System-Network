package Parking;
import Utils.AESUtils;
import Utils.DigitalSignatureUtil;
import Utils.RSAUtils;
import java.io.*;
import java.net.*;
import java.security.PublicKey;
import java.sql.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import static org.bouncycastle.asn1.x509.ObjectDigestInfo.publicKey;

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
            try {

                System.out.println("Encrypted data to be stored:");
                System.out.println("Password: " + encryptedPassword);
                System.out.println("Phone: ");
                System.out.println("User Type: " + userType);
                System.out.println("Car Plate: " + carPlate);
                System.out.println("Wallet Balance: " + walletBalance);
            } catch (Exception e) {
                System.err.println("Error during encryption: " + e.getMessage());
                return false;
            }

            String sql = "INSERT INTO users (full_name, user_type, phone_number, car_plate, password, wallet_balance) VALUES (?, ?, ?, ?, ?, ?)";
            try (Connection conn = DriverManager.getConnection(DB_URL);
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, fullName);
                pstmt.setString(2, userType);
                pstmt.setString(3, phone);
                pstmt.setString(4, carPlate);
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
        private boolean cancelReservation(int reservationId, double refundAmount) {
            String sqlDelete = "DELETE FROM reservations WHERE id = ?";
            String sqlRefund = "UPDATE users SET wallet_balance = wallet_balance + ? WHERE id = ?";

            try (Connection conn = DriverManager.getConnection(DB_URL);
                 PreparedStatement deleteStmt = conn.prepareStatement(sqlDelete);
                 PreparedStatement refundStmt = conn.prepareStatement(sqlRefund)) {

                conn.setAutoCommit(false); // بدء معاملة

                // حذف الحجز
                deleteStmt.setInt(1, reservationId);
                if (deleteStmt.executeUpdate() <= 0) {
                    conn.rollback();
                    return false;
                }

                // إعادة نصف الرسوم
                refundStmt.setDouble(1, refundAmount);
                refundStmt.setInt(2, getCurrentUserId());
                if (refundStmt.executeUpdate() <= 0) {
                    conn.rollback();
                    return false;
                }

                conn.commit(); // تأكيد المعاملة
                return true;
            } catch (SQLException e) {
                System.err.println("Error canceling reservation: " + e.getMessage());
                return false;
            }
        }
        private double calculateRefund(int reservationId) {
            String sql = "SELECT fee FROM reservations WHERE id = ? AND reserved_at > CURRENT_TIMESTAMP";

            try (Connection conn = DriverManager.getConnection(DB_URL);
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, reservationId);

                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getDouble("fee") / 2; // إعادة نصف الرسوم
                    }
                }
            } catch (SQLException e) {
                System.err.println("Error calculating refund: " + e.getMessage());
            }

            return 0.0;
        }
        private boolean deductUserBalance(double amount) {
            String sql = "UPDATE users SET wallet_balance = wallet_balance - ? WHERE id = ? AND wallet_balance >= ?";
            try (Connection conn = DriverManager.getConnection(DB_URL);
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {

                pstmt.setDouble(1, amount);           // خصم المبلغ المطلوب
                pstmt.setInt(2, getCurrentUserId());  // تحديد المستخدم الحالي
                pstmt.setDouble(3, amount);           // التأكد من وجود رصيد كافٍ

                return pstmt.executeUpdate() > 0;     // التحقق من نجاح الخصم
            } catch (SQLException e) {
                System.err.println("Error deducting user balance: " + e.getMessage());
                return false; // في حالة حدوث خطأ أو عدم كفاية الرصيد
            }
        }
        private void handleReserveSpot() throws Exception {
            String availableSpots = getAvailableParkingSpots();
            out.println(availableSpots);
            out.println("END_OF_SPOTS");

            if (availableSpots.equals("No parking spots available.")) {
                return;
            }

            try {
                // Receive encrypted spot number, start time, and end time
                String encryptedSpotNumber = in.readLine();
                String encryptedStartTime = in.readLine();
                String encryptedEndTime = in.readLine();
                int spotNumber = Integer.parseInt(AESUtils.decrypt(encryptedSpotNumber));
                String startTime = AESUtils.decrypt(encryptedStartTime);
                String endTime = AESUtils.decrypt(encryptedEndTime);

                // Verify reservation signature
                String dataToSign = spotNumber + "|" + startTime + "|" + endTime; // using "|" as separator
                PublicKey publicKey = RSAUtils.loadPublicKey("C:/Users/ahmad/Documents/public_key.pem");
                String receivedReservationSignature = in.readLine();
                boolean isReservationSignatureValid = DigitalSignatureUtil.verifyDigitalSignature(dataToSign, receivedReservationSignature, publicKey);
                if (!isReservationSignatureValid) {
                    out.println(AESUtils.encrypt("Error: Invalid reservation data."));
                    return;
                }

                // Calculate the fee
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
                LocalDateTime start = LocalDateTime.parse(startTime, formatter);
                LocalDateTime end = LocalDateTime.parse(endTime, formatter);
                double fee = calculateFee(start, end);
                String feeText = String.valueOf(fee);

                // Encrypt and send fee
                String encryptedFee = RSAUtils.encrypt(feeText, publicKey);
                out.println(encryptedFee);

                // Receive payment signature
                String receivedPaymentSignature = in.readLine();
                String paymentConfirmation = "confirm_payment";
                boolean isPaymentSignatureValid = DigitalSignatureUtil.verifyDigitalSignature(paymentConfirmation, receivedPaymentSignature, publicKey);
                if (!isPaymentSignatureValid) {
                    out.println(AESUtils.encrypt("Error: Invalid payment confirmation."));
                    return;
                }

                if (!deductUserBalance(fee)) {
                    out.println(AESUtils.encrypt("Error: Insufficient balance."));
                    return;
                }

                if (reserveParkingSpot(spotNumber, startTime, endTime)) {
                    out.println(AESUtils.encrypt("Reservation successful!"));
                } else {
                    out.println(AESUtils.encrypt("The spot is already reserved during the specified time."));
                }
            } catch (Exception e) {
                System.err.println("Error during reservation: " + e.getMessage());
                out.println(AESUtils.encrypt("An error occurred during reservation. Please try again."));
            }
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

                checkStmt.setInt(1, spotNumber);
                checkStmt.setString(2, endTime);
                checkStmt.setString(3, startTime);
                checkStmt.setString(4, startTime);
                checkStmt.setString(5, endTime);
                checkStmt.setString(6, startTime);
                checkStmt.setString(7, endTime);

                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next() && rs.getInt("overlap_count") > 0) {
                        System.err.println("Error: The spot is already reserved during the specified time.");
                        return false;
                    }
                }

                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
                LocalDateTime start = LocalDateTime.parse(startTime, formatter);
                LocalDateTime end = LocalDateTime.parse(endTime, formatter);

                double fee = calculateFee(start, end);

                insertStmt.setInt(1, spotNumber);
                insertStmt.setInt(2, getCurrentUserId());
                insertStmt.setString(3, startTime);
                insertStmt.setString(4, endTime);
                insertStmt.setDouble(5, fee);

                return insertStmt.executeUpdate() > 0;
            } catch (SQLException e) {
                System.err.println("Error reserving parking spot: " + e.getMessage());
            }
            return false;
        }
        private double calculateFee(LocalDateTime start, LocalDateTime end) {
            Duration duration = Duration.between(start, end);
            long durationHours = duration.toHours();

            if (duration.toMinutes() % 60 != 0) {
                durationHours++;
            }

            return durationHours * 10.0; // الرسوم 10 وحدات لكل ساعة
        }
        private void handleCancelReservation() throws Exception {
            StringBuilder reservations = new StringBuilder();
            String sqlFetch = """
    SELECT r.id, ps.spot_number, r.reserved_at, r.reserved_until, r.fee
    FROM reservations r
    JOIN parking_spots ps ON r.parking_spot_id = ps.id
    WHERE r.user_id = ?
    """;

            try (Connection conn = DriverManager.getConnection(DB_URL);
                 PreparedStatement fetchStmt = conn.prepareStatement(sqlFetch)) {

                fetchStmt.setInt(1, getCurrentUserId());
                ResultSet rs = fetchStmt.executeQuery();

                int index = 1;
                Map<Integer, Integer> reservationMap = new HashMap<>();

                while (rs.next()) {
                    int reservationId = rs.getInt("id");
                    int spotNumber = rs.getInt("spot_number");
                    String reservedAt = rs.getString("reserved_at");
                    String reservedUntil = rs.getString("reserved_until");
                    double fee = rs.getDouble("fee");

                    reservationMap.put(index, reservationId);

                    String data = index + ". Spot " + spotNumber + ": from " + reservedAt + " to " + reservedUntil + " (Fee: " + fee + ")";
                    String encryptedData = AESUtils.encrypt(data);
                    reservations.append(encryptedData).append("\n");
                    index++;
                }

                if (reservations.length() > 0) {
                    out.println(reservations.toString().trim());
                } else {
                    out.println(AESUtils.encrypt("No reservations found."));
                }
                out.println(AESUtils.encrypt("END_OF_RESERVATIONS"));

                if (reservationMap.isEmpty()) return;

                String encryptedNumber = in.readLine();
                int reservationNumber = Integer.parseInt(AESUtils.decrypt(encryptedNumber));

                // استلام توقيع الرقم
                String receivedSignature = in.readLine();
                System.out.println("Received signature from client: " + receivedSignature);

                String publicKeyPath = "C:/Users/ahmad/Documents/public_key.pem";
                PublicKey publicKey = RSAUtils.loadPublicKey(publicKeyPath);

                // التحقق من التوقيع
                if (!DigitalSignatureUtil.verifyDigitalSignature(String.valueOf(reservationNumber), receivedSignature, publicKey)) {
                    System.err.println("Error: Invalid signature. Reservation number: " + reservationNumber);
                    out.println(AESUtils.encrypt("Error: Invalid signature."));
                    return;
                }
                System.out.println("Signature verification succeeded for reservation number: " + reservationNumber);

                if (!reservationMap.containsKey(reservationNumber)) {
                    out.println(AESUtils.encrypt("Invalid reservation number."));
                    return;
                }

                int reservationId = reservationMap.get(reservationNumber);
                double refundAmount = calculateRefund(reservationId);

                if (refundAmount > 0) {
                    String encryptedRefund = RSAUtils.encrypt(String.valueOf(refundAmount), publicKey);
                    out.println(encryptedRefund);

                    // إلغاء الحجز إذا كان المبلغ المسترد موجودًا
                    if (cancelReservation(reservationId, refundAmount)) {
                        String refundMessage = "Reservation canceled successfully, and the refund was processed.\n";
                        String signedRefundMessage = DigitalSignatureUtil.generateDigitalSignature(refundMessage, RSAUtils.loadPrivateKey("C:/Users/ahmad/Documents/private_key.pem"));
                        out.println(AESUtils.encrypt(refundMessage + " Signature: " + signedRefundMessage));
                    } else {
                        out.println(AESUtils.encrypt("Failed to cancel the reservation."));
                    }
                } else {
                    out.println(AESUtils.encrypt("No refund applicable for this reservation."));
                }
            } catch (Exception e) {
                System.err.println("Error handling cancellation: " + e.getMessage());
                out.println(AESUtils.encrypt("An error occurred. Please try again."));
            }
        }
    }
}