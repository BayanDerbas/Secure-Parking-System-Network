import java.io.*;
import java.net.*;
import java.sql.*;
import org.mindrot.jbcrypt.BCrypt;

public class ParkingServer {
    private static final int PORT = 3000;
    private static final String DB_URL = "jdbc:sqlite:parking_system.db";

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server is listening on port " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("A client has connected from " + clientSocket.getInetAddress());
                new ClientHandler(clientSocket).start();
            }
        } catch (IOException e) {
            System.err.println("Server exception: " + e.getMessage());
        }
    }

    private static boolean registerUser(String fullName, String userType, String phoneNumber, String carPlate, String passwordHash) {
        String sql = "INSERT INTO users (full_name, user_type, phone_number, car_plate, password_hash) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, fullName);
            pstmt.setString(2, userType);
            pstmt.setString(3, phoneNumber);
            pstmt.setString(4, carPlate);
            pstmt.setString(5, passwordHash);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("Error registering user: " + e.getMessage());
            return false;
        }
    }

    private static boolean loginUser(String fullName, String password) {
        String sql = "SELECT password_hash FROM users WHERE full_name = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, fullName);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return BCrypt.checkpw(password, rs.getString("password_hash"));
            }
        } catch (SQLException e) {
            System.err.println("Error during login: " + e.getMessage());
        }
        return false;
    }

    private static class ClientHandler extends Thread {
        private Socket clientSocket;
        private PrintWriter out;
        private BufferedReader in;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                out = new PrintWriter(clientSocket.getOutputStream(), true);

                String operation = in.readLine();
                System.out.println("Operation received: " + operation);

                if ("register".equals(operation)) {
                    String fullName = in.readLine();
                    String userType = in.readLine();
                    String phoneNumber = in.readLine();
                    String carPlate = in.readLine();
                    String password = in.readLine();

                    String passwordHash = BCrypt.hashpw(password, BCrypt.gensalt());
                    boolean isRegistered = registerUser(fullName, userType, phoneNumber, carPlate, passwordHash);
                    out.println(isRegistered ? "User registered successfully!" : "Registration failed!");

                } else if ("login".equals(operation)) {
                    String fullName = in.readLine();
                    String password = in.readLine();
                    boolean isLoggedIn = loginUser(fullName, password);
                    out.println(isLoggedIn ? "Login successful!" : "Login failed!");
                } else {
                    out.println("Invalid operation.");
                }

            } catch (IOException e) {
                System.err.println("Error in client handler: " + e.getMessage());
            } finally {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    System.err.println("Error closing socket: " + e.getMessage());
                }
            }
        }
    }
}






//import org.mindrot.jbcrypt.BCrypt;
//import java.io.*;
//import java.net.*;
//import java.sql.*;
//
//public class ParkingServer {
//
//    private static final int PORT = 3000;
//    private static final String DB_URL = "jdbc:sqlite:parking_system.db";
//
//    public static void main(String[] args) {
//        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
//            System.out.println("Server is listening on port " + PORT);
//
//            while (true) {
//                new ClientHandler(serverSocket.accept()).start();
//            }
//        } catch (IOException e) {
//            System.err.println("Server exception: " + e.getMessage());
//        }
//    }
//
//    // دالة لإنشاء حساب جديد
//    private static boolean registerUser(String fullName, String userType, String phoneNumber, String carPlate, String passwordHash) {
//        String sql = """
//            INSERT INTO users (full_name, user_type, phone_number, car_plate, password_hash)
//            VALUES (?, ?, ?, ?, ?);
//        """;
//
//        try (Connection conn = DriverManager.getConnection(DB_URL);
//             PreparedStatement pstmt = conn.prepareStatement(sql)) {
//            pstmt.setString(1, fullName);
//            pstmt.setString(2, userType);
//            pstmt.setString(3, phoneNumber);
//            pstmt.setString(4, carPlate);
//            pstmt.setString(5, passwordHash); // تأكد من تشفير كلمة المرور
//            pstmt.executeUpdate();
//            return true;
//        } catch (SQLException e) {
//            System.err.println("Error registering user: " + e.getMessage());
//            return false;
//        }
//    }
//
//    // دالة لتسجيل الدخول
//    private static boolean loginUser(String fullName, String password) {
//        String sql = "SELECT password_hash FROM users WHERE full_name = ?";
//
//        try (Connection conn = DriverManager.getConnection(DB_URL);
//             PreparedStatement pstmt = conn.prepareStatement(sql)) {
//            pstmt.setString(1, fullName);
//            ResultSet rs = pstmt.executeQuery();
//            if (rs.next()) {
//                // مقارنة كلمة المرور المدخلة مع القيمة المخزنة
//                return BCrypt.checkpw(password, rs.getString("password_hash"));
//            }
//        } catch (SQLException e) {
//            System.err.println("Error during login: " + e.getMessage());
//        }
//        return false;
//    }
//
//    // فئة لمعالجة العملاء
//    private static class ClientHandler extends Thread {
//        private Socket clientSocket;
//        private PrintWriter out;
//        private BufferedReader in;
//
//        public ClientHandler(Socket socket) {
//            this.clientSocket = socket;
//        }
//
//        public void run() {
//            try {
//                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
//                out = new PrintWriter(clientSocket.getOutputStream(), true);
//
//                // قراءة العملية
//                String operation = in.readLine();
//
//                if ("register".equals(operation)) {
//                    // بيانات تسجيل الحساب
//                    String fullName = in.readLine();
//                    String userType = in.readLine();
//                    String phoneNumber = in.readLine();
//                    String carPlate = in.readLine();
//                    String password = in.readLine();
//
//                    // تشفير كلمة المرور
//                    String passwordHash = BCrypt.hashpw(password, BCrypt.gensalt());
//
//                    // محاولة تسجيل المستخدم
//                    boolean isRegistered = registerUser(fullName, userType, phoneNumber, carPlate, passwordHash);
//                    if (isRegistered) {
//                        out.println("User registered successfully!");
//                    } else {
//                        out.println("Registration failed!");
//                    }
//                } else if ("login".equals(operation)) {
//                    // بيانات تسجيل الدخول
//                    String fullName = in.readLine();
//                    String password = in.readLine();
//
//                    // محاولة تسجيل الدخول
//                    boolean isLoggedIn = loginUser(fullName, password);
//                    if (isLoggedIn) {
//                        out.println("Login successful!");
//                    } else {
//                        out.println("Login failed!");
//                    }
//                } else {
//                    out.println("Invalid operation!");
//                }
//
//            } catch (IOException e) {
//                System.err.println("Client error: " + e.getMessage());
//            } finally {
//                try {
//                    clientSocket.close();
//                } catch (IOException e) {
//                    System.err.println("Error closing client connection: " + e.getMessage());
//                }
//            }
//        }
//    }
//}
