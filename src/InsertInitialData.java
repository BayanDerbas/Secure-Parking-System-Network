import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Scanner;

public class InsertInitialData {
    private static final String DB_URL = "jdbc:sqlite:parking_system.db";

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        // Display the menu options to the user
        System.out.println("Select an operation:");
        System.out.println("1. Add Parking Spots");
        System.out.println("2. Delete a Specific Parking Spot");
        System.out.println("3. Delete All Parking Spots and Recreate Table");
        System.out.print("Enter your choice (1, 2 or 3): ");
        int choice = scanner.nextInt();

        switch (choice) {
            case 1:
                addParkingSpots();
                break;
            case 2:
                deleteParkingSpot();
                break;
            case 3:
                deleteAllParkingSpotsAndRecreateTable();
                break;
            default:
                System.out.println("Invalid choice.");
                break;
        }

        scanner.close();
    }

    // Method to add new parking spots
    private static void addParkingSpots() {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {

            // Insert initial parking spots
            String insertInitialSpots = """
                INSERT INTO parking_spots (spot_number)
                VALUES (1), (2), (3), (4), (5), (6), (7), (8), (9), (10)
                ON CONFLICT(spot_number) DO NOTHING;
            """;
            stmt.execute(insertInitialSpots);

            System.out.println("Parking spots added successfully!");

        } catch (SQLException e) {
            System.err.println("Error adding parking spots: " + e.getMessage());
        }
    }

    // Method to delete a specific parking spot
    private static void deleteParkingSpot() {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter the spot number you want to delete: ");
        int spotNumber = scanner.nextInt();

        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {

            // Delete the specific parking spot
            String deleteSpot = "DELETE FROM parking_spots WHERE spot_number = " + spotNumber;
            int rowsAffected = stmt.executeUpdate(deleteSpot);

            if (rowsAffected > 0) {
                System.out.println("Parking spot number " + spotNumber + " deleted successfully!");
            } else {
                System.out.println("Parking spot number " + spotNumber + " not found.");
            }

        } catch (SQLException e) {
            System.err.println("Error deleting parking spot: " + e.getMessage());
        }
    }

    // Method to delete all parking spots and recreate the table
    private static void deleteAllParkingSpotsAndRecreateTable() {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {

            // Delete all parking spots
            String deleteAllSpots = "DELETE FROM parking_spots";
            stmt.executeUpdate(deleteAllSpots);
            System.out.println("All parking spots have been deleted successfully!");

            // Recreate the table after deleting all spots
            String createTable = """
                CREATE TABLE IF NOT EXISTS parking_spots (
                    spot_number INTEGER PRIMARY KEY,
                    is_reserved BOOLEAN DEFAULT 0
                );
            """;
            stmt.executeUpdate(createTable);
            System.out.println("Parking spots table recreated successfully!");

            // Optionally, add initial spots again after recreating the table
            addParkingSpots();

        } catch (SQLException e) {
            System.err.println("Error deleting all parking spots or recreating the table: " + e.getMessage());
        }
    }
}