package DataBase;
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
    // إنشاء الجداول إذا لم تكن موجودة
    private static void createTables() {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            if (conn != null) {
                System.out.println("Connected to the database!");

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

                String createParkingSpotsTable = """
                CREATE TABLE IF NOT EXISTS parking_spots (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    spot_number INTEGER NOT NULL UNIQUE,
                    is_reserved BOOLEAN NOT NULL DEFAULT 0
                );
                """;

                String createReservationsTable = """
                CREATE TABLE IF NOT EXISTS reservations (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    parking_spot_id INTEGER NOT NULL,
                    user_id INTEGER NOT NULL,
                    reserved_at TEXT NOT NULL,
                    reserved_until TEXT NOT NULL,
                    fee REAL NOT NULL DEFAULT 0.0,
                    FOREIGN KEY (parking_spot_id) REFERENCES parking_spots(id),
                    FOREIGN KEY (user_id) REFERENCES users(id)
                );
                """;

                try (Statement stmt = conn.createStatement()) {
                    stmt.execute(createUsersTable);
                    stmt.execute(createParkingSpotsTable);
                    stmt.execute(createReservationsTable);
                    System.out.println("Tables created successfully!");

                    String insertInitialSpots = """
                    INSERT INTO parking_spots (spot_number, is_reserved)
                    VALUES 
                        (1, 0), (2, 0), (3, 0), (4, 0), (5, 0),
                        (6, 0), (7, 0), (8, 0), (9, 0), (10, 0)
                    ON CONFLICT(spot_number) DO NOTHING;
                    """;
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

                // استعلام لعرض جميع المستخدمين
                String viewUsers = "SELECT * FROM users;";
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(viewUsers)) {
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
                }

                // استعلام لعرض مواقف السيارات
                String viewParkingSpots = "SELECT * FROM parking_spots;";
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(viewParkingSpots)) {
                    System.out.println("Parking Spots:");
                    while (rs.next()) {
                        int id = rs.getInt("id");
                        int spotNumber = rs.getInt("spot_number");
                        boolean isReserved = rs.getBoolean("is_reserved");
                        System.out.println("ID: " + id + ", Spot Number: " + spotNumber);
                    }
                }

                // استعلام لعرض الحجوزات
                String viewReservations = """
                        SELECT ps.spot_number, u.full_name, r.reserved_at, r.reserved_until
                        FROM reservations r
                        JOIN parking_spots ps ON r.parking_spot_id = ps.id
                        JOIN users u ON r.user_id = u.id
                        ORDER BY r.reserved_at;
                        """;
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(viewReservations)) {
                    System.out.println("Reservations:");
                    while (rs.next()) {
                        int spotNumber = rs.getInt("spot_number");
                        String fullName = rs.getString("full_name");
                        String reservedAt = rs.getString("reserved_at");
                        String reservedUntil = rs.getString("reserved_until");
                        System.out.println("Spot Number: " + spotNumber + ", Reserved By: " + fullName +
                                ", Reserved At: " + reservedAt + ", Until: " + reservedUntil);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving data: " + e.getMessage());
        }
    }
    // حذف البيانات
    private static void deleteData() {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            if (conn != null) {
                System.out.println("Connected to the database!");

                String deleteUsersData = "DELETE FROM users;";
                String deleteParkingSpotsData = "DELETE FROM parking_spots;";
                String deleteReservationsData = "DELETE FROM reservations;";

                try (Statement stmt = conn.createStatement()) {
                    stmt.executeUpdate(deleteUsersData);
                    stmt.executeUpdate(deleteParkingSpotsData);
                    stmt.executeUpdate(deleteReservationsData);
                    System.out.println("All data has been deleted.");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error deleting data: " + e.getMessage());
        }
    }
    // حذف الجداول
    private static void dropTables() {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            if (conn != null) {
                String dropUsersTable = "DROP TABLE IF EXISTS users;";
                String dropParkingSpotsTable = "DROP TABLE IF EXISTS parking_spots;";
                String dropReservationsTable = "DROP TABLE IF EXISTS reservations;";

                try (Statement stmt = conn.createStatement()) {
                    stmt.execute(dropReservationsTable);
                    stmt.execute(dropParkingSpotsTable);
                    stmt.execute(dropUsersTable);
                    System.out.println("Tables dropped successfully!");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error dropping tables: " + e.getMessage());
        }
    }
}