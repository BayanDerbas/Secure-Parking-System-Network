package Utils;
import java.security.*;
import java.util.Base64;

public class DigitalSignatureUtil {
    // توليد المفتاح العام والخاص باستخدام خوارزمية RSA
    public static KeyPair generateKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048); // يمكن تحديد حجم المفتاح
        System.out.println("Generating RSA key pair...");
        return keyPairGenerator.generateKeyPair();
    }
    // توليد التوقيع الرقمي باستخدام المفتاح الخاص
    public static String generateDigitalSignature(String data, PrivateKey privateKey) throws Exception {
        String algorithm = "SHA256withRSA";
        System.out.println("..............................Generating digital..............................");
        System.out.println("Generating digital signature using algorithm: " + algorithm);
        System.out.println("Data to be signed: " + data);

        Signature signature = Signature.getInstance(algorithm);
        signature.initSign(privateKey);
        signature.update(data.getBytes());
        byte[] signedData = signature.sign();

        String encodedSignature = Base64.getEncoder().encodeToString(signedData);
        System.out.println("Generated digital signature (Base64 encoded): " + encodedSignature);
        return encodedSignature;  // إرجاع التوقيع في شكل مشفر
    }
    // التحقق من صحة التوقيع باستخدام المفتاح العام
    public static boolean verifyDigitalSignature(String data, String signatureStr, PublicKey publicKey) throws Exception {
        String algorithm = "SHA256withRSA";
        System.out.println("Verifying digital signature using algorithm: " + algorithm);
        System.out.println("Data to verify: " + data);

        Signature signature = Signature.getInstance(algorithm);
        signature.initVerify(publicKey);
        signature.update(data.getBytes());

        byte[] signatureBytes = Base64.getDecoder().decode(signatureStr);  // فك تشفير التوقيع
        System.out.println("Signature to verify (Base64 decoded): " + new String(signatureBytes));

        boolean isVerified = signature.verify(signatureBytes);
        System.out.println("Signature verification result: " + isVerified);
        return isVerified;
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

            // التحقق من صحة التوقيع
            boolean isVerified = verifyDigitalSignature(data, signature, publicKey);
            System.out.println("Signature Verified: " + isVerified);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
