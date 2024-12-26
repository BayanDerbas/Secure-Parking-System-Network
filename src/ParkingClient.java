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
        try {
            String rawPassword = scanner.nextLine();
            System.out.println("Password before encryption: " + rawPassword); // Debugging
            String encryptedPassword = AESUtils.encrypt(rawPassword);
            System.out.println("Encrypted password: " + encryptedPassword); // Debugging
            out.println(encryptedPassword);
        } catch (Exception e) {
            System.err.println("Error encrypting password: " + e.getMessage());
            return;
        }

        System.out.println("Server response: " + in.readLine());
    }
    private static void handleLogin(PrintWriter out, BufferedReader in, Scanner scanner) throws IOException {
        out.println("login");
        System.out.print("Enter your full name: ");
        out.println(scanner.nextLine());
        System.out.print("Enter your password: ");
        try {
            String encryptedPassword = AESUtils.encrypt(scanner.nextLine());
            out.println(encryptedPassword);
        } catch (Exception e) {
            System.err.println("Error encrypting password: " + e.getMessage());
            return;
        }

        String serverResponse = in.readLine();
        System.out.println("Server response: " + serverResponse);

        if (serverResponse.equals("Login successful!")) {
            userMenu(out, in, scanner);
        }
    }
    private static void userMenu(PrintWriter out, BufferedReader in, Scanner scanner) throws IOException {
        while (true) {
            System.out.println("1. Reserve a parking spot");
            System.out.println("2. View your reservations");
            System.out.println("3. Cancel a reservation"); // خيار جديد
            System.out.println("4. Logout");
            System.out.print("Choose an option: ");
            int choice = scanner.nextInt();
            scanner.nextLine(); // استهلاك السطر المتبقي

            switch (choice) {
                case 1 -> handleReserveSpot(out, in, scanner);
                case 2 -> handleViewReservations(out, in);
                case 3 -> handleCancelReservation(out, in, scanner); // استدعاء دالة الإلغاء
                case 4 -> {
                    System.out.println("Logging out...");
                    return;
                }
                default -> System.out.println("Invalid option. Try again.");
            }
        }
    }
    private static void handleCancelReservation(PrintWriter out, BufferedReader in, Scanner scanner) throws IOException {
        handleViewReservations(out, in); // عرض الحجوزات الحالية
        System.out.print("Enter the spot number to cancel: ");
        int spotNumber = scanner.nextInt();
        scanner.nextLine(); // استهلاك السطر المتبقي

        try {
            String encryptedSpotNumber = AESUtils.encrypt(String.valueOf(spotNumber)); // تشفير البيانات
            out.println("cancel_reservation"); // إرسال الطلب
            out.println(encryptedSpotNumber); // إرسال الرقم المشفر

            String response = AESUtils.decrypt(in.readLine()); // فك التشفير للاستجابة
            System.out.println(response);
        } catch (Exception e) {
            System.err.println("Error in cancellation process: " + e.getMessage());
        }
    }
    private static void handleViewReservations(PrintWriter out, BufferedReader in) throws IOException {
        out.println("view_reservations"); // إرسال الطلب إلى الخادم

        System.out.println("Your reservations:");
        StringBuilder reservations = new StringBuilder();
        String line;
        while (true) {
            line = in.readLine();
            try {
                line = AESUtils.decrypt(line); // فك التشفير
            } catch (Exception e) {
                System.err.println("Error decrypting data: " + e.getMessage());
                break;
            }
            if (line.equals("END_OF_RESERVATIONS")) { // التحقق من الإشارة النهائية
                break;
            }
            reservations.append(line).append("\n");
        }
        System.out.println(reservations.toString().trim());
    }
    private static void handleReserveSpot(PrintWriter out, BufferedReader in, Scanner scanner) throws IOException {
        out.println("reserve_spot");

        System.out.println("Available spots:");
        StringBuilder spots = new StringBuilder();
        String line;
        while (!(line = in.readLine()).equals("END_OF_SPOTS")) {
            spots.append(line).append("\n");
        }
        System.out.println(spots.toString().trim());

        if (spots.toString().trim().equals("No parking spots available.")) {
            System.out.println("No parking spots available. Returning to main menu.");
            return;
        }

        System.out.print("Enter the spot number you want to reserve: ");
        int spotNumber = scanner.nextInt();
        scanner.nextLine();
        System.out.print("Enter the reservation start time (e.g., 2024-12-31 14:00): ");
        String startTime = scanner.nextLine();
        System.out.print("Enter the reservation end time (e.g., 2024-12-31 16:00): ");
        String endTime = scanner.nextLine();

        try {
            // تشفير البيانات
            String encryptedSpotNumber = AESUtils.encrypt(String.valueOf(spotNumber));
            String encryptedStartTime = AESUtils.encrypt(startTime);
            String encryptedEndTime = AESUtils.encrypt(endTime);

            // إرسال البيانات المشفرة
            out.println(encryptedSpotNumber);
            out.println(encryptedStartTime);
            out.println(encryptedEndTime);
        } catch (Exception e) {
            System.err.println("Error encrypting reservation data: " + e.getMessage());
            return;
        }

        String serverResponse = in.readLine();
        System.out.println("Server response: " + serverResponse);
    }
}