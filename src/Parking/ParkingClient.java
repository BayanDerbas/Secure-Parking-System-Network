package Parking;
import Utils.*;
import java.io.*;
import java.net.Socket;
import java.security.*;
import java.time.LocalDateTime;
import java.time.format.*;
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
        String rawPassword = scanner.nextLine();
        try {
            String encryptedPassword = AESUtils.encrypt(rawPassword);
            out.println(encryptedPassword);
        } catch (Exception e) {
            System.err.println("Error encrypting password: " + e.getMessage());
            return;
        }

        System.out.print("Enter your wallet balance: ");
        double walletBalance = scanner.nextDouble();
        scanner.nextLine(); // consume the newline character
        out.println(walletBalance);

        System.out.println("Server response: " + in.readLine());
        System.out.println("Returning to main menu...");
    }
    private static void handleLogin(PrintWriter out, BufferedReader in, Scanner scanner) throws Exception {
        System.out.println("Sending login request to the server...");
        out.println("login");

        System.out.print("Enter your full name: ");
        String fullName = scanner.nextLine();
        out.println(fullName);
        System.out.println("Sent full name: " + fullName);

        System.out.print("Enter your password: ");
        String rawPassword = scanner.nextLine();
        String encryptedPassword = AESUtils.encrypt(rawPassword);
        out.println(encryptedPassword);
        System.out.println("Sent encrypted password.");

        System.out.println("Waiting for server response...");
        String serverResponse = in.readLine();
        System.out.println("Server response: " + serverResponse);

        if ("Login successful!".equals(serverResponse)) {
            userMenu(out, in, scanner);
        } else {
            System.out.println("Login failed!");
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

        // عرض المواقف المتاحة المستلمة من الخادم
        StringBuilder availableSpots = new StringBuilder();
        String line;
        while (true) {
            line = in.readLine();
            if (line.equals("END_OF_SPOTS")) break;
            availableSpots.append(line).append("\n");
        }
        System.out.println(availableSpots.toString().trim());

        if (availableSpots.toString().trim().equals("No parking spots available.")) {
            System.out.println("No parking spots available. Returning to main menu.");
            return;
        }

        try {
            // إدخال بيانات الحجز
            System.out.print("Enter the spot number: ");
            int spotNumber = scanner.nextInt();
            scanner.nextLine();

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            System.out.print("Enter the start time (yyyy-MM-dd HH:mm): ");
            String startTimeInput = scanner.nextLine();
            LocalDateTime startTime = parseDateTime(startTimeInput, formatter);

            System.out.print("Enter the end time (yyyy-MM-dd HH:mm): ");
            String endTimeInput = scanner.nextLine();
            LocalDateTime endTime = parseDateTime(endTimeInput, formatter);

            // تشفير البيانات المرسلة باستخدام AES
            out.println(AESUtils.encrypt(String.valueOf(spotNumber)));
            out.println(AESUtils.encrypt(startTime.format(formatter)));
            out.println(AESUtils.encrypt(endTime.format(formatter)));

            // إنشاء التوقيع الرقمي وإرساله باستخدام RSA
            PrivateKey privateKey = RSAUtils.loadPrivateKey("C:/Users/ahmad/Documents/private_key.pem");
            String dataToSign = spotNumber + "|" + startTime.format(formatter) + "|" + endTime.format(formatter);
            String reservationSignature = DigitalSignatureUtil.generateDigitalSignature(dataToSign, privateKey);
            out.println(reservationSignature);

            // استقبال الرسوم المشفرة من الخادم (بـ RSA)
            String encryptedFee = in.readLine();
            String decryptedFee = RSAUtils.decrypt(encryptedFee, privateKey);
            double fee = Double.parseDouble(decryptedFee);
            System.out.println("The reservation fee is: " + fee);

            // تأكيد الدفع باستخدام RSA
            System.out.print("Do you want to proceed with the payment? (yes/no): ");
            String confirmation = scanner.nextLine().trim().toLowerCase();
            if (confirmation.equals("no")) {
                out.println("cancel_payment");
                System.out.println("Reservation canceled.");
                return;
            }

            // إرسال توقيع تأكيد الدفع باستخدام RSA
            String paymentConfirmation = "confirm_payment";
            String paymentSignature = DigitalSignatureUtil.generateDigitalSignature(paymentConfirmation, privateKey);
            out.println(paymentSignature);

            // استقبال الرد النهائي من الخادم
            String encryptedResponse = in.readLine();
            String decryptedResponse = RSAUtils.decrypt(encryptedResponse, privateKey);
            System.out.println(decryptedResponse);

            if (decryptedResponse.equals("Payment successful!")) {
                // استقبال رسالة الحجز من الخادم (مشفره بـ AES)
                String encryptedReservationMessage = in.readLine();
                String decryptedReservationMessage = AESUtils.decrypt(encryptedReservationMessage);
                System.out.println(decryptedReservationMessage); // عرض رسالة الحجز الناجحة أو الخطأ
            } else {
                System.out.println("Payment failed.");
            }
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

            // إرسال الرقم المشفر
            out.println(AESUtils.encrypt(String.valueOf(reservationNumber)));

            // تحميل المفتاح الخاص لتوقيع البيانات
            PrivateKey privateKey = RSAUtils.loadPrivateKey("C:/Users/ahmad/Documents/private_key.pem");

            // توقيع الرقم المراد إلغاؤه
            String signedData = DigitalSignatureUtil.generateDigitalSignature(String.valueOf(reservationNumber), privateKey);
            out.println(signedData); // إرسال التوقيع للخادم

            // استقبال مبلغ الاسترداد المشفر
            String encryptedRefund = in.readLine();
            PrivateKey clientPrivateKey = RSAUtils.loadPrivateKey("C:/Users/ahmad/Documents/private_key.pem");
            String refundAmount = RSAUtils.decrypt(encryptedRefund, clientPrivateKey);
            System.out.println("Refund Amount: " + refundAmount);

            // استقبال توقيع مبلغ الاسترداد من الخادم
            String refundSignature = in.readLine();

            // التحقق من التوقيع
            PublicKey publicKey = RSAUtils.loadPublicKey("C:/Users/ahmad/Documents/public_key.pem");
            if (!DigitalSignatureUtil.verifyDigitalSignature(refundAmount, refundSignature, publicKey)) {
                System.err.println("Error: Invalid refund signature.");
                return;
            }

            // استقبال الرسالة النهائية من الخادم
            String response = AESUtils.decrypt(in.readLine());
            System.out.println(response);
        } catch (Exception e) {
            System.err.println("Error during cancellation: " + e.getMessage());
        }
    }
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
    private static LocalDateTime parseDateTime(String dateTimeInput, DateTimeFormatter formatter) {
        try {
            return LocalDateTime.parse(dateTimeInput, formatter);
        } catch (DateTimeParseException e) {
            System.err.println("Error: Invalid date/time format. Please use the format yyyy-MM-dd HH:mm");
            return null; // إعادة القيمة null عند حدوث خطأ
        }
    }
}