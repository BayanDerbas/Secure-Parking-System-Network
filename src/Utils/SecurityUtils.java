package Utils;
import org.owasp.encoder.Encode;
import java.sql.*;

public class SecurityUtils {
    // تنفيذ استعلام SELECT آمن باستخدام PreparedStatement
    public static ResultSet executeSecureQuery(Connection connection, String query, Object... params) throws SQLException {
        PreparedStatement preparedStatement = connection.prepareStatement(query);
        for (int i = 0; i < params.length; i++) {
            preparedStatement.setObject(i + 1, params[i]);
        }
        return preparedStatement.executeQuery();
    }
    // تنفيذ استعلامات التعديل (INSERT, UPDATE, DELETE) بشكل آمن
    public static int executeSecureUpdate(Connection connection, String query, Object... params) throws SQLException {
        PreparedStatement preparedStatement = connection.prepareStatement(query);
        for (int i = 0; i < params.length; i++) {
            preparedStatement.setObject(i + 1, params[i]);
        }
        return preparedStatement.executeUpdate();
    }
    // ترميز النصوص لتجنب هجمات XSS عند عرض البيانات
    public static String encodeForHtml(String userInput) {
        return Encode.forHtml(userInput);
    }
    // التحقق من المدخلات (يسمح فقط بالحروف والأرقام وبعض الرموز)
    public static boolean isValidInput(String input) {
        String regex = "^[a-zA-Z0-9._-]+$";
        return input != null && input.matches(regex);
    }
    // تنظيف المدخلات لإزالة الأحرف الضارة
    public static String sanitizeInput(String input) {
        if (input == null) return "";
        return input.replaceAll("[<>\"']", "");
    }
    // مثال لاستخدام هذه التوابع
    public static void main(String[] args) {
        String username = "admin'; DROP TABLE users; --";
        String password = "password123";
        // التحقق من صحة المدخلات
        if (!isValidInput(username) || !isValidInput(password)) {
            System.out.println("مدخلات غير صالحة.");
            return;
        }
        // الاتصال بقاعدة البيانات SQLite
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:database.db")) {
            // تنفيذ استعلام آمن باستخدام PreparedStatement
            String query = "SELECT * FROM users WHERE username = ? AND password = ?";
            ResultSet rs = executeSecureQuery(connection, query, username, password);
            // عرض النتائج بعد الترميز لتجنب XSS
            while (rs.next()) {
                String user = rs.getString("username");
                String encodedUser = encodeForHtml(user);  // ترميز النصوص
                System.out.println("User: " + encodedUser);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}