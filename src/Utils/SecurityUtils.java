package Utils;
import java.sql.*;
public class SecurityUtils {
    // منع SQL Injection باستخدام Prepared Statements
    public static PreparedStatement prepareSafeStatement(Connection conn, String query, Object... parameters) throws SQLException {
        System.out.println("Preparing SQL statement: " + query);
        // طباعة القيم التي سيتم استخدامها
        for (int i = 0; i < parameters.length; i++) {
            System.out.println("Parameter " + (i + 1) + ": " + parameters[i]);
        }
        PreparedStatement stmt = conn.prepareStatement(query);
        // تعيين القيم بشكل آمن
        for (int i = 0; i < parameters.length; i++) {
            stmt.setObject(i + 1, parameters[i]);
        }
        System.out.println("Statement prepared successfully!");
        return stmt;
    }
    // تنظيف المدخلات لمنع XSS
    public static String sanitizeForXSS(String input) {
        if (input == null) {
            System.out.println("Input is null, returning empty string.");
            return "";
        }
        System.out.println("Sanitizing input: " + input);
        // استخدام HTML escaping لتنظيف المدخلات
        String sanitizedInput = input.replaceAll("<", "&lt;")
                .replaceAll(">", "&gt;")
                .replaceAll("\"", "&quot;")
                .replaceAll("&", "&amp;");
        System.out.println("Sanitized input: " + sanitizedInput);
        return sanitizedInput;
    }
}


