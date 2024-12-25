import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

public class AESUtility {

    // توليد مفتاح بطول محدد
    public static String generateKey(int length) {
        SecureRandom random = new SecureRandom();
        byte[] keyBytes = new byte[length];
        random.nextBytes(keyBytes);
        return Base64.getEncoder().encodeToString(keyBytes).substring(0, length);
    }

    // فك التشفير
    public static String decrypt(String encryptedText, String sessionKey) throws Exception {
        SecretKeySpec secretKey = new SecretKeySpec(sessionKey.getBytes(), "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        byte[] decodedBytes = Base64.getDecoder().decode(encryptedText);
        byte[] decryptedData = cipher.doFinal(decodedBytes);
        return new String(decryptedData);
    }

    // التشفير
    public static String encrypt(String plaintext, String sessionKey) throws Exception {
        SecretKeySpec secretKey = new SecretKeySpec(sessionKey.getBytes(), "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        byte[] encryptedData = cipher.doFinal(plaintext.getBytes());
        return Base64.getEncoder().encodeToString(encryptedData);
    }

    public static void main(String[] args) {
        try {
            // مثال لاستخدام الكلاس
            String sessionKey = generateKey(16); // مفتاح بطول 16 حرفًا
            System.out.println("Generated Key: " + sessionKey);

            String originalText = "Hello, AES!";
            System.out.println("Original Text: " + originalText);

            String encryptedText = encrypt(originalText, sessionKey);
            System.out.println("Encrypted Text: " + encryptedText);

            String decryptedText = decrypt(encryptedText, sessionKey);
            System.out.println("Decrypted Text: " + decryptedText);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
