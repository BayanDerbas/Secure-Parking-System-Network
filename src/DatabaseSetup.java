import java.sql.*;
import java.util.Scanner;

public class DatabaseSetup {
    private static final String DB_URL = "jdbc:sqlite:parking_system.db";

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        while (true) {
            // عرض الخيارات للمستخدم
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
                    createTables();
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

    // دالة لإنشاء الجداول إذا لم تكن موجودة
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
                        password_hash TEXT NOT NULL
                    );
                """;

                // إنشاء جدول مواقف السيارات
                String createParkingSpotsTable = """
                    CREATE TABLE IF NOT EXISTS parking_spots (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        spot_number INTEGER NOT NULL UNIQUE,
                        is_reserved BOOLEAN NOT NULL DEFAULT 0,
                        reserved_by INTEGER,
                        FOREIGN KEY (reserved_by) REFERENCES users (id)
                    );
                """;

                try (Statement stmt = conn.createStatement()) {
                    stmt.execute(createUsersTable);
                    stmt.execute(createParkingSpotsTable);
                    System.out.println("Tables created successfully!");
                }
            }
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    // دالة لعرض جميع البيانات من جداول المستخدمين ومواقف السيارات
    private static void viewData() {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            if (conn != null) {
                System.out.println("Connected to the database!");

                // عرض البيانات من جدول المستخدمين
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
                        String passwordHash = rs.getString("password_hash");
                        System.out.println("ID: " + id + ", Name: " + fullName + ", Type: " + userType +
                                ", Phone: " + phoneNumber + ", Car Plate: " + carPlate + ", Password Hash: " + passwordHash);
                    }
                }

                // عرض البيانات من جدول مواقف السيارات
                String viewParkingSpots = "SELECT * FROM parking_spots;";
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(viewParkingSpots)) {
                    System.out.println("Parking Spots:");
                    while (rs.next()) {
                        int id = rs.getInt("id");
                        int spotNumber = rs.getInt("spot_number");
                        boolean isReserved = rs.getBoolean("is_reserved");
                        int reservedBy = rs.getInt("reserved_by");
                        System.out.println("ID: " + id + ", Spot Number: " + spotNumber + ", Reserved: " + isReserved +
                                ", Reserved By User ID: " + reservedBy);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving data: " + e.getMessage());
        }
    }

    // دالة لحذف جميع البيانات من الجداول
    private static void deleteData() {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            if (conn != null) {
                System.out.println("Connected to the database!");

                // حذف جميع البيانات من جدول المستخدمين
                String deleteUsersData = "DELETE FROM users;";
                String deleteParkingSpotsData = "DELETE FROM parking_spots;";

                try (Statement stmt = conn.createStatement()) {
                    stmt.executeUpdate(deleteUsersData);
                    stmt.executeUpdate(deleteParkingSpotsData);
                    System.out.println("All data has been deleted.");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error deleting data: " + e.getMessage());
        }
    }

    // دالة لحذف الجداول
    private static void dropTables() {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            if (conn != null) {
                System.out.println("Connected to the database!");

                // حذف جداول المستخدمين ومواقف السيارات
                String dropUsersTable = "DROP TABLE IF EXISTS users;";
                String dropParkingSpotsTable = "DROP TABLE IF EXISTS parking_spots;";

                try (Statement stmt = conn.createStatement()) {
                    stmt.executeUpdate(dropUsersTable);
                    stmt.executeUpdate(dropParkingSpotsTable);
                    System.out.println("Tables have been dropped.");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error dropping tables: " + e.getMessage());
        }
    }
}
