package Parking;
import Utils.*;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
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
        String fullName = SecurityUtils.sanitizeForXSS(scanner.nextLine());  // حماية ضد XSS
        out.println(fullName);
        System.out.print("Enter your user type (Visitor/Employee): ");
        String userType = SecurityUtils.sanitizeForXSS(scanner.nextLine());  // حماية ضد XSS
        out.println(userType);
        System.out.print("Enter your phone number: ");
        String phoneNumber = SecurityUtils.sanitizeForXSS(scanner.nextLine());  // حماية ضد XSS
        out.println(phoneNumber);
        System.out.print("Enter your car plate: ");
        String carPlate = SecurityUtils.sanitizeForXSS(scanner.nextLine());  // حماية ضد XSS
        out.println(carPlate);
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
            System.out.print("Enter distinguished name for certificate (e.g., CN=John Doe, OU=IT, O=Company, C=US): ");
            String distinguishedName = SecurityUtils.sanitizeForXSS(scanner.nextLine());  // حماية ضد XSS
            out.println("certificate");
            out.println(distinguishedName);
            System.out.println("Server response: " + in.readLine());
        }
    }
    private static void handleLogin(PrintWriter out, BufferedReader in, Scanner scanner) throws Exception {
        System.out.println("Sending login request to the server...");
        out.println("login");
        System.out.print("Enter your full name: ");
        String fullName = SecurityUtils.sanitizeForXSS(scanner.nextLine());  // حماية ضد XSS
        out.println(fullName);
        System.out.println("Sent full name: " + fullName);
        System.out.print("Enter your password: ");
        String rawPassword = scanner.nextLine();
        String encryptedPassword = AESUtils.encrypt(rawPassword);
        out.println(encryptedPassword);
        System.out.println("Sent encrypted password.");

        System.out.println("........................Digital Certificate........................");
        String certificatePath = "C:\\Users\\ahmad\\Documents\\" + fullName + "_certificate.crt";
        File certificateFile = new File(certificatePath);
        if (certificateFile.exists()) {
            byte[] certificateBytes = Files.readAllBytes(certificateFile.toPath());
            String encodedCertificate = Base64.getEncoder().encodeToString(certificateBytes);
            out.println(encodedCertificate); // إرسال الشهادة
        } else {
            System.err.println("Certificate file not found.");
        }

        System.out.println("Waiting for server response...");
        String serverResponse = in.readLine();
        System.out.println("Server response: " + serverResponse);

        if ("Login successful!".equals(serverResponse)) {
            // استقبال نوع المستخدم
            String userType = in.readLine();
            System.out.println("User type: " + userType);
            userMenu(out, in, scanner, fullName, userType); // تمرير نوع المستخدم
        } else {
            System.out.println("Login failed!");
        }
    }
    private static void userMenu(PrintWriter out, BufferedReader in, Scanner scanner, String fullName, String userType) throws Exception {
        while (true) {
            if ("Employee".equalsIgnoreCase(userType)) {
                // عرض الخيارات للموظف
                System.out.println("1. View parking spots");
                System.out.println("2. View all visitors");
                System.out.println("3. Add a parking spot");
                System.out.println("4. Remove a parking spot");
                System.out.println("5. Cancel parking spot reservation"); // خيار إلغاء الحجز
                System.out.println("6. View all reservations"); // خيار عرض جميع الحجوزات
            } else {
                // عرض الخيارات للمستخدم العادي
                System.out.println("1. Reserve a parking spot");
                System.out.println("2. View your reservations");
            }
            System.out.println("7. Logout"); // التعديل في رقم الخروج ليكون مناسبًا
            System.out.print("Choose an option: ");
            int choice = scanner.nextInt();
            scanner.nextLine(); // استهلاك السطر المتبقي

            switch (choice) {
                case 1 -> {
                    if ("Employee".equalsIgnoreCase(userType)) {
                        handleViewParkingSpots(out, in);
                    } else {
                        handleReserveSpot(out, in, scanner, fullName);
                    }
                }
                case 2 -> {
                    if ("Employee".equalsIgnoreCase(userType)) {
                        handleViewAllVisitors(out, in);
                    } else {
                        handleViewReservations(out, in, fullName);
                    }
                }
                case 3 -> {
                    if ("Employee".equalsIgnoreCase(userType)) {
                        handleAddParkingSpot(out, scanner, in); // تمرير BufferedReader هنا
                    } else {
                        System.out.println("Invalid option. Try again.");
                    }
                }
                case 4 -> {
                    if ("Employee".equalsIgnoreCase(userType)) {
                        handleRemoveParkingSpot(out, scanner);
                    } else {
                        System.out.println("Invalid option. Try again.");
                    }
                }
                case 5 -> {
                    if ("Employee".equalsIgnoreCase(userType)) {
                        handleCancelReservation(out, in, scanner); // إضافة خيار إلغاء الحجز للموظف
                    } else {
                        System.out.println("Invalid option. Try again.");
                    }
                }
                case 6 -> {
                    if ("Employee".equalsIgnoreCase(userType)) {
                        handleViewAllReservations(out, in); // عرض جميع الحجوزات للموظف
                    } else {
                        System.out.println("Invalid option. Try again.");
                    }
                }
                case 7 -> {
                    System.out.println("Logging out...");
                    return;
                }
                default -> System.out.println("Invalid option. Try again.");
            }
        }
    }
    private static void handleViewParkingSpots(PrintWriter out, BufferedReader in) throws IOException {
        try {
            out.println("view_parking_spots");
            String response = in.readLine();
            System.out.println("Response: " + response);

            // التحقق إذا كانت الرسالة تحتوي على نص غير مشفر
            if (response.equals("Parking spot added successfully.") || response.equals("Parking spot removed successfully.")) {
                // إذا كانت الرسالة عبارة عن عملية ناجحة دون بيانات مشفرة
                System.out.println(response);
            } else {
                // فك تشفير Base64 إذا كانت الرسالة مشفرة
                try {
                    byte[] encryptedBytes = Base64.getDecoder().decode(response);
                    String encryptedText = new String(encryptedBytes, StandardCharsets.UTF_8);
                    System.out.println("Encrypted Text: " + encryptedText);

                    // فك تشفير النص المشفر باستخدام AES
                    String decryptedText = AESUtils.decrypt(encryptedText);
                    System.out.println("Decrypted Text: " + decryptedText);

                    System.out.println("Available parking spots:");
                    System.out.println(decryptedText);
                } catch (IllegalArgumentException e) {
                    // في حال كانت الرسالة غير مشفرة وحدث خطأ عند فك تشفيرها
                    System.err.println("Error: Invalid Base64 format or unrecognized response.");
                }
            }
        } catch (Exception e) {
            System.err.println("Error viewing parking spots: " + e.getMessage());
            e.printStackTrace();
        }
    }
    private static void handleViewAllVisitors(PrintWriter out, BufferedReader in) throws Exception {
        out.println("view_all_visitors"); // إرسال الطلب
        System.out.println("Sent request: view_all_visitors"); // طباعة الطلب

        String response = in.readLine(); // استقبال الرد

        // التحقق من نوع الرسالة المستلمة
        if (response.equals("No visitors found.") || response.equals("Error fetching visitors.")) {
            // إذا كانت الرسالة عبارة عن نص عادي
            System.out.println("Server Response: " + response); // طباعة الرد
        } else {
            // إذا كانت الرسالة مشفرة بـ AES
            try {
                String serverResponse = AESUtils.decrypt(response); // فك التشفير
                System.out.println("Server Response: Visitors:\n" + serverResponse); // طباعة الرد
            } catch (Exception e) {
                System.err.println("Error decrypting server response: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    private static void handleAddParkingSpot(PrintWriter out, Scanner scanner, BufferedReader in) {
        try {
            out.println("add_parking_spot");
            System.out.print("Enter the spot number to add: ");
            int spotNumber = scanner.nextInt();
            scanner.nextLine(); // استهلاك السطر المتبقي
            out.println(spotNumber);
            System.out.println("Sent request to add parking spot.");

            // بعد إضافة الموقف، يجب إعادة عرض المواقف
            handleViewParkingSpots(out, in);  // هنا نمرر BufferedReader بدلاً من Scanner
        } catch (Exception e) {
            System.err.println("Error adding parking spot: " + e.getMessage());
        }
    }
    private static void handleRemoveParkingSpot(PrintWriter out, Scanner scanner) {
        try {
            out.println("remove_parking_spot");
            System.out.print("Enter the spot number to remove: ");
            int spotNumber = scanner.nextInt();
            scanner.nextLine(); // استهلاك السطر المتبقي
            out.println(spotNumber);
            System.out.println("Sent request to remove parking spot.");
        } catch (Exception e) {
            System.err.println("Error removing parking spot: " + e.getMessage());
        }
    }
    private static void handleViewAllReservations(PrintWriter out, BufferedReader in) throws Exception {
        out.println("view_reserved_parking_spots");  // إرسال طلب عرض جميع الحجوزات
        System.out.println("Sent request: view_reserved_parking_spots");

        // استقبال الرد المشفر من الخادم
        String encryptedResponse = in.readLine();
        String serverResponse = AESUtils.decrypt(encryptedResponse);  // فك التشفير
        System.out.println("Server Response: \n" + serverResponse);  // طباعة الرد
    }
    private static void handleCancelReservation(PrintWriter out, BufferedReader in, Scanner scanner) throws Exception {
        // عرض جميع الحجوزات للعميل
        out.println("view_reserved_parking_spots"); // إرسال طلب للعرض

        // استلام قائمة الحجوزات
        String response = AESUtils.decrypt(in.readLine());
        System.out.println("Available Reservations:\n" + response);

        // طلب من العميل إدخال معرف الحجز الذي يريد إلغاءه
        System.out.print("Enter reservation ID to cancel: ");
        String reservationId = scanner.nextLine();

        // إرسال طلب إلغاء الحجز إلى الخادم
        out.println("handleCancelReservation");
        out.println(reservationId);  // إرسال معرف الحجز إلى الخادم

        // استلام الرد من الخادم
        String cancelResponse = AESUtils.decrypt(in.readLine());
        System.out.println(cancelResponse);  // طباعة الرد (إلغاء الحجز أو خطأ)
    }
    private static void handleReserveSpot(PrintWriter out, BufferedReader in, Scanner scanner, String fullName) throws IOException {
        // طلب عرض المواقف المتاحة
        out.println("reserve_spot");
        System.out.println("Available parking spots:");
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
            startTimeInput = SecurityUtils.sanitizeForXSS(startTimeInput);
            LocalDateTime startTime = parseDateTime(startTimeInput, formatter);

            System.out.print("Enter the end time (yyyy-MM-dd HH:mm): ");
            String endTimeInput = scanner.nextLine();
            endTimeInput = SecurityUtils.sanitizeForXSS(endTimeInput);
            LocalDateTime endTime = parseDateTime(endTimeInput, formatter);

            // تشفير البيانات المدخلة باستخدام AES قبل إرسالها
            String encryptedSpotNumber = AESUtils.encrypt(String.valueOf(spotNumber));
            String encryptedStartTime = AESUtils.encrypt(startTime.format(formatter));
            String encryptedEndTime = AESUtils.encrypt(endTime.format(formatter));

            out.println(encryptedSpotNumber);
            out.println(encryptedStartTime);
            out.println(encryptedEndTime);

            // إرسال الشهادة الرقمية
            System.out.println("........................Digital Certificate........................");
            String certificatePath = "C:/Users/ahmad/Documents/" + fullName + "_certificate.crt";
            File certificateFile = new File(certificatePath);
            if (certificateFile.exists()) {
                byte[] certificateBytes = Files.readAllBytes(certificateFile.toPath());
                String encodedCertificate = Base64.getEncoder().encodeToString(certificateBytes);
                System.out.println("Certificate content (Base64 encoded):");
                System.out.println(encodedCertificate);
                out.println(encodedCertificate);
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
            if (certificateFile.exists()) {
                byte[] certificateBytes = Files.readAllBytes(certificateFile.toPath());
                String encodedCertificate = Base64.getEncoder().encodeToString(certificateBytes);
                out.println(encodedCertificate);
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
                String encryptedReservationMessage = in.readLine();
                String decryptedReservationMessage = AESUtils.decrypt(encryptedReservationMessage);
                System.out.println(decryptedReservationMessage);
            } else {
                System.out.println("Payment failed.");
            }
        } catch (Exception e) {
            System.err.println("Error during reservation: " + e.getMessage());
        }
    }
    private static void handleViewReservations(PrintWriter out, BufferedReader in, String fullName) throws IOException {
        // إرسال طلب عرض الحجوزات إلى الخادم
        out.println("view_reservations");

        // إرسال الشهادة الرقمية
        System.out.println("Sending certificate to the server...");
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
            return;
        }

        // الانتظار لعرض الحجوزات
        Set<String> reservations = new HashSet<>(); // استخدام مجموعة لمنع التكرار
        System.out.println("Your reservations:");
        String line;
        while (true) {
            line = in.readLine();
            if (line == null || line.equals("END_OF_RESERVATIONS")) { // انتهاء البيانات
                break;
            }
            line = SecurityUtils.sanitizeForXSS(line); // حماية ضد XSS
            if (!reservations.contains(line)) { // التحقق من عدم التكرار
                reservations.add(line);
                System.out.println(line);
            }
        }
        System.out.println("........................End of Reservations........................");
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