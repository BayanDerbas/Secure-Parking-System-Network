package Utils;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

public class AESUtils {
    private static final String AES = "AES";
    private static final String SECRET_KEY = "1234567890123456"; // المفتاح السري

    public static String encrypt(String data) throws Exception {
        SecretKeySpec key = new SecretKeySpec(SECRET_KEY.getBytes(), AES);
        Cipher cipher = Cipher.getInstance(AES);
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] encryptedData = cipher.doFinal(data.getBytes());
        String encryptedString = Base64.getEncoder().encodeToString(encryptedData);

        // طباعة العملية مع اسم الخوارزمية
        System.out.println("Encrypting Data [Algorithm: " + AES + "]: " + data + " -> " + encryptedString);
        return encryptedString;
    }

    public static String decrypt(String encryptedData) throws Exception {
        SecretKeySpec key = new SecretKeySpec(SECRET_KEY.getBytes(), AES);
        Cipher cipher = Cipher.getInstance(AES);
        cipher.init(Cipher.DECRYPT_MODE, key);
        byte[] decodedData = Base64.getDecoder().decode(encryptedData);
        String decryptedString = new String(cipher.doFinal(decodedData));

        // طباعة العملية مع اسم الخوارزمية
        System.out.println("Decrypting Data [Algorithm: " + AES + "]: " + encryptedData + " -> " + decryptedString);
        return decryptedString;
    }
}

