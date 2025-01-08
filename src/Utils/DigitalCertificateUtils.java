package Utils;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.*;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.openssl.PEMWriter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.*;
import java.io.*;
import java.security.*;
import java.security.cert.*;
import java.security.spec.*;
import java.util.Base64;
import java.util.Date;
import java.math.BigInteger;

public class DigitalCertificateUtils {
    // تحويل المفتاح الخاص إلى Base64
    public static String convertPrivateKeyToBase64(PrivateKey privateKey) {
        return Base64.getEncoder().encodeToString(privateKey.getEncoded());
    }
    // تحويل المفتاح العام إلى Base64
    public static String convertPublicKeyToBase64(PublicKey publicKey) {
        return Base64.getEncoder().encodeToString(publicKey.getEncoded());
    }
    // توليد مفتاح خاص وعام
    public static KeyPair generateKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);  // تحديد طول المفتاح 2048 بت
        return keyPairGenerator.generateKeyPair();
    }
    // توليد CSR باستخدام BouncyCastle
    public static String generateCSR(KeyPair keyPair, String distinguishedName) throws Exception {
        X500Name subject = new X500Name(distinguishedName);
        // تحويل المفتاح العام إلى SubjectPublicKeyInfo
        SubjectPublicKeyInfo subjectPublicKeyInfo = SubjectPublicKeyInfo.getInstance(keyPair.getPublic().getEncoded());

        // إنشاء شهادة CSR
        PKCS10CertificationRequestBuilder p10Builder = new PKCS10CertificationRequestBuilder(subject, subjectPublicKeyInfo);
        ContentSigner contentSigner = new JcaContentSignerBuilder("SHA256withRSA").build(keyPair.getPrivate());
        PKCS10CertificationRequest csr = p10Builder.build(contentSigner);

        // تحويل CSR إلى صيغة PEM (نص مشفر)
        StringWriter writer = new StringWriter();
        PEMWriter pemWriter = new PEMWriter(writer);
        pemWriter.writeObject(csr);
        pemWriter.close();

        return writer.toString();
    }
    // تحويل CSR PEM إلى بايتات
    public static byte[] convertPEMToByteArray(String pem) {
        // إزالة الترويسة والذيل
        String cleanedPem = pem.replace("-----BEGIN CERTIFICATE REQUEST-----", "")
                .replace("-----END CERTIFICATE REQUEST-----", "")
                .replaceAll("\\s+", "");

        return java.util.Base64.getDecoder().decode(cleanedPem);
    }
    // توقيع CSR بواسطة CA باستخدام BouncyCastle
    public static X509Certificate signCSR(String csrPem, KeyPair caKeyPair, String caDistinguishedName) throws Exception {
        X500Name issuerName = new X500Name(caDistinguishedName);

        // تحويل CSR من PEM إلى byte array
        byte[] csrBytes = convertPEMToByteArray(csrPem);

        // تحميل CSR من bytes
        PKCS10CertificationRequest csrRequest = new PKCS10CertificationRequest(csrBytes);

        // استخراج المفتاح العام من CSR
        SubjectPublicKeyInfo subjectPublicKeyInfo = csrRequest.getSubjectPublicKeyInfo();
        PublicKey publicKey = KeyFactory.getInstance("RSA")
                .generatePublic(new X509EncodedKeySpec(subjectPublicKeyInfo.getEncoded()));

        // إنشاء شهادة جديدة
        Date notBefore = new Date();
        Date notAfter = new Date(System.currentTimeMillis() + 365L * 24 * 60 * 60 * 1000);  // سنة واحدة من الآن
        X500Name subject = csrRequest.getSubject();

        // إنشاء رقم تسلسلي للشهادة
        BigInteger serialNumber = BigInteger.valueOf(System.currentTimeMillis());

        // إنشاء الشهادة باستخدام X509v3CertificateBuilder
        X509v3CertificateBuilder certBuilder = new X509v3CertificateBuilder(
                issuerName, serialNumber, notBefore, notAfter, subject, subjectPublicKeyInfo);

        ContentSigner contentSigner = new JcaContentSignerBuilder("SHA256withRSA").build(caKeyPair.getPrivate());

        // بناء الشهادة
        X509CertificateHolder certificateHolder = certBuilder.build(contentSigner);

        // تحويل إلى X509Certificate
        return new JcaX509CertificateConverter().getCertificate(certificateHolder);
    }
    public static void saveCertificateToFile(X509Certificate certificate, String filePath) throws Exception {
        // حفظ الشهادة في ملف بصيغة .cer أو .crt
        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            fos.write(certificate.getEncoded());
        }
    }
}