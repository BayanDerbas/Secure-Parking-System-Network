package Utils;
import java.sql.*;
import java.util.regex.*;
public class SecurityUtils {

    // منع SQL Injection باستخدام Prepared Statements
    public static PreparedStatement prepareSafeStatement(Connection conn, String query, Object... parameters) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement(query);
        // تعيين القيم بشكل آمن
        for (int i = 0; i < parameters.length; i++) {
            stmt.setObject(i + 1, parameters[i]);
        }
        return stmt;
    }

    // تنظيف المدخلات لمنع XSS
    public static String sanitizeForXSS(String input) {
        if (input == null) return "";
        // استخدام HTML escaping لتنظيف المدخلات
        return input.replaceAll("<", "&lt;")
                .replaceAll(">", "&gt;")
                .replaceAll("\"", "&quot;")
                .replaceAll("&", "&amp;");
    }

    // تحقق من أن المدخلات لا تحتوي على أي أكواد HTML ضارة (XSS)
    public static boolean containsXSS(String input) {
        String regex = "<[^>]*script.*?>.*?</[^>]*script.*?>"; // regex للبحث عن وسم script
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(input);
        return matcher.find();
    }

    // استخدام PreparedStatement بشكل آمن لتنفيذ استعلامات SQL
    public static void executeQuery(Connection conn, String query, Object... parameters) {
        try (PreparedStatement stmt = prepareSafeStatement(conn, query, parameters)) {
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("SQL Error: " + e.getMessage());
        }
    }
}
