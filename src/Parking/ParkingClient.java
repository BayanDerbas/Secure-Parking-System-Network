package Parking;
import Utils.AESUtils;
import Utils.RSAUtils;
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
        } catch (Exception e) {
            throw new RuntimeException(e);
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

        System.out.print("Enter your wallet balance: ");
        double walletBalance = scanner.nextDouble();
        scanner.nextLine(); // استهلاك السطر المتبقي
        out.println(walletBalance);

        System.out.println("Server response: " + in.readLine());
        System.out.println("Returning to main menu...");
    }
    private static void handleLogin(PrintWriter out, BufferedReader in, Scanner scanner) throws Exception {
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
    private static void userMenu(PrintWriter out, BufferedReader in, Scanner scanner) throws Exception {
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
    private static void handleReserveSpot(PrintWriter out, BufferedReader in, Scanner scanner) throws IOException {
        // طلب عرض المواقف المتاحة
        out.println("reserve_spot");
        System.out.println("Available parking spots:");

        StringBuilder availableSpots = new StringBuilder();
        String line;
        while (true) {
            line = in.readLine();
            if (line.equals("END_OF_SPOTS")) break; // نهاية القائمة
            availableSpots.append(line).append("\n");
        }
        System.out.println(availableSpots.toString().trim());

        if (availableSpots.toString().trim().equals("No parking spots available.")) {
            System.out.println("No parking spots available. Returning to main menu.");
            return;
        }

        try {
            System.out.print("Enter the spot number: ");
            int spotNumber = scanner.nextInt();
            scanner.nextLine(); // استهلاك السطر المتبقي

            System.out.print("Enter the start time (yyyy-MM-dd HH:mm): ");
            String startTime = scanner.nextLine();

            System.out.print("Enter the end time (yyyy-MM-dd HH:mm): ");
            String endTime = scanner.nextLine();

            // تشفير البيانات باستخدام AES
            out.println(AESUtils.encrypt(String.valueOf(spotNumber)));
            out.println(AESUtils.encrypt(startTime));
            out.println(AESUtils.encrypt(endTime));

            // استقبال الرسوم
            double fee = Double.parseDouble(in.readLine());
            System.out.println("The reservation fee is: " + fee);
            System.out.print("Do you want to proceed with the payment? (yes/no): ");
            String confirmation = scanner.nextLine();

            if (confirmation.equalsIgnoreCase("no")) {
                out.println("cancel_payment");
                System.out.println("Reservation canceled.");
                return;
            }

            // تشفير الدفع باستخدام المفتاح العمومي
            String publicKeyPath = "C:/Users/ahmad/Documents/public_key.pem"; // تعديل المسار حسب الحاجة
            String encryptedPayment = RSAUtils.encrypt("confirm_payment", RSAUtils.loadPublicKey(publicKeyPath));
            out.println(encryptedPayment);

            // استقبال استجابة الخادم
            String response = AESUtils.decrypt(in.readLine());
            System.out.println(response);
        } catch (Exception e) {
            System.err.println("Error during reservation: " + e.getMessage());
        }
    }
    private static void handleCancelReservation(PrintWriter out, BufferedReader in, Scanner scanner) throws Exception {
        out.println("cancel_reservation"); // إرسال أمر الإلغاء للخادم

        System.out.println("Your reservations:");
        StringBuilder reservations = new StringBuilder();
        String line;
        while (true) {
            line = AESUtils.decrypt(in.readLine()); // استقبال البيانات بشكل مشفر
            if (line.equals("END_OF_RESERVATIONS")) break;
            reservations.append(line).append("\n");
        }
        System.out.println(reservations.toString().trim());

        if (reservations.toString().trim().equals("No reservations found.")) {
            System.out.println("You have no reservations to cancel.");
            return;
        }

        try {
            System.out.print("Enter the reservation number to cancel: ");
            int reservationNumber = scanner.nextInt();
            scanner.nextLine(); // استهلاك السطر المتبقي

            out.println(AESUtils.encrypt(String.valueOf(reservationNumber))); // إرسال الرقم بشكل مشفر

            String response = AESUtils.decrypt(in.readLine());
            System.out.println(response);
        } catch (Exception e) {
            System.err.println("Error during cancellation: " + e.getMessage());
        }
    }
    //    private static void handleCancelReservation(PrintWriter out, BufferedReader in, Scanner scanner) throws Exception {
//        // طلب عرض الحجوزات من الخادم
//        out.println("view_reservations");
//        System.out.println("Your current reservations:");
//
//        StringBuilder reservations = new StringBuilder();
//        String line;
//
//        while (true) {
//            line = in.readLine();
//            if (line == null) break; // معالجة الأخطاء في حالة انقطاع الاتصال
//            try {
//                line = AESUtils.decrypt(line); // فك التشفير
//            } catch (Exception e) {
//                System.err.println("Error decrypting data: " + e.getMessage());
//                return;
//            }
//            if (line.equals("END_OF_RESERVATIONS")) break; // نهاية القائمة
//            reservations.append(line).append("\n");
//        }
//
//        System.out.println(reservations.toString().trim());
//
//        // التحقق من وجود حجوزات
//        if (reservations.toString().trim().equals("No reservations found.")) {
//            System.out.println("No reservations found. Returning to main menu.");
//            return;
//        }
//
//        // طلب رقم الحجز للإلغاء
//        System.out.print("Enter the reservation number to cancel: ");
//        int reservationNumber = scanner.nextInt();
//        scanner.nextLine(); // استهلاك السطر المتبقي
//
//        try {
//            // تشفير الرقم المرسل
//            String encryptedReservationNumber = AESUtils.encrypt(String.valueOf(reservationNumber));
//            out.println("cancel_reservation");
//            out.println(encryptedReservationNumber);
//
//            // استقبال استجابة الخادم
//            String response = AESUtils.decrypt(in.readLine());
//            System.out.println(response);
//            System.out.println("..............................................................");
//        } catch (Exception e) {
//            System.err.println("Error in cancellation process: " + e.getMessage());
//            System.out.println("..............................................................");
//        }
//    }
    private static void handleViewReservations(PrintWriter out, BufferedReader in) throws IOException {
        // إرسال طلب عرض الحجوزات إلى الخادم
        out.println("view_reservations");

        System.out.println("Your reservations:");
        StringBuilder reservations = new StringBuilder();
        String line;
        while (true) {
            line = in.readLine();
            if (line == null) break; // معالجة الأخطاء
            try {
                line = AESUtils.decrypt(line); // فك التشفير
            } catch (Exception e) {
                System.err.println("Error decrypting data: " + e.getMessage());
                break;
            }
            if (line.equals("END_OF_RESERVATIONS")) { // انتهاء البيانات
                break;
            }
            reservations.append(line).append("\n");
        }
        System.out.println(reservations.toString().trim());
        System.out.println("..............................................................");
    }
}