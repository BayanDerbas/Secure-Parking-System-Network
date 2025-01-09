package Utils;
import java.sql.*;
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
}

