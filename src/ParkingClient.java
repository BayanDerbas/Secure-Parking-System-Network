import java.io.*;
import java.net.Socket;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Scanner;

public class ParkingClient {
    private static final String SERVER_HOST = "localhost"; // عنوان الخادم
    private static final int SERVER_PORT = 3000; // منفذ الخادم
    private static PublicKey serverPublicKey;

    private static void fetchServerPublicKey(PrintWriter out, BufferedReader in) throws Exception {
        out.println("get_public_key");
        String base64PublicKey = in.readLine(); // قراءة المفتاح العام من الخادم
        byte[] keyBytes = Base64.getDecoder().decode(base64PublicKey);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        serverPublicKey = keyFactory.generatePublic(spec);
    }
    private static void sendEncryptedMessage(String message, PrintWriter out) throws Exception {
        String encryptedMessage = RSAUtils.encrypt(message, serverPublicKey);
        out.println(encryptedMessage);
    }
    private static String receiveDecryptedMessage(BufferedReader in) throws Exception {
        String encryptedMessage = in.readLine();
        return RSAUtils.decrypt(encryptedMessage);
    }

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