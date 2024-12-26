import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class ParkingClient {
    private static final String SERVER_HOST = "localhost"; // عنوان الخادم
    private static final int SERVER_PORT = 3000; // منفذ الخادم
    public static void main(String[] args) {
        try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             Scanner scanner = new Scanner(System.in)) {

            System.out.println("Connected to the server!");

            while (true) {
                System.out.println("Welcome to Parking System!");
                System.out.println("1. Register");
                System.out.println("2. Login");
                System.out.println("3. Exit");
                System.out.print("Choose an option: ");
                int choice = scanner.nextInt();
                scanner.nextLine(); // استهلاك السطر المتبقي

                switch (choice) {
                    case 1 -> handleRegister(out, in, scanner);
                    case 2 -> handleLogin(out, in, scanner);
                    case 3 -> {
                        System.out.println("Exiting...");
                        return;
                    }
                    default -> System.out.println("Invalid option. Try again.");
                }
            }

        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
    private static void handleRegister(PrintWriter out, BufferedReader in, Scanner scanner) throws IOException {
        out.println("register");
        System.out.print("Enter your full name: ");
        out.println(scanner.nextLine());
        System.out.print("Enter your user type (Visitor/Employee): ");
        out.println(scanner.nextLine());
        System.out.print("Enter your phone number: ");
        out.println(scanner.nextLine());
        System.out.print("Enter your car plate: ");
        out.println(scanner.nextLine());
        System.out.print("Enter your password: ");
        out.println(scanner.nextLine());

        System.out.println("Server response: " + in.readLine());
    }
    private static void handleLogin(PrintWriter out, BufferedReader in, Scanner scanner) throws IOException {
        out.println("login");
        System.out.print("Enter your full name: ");
        out.println(scanner.nextLine());
        System.out.print("Enter your password: ");
        out.println(scanner.nextLine());

        String serverResponse = in.readLine();
        System.out.println("Server response: " + serverResponse);

        if (serverResponse.equals("Login successful!")) {
            userMenu(out, in, scanner);
        }
    }
    private static void userMenu(PrintWriter out, BufferedReader in, Scanner scanner) throws IOException {
        while (true) {
            System.out.println("1. Reserve a parking spot");
            System.out.println("2. Logout");
            System.out.print("Choose an option: ");
            int choice = scanner.nextInt();
            scanner.nextLine(); // استهلاك السطر المتبقي

            switch (choice) {
                case 1 -> handleReserveSpot(out, in, scanner);  // استدعاء دالة الحجز
                case 2 -> {
                    System.out.println("Logging out...");
                    return;  // العودة إلى القائمة الرئيسية
                }
                default -> System.out.println("Invalid option. Try again.");
            }
        }
    }
    private static void handleReserveSpot(PrintWriter out, BufferedReader in, Scanner scanner) throws IOException {
        out.println("reserve_spot");

        System.out.println("Available spots:");
        StringBuilder spots = new StringBuilder();
        String line;
        while (!(line = in.readLine()).equals("END_OF_SPOTS")) { // قراءة حتى نهاية الرسائل
            spots.append(line).append("\n");
        }
        System.out.println(spots.toString().trim());

        if (spots.toString().trim().equals("No parking spots available.")) {
            System.out.println("No parking spots available. Returning to main menu.");
            return;
        }

        System.out.print("Enter the spot number you want to reserve: ");
        out.println(scanner.nextInt());
        scanner.nextLine(); // استهلاك السطر المتبقي
        System.out.print("Enter the reservation start time (e.g., 2024-12-31 14:00): ");
        out.println(scanner.nextLine());
        System.out.print("Enter the reservation end time (e.g., 2024-12-31 16:00): ");
        out.println(scanner.nextLine());

        String serverResponse = in.readLine();
        System.out.println("Server response: " + serverResponse);

        // عرض المواقف المتاحة مرة أخرى بعد الحجز
        handleReserveSpot(out, in, scanner);
    }
}
