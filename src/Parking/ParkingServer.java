package Parking;
import Utils.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.*;
import java.sql.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
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
        private static PrintWriter out;
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
                        case "view_parking_spots":
                            handleViewParkingSpots();
                            break;
                        case "view_all_visitors":
                            sendAllVisitors();
                            break;
                        case "reserve_spot":
                            handleReserveSpot();
                            break;
                        case "view_reservations":
                            handleViewReservations();
                            break;
                        case "add_parking_spot":
                            handleAddParkingSpot();
                            break;
                        case "remove_parking_spot":
                            handleRemoveParkingSpot();
                            break;
                        case "view_reserved_parking_spots":  // تأكد من أن العملية تتطابق تمامًا
                            handleViewAllReservedParkingSpots();
                            break;
                        case "Edit_Parking_Spot_Name":
                            handleEditParkingSpotName();
                            break;
                        default:
                            out.println(AESUtils.encrypt("Invalid operation."));  // إذا كانت العملية غير صالحة
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
        private int getUserIdByFullName(String fullName) {
            String sql = "SELECT id FROM users WHERE full_name = ?";
            try (Connection conn = DriverManager.getConnection(DB_URL);
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, fullName);

                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt("id");
                    }
                }
            } catch (SQLException e) {
                System.err.println("Error retrieving user ID: " + e.getMessage());
            }
            return -1; // إذا لم يتم العثور على المستخدم
        }
        private boolean checkCertificateExists(int userId) {
            String sql = "SELECT COUNT(*) FROM certificates WHERE user_id = ?";
            try (Connection conn = DriverManager.getConnection(DB_URL);
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, userId);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    return rs.getInt(1) > 0; // إذا كان هناك سجل، الشهادة موجودة
                }
            } catch (SQLException e) {
                System.err.println("Error checking certificate existence: " + e.getMessage());
            }
            return false;
        }
        private boolean storeCertificate(int userId, String certificatePem, String publicKeyPem, String privateKeyPem) {
            String encryptedPrivateKey = null;

            // تشفير المفتاح الخاص
            try {
                if (privateKeyPem != null && !privateKeyPem.isEmpty()) {
                    encryptedPrivateKey = AESUtils.encrypt(privateKeyPem);  // تشفير المفتاح الخاص
                    System.out.println("Encrypted Private Key: " + encryptedPrivateKey); // طباعة المفتاح المشفر
                }
            } catch (Exception e) {
                System.err.println("Error encrypting private key: " + e.getMessage());
                return false;
            }

            // حفظ البيانات في قاعدة البيانات
            String sql = "INSERT INTO certificates (user_id, certificate, public_key, private_key) VALUES (?, ?, ?, ?)";
            try (Connection conn = DriverManager.getConnection(DB_URL);
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, userId);
                pstmt.setString(2, certificatePem);
                pstmt.setString(3, publicKeyPem);
                pstmt.setString(4, encryptedPrivateKey);  // تخزين المفتاح الخاص المشفر

                pstmt.executeUpdate();
                System.out.println("Certificate stored successfully!");
                return true;

            } catch (SQLException e) {
                System.err.println("Error storing certificate: " + e.getMessage());
                System.err.println("SQL State: " + e.getSQLState());
                System.err.println("Error Code: " + e.getErrorCode());
                return false;
            }
        }
        private boolean verifyCertificate(String encodedCertificate) {
            try {
                // تحويل الشهادة من Base64 إلى تنسيق بايت
                byte[] certificateBytes = Base64.getDecoder().decode(encodedCertificate);
                // تحميل الشهادة
                CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
                ByteArrayInputStream certStream = new ByteArrayInputStream(certificateBytes);
                X509Certificate certificate = (X509Certificate) certificateFactory.generateCertificate(certStream);

                // التحقق من صحة الشهادة
                certificate.checkValidity();

                // إضافة تفاصيل التوثيق:
                String verificationKey = "CA Public Key"; // المفتاح العام لـ CA يمكن استبداله بأي مفتاح يتم استخدامه في التحقق
                LocalDateTime verificationTime = LocalDateTime.now();

                System.out.println("Certificate is valid.");
                System.out.println("Verified using key: " + verificationKey);
                // عند إرسال الشهادة:
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                String formattedDateTime = LocalDateTime.now().format(formatter);
                System.out.println("Verification timestamp: " + formattedDateTime); // التاريخ الذي تم فيه التحقق
                return true;
            } catch (CertificateException e) {
                System.err.println("Error verifying certificate: " + e.getMessage());
                return false;
            }
        }
        private String getUserType(String fullName) {
            String sql = "SELECT user_type FROM users WHERE full_name = ?";
            try (Connection conn = DriverManager.getConnection(DB_URL);
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, fullName);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    return rs.getString("user_type");
                }
            } catch (SQLException e) {
                System.err.println("Error fetching user type: " + e.getMessage());
            }
            return "Visitor";  // إذا لم يكن نوع المستخدم موجودًا، نعيد "زائر" كإفتراض
        }
        private void handleLogin() throws IOException {
            System.out.println("Handling login...");
            String fullName = SecurityUtils.sanitizeForXSS(in.readLine());
            System.out.println("Received full name: " + fullName);
            String encryptedPassword = in.readLine();

            String rawPassword;
            try {
                rawPassword = AESUtils.decrypt(encryptedPassword);
                System.out.println("Decrypted password successfully.");
            } catch (Exception e) {
                System.err.println("Error decrypting password: " + e.getMessage());
                out.println("Login failed!");
                return;
            }

            String encodedCertificate = in.readLine();
            System.out.println("Received certificate from client. Verifying...");

            if (verifyCertificate(encodedCertificate)) {
                System.out.println("Certificate verified successfully.");
                int userId = getUserIdByFullName(fullName);
                if (userId != -1 && checkCertificateExists(userId)) {
                    System.out.println("Certificate exists in the database.");
                    boolean isLoggedIn = loginUser(fullName, rawPassword);
                    if (isLoggedIn) {
                        String userType = getUserType(fullName); // جلب نوع المستخدم
                        System.out.println("Login successful for user: " + fullName + " (Type: " + userType + ")");
                        out.println("Login successful!");
                        out.println(userType); // إرسال نوع المستخدم
                        currentUser = fullName;
                    } else {
                        System.out.println("Login failed for user: " + fullName);
                        out.println("Login failed!");
                    }
                } else {
                    System.out.println("Certificate not found in the database for user: " + fullName);
                    out.println("Login failed! Certificate not found.");
                }
            } else {
                System.out.println("Certificate verification failed for user: " + fullName);
                out.println("Login failed! Error verifying certificate.");
            }
        }
        private boolean loginUser(String fullName, String rawPassword) {
            String sql = "SELECT password FROM users WHERE full_name = ?";
            try (Connection conn = DriverManager.getConnection(DB_URL);
                 PreparedStatement pstmt = SecurityUtils.prepareSafeStatement(conn, sql, fullName)) {  // استخدام Safe Statement
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    String storedEncryptedPassword = rs.getString("password");
                    // تشفير كلمة المرور للمقارنة
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
        private void handleRegister() throws IOException {
            String fullName = SecurityUtils.sanitizeForXSS(in.readLine());  // حماية ضد XSS
            String userType = SecurityUtils.sanitizeForXSS(in.readLine());  // حماية ضد XSS
            String phoneNumber = SecurityUtils.sanitizeForXSS(in.readLine());  // حماية ضد XSS
            String carPlate = SecurityUtils.sanitizeForXSS(in.readLine());  // حماية ضد XSS
            String encryptedPassword = in.readLine();
            double walletBalance = Double.parseDouble(in.readLine());

            // التحقق من وجود المستخدم مسبقًا
            int userId = getUserIdByFullName(fullName);
            if (userId != -1) {
                out.println("User already exists!");
                return;
            }

            // تسجيل المستخدم
            if (registerUser(fullName, userType, phoneNumber, carPlate, encryptedPassword, walletBalance)) {
                out.println("User registered successfully!");

                // طلب الشهادة الرقمية
                if (in.readLine().equals("certificate")) {
                    String distinguishedName = in.readLine();
                    try {
                        // توليد المفاتيح
                        KeyPair keyPair = DigitalCertificateUtils.generateKeyPair();

                        // توليد CSR
                        String csr = DigitalCertificateUtils.generateCSR(keyPair, distinguishedName);

                        // توقيع الشهادة
                        String caDistinguishedName = "CN=CA, O=Authority, C=US";
                        KeyPair caKeyPair = DigitalCertificateUtils.generateKeyPair();
                        X509Certificate certificate = DigitalCertificateUtils.signCSR(csr, caKeyPair, caDistinguishedName);

                        // تحويل المفاتيح والشهادة إلى Base64
                        String certificatePem = certificate.toString();
                        String publicKeyPem = DigitalCertificateUtils.convertPublicKeyToBase64(keyPair.getPublic());
                        String privateKeyPem = DigitalCertificateUtils.convertPrivateKeyToBase64(keyPair.getPrivate());

                        // طباعة المفاتيح والشهادة
                        System.out.println("Public Key (Base64):");
                        System.out.println(publicKeyPem);

                        System.out.println("Private Key (Base64):");
                        System.out.println(privateKeyPem);

                        System.out.println("Certificate (PEM):");
                        System.out.println(certificatePem);

                        // حفظ إلى قاعدة البيانات
                        userId = getUserIdByFullName(fullName);
                        if (storeCertificate(userId, certificatePem, publicKeyPem, privateKeyPem)) {
                            out.println("Digital certificate created and stored successfully!");

                            // حفظ الشهادة في ملف
                            String certificateFilePath = "C:/Users/ahmad/Documents/" + fullName + "_certificate.crt";
                            try {
                                DigitalCertificateUtils.saveCertificateToFile(certificate, certificateFilePath);
                                System.out.println("Certificate saved to: " + certificateFilePath);
                            } catch (Exception e) {
                                System.err.println("Failed to save certificate to file: " + e.getMessage());
                            }
                        } else {
                            out.println("Failed to store the digital certificate.");
                        }
                    } catch (Exception e) {
                        out.println("Failed to create digital certificate: " + e.getMessage());
                    }
                }
            } else {
                out.println("Registration failed!");
            }
        }
        private boolean registerUser(String fullName, String userType, String phone, String carPlate, String encryptedPassword, double walletBalance) {
            try {
                System.out.println("Encrypted data to be stored:");
                System.out.println("Password: " + encryptedPassword);
                System.out.println("Phone: " + phone);
                System.out.println("User Type: " + userType);
                System.out.println("Car Plate: " + carPlate);
                System.out.println("Wallet Balance: " + walletBalance);
            } catch (Exception e) {
                System.err.println("Error during encryption: " + e.getMessage());
                return false;
            }

            String sql = "INSERT INTO users (full_name, user_type, phone_number, car_plate, password, wallet_balance) VALUES (?, ?, ?, ?, ?, ?)";
            try (Connection conn = DriverManager.getConnection(DB_URL);
                 PreparedStatement pstmt = SecurityUtils.prepareSafeStatement(conn, sql, fullName, userType, phone, carPlate, encryptedPassword, walletBalance)) {  // استخدام Safe Statement
                pstmt.executeUpdate();
                System.out.println("User registered successfully!");
                return true;
            } catch (SQLException e) {
                System.err.println("Error during user registration: " + e.getMessage());
                return false;
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
                ORDER BY CAST(spot_number AS UNSIGNED)
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
        private boolean isValidTimeRange(String startTime, String endTime) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            LocalDateTime start = LocalDateTime.parse(startTime, formatter);
            LocalDateTime end = LocalDateTime.parse(endTime, formatter);
            return !start.isBefore(LocalDateTime.now()) && !start.isAfter(end);
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
            // استلام الشهادة الرقمية من العميل
            String encodedCertificate = in.readLine();
            System.out.println("Received certificate from client: " + encodedCertificate);  // سجل الشهادة المستلمة

            // التحقق من صحة الشهادة الرقمية
            if (verifyCertificate(encodedCertificate)) {
                int userId = getCurrentUserId();
                System.out.println("User ID: " + userId);  // سجل الـ userId

                // التحقق من وجود الشهادة في قاعدة البيانات
                if (!checkCertificateExists(userId)) {
                    out.println(AESUtils.encrypt("Login failed! Certificate not found."));
                    return;
                }

                // استعلام لعرض الحجوزات
                String sql = """
            SELECT ps.spot_number, r.reserved_at, r.reserved_until, r.fee
            FROM reservations r
            JOIN parking_spots ps ON r.parking_spot_id = ps.id
            WHERE r.user_id = ?
            ORDER BY r.reserved_at;
        """;

                Set<String> uniqueReservations = new HashSet<>(); // مجموعة لتخزين الحجوزات الفريدة
                List<String> reservationsList = new ArrayList<>(); // قائمة لتخزين جميع الحجوزات

                try (Connection conn = DriverManager.getConnection(DB_URL);
                     PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setInt(1, getCurrentUserId());
                    ResultSet rs = stmt.executeQuery();
                    int index = 1;
                    while (rs.next()) {
                        int spotNumber = rs.getInt("spot_number");
                        String reservedAt = AESUtils.decrypt(rs.getString("reserved_at"));
                        String reservedUntil = AESUtils.decrypt(rs.getString("reserved_until"));
                        double fee = rs.getDouble("fee");
                        String data = "Spot " + spotNumber + ": from " + reservedAt + " to " + reservedUntil + " (Fee: $" + fee + ")";
                        // إضافة الحجز إلى القائمة قبل التحقق من التكرار
                        reservationsList.add(data);
                    }

                    // الآن التحقق من التكرار
                    for (String reservation : reservationsList) {
                        if (!uniqueReservations.contains(reservation)) {
                            uniqueReservations.add(reservation); // إضافة الحجز الفريد إلى المجموعة
                            out.println(SecurityUtils.sanitizeForXSS(index + ". " + reservation)); // إرسال الحجز الفريد مع حماية XSS
                            index++;
                        }
                    }

                    if (index == 1) {
                        out.println("No reservations found.");
                    }
                } catch (Exception e) {
                    System.err.println("Error fetching or decrypting reservations: " + e.getMessage());
                }

                out.println("END_OF_RESERVATIONS");
            } else {
                out.println(AESUtils.encrypt("Login failed! Error verifying certificate."));
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
                // استقبال البيانات المشفرة من العميل
                String encryptedSpotNumber = in.readLine();
                String encryptedStartTime = in.readLine();
                String encryptedEndTime = in.readLine();

                // استلام الشهادة من العميل
                String encodedCertificate = in.readLine();

                // التحقق من الشهادة في قاعدة البيانات
                int userId = getCurrentUserId();  // الحصول على user_id
                if (!checkCertificateExists(userId)) {
                    out.println(AESUtils.encrypt("Error: Certificate not found in database."));
                    return;
                }

                // التحقق من الشهادة الرقمية
                if (!verifyCertificate(encodedCertificate)) {
                    out.println(AESUtils.encrypt("Error: Invalid certificate."));
                    return;
                }

                // فك تشفير البيانات باستخدام AES
                int spotNumber = Integer.parseInt(AESUtils.decrypt(encryptedSpotNumber));
                String startTime = AESUtils.decrypt(encryptedStartTime);
                String endTime = AESUtils.decrypt(encryptedEndTime);

                // التحقق من صحة نطاق الوقت
                if (!isValidTimeRange(startTime, endTime)) {
                    out.println(AESUtils.encrypt("Error: Invalid time range."));
                    return;
                }

                // التحقق من التوقيع الرقمي باستخدام RSA
                String dataToSign = encryptedSpotNumber + "|" + encryptedStartTime + "|" + encryptedEndTime;
                PublicKey publicKey = RSAUtils.loadPublicKey("C:/Users/ahmad/Documents/public_key.pem");
                String receivedReservationSignature = in.readLine();

                if (!DigitalSignatureUtils.verifyDigitalSignature(dataToSign, receivedReservationSignature, publicKey)) {
                    out.println(AESUtils.encrypt("Error: Invalid reservation data."));
                    return;
                }

                // تحويل الوقت من String إلى LocalDateTime
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
                LocalDateTime start = LocalDateTime.parse(startTime, formatter);
                LocalDateTime end = LocalDateTime.parse(endTime, formatter);
                double fee = calculateFee(start, end);

                // إرسال الرسوم المشفرة باستخدام RSA
                String encryptedFee = RSAUtils.encrypt(String.valueOf(fee), publicKey);
                out.println(encryptedFee);

                // استقبال الشهادة الرقمية لتوثيق الدفع
                String encodedPaymentCertificate = in.readLine();
                if (!verifyCertificate(encodedPaymentCertificate)) {
                    out.println(AESUtils.encrypt("Error: Invalid certificate for payment."));
                    return;
                }

                // استقبال توقيع الدفع من العميل
                String receivedPaymentSignature = in.readLine();
                String paymentConfirmation = "confirm_payment";
                if (!DigitalSignatureUtils.verifyDigitalSignature(paymentConfirmation, receivedPaymentSignature, publicKey)) {
                    out.println(AESUtils.encrypt("Error: Invalid payment confirmation."));
                    return;
                }

                // التحقق من الرصيد
                if (!deductUserBalance(fee)) {
                    out.println(AESUtils.encrypt("Error: Insufficient balance."));
                    return;
                }

                // إرسال رسالة الدفع الناجحة
                String paymentSuccessMessage = "Payment successful!";
                String encryptedPaymentSuccessMessage = RSAUtils.encrypt(paymentSuccessMessage, publicKey);
                out.println(encryptedPaymentSuccessMessage);

                // إجراء الحجز
                if (reserveParkingSpot(spotNumber, startTime, endTime)) {
                    String reservationSuccessMessage = "Reservation successful!";

                    String reservationSignature = receivedReservationSignature;  // التوقيع الرقمي المستلم
                    String paymentSignature = receivedPaymentSignature;  // توقيع الدفع المستلم

                    String insertReservationWithSignatures = """
            INSERT INTO reservations (parking_spot_id, user_id, reserved_at, reserved_until, fee, digital_signature_reservation, digital_signature_payment)
            VALUES ((SELECT id FROM parking_spots WHERE spot_number = ?), ?, ?, ?, ?, ?, ?);
            """;

                    try (Connection conn = DriverManager.getConnection(DB_URL);
                         PreparedStatement stmt = conn.prepareStatement(insertReservationWithSignatures)) {
                        stmt.setInt(1, spotNumber);
                        stmt.setInt(2, userId);
                        stmt.setString(3, AESUtils.encrypt(startTime));
                        stmt.setString(4, AESUtils.encrypt(endTime));
                        stmt.setDouble(5, fee);
                        stmt.setString(6, reservationSignature);  // تخزين التوقيع الرقمي للحجز
                        stmt.setString(7, paymentSignature);     // تخزين التوقيع الرقمي للدفع

                        stmt.executeUpdate();
                        out.println(AESUtils.encrypt(reservationSuccessMessage));
                    } catch (SQLException e) {
                        System.err.println("Error saving reservation with digital signatures: " + e.getMessage());
                        out.println(AESUtils.encrypt("Error: Unable to store reservation."));
                    }
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

                // تحقق من وجود تداخل في الحجز
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

                // تشفير البيانات قبل إدخالها في قاعدة البيانات
                String encryptedStartTime = AESUtils.encrypt(startTime);
                String encryptedEndTime = AESUtils.encrypt(endTime);

                // حساب الرسوم
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
                LocalDateTime start = LocalDateTime.parse(startTime, formatter);
                LocalDateTime end = LocalDateTime.parse(endTime, formatter);

                double fee = calculateFee(start, end);

                // إدخال البيانات المشفرة
                insertStmt.setInt(1, spotNumber);
                insertStmt.setInt(2, getCurrentUserId());  // تأكد من أنك تقوم بتحديد ID المستخدم
                insertStmt.setString(3, encryptedStartTime);
                insertStmt.setString(4, encryptedEndTime);
                insertStmt.setDouble(5, fee);

                return insertStmt.executeUpdate() > 0;
            } catch (Exception e) {
                System.err.println("Error reserving parking spot: " + e.getMessage());
            }
            return false;
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
        private double calculateFee(LocalDateTime start, LocalDateTime end) {
            Duration duration = Duration.between(start, end);
            long durationHours = duration.toHours();

            if (duration.toMinutes() % 60 != 0) {
                durationHours++;
            }

            return durationHours * 10.0; // الرسوم 10 وحدات لكل ساعة
        }
        private void sendAllVisitors() throws Exception {
            String sql = "SELECT full_name FROM users WHERE user_type = 'Visitor'";
            try (Connection conn = DriverManager.getConnection(DB_URL);
                 PreparedStatement pstmt = conn.prepareStatement(sql); // استخدام PreparedStatement بدلاً من Statement
                 ResultSet rs = pstmt.executeQuery()) {

                StringBuilder visitorsList = new StringBuilder();
                while (rs.next()) {
                    // تنظيف البيانات القادمة من قاعدة البيانات لمنع XSS
                    String sanitizedFullName = SecurityUtils.sanitizeForXSS(rs.getString("full_name"));
                    visitorsList.append(sanitizedFullName).append("\n");
                }

                String response = visitorsList.length() == 0 ? "No visitors found." : visitorsList.toString();
                String encryptedResponse = AESUtils.encrypt(response);
                out.println(encryptedResponse); // إرسال الرد المشفر
                System.out.println("Sent response: \n" + response); // طباعة الرد
            } catch (SQLException e) {
                System.err.println("Error fetching visitors: " + e.getMessage());
                String encryptedResponse = AESUtils.encrypt("Error fetching visitors.");
                out.println(encryptedResponse); // إرسال خطأ مشفر
            }
        }
        private void handleAddParkingSpot() throws IOException {
            int spotNumber = Integer.parseInt(in.readLine());
            String sql = "INSERT INTO parking_spots (spot_number) VALUES (?)";
            try (Connection conn = DriverManager.getConnection(DB_URL);
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, spotNumber);
                int rows = pstmt.executeUpdate();
                if (rows > 0) {
                    System.out.println("Parking spot " + spotNumber + " added successfully.");
                    out.println("Parking spot added successfully.");

                    // إعادة إرسال جميع المواقف بعد إضافة الجديد
                    handleViewParkingSpots();
                } else {
                    out.println("Failed to add parking spot.");
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        private void handleViewParkingSpots() throws IOException {
            String sql = "SELECT spot_number FROM parking_spots ORDER BY CAST(spot_number AS UNSIGNED)";
            try (Connection conn = DriverManager.getConnection(DB_URL);
                 PreparedStatement pstmt = SecurityUtils.prepareSafeStatement(conn, sql);  // استخدام prepared statement آمن
                 ResultSet rs = pstmt.executeQuery()) {

                StringBuilder spotsList = new StringBuilder();
                while (rs.next()) {
                    spotsList.append("Spot: ").append(rs.getString("spot_number")).append(", ");
                }

                // تنظيف البيانات قبل إرسالها للعميل
                String sanitizedSpotsList = SecurityUtils.sanitizeForXSS(spotsList.toString());

                // تشفير القائمة وإرسالها
                String encryptedSpots = AESUtils.encrypt(sanitizedSpotsList);
                String base64Spots = Base64.getEncoder().encodeToString(encryptedSpots.getBytes(StandardCharsets.UTF_8));
                out.println(base64Spots);
            } catch (Exception e) {
                System.err.println("Error fetching parking spots: " + e.getMessage());
                out.println("Error fetching parking spots.");
            }
        }
        private void handleEditParkingSpotName() throws Exception {
            try {
                // قراءة البيانات المدخلة (اسم الموقف القديم والجديد)
                String encryptedOldSpotName = in.readLine();
                String encryptedNewSpotName = in.readLine();

                // فك التشفير
                String oldSpotName = AESUtils.decrypt(encryptedOldSpotName);
                String newSpotName = AESUtils.decrypt(encryptedNewSpotName);

                // تنظيف المدخلات لمنع XSS
                String sanitizedOldSpotName = SecurityUtils.sanitizeForXSS(oldSpotName);
                String sanitizedNewSpotName = SecurityUtils.sanitizeForXSS(newSpotName);

                if (sanitizedOldSpotName.isEmpty() || sanitizedNewSpotName.isEmpty()) {
                    out.println(AESUtils.encrypt("Invalid input."));
                    return;
                }

                if (sanitizedOldSpotName.equals(sanitizedNewSpotName)) {
                    out.println(AESUtils.encrypt("Old and new spot names cannot be the same."));
                    return;
                }

                // تحديث الموقف في قاعدة البيانات باستخدام PreparedStatement
                String sql = "UPDATE parking_spots SET spot_number = ? WHERE spot_number = ?";
                try (Connection conn = DriverManager.getConnection(DB_URL);
                     PreparedStatement pstmt = SecurityUtils.prepareSafeStatement(conn, sql, sanitizedNewSpotName, sanitizedOldSpotName)) {

                    int rowsUpdated = pstmt.executeUpdate();
                    if (rowsUpdated > 0) {
                        out.println(AESUtils.encrypt("Parking spot updated successfully."));
                    } else {
                        out.println(AESUtils.encrypt("Parking spot not found."));
                    }
                }
            } catch (Exception e) {
                System.err.println("Error updating parking spot: " + e.getMessage());
                out.println(AESUtils.encrypt("Error updating parking spot."));
            }
        }
        private void handleRemoveParkingSpot() throws IOException {
            try {
                // قراءة الرقم المدخل
                String sanitizedSpotNumber = in.readLine();

                // تحويل الرقم المدخل إلى عدد صحيح بعد التنظيف
                int spotNumber = Integer.parseInt(sanitizedSpotNumber);

                // تحضير الاستعلام بشكل آمن
                String sql = "DELETE FROM parking_spots WHERE spot_number = ?";
                try (Connection conn = DriverManager.getConnection(DB_URL);
                     PreparedStatement pstmt = SecurityUtils.prepareSafeStatement(conn, sql, spotNumber)) {

                    int rows = pstmt.executeUpdate();
                    if (rows > 0) {
                        System.out.println("Parking spot " + spotNumber + " removed successfully.");
                        out.println("Parking spot removed successfully.");

                        // استرجاع المواقف المتاحة بعد الحذف
                        handleViewParkingSpots();  // عرض المواقف بعد الحذف
                    } else {
                        out.println("Failed to remove parking spot. Spot not found.");
                    }
                }
            } catch (SQLException e) {
                System.err.println("Error removing parking spot: " + e.getMessage());
                out.println("Error removing parking spot: " + e.getMessage());
            }
        }
        private void handleViewAllReservedParkingSpots() throws Exception {
            try (Connection conn = DriverManager.getConnection(DB_URL)) {
                // الاستعلام الأساسي
                String sql = """
SELECT r.id AS reservation_id, ps.spot_number, u.full_name, r.reserved_at, r.reserved_until, r.fee
FROM reservations r
INNER JOIN parking_spots ps ON r.parking_spot_id = ps.id
INNER JOIN users u ON r.user_id = u.id
ORDER BY r.reserved_at
""";

                // استخدام HashSet لتخزين الحجوزات الفريدة بناءً على spotNumber, reservedAt, reservedUntil
                Set<String> uniqueReservations = new HashSet<>();
                List<String> result = new ArrayList<>();

                // استخدام PreparedStatement مع الحماية
                try (PreparedStatement stmt = SecurityUtils.prepareSafeStatement(conn, sql);
                     ResultSet rs = stmt.executeQuery()) {

                    while (rs.next()) {
                        // قراءة البيانات وتنظيفها لمنع XSS
                        String reservationId = SecurityUtils.sanitizeForXSS(rs.getString("reservation_id"));
                        int spotNumber = rs.getInt("spot_number");
                        String fullName = SecurityUtils.sanitizeForXSS(rs.getString("full_name")); // تنظيف المدخلات
                        String reservedAt = AESUtils.decrypt(SecurityUtils.sanitizeForXSS(rs.getString("reserved_at")));
                        String reservedUntil = AESUtils.decrypt(SecurityUtils.sanitizeForXSS(rs.getString("reserved_until")));
                        double fee = rs.getDouble("fee");

                        // إنشاء مفتاح فريد للحجز
                        String reservationKey = spotNumber + "_" + reservedAt + "_" + reservedUntil;

                        // تحقق من الحجز في المجموعة
                        if (!uniqueReservations.contains(reservationKey)) {
                            uniqueReservations.add(reservationKey); // إضافة الحجز الفريد
                            String reservationEntry = String.format(
                                    "Spot %d, Reserved By: %s, From: %s, Until: %s, Fee: $%.2f",
                                    spotNumber, fullName, reservedAt, reservedUntil, fee
                            );
                            result.add(reservationEntry); // إضافة الحجز إلى القائمة
                        }
                    }
                }

                // إرسال الرد إلى العميل
                if (!result.isEmpty()) {
                    StringBuilder response = new StringBuilder();
                    int counter = 1; // عداد للحجوزات
                    for (String reservation : result) {
                        response.append("Reservation ID: ").append(counter++)
                                .append(", ").append(reservation).append("\n");
                    }
                    // إرسال الرد مشفراً
                    out.println(AESUtils.encrypt(SecurityUtils.sanitizeForXSS(response.toString())));
                } else {
                    // في حالة عدم وجود حجوزات
                    out.println(AESUtils.encrypt(SecurityUtils.sanitizeForXSS("No reserved parking spots found.")));
                }

            } catch (Exception e) {
                System.err.println("Error retrieving reservations: " + e.getMessage());
                out.println(AESUtils.encrypt(SecurityUtils.sanitizeForXSS("Error retrieving reservations.")));
            }
        }
    }
}