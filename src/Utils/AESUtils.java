package Utils;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

public class AESUtils {
    private static final String AES = "AES";
    private static final String SECRET_KEY = "1234567890123456"; // المفتاح الثابت (16 بايت)
    // تشفير النص باستخدام المفتاح الثابت
    public static String encrypt(String data) throws Exception {
        SecretKeySpec keySpec = new SecretKeySpec(SECRET_KEY.getBytes(), AES);
        Cipher cipher = Cipher.getInstance(AES);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec);
        byte[] encryptedData = cipher.doFinal(data.getBytes());
        String encryptedText = Base64.getEncoder().encodeToString(encryptedData);

        // طباعة النصوص المشفرة مع اسم الخوارزمية
        System.out.println("Encrypting using " + AES + "...");
        System.out.println("Plain Text: " + data);
        System.out.println("Encrypted Text: " + encryptedText);

        return encryptedText;
    }
    // فك التشفير باستخدام المفتاح الثابت
    public static String decrypt(String encryptedData) throws Exception {
        SecretKeySpec keySpec = new SecretKeySpec(SECRET_KEY.getBytes(), AES);
        Cipher cipher = Cipher.getInstance(AES);
        cipher.init(Cipher.DECRYPT_MODE, keySpec);
        byte[] decodedData = Base64.getDecoder().decode(encryptedData);
        String decryptedText = new String(cipher.doFinal(decodedData));

        // طباعة النصوص المفككة مع اسم الخوارزمية
        System.out.println("Decrypting using " + AES + "...");
        System.out.println("Encrypted Text: " + encryptedData);
        System.out.println("Decrypted Text: " + decryptedText);

        return decryptedText;
    }
}
