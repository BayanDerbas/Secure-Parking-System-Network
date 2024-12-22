import java.io.*;
import java.net.*;
import java.util.*;

public class ParkingClient {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 3000;
    private static Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        try (Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            boolean loggedIn = false;

            while (true) {
                System.out.println("Welcome to Parking System!");
                System.out.println("1. Create an account");
                System.out.println("2. Login");
                System.out.println("3. Exit");
                System.out.print("Please choose an option (1, 2, or 3): ");
                String choice = scanner.nextLine();

                if ("1".equals(choice)) {
                    createAccount(out, in);
                } else if ("2".equals(choice)) {
                    loggedIn = login(out, in);
                } else if ("3".equals(choice)) {
                    System.out.println("Exiting...");
                    break;
                } else {
                    System.out.println("Invalid option selected.");
                }

                // If logged in successfully, show parking options
                if (loggedIn) {
                    showParkingOptions(out, in);
                }
            }
        } catch (IOException e) {
            System.err.println("Client error: " + e.getMessage());
        }
    }

    private static void createAccount(PrintWriter out, BufferedReader in) throws IOException {
        System.out.println("Please enter your full name:");
        String fullName = scanner.nextLine();

        System.out.println("Please enter your user type (Employee/Visitor):");
        String userType = scanner.nextLine();

        System.out.println("Please enter your phone number:");
        String phoneNumber = scanner.nextLine();

        System.out.println("Please enter your car plate:");
        String carPlate = scanner.nextLine();

        System.out.println("Please enter your password:");
        String password = scanner.nextLine();

        out.println("register");
        out.println(fullName);
        out.println(userType);
        out.println(phoneNumber);
        out.println(carPlate);
        out.println(password);

        String response = in.readLine();
        System.out.println(response);
    }

    private static boolean login(PrintWriter out, BufferedReader in) throws IOException {
        System.out.println("Please enter your full name:");
        String fullName = scanner.nextLine();

        System.out.println("Please enter your password:");
        String password = scanner.nextLine();

        out.println("login");
        out.println(fullName);
        out.println(password);

        String response = in.readLine();
        System.out.println(response);

        return response.equals("Login successful!");
    }

    private static void showParkingOptions(PrintWriter out, BufferedReader in) throws IOException {
        while (true) {
            System.out.println("1. Reserve parking space");
            System.out.println("2. Exit");
            System.out.print("Please choose an option (1 or 2): ");
            String choice = scanner.nextLine();

            if ("1".equals(choice)) {
                System.out.println("Welcome to the parking system!");
                // You can add further functionality for reserving parking spaces here
            } else if ("2".equals(choice)) {
                System.out.println("Exiting...");
                break;
            } else {
                System.out.println("Invalid option selected.");
            }
        }
    }
}








//import java.io.*;
//import java.net.*;
//import java.util.*;
//
//public class ParkingClient {
//    private static final String SERVER_ADDRESS = "localhost";
//    private static final int SERVER_PORT = 3000;
//    private static Scanner scanner = new Scanner(System.in);
//
//    public static void main(String[] args) {
//        try (Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
//             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
//             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
//
//            // عرض قائمة خيارات للمستخدم
//            System.out.println("Welcome to Parking System!");
//            System.out.println("1. Create an account");
//            System.out.println("2. Login");
//            System.out.print("Please choose an option (1 or 2): ");
//            String choice = scanner.nextLine();
//
//            if ("1".equals(choice)) {
//                // إنشاء حساب
//                createAccount(out, in);
//            } else if ("2".equals(choice)) {
//                // تسجيل الدخول
//                login(out, in);
//            } else {
//                System.out.println("Invalid option selected.");
//            }
//
//            // بعد عملية التسجيل أو تسجيل الدخول، يتم عرض خيار Close
//            System.out.println("Would you like to close the connection? (yes/no)");
//            String closeChoice = scanner.nextLine();
//            if ("yes".equalsIgnoreCase(closeChoice)) {
//                System.out.println("Closing the connection...");
//            } else {
//                System.out.println("You can continue using the system.");
//            }
//
//        } catch (IOException e) {
//            System.err.println("Client error: " + e.getMessage());
//        }
//    }
//
//    // دالة لإنشاء حساب جديد
//    private static void createAccount(PrintWriter out, BufferedReader in) throws IOException {
//        System.out.println("Please enter your full name:");
//        String fullName = scanner.nextLine();
//
//        System.out.println("Please enter your user type (Employee/Visitor):");
//        String userType = scanner.nextLine();
//
//        System.out.println("Please enter your phone number:");
//        String phoneNumber = scanner.nextLine();
//
//        System.out.println("Please enter your car plate:");
//        String carPlate = scanner.nextLine();
//
//        System.out.println("Please enter your password:");
//        String password = scanner.nextLine();
//
//        // إرسال البيانات لإنشاء الحساب
//        out.println("register");
//        out.println(fullName);
//        out.println(userType);
//        out.println(phoneNumber);
//        out.println(carPlate);
//        out.println(password);
//
//        // قراءة الاستجابة من السيرفر
//        String response = in.readLine();
//        System.out.println(response);
//    }
//
//    // دالة لتسجيل الدخول
//    private static void login(PrintWriter out, BufferedReader in) throws IOException {
//        System.out.println("Please enter your full name:");
//        String fullName = scanner.nextLine();
//
//        System.out.println("Please enter your password:");
//        String password = scanner.nextLine();
//
//        // إرسال البيانات لتسجيل الدخول
//        out.println("login");
//        out.println(fullName);
//        out.println(password);
//
//        // قراءة الاستجابة من السيرفر
//        String response = in.readLine();
//        System.out.println(response);
//    }
//}
