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
        String fullName = SecurityUtils.sanitizeForXSS(scanner.nextLine()); // حماية ضد XSS
        out.println(fullName);
        System.out.println("Sent full name: " + fullName);

        System.out.print("Enter your password: ");
        String rawPassword = scanner.nextLine();
        String encryptedPassword = AESUtils.encrypt(rawPassword);
        out.println(encryptedPassword);
        System.out.println("Sent encrypted password.");

        System.out.println("........................Digital Certificate........................");
        String certificatePath = "C:/Users/ahmad/Documents/" + fullName + "_certificate.crt";
        File certificateFile = new File(certificatePath);

        if (certificateFile.exists()) {
            System.out.println("Certificate file found. Encoding...");
            byte[] certificateBytes = Files.readAllBytes(certificateFile.toPath());
            String encodedCertificate = Base64.getEncoder().encodeToString(certificateBytes);
            out.println(encodedCertificate);
            System.out.println("Certificate sent to the server.");
        } else {
            System.err.println("Certificate file not found at path: " + certificatePath);
            out.println(""); // إرسال قيمة فارغة لتجنب انتظار الخادم
        }

        System.out.println("Waiting for server response...");
        String serverResponse = in.readLine();
        System.out.println("Server response: " + serverResponse);

        if ("Login successful!".equals(serverResponse)) {
            String userType = in.readLine(); // استقبال نوع المستخدم
            System.out.println("User type: " + userType);
            userMenu(out, in, scanner, fullName, userType); // تمرير نوع المستخدم
        } else {
            System.out.println("Login failed!");
        }
    }
    private static void userMenu(PrintWriter out, BufferedReader in, Scanner scanner, String fullName, String userType) throws Exception {
        while (true) {
            try {
                // عرض الخيارات بناءً على نوع المستخدم
                if ("Employee".equalsIgnoreCase(userType)) {
                    System.out.println("1. View parking spots");
                    System.out.println("2. View all visitors");
                    System.out.println("3. Add a parking spot");
                    System.out.println("4. Remove a parking spot");
                    System.out.println("5. View all reservations");
                    System.out.println("6. Reserve a parking spot");
                    System.out.println("7. View your reservations");
                    System.out.println("8. Edit a parking spot"); // خيار تعديل الموقف
                } else {
                    System.out.println("1. Reserve a parking spot");
                    System.out.println("2. View your reservations");
                }
                System.out.println("9. Logout"); // رقم الخروج محدث ليكون مناسبًا
                System.out.print("Choose an option: ");

                // التحقق من الإدخال
                if (!scanner.hasNextInt()) {
                    System.out.println("Invalid input. Please enter a valid number.");
                    scanner.next(); // تخطي الإدخال غير الصحيح
                    continue; // إعادة عرض القائمة
                }

                int choice = scanner.nextInt();
                scanner.nextLine(); // استهلاك السطر المتبقي

                // التعامل مع الخيارات
                switch (choice) {
                    case 1 -> {
                        if ("Employee".equalsIgnoreCase(userType)) {
                            handleViewParkingSpots(out, in, fullName);
                        } else {
                            handleReserveSpot(out, in, scanner, fullName);
                        }
                    }
                    case 2 -> {
                        if ("Employee".equalsIgnoreCase(userType)) {
                            handleViewAllVisitors(out, in, fullName);
                        } else {
                            handleViewReservations(out, in, fullName);
                        }
                    }
                    case 3 -> {
                        if ("Employee".equalsIgnoreCase(userType)) {
                            handleAddParkingSpot(out, scanner, in, fullName);
                        } else {
                            System.out.println("Invalid option. Try again.");
                        }
                    }
                    case 4 -> {
                        if ("Employee".equalsIgnoreCase(userType)) {
                            handleRemoveParkingSpot(out, scanner, in, fullName);
                        } else {
                            System.out.println("Invalid option. Try again.");
                        }
                    }
                    case 5 -> {
                        if ("Employee".equalsIgnoreCase(userType)) {
                            handleViewAllReservations(out, in, fullName);
                        } else {
                            System.out.println("Invalid option. Try again.");
                        }
                    }
                    case 6 -> {
                        if ("Employee".equalsIgnoreCase(userType)) {
                            handleReserveSpot(out, in, scanner, fullName);
                        } else {
                            System.out.println("Invalid option. Try again.");
                        }
                    }
                    case 7 -> {
                        if ("Employee".equalsIgnoreCase(userType)) {
                            handleViewReservations(out, in, fullName);
                        } else {
                            System.out.println("Invalid option. Try again.");
                        }
                    }
                    case 8 -> {
                        if ("Employee".equalsIgnoreCase(userType)) {
                            handleEditParkingSpotName(out, in, scanner, fullName); // استدعاء وظيفة تعديل الموقف
                        } else {
                            System.out.println("Invalid option. Try again.");
                        }
                    }
                    case 9 -> {
                        System.out.println("Logging out...");
                        return; // الخروج من القائمة
                    }
                    default -> System.out.println("Invalid option. Try again.");
                }
            } catch (Exception e) {
                System.out.println("An error occurred. Please try again.");
                scanner.nextLine(); // تنظيف الإدخال الخاطئ
            }
        }
    }
    private static void handleViewParkingSpots(PrintWriter out, BufferedReader in, String fullName) throws IOException {
        try {
            // إرسال طلب عرض المواقف
            out.println("view_parking_spots");

            // إرسال الشهادة الرقمية إلى الخادم
            System.out.println("........................Digital Certificate........................");
            String certificatePath = "C:/Users/ahmad/Documents/" + fullName + "_certificate.crt";
            File certificateFile = new File(certificatePath);
            if (certificateFile.exists()) {
                byte[] certificateBytes = Files.readAllBytes(certificateFile.toPath());
                String encodedCertificate = Base64.getEncoder().encodeToString(certificateBytes);
                System.out.println("Certificate content (Base64 encoded):");
                System.out.println(encodedCertificate);
                out.println(encodedCertificate); // إرسال الشهادة إلى الخادم
            } else {
                System.err.println("Certificate file not found.");
                return;
            }

            // استقبال البيانات المشفرة من الخادم
            String response = in.readLine();
            System.out.println("Response: " + response);

            // التحقق إذا كانت الرسالة تحتوي على نص غير مشفر
            if (response.equals("Parking spot added successfully.") || response.equals("Parking spot removed successfully.")) {
                // إذا كانت الرسالة عبارة عن عملية ناجحة دون بيانات مشفرة
                System.out.println(response);
            } else {
                // تنظيف البيانات للحماية ضد XSS
                response = SecurityUtils.sanitizeForXSS(response);

                // فك تشفير Base64 إذا كانت الرسالة مشفرة
                try {
                    byte[] encryptedBytes = Base64.getDecoder().decode(response);
                    String encryptedText = new String(encryptedBytes, StandardCharsets.UTF_8);
                    System.out.println("Encrypted Text: " + AESUtils.decrypt(encryptedText));

                    // فك تشفير النص المشفر باستخدام AES
                    String decryptedText = AESUtils.decrypt(encryptedText);
                    System.out.println("Decrypted Text: " + decryptedText);

                    System.out.println("Available parking spots:");
                    System.out.println(decryptedText);
                    // إرسال الشهادة الرقمية إلى الخادم
                    System.out.println("........................Digital Certificate........................");
                    certificatePath = "C:/Users/ahmad/Documents/" + fullName + "_certificate.crt";
                    certificateFile = new File(certificatePath);
                    if (certificateFile.exists()) {
                        byte[] certificateBytes = Files.readAllBytes(certificateFile.toPath());
                        String encodedCertificate = Base64.getEncoder().encodeToString(certificateBytes);
                        System.out.println("Certificate content (Base64 encoded):");
                        System.out.println(encodedCertificate);
                        out.println(encodedCertificate); // إرسال الشهادة إلى الخادم
                    } else {
                        System.err.println("Certificate file not found.");
                        return;
                    }
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
    private static void handleViewAllVisitors(PrintWriter out, BufferedReader in,String fullName) throws Exception {
        out.println("view_all_visitors"); // إرسال الطلب
        // إرسال الشهادة الرقمية إلى الخادم
        System.out.println("........................Digital Certificate........................");
        String certificatePath = "C:/Users/ahmad/Documents/" + fullName + "_certificate.crt";
        File certificateFile = new File(certificatePath);
        if (certificateFile.exists()) {
            byte[] certificateBytes = Files.readAllBytes(certificateFile.toPath());
            String encodedCertificate = Base64.getEncoder().encodeToString(certificateBytes);
            System.out.println("Certificate content (Base64 encoded):");
            System.out.println(encodedCertificate);
            out.println(encodedCertificate); // إرسال الشهادة إلى الخادم
        } else {
            System.err.println("Certificate file not found.");
            return;
        }
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
                // إرسال الشهادة الرقمية إلى الخادم
                System.out.println("........................Digital Certificate........................");
                certificatePath = "C:/Users/ahmad/Documents/" + fullName + "_certificate.crt";
                certificateFile = new File(certificatePath);
                if (certificateFile.exists()) {
                    byte[] certificateBytes = Files.readAllBytes(certificateFile.toPath());
                    String encodedCertificate = Base64.getEncoder().encodeToString(certificateBytes);
                    System.out.println("Certificate content (Base64 encoded):");
                    System.out.println(encodedCertificate);
                    out.println(encodedCertificate);  // إرسال الشهادة إلى الخادم
                } else {
                    System.err.println("Certificate file not found.");
                    return;
                }
            } catch (Exception e) {
                System.err.println("Error decrypting server response: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    private static void handleAddParkingSpot(PrintWriter out, Scanner scanner, BufferedReader in,String fullName) {
        try {
            // عرض المواقف المتاحة
            handleViewParkingSpots(out, in ,fullName);
            out.println("add_parking_spot");
            System.out.print("Enter the spot number to add: ");
            int spotNumber = scanner.nextInt();
            scanner.nextLine(); // استهلاك السطر المتبقي
            out.println(spotNumber);
            System.out.println("Sent request to add parking spot.");
            // إرسال الشهادة الرقمية إلى الخادم
            System.out.println("........................Digital Certificate........................");
            String certificatePath = "C:/Users/ahmad/Documents/" + fullName + "_certificate.crt";
            File certificateFile = new File(certificatePath);
            if (certificateFile.exists()) {
                byte[] certificateBytes = Files.readAllBytes(certificateFile.toPath());
                String encodedCertificate = Base64.getEncoder().encodeToString(certificateBytes);
                System.out.println("Certificate content (Base64 encoded):");
                System.out.println(encodedCertificate);
                out.println(encodedCertificate);  // إرسال الشهادة إلى الخادم
            } else {
                System.err.println("Certificate file not found.");
                return;
            }
        } catch (Exception e) {
            System.err.println("Error adding parking spot: " + e.getMessage());
        }
    }
    private static void handleRemoveParkingSpot(PrintWriter out, Scanner scanner, BufferedReader in,String fullName) throws IOException {
        // عرض المواقف المتاحة قبل الطلب
        handleViewParkingSpots(out, in,fullName);

        try {
            out.println("remove_parking_spot");
            System.out.print("Enter the spot number to remove: ");
            int spotNumber = scanner.nextInt();
            scanner.nextLine(); // استهلاك السطر المتبقي

            // تنظيف المدخلات قبل الإرسال
            String sanitizedSpotNumber = SecurityUtils.sanitizeForXSS(String.valueOf(spotNumber));
            out.println(sanitizedSpotNumber);
            System.out.println("Sent request to remove parking spot.");

            // إرسال الشهادة الرقمية إلى الخادم
            System.out.println("........................Digital Certificate........................");
            String certificatePath = "C:/Users/ahmad/Documents/" + fullName + "_certificate.crt";
            File certificateFile = new File(certificatePath);
            if (certificateFile.exists()) {
                byte[] certificateBytes = Files.readAllBytes(certificateFile.toPath());
                String encodedCertificate = Base64.getEncoder().encodeToString(certificateBytes);
                System.out.println("Certificate content (Base64 encoded):");
                System.out.println(encodedCertificate);
                out.println(encodedCertificate);  // إرسال الشهادة إلى الخادم
            } else {
                System.err.println("Certificate file not found.");
                return;
            }
        } catch (Exception e) {
            System.err.println("Error removing parking spot: " + e.getMessage());
        }
    }
    private static void handleEditParkingSpotName(PrintWriter out, BufferedReader in, Scanner scanner, String fullName) throws Exception {
        // عرض المواقف المتاحة
        handleViewParkingSpots(out, in ,fullName);

        // مطالبة المستخدم بإدخال الأرقام
        System.out.print("Enter the old parking spot number: ");
        int oldSpotNumber = scanner.nextInt();
        scanner.nextLine(); // استهلاك السطر المتبقي

        System.out.print("Enter the new parking spot number: ");
        int newSpotNumber = scanner.nextInt();
        scanner.nextLine(); // استهلاك السطر المتبقي

        // إرسال العملية والبيانات المشفرة
        out.println("Edit_Parking_Spot_Name");
        out.println(AESUtils.encrypt(String.valueOf(oldSpotNumber)));
        out.println(AESUtils.encrypt(String.valueOf(newSpotNumber)));

        // قراءة الرد
        String response = in.readLine();
        String decryptedResponse = AESUtils.decrypt(response);
        System.out.println(decryptedResponse);

        // إرسال الشهادة الرقمية إلى الخادم
        System.out.println("........................Digital Certificate........................");
        String certificatePath = "C:/Users/ahmad/Documents/" + fullName + "_certificate.crt";
        File certificateFile = new File(certificatePath);
        if (certificateFile.exists()) {
            byte[] certificateBytes = Files.readAllBytes(certificateFile.toPath());
            String encodedCertificate = Base64.getEncoder().encodeToString(certificateBytes);
            System.out.println("Certificate content (Base64 encoded):");
            System.out.println(encodedCertificate);
            out.println(encodedCertificate);  // إرسال الشهادة إلى الخادم
        } else {
            System.err.println("Certificate file not found.");
            return;
        }
    }
    private static void handleViewAllReservations(PrintWriter out, BufferedReader in,String fullName) throws Exception {
        // إرسال الطلب للخادم
        String request = SecurityUtils.sanitizeForXSS("view_reserved_parking_spots");
        out.println(request); // تنظيف المدخلات قبل الإرسال
        // إرسال الشهادة الرقمية إلى الخادم
        System.out.println("........................Digital Certificate........................");
        String certificatePath = "C:/Users/ahmad/Documents/" + fullName + "_certificate.crt";
        File certificateFile = new File(certificatePath);
        if (certificateFile.exists()) {
            byte[] certificateBytes = Files.readAllBytes(certificateFile.toPath());
            String encodedCertificate = Base64.getEncoder().encodeToString(certificateBytes);
            System.out.println("Certificate content (Base64 encoded):");
            System.out.println(encodedCertificate);
            out.println(encodedCertificate);  // إرسال الشهادة إلى الخادم
        } else {
            System.err.println("Certificate file not found.");
            return;
        }
        System.out.println("Sent request: " + request);

        // استقبال الرد المشفر من الخادم
        String encryptedResponse = in.readLine();
        if (encryptedResponse != null) {
            String serverResponse = AESUtils.decrypt(encryptedResponse); // فك التشفير
            System.out.println("Server Response: \n" + serverResponse); // طباعة الرد
            // إرسال الشهادة الرقمية إلى الخادم
            System.out.println("........................Digital Certificate........................");
            certificatePath = "C:/Users/ahmad/Documents/" + fullName + "_certificate.crt";
            certificateFile = new File(certificatePath);
            if (certificateFile.exists()) {
                byte[] certificateBytes = Files.readAllBytes(certificateFile.toPath());
                String encodedCertificate = Base64.getEncoder().encodeToString(certificateBytes);
                System.out.println("Certificate content (Base64 encoded):");
                System.out.println(encodedCertificate);
                out.println(encodedCertificate);  // إرسال الشهادة إلى الخادم
            } else {
                System.err.println("Certificate file not found.");
                return;
            }
        } else {
            System.err.println("No response received from server.");
        }
    }
    private static void handleReserveSpot(PrintWriter out, BufferedReader in, Scanner scanner, String fullName) throws IOException {
        // طلب عرض المواقف المتاحة
        out.println("reserve_spot");
        // إرسال الشهادة الرقمية إلى الخادم
        System.out.println("........................Digital Certificate........................");
        String certificatePath = "C:/Users/ahmad/Documents/" + fullName + "_certificate.crt";
        File certificateFile = new File(certificatePath);
        if (certificateFile.exists()) {
            byte[] certificateBytes = Files.readAllBytes(certificateFile.toPath());
            String encodedCertificate = Base64.getEncoder().encodeToString(certificateBytes);
            System.out.println("Certificate content (Base64 encoded):");
            System.out.println(encodedCertificate);
            out.println(encodedCertificate);  // إرسال الشهادة إلى الخادم
        } else {
            System.err.println("Certificate file not found.");
            return;
        }
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
            certificatePath = "C:/Users/ahmad/Documents/" + fullName + "_certificate.crt";
            certificateFile = new File(certificatePath);
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
            String reservationSignature = DigitalSignatureUtils.generateDigitalSignature(dataToSign, privateKey);
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

            // إرسال الشهادة الرقمية
            System.out.println("........................Digital Certificate........................");
             certificatePath = "C:/Users/ahmad/Documents/" + fullName + "_certificate.crt";
             certificateFile = new File(certificatePath);
            if (certificateFile.exists()) {
                byte[] certificateBytes = Files.readAllBytes(certificateFile.toPath());
                String encodedCertificate = Base64.getEncoder().encodeToString(certificateBytes);
                System.out.println("Certificate content (Base64 encoded):");
                System.out.println(encodedCertificate);
                out.println(encodedCertificate);
            } else {
                System.err.println("Certificate file not found.");
            }

            // إرسال توقيع تأكيد الدفع باستخدام RSA
            String paymentConfirmation = "confirm_payment";
            String paymentSignature = DigitalSignatureUtils.generateDigitalSignature(paymentConfirmation, privateKey);
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
        // إرسال الشهادة الرقمية إلى الخادم
        System.out.println("........................Digital Certificate........................");
        String certificatePath = "C:/Users/ahmad/Documents/" + fullName + "_certificate.crt";
        File certificateFile = new File(certificatePath);
        if (certificateFile.exists()) {
            byte[] certificateBytes = Files.readAllBytes(certificateFile.toPath());
            String encodedCertificate = Base64.getEncoder().encodeToString(certificateBytes);
            System.out.println("Certificate content (Base64 encoded):");
            System.out.println(encodedCertificate);
            out.println(encodedCertificate);  // إرسال الشهادة إلى الخادم
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
        // إرسال الشهادة الرقمية إلى الخادم
        System.out.println("........................Digital Certificate........................");
        certificatePath = "C:/Users/ahmad/Documents/" + fullName + "_certificate.crt";
        certificateFile = new File(certificatePath);
        if (certificateFile.exists()) {
            byte[] certificateBytes = Files.readAllBytes(certificateFile.toPath());
            String encodedCertificate = Base64.getEncoder().encodeToString(certificateBytes);
            System.out.println("Certificate content (Base64 encoded):");
            System.out.println(encodedCertificate);
            out.println(encodedCertificate);  // إرسال الشهادة إلى الخادم
        } else {
            System.err.println("Certificate file not found.");
            return;
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