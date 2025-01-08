package Parking;
import Utils.*;
import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.security.*;
import java.time.LocalDateTime;
import java.time.format.*;
import java.util.*;
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

        // قراءة البيانات
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

        // تشفير كلمة المرور
        try {
            String encryptedPassword = AESUtils.encrypt(rawPassword);
            out.println(encryptedPassword);
        } catch (Exception e) {
            System.err.println("Error encrypting password: " + e.getMessage());
            return;
        }

        System.out.print("Enter your wallet balance: ");
        out.println(scanner.nextDouble());
        scanner.nextLine();

        // استجابة الخادم
        System.out.println("Server response: " + in.readLine());

        // طلب الشهادة الرقمية
        System.out.println("Do you want to create a digital certificate? (yes/no): ");
        if (scanner.nextLine().equalsIgnoreCase("yes")) {
            System.out.print("Enter distinguished name for certificate (e.g., CN=John Doe, OU=IT, O=Company, C=US):");
            out.println("certificate");
            out.println(scanner.nextLine());
            System.out.println("Server response: " + in.readLine());
        }
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
        System.out.println("........................Digital Certificate........................");
        // إرسال الشهادة
        String certificatePath = "C:\\Users\\ahmad\\Documents\\" + fullName + "_certificate.crt";
        File certificateFile = new File(certificatePath);
        if (certificateFile.exists()) {
            // قراءة الشهادة من الملف
            byte[] certificateBytes = Files.readAllBytes(certificateFile.toPath());
            String encodedCertificate = Base64.getEncoder().encodeToString(certificateBytes);
            System.out.println("Certificate content (Base64 encoded):");
            System.out.println(encodedCertificate); // طباعة الشهادة المشفرة
            out.println(encodedCertificate); // إرسال الشهادة إلى الخادم
            // عند إرسال الشهادة:
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String formattedDateTime = LocalDateTime.now().format(formatter);
            System.out.println("Sent certificate at: " + formattedDateTime); // تاريخ إرسال الشهادة مع فراغات بين التاريخ والساعة
        } else {
            System.err.println("Certificate file not found.");
        }

        // إضافة مفتاح التوثيق
        String usedKey = "Client's Private Key"; // يمكن استبداله بالمفتاح الذي تم استخدامه للإرسال
        System.out.println("Verification key used: " + usedKey);

        System.out.println("Waiting for server response...");
        String serverResponse = in.readLine();
        System.out.println("Server response: " + serverResponse);
        if ("Login successful!".equals(serverResponse)) {
            userMenu(out, in, scanner, fullName);  // تمرير fullName هنا
        } else {
            System.out.println("Login failed!");
        }
    }
    private static void userMenu(PrintWriter out, BufferedReader in, Scanner scanner, String fullName) throws Exception {
        while (true) {
            System.out.println("1. Reserve a parking spot");
            System.out.println("2. View your reservations");
            System.out.println("3. Cancel a reservation"); // خيار جديد
            System.out.println("4. Logout");
            System.out.print("Choose an option: ");
            int choice = scanner.nextInt();
            scanner.nextLine(); // استهلاك السطر المتبقي

            switch (choice) {
                case 1 -> handleReserveSpot(out, in, scanner, fullName);  // تمرير fullName هنا
                case 2 -> handleViewReservations(out, in,fullName);
                case 3 -> handleCancelReservation(out, in, scanner,fullName); // استدعاء دالة الإلغاء
                case 4 -> {
                    System.out.println("Logging out...");
                    return;
                }
                default -> System.out.println("Invalid option. Try again.");
            }
        }
    }
    private static void handleReserveSpot(PrintWriter out, BufferedReader in, Scanner scanner, String fullName) throws IOException {
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
            // تشفير البيانات المدخلة باستخدام AES قبل إرسالها
            String encryptedSpotNumber = AESUtils.encrypt(String.valueOf(spotNumber));
            String encryptedStartTime = AESUtils.encrypt(startTime.format(formatter));
            String encryptedEndTime = AESUtils.encrypt(endTime.format(formatter));
            out.println(encryptedSpotNumber);
            out.println(encryptedStartTime);
            out.println(encryptedEndTime);
            System.out.println("........................Digital Certificate........................");
            // استخدام fullName لتحديد مسار الشهادة
            String certificatePath = "C:/Users/ahmad/Documents/" + fullName + "_certificate.crt";
            File certificateFile = new File(certificatePath);
            if (certificateFile.exists()) {
                // قراءة الشهادة من الملف
                byte[] certificateBytes = Files.readAllBytes(certificateFile.toPath());
                String encodedCertificate = Base64.getEncoder().encodeToString(certificateBytes);
                System.out.println("Certificate content (Base64 encoded):");
                System.out.println(encodedCertificate); // طباعة الشهادة المشفرة
                out.println(encodedCertificate); // إرسال الشهادة إلى الخادم
            } else {
                System.err.println("Certificate file not found.");
            }
            // إنشاء التوقيع الرقمي وإرساله باستخدام RSA
            PrivateKey privateKey = RSAUtils.loadPrivateKey("C:/Users/ahmad/Documents/private_key.pem");
            String dataToSign = encryptedSpotNumber + "|" + encryptedStartTime + "|" + encryptedEndTime;
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
            // إرسال الشهادة مع تأكيد الدفع
            System.out.println("........................Digital Certificate for Payment Verification........................");
             certificatePath = "C:/Users/ahmad/Documents/" + fullName + "_certificate.crt";
             certificateFile = new File(certificatePath);
            if (certificateFile.exists()) {
                // قراءة الشهادة من الملف
                byte[] certificateBytes = Files.readAllBytes(certificateFile.toPath());
                String encodedCertificate = Base64.getEncoder().encodeToString(certificateBytes);
                System.out.println("Certificate content (Base64 encoded):");
                System.out.println(encodedCertificate); // طباعة الشهادة المشفرة
                out.println(encodedCertificate); // إرسال الشهادة إلى الخادم مع تأكيد الدفع
            } else {
                System.err.println("Certificate file not found.");
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
    private static void handleCancelReservation(PrintWriter out, BufferedReader in, Scanner scanner,String fullName) throws Exception {
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
            System.out.println("........................Digital Certificate........................");
            // إضافة الشهادة الرقمية
            try {
                // استخدام fullName لتحديد مسار الشهادة
                String certificatePath = "C:/Users/ahmad/Documents/" + fullName + "_certificate.crt";
                File certificateFile = new File(certificatePath);
                if (certificateFile.exists()) {
                    // قراءة الشهادة من الملف
                    byte[] certificateBytes = Files.readAllBytes(certificateFile.toPath());
                    String encodedCertificate = Base64.getEncoder().encodeToString(certificateBytes);
                    System.out.println("Certificate content (Base64 encoded):");
                    System.out.println(encodedCertificate); // طباعة الشهادة المشفرة
                    out.println(encodedCertificate); // إرسال الشهادة إلى الخادم
                } else {
                    System.err.println("Certificate file not found.");
                }
            } catch (IOException e) {
                System.err.println("Error reading the certificate: " + e.getMessage());
            }
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
    private static void handleViewReservations(PrintWriter out, BufferedReader in, String fullName) throws IOException {
        // إرسال طلب عرض الحجوزات إلى الخادم
        out.println("view_reservations");
        System.out.println("Your reservations:");
        Set<String> reservations = new HashSet<>(); // استخدام مجموعة لمنع التكرار

        String line;
        while (true) {
            line = in.readLine();
            if (line == null || line.equals("END_OF_RESERVATIONS")) { // انتهاء البيانات
                break;
            }
            if (!reservations.contains(line)) { // التحقق من عدم التكرار
                reservations.add(line);
                System.out.println(line);
            }
        }
        System.out.println("........................Digital Certificate........................");

        // إضافة الشهادة الرقمية قبل إرسال الطلب
        try {
            // قراءة الشهادة من الملف
            String certificatePath = "C:/Users/ahmad/Documents/" + fullName + "_certificate.crt";
            File certificateFile = new File(certificatePath);
            if (certificateFile.exists()) {
                byte[] certificateBytes = Files.readAllBytes(certificateFile.toPath());
                String encodedCertificate = Base64.getEncoder().encodeToString(certificateBytes);
                System.out.println("Certificate content (Base64 encoded):");
                System.out.println(encodedCertificate); // طباعة الشهادة المشفرة
                out.println(encodedCertificate); // إرسال الشهادة إلى الخادم
            } else {
                System.err.println("Certificate file not found.");
            }
        } catch (IOException e) {
            System.err.println("Error reading the certificate: " + e.getMessage());
        }
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