package DataBase;
import Utils.AESUtils;
import Utils.RSAUtils;

import java.sql.*;
import java.util.*;
public class DatabaseSetup {
    private static final String DB_URL = "jdbc:sqlite:parking_system.db";
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        // التأكد من إنشاء الجداول قبل البدء في العمليات الأخرى
        createTables();
        while (true) {
            System.out.println("Select an option:");
            System.out.println("1. View all data");
            System.out.println("2. Delete all data");
            System.out.println("3. Create tables");
            System.out.println("4. Drop tables (Delete tables)");
            System.out.println("5. Exit");

            int choice = scanner.nextInt();
            scanner.nextLine();  // Consume newline
            switch (choice) {
                case 1:
                    viewData();
                    break;
                case 2:
                    deleteData();
                    break;
                case 3:
                    createTables();  // Ensure tables are created again if needed
                    break;
                case 4:
                    dropTables();
                    break;
                case 5:
                    System.out.println("Exiting...");
                    scanner.close();
                    return;
                default:
                    System.out.println("Invalid option. Please try again.");
            }
        }
    }
    private static void createTables() {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            if (conn != null) {
                System.out.println("Connected to the database!");
                // إنشاء جدول المستخدمين
                String createUsersTable = """
                    CREATE TABLE IF NOT EXISTS users (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        full_name TEXT NOT NULL,
                        user_type TEXT NOT NULL,
                        phone_number TEXT NOT NULL,
                        car_plate TEXT NOT NULL,
                        password TEXT NOT NULL,
                        wallet_balance REAL DEFAULT 0.0
                    );
                    """;
                // إنشاء جدول مواقف السيارات
                String createParkingSpotsTable = """
                    CREATE TABLE IF NOT EXISTS parking_spots (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        spot_number INTEGER NOT NULL UNIQUE,
                        is_reserved BOOLEAN NOT NULL DEFAULT 0
                    );
                    """;
                // إنشاء جدول الحجوزات
                String createReservationsTable = """
                    CREATE TABLE IF NOT EXISTS reservations (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        parking_spot_id INTEGER NOT NULL,
                        user_id INTEGER NOT NULL,
                        reserved_at TEXT NOT NULL,
                        reserved_until TEXT NOT NULL,
                        fee REAL NOT NULL DEFAULT 0.0,
                        digital_signature_reservation TEXT,  -- توقيع الحجز الرقمي
                        digital_signature_payment TEXT,      -- توقيع الدفع الرقمي
                        FOREIGN KEY (parking_spot_id) REFERENCES parking_spots(id),
                        FOREIGN KEY (user_id) REFERENCES users(id)
                    );
                    """;
                // إنشاء جدول الشهادات
                String createCertificatesTable = """
                        CREATE TABLE IF NOT EXISTS certificates (
                                id INTEGER PRIMARY KEY AUTOINCREMENT,
                                user_id INTEGER NOT NULL,
                                certificate TEXT NOT NULL,
                                FOREIGN KEY (user_id) REFERENCES users (id)
                            );
                    """;
                // تنفيذ جمل إنشاء الجداول
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute(createUsersTable);
                    stmt.execute(createParkingSpotsTable);
                    stmt.execute(createReservationsTable);
                    stmt.execute(createCertificatesTable); // إنشاء جدول الشهادات
                    System.out.println("Tables created successfully!");
                }
                // إضافة المواقف المبدئية
                String insertInitialSpots = """
                    INSERT INTO parking_spots (spot_number)
                    VALUES 
                        (1), (2), (3), (4), (5),
                        (6), (7), (8), (9), (10)
                    ON CONFLICT(spot_number) DO NOTHING;
                    """;
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute(insertInitialSpots);
                    System.out.println("Initial parking spots added successfully!");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error creating tables or inserting parking spots: " + e.getMessage());
        }
    }
    private static void viewData() {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            if (conn != null) {
                System.out.println("Connected to the database!");
                System.out.println(".........................................................");

                // استعلام لعرض جميع المستخدمين
                viewUsers(conn);

                // استعلام لعرض الشهادات الرقمية
                viewCertificates(conn);

                // استعلام لعرض مواقف السيارات
                viewParkingSpots(conn);

                // استعلام لعرض الحجوزات مع التوقيع الرقمي للحجز وتوقيع الدفع
                viewReservations(conn);
                viewSignatures(conn);
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving data: " + e.getMessage());
        }
    }
    private static void viewUsers(Connection conn) {
        String viewUsers = "SELECT * FROM users;";
        try (PreparedStatement pstmt = conn.prepareStatement(viewUsers);
             ResultSet rs = pstmt.executeQuery()) {
            System.out.println("Users:");
            while (rs.next()) {
                int id = rs.getInt("id");
                String fullName = rs.getString("full_name");
                String userType = rs.getString("user_type");
                String phoneNumber = rs.getString("phone_number");
                String carPlate = rs.getString("car_plate");
                String password = rs.getString("password");
                double walletBalance = rs.getDouble("wallet_balance");

                System.out.println("ID: " + id + ", Name: " + fullName + ", Type: " + userType +
                        ", Phone: " + phoneNumber + ", Car Plate: " + carPlate +
                        ", Password: " + password + ", Wallet Balance: " + walletBalance);
            }
            System.out.println(".........................................................");
        } catch (SQLException e) {
            System.err.println("Error retrieving users data: " + e.getMessage());
        }
    }
    private static void viewCertificates(Connection conn) {
        String viewCertificates = """
        SELECT u.full_name, c.certificate
        FROM certificates c
        JOIN users u ON c.user_id = u.id
        ORDER BY u.id;
    """;
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(viewCertificates)) {
            System.out.println("Certificates:");
            String lastUser = "";
            while (rs.next()) {
                String fullName = rs.getString("full_name");
                String certificate = rs.getString("certificate");

                // تحقق من أن المستخدم الحالي مختلف عن المستخدم السابق
                if (!fullName.equals(lastUser)) {
                    System.out.println("User: " + fullName + "\nCertificate:\n" + certificate);
                }
                lastUser = fullName;  // تحديث المستخدم الأخير
            }
            System.out.println(".........................................................");
        } catch (SQLException e) {
            System.err.println("Error retrieving certificates data: " + e.getMessage());
        }
    }
    private static void viewParkingSpots(Connection conn) {
        String viewParkingSpots = "SELECT * FROM parking_spots;";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(viewParkingSpots)) {
            System.out.println(".........................................................");
            System.out.println("Parking Spots:");
            while (rs.next()) {
                int id = rs.getInt("id");
                int spotNumber = rs.getInt("spot_number");

                System.out.println("ID: " + id + ", Spot Number: " + spotNumber);
            }
            System.out.println(".........................................................");
        } catch (SQLException e) {
            System.err.println("Error retrieving parking spots data: " + e.getMessage());
        }
    }
    private static void viewReservations(Connection conn) {
        String viewReservations = """
    SELECT ps.spot_number, u.full_name, r.reserved_at, r.reserved_until, r.fee
    FROM reservations r
    JOIN parking_spots ps ON r.parking_spot_id = ps.id
    JOIN users u ON r.user_id = u.id
    ORDER BY r.reserved_at;
    """;

        try (PreparedStatement pstmt = conn.prepareStatement(viewReservations);
             ResultSet rs = pstmt.executeQuery()) {
            System.out.println("Reservations:");

            // استخدام قائمة لتخزين الحجوزات الفريدة
            List<String> reservations = new ArrayList<>();

            while (rs.next()) {
                int spotNumber = rs.getInt("spot_number");
                String fullName = rs.getString("full_name");

                // فك تشفير أوقات الحجز
                String encryptedReservedAt = rs.getString("reserved_at");
                String encryptedReservedUntil = rs.getString("reserved_until");

                String reservedAt = AESUtils.decrypt(encryptedReservedAt);
                String reservedUntil = AESUtils.decrypt(encryptedReservedUntil);
                double fee = rs.getDouble("fee");

                // تجميع بيانات الحجز
                String reservationEntry = String.format(
                        "Spot Number: %d, Reserved By: %s, Reserved At: %s, Until: %s, Fee: $%.2f",
                        spotNumber, fullName, reservedAt, reservedUntil, fee
                );

                // التحقق من أن الحجز غير مكرر
                if (!reservations.contains(reservationEntry)) {
                    reservations.add(reservationEntry);
                }
            }

            // عرض الحجوزات كقائمة تعدادات
            if (!reservations.isEmpty()) {
                for (int i = 0; i < reservations.size(); i++) {
                    System.out.printf("%d. %s%n", i + 1, reservations.get(i));
                }
            } else {
                System.out.println("No reservations found.");
            }

        } catch (SQLException e) {
            System.err.println("Error retrieving reservations data: " + e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    private static void viewSignatures(Connection conn) {
        String viewSignatures = """
        SELECT ps.spot_number, u.full_name, r.digital_signature_reservation, r.digital_signature_payment
        FROM reservations r
        JOIN parking_spots ps ON r.parking_spot_id = ps.id
        JOIN users u ON r.user_id = u.id
        WHERE r.digital_signature_reservation IS NOT NULL 
          AND r.digital_signature_payment IS NOT NULL
        ORDER BY r.reserved_at;
    """;

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(viewSignatures)) {
            System.out.println();
            System.out.println("Completed Signatures:");

            while (rs.next()) {
                int spotNumber = rs.getInt("spot_number");
                String fullName = rs.getString("full_name");
                String reservationDigitalSignature = rs.getString("digital_signature_reservation");
                String paymentDigitalSignature = rs.getString("digital_signature_payment");

                // Formatting the output for better readability
                System.out.printf("Spot Number: %d, Reserved By: %s%n", spotNumber, fullName);
                System.out.printf("Reservation Signature: %s%n", reservationDigitalSignature);
                System.out.printf("Payment Signature: %s%n", paymentDigitalSignature);
                System.out.println("------------------------------------");
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving signatures data: " + e.getMessage());
        }
    }
    private static void deleteData() {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            if (conn != null) {
                System.out.println("Connected to the database!");

                String deleteUsersData = "DELETE FROM users;";
                String deleteParkingSpotsData = "DELETE FROM parking_spots;";
                String deleteReservationsData = "DELETE FROM reservations;";

                try (PreparedStatement pstmt = conn.prepareStatement(deleteUsersData)) {
                    pstmt.executeUpdate();
                }

                try (PreparedStatement pstmt = conn.prepareStatement(deleteParkingSpotsData)) {
                    pstmt.executeUpdate();
                }

                try (PreparedStatement pstmt = conn.prepareStatement(deleteReservationsData)) {
                    pstmt.executeUpdate();
                }

                System.out.println("All data has been deleted.");
            }
        } catch (SQLException e) {
            System.err.println("Error deleting data: " + e.getMessage());
        }
    }
    private static void dropTables() {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            if (conn != null) {
                String dropUsersTable = "DROP TABLE IF EXISTS users;";
                String dropParkingSpotsTable = "DROP TABLE IF EXISTS parking_spots;";
                String dropReservationsTable = "DROP TABLE IF EXISTS reservations;";

                try (PreparedStatement pstmt = conn.prepareStatement(dropReservationsTable)) {
                    pstmt.executeUpdate();
                }

                try (PreparedStatement pstmt = conn.prepareStatement(dropParkingSpotsTable)) {
                    pstmt.executeUpdate();
                }

                try (PreparedStatement pstmt = conn.prepareStatement(dropUsersTable)) {
                    pstmt.executeUpdate();
                }

                System.out.println("Tables dropped successfully!");
            }
        } catch (SQLException e) {
            System.err.println("Error dropping tables: " + e.getMessage());
        }
    }
}