package Utils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.io.pem.PemReader;
import javax.crypto.Cipher;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
//public class RSAUtils {
//    private static final String ALGORITHM = "RSA";
//    static {
//        Security.addProvider(new BouncyCastleProvider()); // إضافة مزود Bouncy Castle
//    }
//    // تشفير النص باستخدام المفتاح العام
//    public static String encrypt(String plainText, PublicKey publicKey) throws Exception {
//        Cipher cipher = Cipher.getInstance(ALGORITHM, "BC");
//        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
//        byte[] encryptedBytes = cipher.doFinal(plainText.getBytes());
//        String encryptedText = Base64.getEncoder().encodeToString(encryptedBytes);
//        // طباعة النصوص المشفرة مع اسم الخوارزمية
//        System.out.println("........................RSA Encrypting........................");
//        System.out.println("Encrypting using " + ALGORITHM + "...");
//        System.out.println("Plain Text: " + plainText);
//        System.out.println("Encrypted Text: " + encryptedText);
//
//        return encryptedText;
//    }
//    // فك التشفير باستخدام المفتاح الخاص
//    public static String decrypt(String encryptedText, PrivateKey privateKey) throws Exception {
//        Cipher cipher = Cipher.getInstance(ALGORITHM, "BC");
//        cipher.init(Cipher.DECRYPT_MODE, privateKey);
//        byte[] decodedBytes = Base64.getDecoder().decode(encryptedText);
//        String decryptedText = new String(cipher.doFinal(decodedBytes));
//
//        // طباعة النصوص المفككة مع اسم الخوارزمية
//        System.out.println("........................RSA Decrypting........................");
//        System.out.println("Decrypting using " + ALGORITHM + "...");
//        System.out.println("Encrypted Text: " + encryptedText);
//        System.out.println("Decrypted Text: " + decryptedText);
//
//        return decryptedText;
//    }
//    // تحميل المفتاح الخاص من ملف
//    public static PrivateKey loadPrivateKey(String filePath) throws Exception {
//        String key = new String(Files.readAllBytes(Paths.get(filePath)));
//        key = key.replaceAll("-----\\w+ PRIVATE KEY-----", "").replaceAll("\\s", "");
//        byte[] keyBytes = Base64.getDecoder().decode(key);
//        KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM, "BC");
//        return keyFactory.generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
//    }
//    // تحميل المفتاح العام من ملف
//    public static PublicKey loadPublicKey(String filePath) throws Exception {
//        try (PemReader pemReader = new PemReader(new FileReader(filePath))) {
//            byte[] content = pemReader.readPemObject().getContent();
//            KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM, "BC");
//            return keyFactory.generatePublic(new X509EncodedKeySpec(content));
//        }
//    }
//}

public class RSAUtils {
    private static final String ALGORITHM = "RSA";
    static {
        Security.addProvider(new BouncyCastleProvider()); // إضافة مزود Bouncy Castle
    }
    // تشفير النص باستخدام المفتاح العام
    public static String encrypt(String plainText, PublicKey publicKey) throws Exception {
        Cipher cipher = Cipher.getInstance(ALGORITHM, "BC");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] encryptedBytes = cipher.doFinal(plainText.getBytes());
        String encryptedText = Base64.getEncoder().encodeToString(encryptedBytes);
        // طباعة النصوص المشفرة مع اسم الخوارزمية
        System.out.println("........................RSA Encrypting........................");
        System.out.println("Encrypting using " + ALGORITHM + "...");
        System.out.println("Plain Text: " + plainText);
        System.out.println("Encrypted Text: " + encryptedText);

        return encryptedText;
    }
    // فك التشفير باستخدام المفتاح الخاص
    public static String decrypt(String encryptedText, PrivateKey privateKey) throws Exception {
        Cipher cipher = Cipher.getInstance(ALGORITHM, "BC");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] decodedBytes = Base64.getDecoder().decode(encryptedText);
        String decryptedText = new String(cipher.doFinal(decodedBytes));

        // طباعة النصوص المفككة مع اسم الخوارزمية
        System.out.println("........................RSA Decrypting........................");
        System.out.println("Decrypting using " + ALGORITHM + "...");
        System.out.println("Encrypted Text: " + encryptedText);
        System.out.println("Decrypted Text: " + decryptedText);

        return decryptedText;
    }
    // تحميل المفتاح الخاص من ملف
    public static PrivateKey loadPrivateKey(String filePath) throws Exception {
        String key = new String(Files.readAllBytes(Paths.get(filePath)));
        key = key.replaceAll("-----\\w+ PRIVATE KEY-----", "").replaceAll("\\s", "");
        byte[] keyBytes = Base64.getDecoder().decode(key);
        KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM, "BC");
        return keyFactory.generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
    }
    // تحميل المفتاح العام من ملف
    public static PublicKey loadPublicKey(String filePath) throws Exception {
        try (PemReader pemReader = new PemReader(new FileReader(filePath))) {
            byte[] content = pemReader.readPemObject().getContent();
            KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM, "BC");
            return keyFactory.generatePublic(new X509EncodedKeySpec(content));
        }
    }
}