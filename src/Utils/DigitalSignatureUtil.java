package Utils;

import java.security.*;
import java.util.Base64;

public class DigitalSignatureUtil {
    // توليد المفتاح العام والخاص باستخدام خوارزمية RSA
    public static KeyPair generateKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048); // يمكن تحديد حجم المفتاح
        return keyPairGenerator.generateKeyPair();
    }
    // توليد التوقيع الرقمي باستخدام المفتاح الخاص
    public static String generateDigitalSignature(String data, PrivateKey privateKey) throws Exception {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(privateKey);
        signature.update(data.getBytes());
        byte[] signedData = signature.sign();
        return Base64.getEncoder().encodeToString(signedData);  // إرجاع التوقيع في شكل مشفر
    }
    // التحقق من صحة التوقيع باستخدام المفتاح العام
    public static boolean verifyDigitalSignature(String data, String signatureStr, PublicKey publicKey) throws Exception {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initVerify(publicKey);
        signature.update(data.getBytes());
        byte[] signatureBytes = Base64.getDecoder().decode(signatureStr);  // فك تشفير التوقيع
        return signature.verify(signatureBytes);
    }
    // مثال على استخدام الفئات
    public static void main(String[] args) {
        try {
            // توليد مفتاح عام وخاص
            KeyPair keyPair = generateKeyPair();
            PrivateKey privateKey = keyPair.getPrivate();
            PublicKey publicKey = keyPair.getPublic();

            // البيانات التي سيتم توقيعها
            String data = "This is a test message for digital signature";

            // توليد التوقيع الرقمي
            String signature = generateDigitalSignature(data, privateKey);
            System.out.println("Generated Signature: " + signature);

            // التحقق من صحة التوقيع
            boolean isVerified = verifyDigitalSignature(data, signature, publicKey);
            System.out.println("Signature Verified: " + isVerified);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
