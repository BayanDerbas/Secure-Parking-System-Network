import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;
import java.util.Scanner;

public class ParkingClient {
    private static final String DB_URL = "jdbc:sqlite:parking_system.db";

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        boolean isRunning = true;
        while (isRunning) {
            System.out.println("Welcome to Parking System!");
            System.out.println("1. Create an account");
            System.out.println("2. Login");
            System.out.println("3. Exit");
            System.out.print("Please choose an option (1, 2, or 3): ");
            int choice = scanner.nextInt();
            scanner.nextLine(); // consume newline

            switch (choice) {
                case 1:
                    createAccount(scanner);
                    break;
                case 2:
                    login(scanner);
                    break;
                case 3:
                    System.out.println("Exiting...");
                    isRunning = false;  // Exit the loop and end the program
                    break;
                default:
                    System.out.println("Invalid choice. Please try again.");
                    break;
            }
        }

        scanner.close();  // Ensure the scanner is closed after the loop ends
    }

    private static void createAccount(Scanner scanner) {
        System.out.print("Please enter your full name: ");
        String fullName = scanner.nextLine();

        System.out.print("Please enter your user type (Employee/Visitor): ");
        String userType = scanner.nextLine();

        System.out.print("Please enter your phone number: ");
        String phoneNumber = scanner.nextLine();

        System.out.print("Please enter your car plate: ");
        String carPlate = scanner.nextLine();

        System.out.print("Please enter your password: ");
        String password = scanner.nextLine();

        // Hash the password before storing it
        String passwordHash = BCrypt.hashpw(password, BCrypt.gensalt());

        // Register the user in the database
        boolean isRegistered = registerUser(fullName, userType, phoneNumber, carPlate, passwordHash);
        if (isRegistered) {
            System.out.println("Account created successfully!");
        } else {
            System.out.println("Error creating account.");
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

    private static void login(Scanner scanner) {
        System.out.print("Please enter your full name: ");
        String name = scanner.nextLine();
        System.out.print("Please enter your password: ");
        String password = scanner.nextLine();

        if (authenticateUser(name, password)) {
            System.out.println("Login successful!");
            // Show parking menu after login
            showParkingMenu(scanner, name);
        } else {
            System.out.println("Invalid credentials.");
        }
    }

    private static boolean authenticateUser(String name, String password) {
        String sql = "SELECT id, password_hash FROM users WHERE full_name = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String storedPasswordHash = rs.getString("password_hash");
                int userId = rs.getInt("id");
                return BCrypt.checkpw(password, storedPasswordHash);
            } else {
                return false;
            }
        } catch (SQLException e) {
            System.err.println("Error authenticating user: " + e.getMessage());
            return false;
        }
    }

    private static void showParkingMenu(Scanner scanner, String userName) {
        boolean exit = false;
        while (!exit) {
            System.out.println("Parking Menu:");
            System.out.println("1. View available parking spots");
            System.out.println("2. Reserve a parking spot");
            System.out.println("3. Cancel a reservation");
            System.out.println("4. Exit");
            System.out.print("Please choose an option (1, 2, 3, or 4): ");
            int choice = scanner.nextInt();
            scanner.nextLine(); // consume newline

            switch (choice) {
                case 1:
                    showAvailableSpots();
                    break;
                case 2:
                    reserveParkingSpot(scanner, userName);
                    break;
                case 3:
                    cancelReservation(scanner, userName);
                    break;
                case 4:
                    System.out.println("Exiting parking menu...");
                    exit = true;
                    break;
                default:
                    System.out.println("Invalid choice. Please try again.");
                    break;
            }
        }
    }

    private static void showAvailableSpots() {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            String query = "SELECT spot_number, is_reserved, reserved_at FROM parking_spots WHERE is_reserved = FALSE OR reserved_at IS NULL";
            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(query)) {
                System.out.println("Available parking spots: ");
                while (rs.next()) {
                    int spotNumber = rs.getInt("spot_number");
                    System.out.println(spotNumber + ". Spot " + spotNumber);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching available spots: " + e.getMessage());
        }
    }

    private static void reserveParkingSpot(Scanner scanner, String userName) {
        showAvailableSpots();  // Show available parking spots first

        System.out.print("Enter the parking spot number you want to reserve: ");
        int spotNumber = scanner.nextInt();
        scanner.nextLine();  // consume newline

        System.out.print("Enter the reservation time (YYYY-MM-DD HH:MM): ");
        String reservedAt = scanner.nextLine();

        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            // تحقق من حالة الموقف قبل الحجز
            String checkAvailability = "SELECT is_reserved, reserved_at FROM parking_spots WHERE spot_number = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(checkAvailability)) {
                pstmt.setInt(1, spotNumber);
                ResultSet rs = pstmt.executeQuery();

                if (rs.next()) {
                    boolean isReserved = rs.getBoolean("is_reserved");
                    String existingReservedAt = rs.getString("reserved_at");

                    if (isReserved && existingReservedAt.equals(reservedAt)) {
                        System.out.println("This spot is already reserved at the specified time.");
                    } else if (isReserved) {
                        System.out.println("This spot is already reserved, but you can select a different time.");
                    } else {
                        reserveSpot(spotNumber, userName, reservedAt, conn);
                    }
                } else {
                    System.out.println("Invalid parking spot number.");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error reserving parking spot: " + e.getMessage());
        }
    }

    private static void reserveSpot(int spotNumber, String userName, String reservedAt, Connection conn) throws SQLException {
        // الحصول على user_id بناءً على الاسم
        String getUserId = "SELECT id FROM users WHERE full_name = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(getUserId)) {
            pstmt.setString(1, userName);
            ResultSet userRs = pstmt.executeQuery();
            if (userRs.next()) {
                int userId = userRs.getInt("id");

                // حجز المكان وتحديث التفاصيل
                String reserveSpot = "UPDATE parking_spots SET is_reserved = TRUE, reserved_by = ?, reserved_at = ? WHERE spot_number = ?";
                try (PreparedStatement reserveStmt = conn.prepareStatement(reserveSpot)) {
                    reserveStmt.setInt(1, userId);
                    reserveStmt.setString(2, reservedAt);
                    reserveStmt.setInt(3, spotNumber);
                    int rowsAffected = reserveStmt.executeUpdate();
                    if (rowsAffected > 0) {
                        System.out.println("Spot " + spotNumber + " has been reserved successfully for " + reservedAt + ".");
                    } else {
                        System.out.println("Failed to reserve spot. Please try again.");
                    }
                }
            } else {
                System.out.println("User not found.");
            }
        }
    }

    private static void cancelReservation(Scanner scanner, String userName) {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            String query = "SELECT spot_number, reserved_at FROM parking_spots WHERE reserved_by = (SELECT id FROM users WHERE full_name = ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setString(1, userName);
                ResultSet rs = pstmt.executeQuery();

                System.out.println("Your reserved spots: ");
                boolean hasReservations = false;
                while (rs.next()) {
                    int spotNumber = rs.getInt("spot_number");
                    String reservedAt = rs.getString("reserved_at");
                    System.out.println("Spot " + spotNumber + " reserved at " + reservedAt);
                    hasReservations = true;
                }

                if (!hasReservations) {
                    System.out.println("You have no reservations.");
                    return;
                }

                System.out.print("Enter the parking spot number you want to cancel reservation for: ");
                int spotNumberToCancel = scanner.nextInt();
                scanner.nextLine();  // consume newline

                String cancelSQL = "UPDATE parking_spots SET is_reserved = FALSE, reserved_by = NULL, reserved_at = NULL WHERE reserved_by = (SELECT id FROM users WHERE full_name = ?) AND spot_number = ?";
                try (PreparedStatement cancelStmt = conn.prepareStatement(cancelSQL)) {
                    cancelStmt.setString(1, userName);
                    cancelStmt.setInt(2, spotNumberToCancel);
                    int rowsAffected = cancelStmt.executeUpdate();

                    if (rowsAffected > 0) {
                        System.out.println("Reservation for Spot " + spotNumberToCancel + " has been canceled successfully.");
                    } else {
                        System.out.println("You have not reserved this spot.");
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error canceling reservation: " + e.getMessage());
        }
    }
}